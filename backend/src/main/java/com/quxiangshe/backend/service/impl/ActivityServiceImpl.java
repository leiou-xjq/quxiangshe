package com.quxiangshe.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quxiangshe.backend.entity.UserActivity;
import com.quxiangshe.backend.entity.Follow;
import com.quxiangshe.backend.mapper.UserActivityMapper;
import com.quxiangshe.backend.mapper.FollowMapper;
import com.quxiangshe.backend.service.IActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDate;
import java.util.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户活跃度服务实现类
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements IActivityService {
    
    private final UserActivityMapper userActivityMapper;
    private final StringRedisTemplate redisTemplate;
    private final FollowMapper followMapper;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RedissonClient redissonClient;
    
    private static final String ACTIVITY_KEY_PREFIX = "user:activity:";
    private static final String FANS_ACTIVE_PREFIX = "author:";
    private static final String FANS_ACTIVE_SUFFIX = ":active_fans";
    private static final String FANS_NORMAL_SUFFIX = ":normal_fans";
    private static final String LOGIN_DAYS_FIELD = "loginDays";
    private static final String INTERACTION_COUNT_FIELD = "interactionCount";
    private static final String ACTIVITY_SCORE_FIELD = "activityScore";
    private static final String TODAY_INTERACTION_FIELD = "todayInteraction";
    
    // ========== 用户活跃度 Redis 计数器 ==========
    private static final String USER_INTERACTION_COUNT_KEY = "user:activity:interaction:";
    private static final String USER_TODAY_INTERACTION_KEY = "user:activity:today:";
    private static final String USER_LOGIN_DAYS_KEY = "user:activity:logindays:";
    
    // 活跃度计数递增 Lua 脚本
    private static final String INTERACTION_INCREMENT_SCRIPT = 
            "local current = redis.call('INCR', KEYS[1]) " +
            "return current";
    
    // 活跃度计数递减 Lua 脚本
    private static final String INTERACTION_DECREMENT_SCRIPT = 
            "local current = redis.call('DECR', KEYS[1]) " +
            "if current < 0 then " +
            "  redis.call('SET', KEYS[1], 0) " +
            "  return 0 " +
            "end " +
            "return current";
    
    @Value("${fans.threshold.active:120.0}")
    private double activeThreshold;
    @Value("${fans.threshold.normal:20.0}")
    private double normalThreshold;
    @Value("${fans.threshold.decay:0.8}")
    private double decayRate;
    @Value("${fans.threshold.big-blogger:100000}")
    private int bigBloggerThreshold;
    
    @Value("${fans.batch.redis:5000}")
    private int redisBatchSize;
    @Value("${fans.batch.db:500}")
    private int dbBatchSize;
    
    // 缓存过期时间（0=永不过期，由定时任务控制更新）
    private static final long REDIS_CACHE_EXPIRE_HOURS = 0;
    private static final long FANS_CLASS_EXPIRE_HOURS = 0;
    
    @Override
    public void recordLogin(Long userId) {
        try {
            // 使用 Redis + 分布式锁 + Lua 脚本原子递增登录天数
            incrementLoginDaysWithRedis(userId);
            
            // 异步更新数据库
            asyncSaveLoginDaysToDb(userId);
            
            // 更新 Redis 缓存并触发粉丝分类更新
            UserActivity activity = userActivityMapper.selectByUserId(userId);
            if (activity != null) {
                updateRedisCache(userId, activity);
                triggerFansClassificationUpdate(userId);
            }
            
            log.debug("记录用户登录: userId={}", userId);
            
        } catch (DataAccessException e) {
            log.error("数据库记录用户登录失败: userId={}, 原因: {}", userId, e.getMessage());
        } catch (Exception e) {
            log.error("记录用户登录失败: userId={}, 原因: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 使用 Redis + 分布式锁 + Lua 脚本原子递增登录天数
     */
    private void incrementLoginDaysWithRedis(Long userId) {
        RLock lock = redissonClient != null ? redissonClient.getLock("login:user:" + userId) : null;
        if (lock != null) {
            lock.lock();
        }
        try {
            LocalDate today = LocalDate.now();
            String todayStr = today.toString();
            
            // 获取上次登录日期
            String lastLoginKey = USER_LOGIN_DAYS_KEY + userId + ":last";
            String lastLoginDate = redisTemplate.opsForValue().get(lastLoginKey);
            
            String loginDaysKey = USER_LOGIN_DAYS_KEY + userId + ":count";
            
            if (lastLoginDate == null || !lastLoginDate.equals(todayStr)) {
                // 新的一天，递增登录天数
                redisTemplate.execute(
                    RedisScript.of(INTERACTION_INCREMENT_SCRIPT, Long.class),
                    Collections.singletonList(loginDaysKey)
                );
                // 更新上次登录日期
                redisTemplate.opsForValue().set(lastLoginKey, todayStr);
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }
    
    /**
     * 异步保存登录天数到数据库
     */
    private void asyncSaveLoginDaysToDb(Long userId) {
        try {
            String loginDaysStr = redisTemplate.opsForValue().get(USER_LOGIN_DAYS_KEY + userId + ":count");
            String lastLoginDate = redisTemplate.opsForValue().get(USER_LOGIN_DAYS_KEY + userId + ":last");
            
            if (loginDaysStr != null) {
                UserActivity activity = userActivityMapper.selectByUserId(userId);
                LocalDate today = LocalDate.now();
                
                if (activity == null) {
                    activity = new UserActivity();
                    activity.setUserId(userId);
                    activity.setLoginDays(Integer.parseInt(loginDaysStr));
                    activity.setInteractionCount(0);
                    activity.setTodayInteractionCount(0);
                    activity.setTodayInteractionDate(today);
                    activity.setLastLoginDate(lastLoginDate != null ? LocalDate.parse(lastLoginDate) : null);
                    activity.setActivityScore(5.0);
                    userActivityMapper.insert(activity);
                } else {
                    if (lastLoginDate != null) {
                        activity.setLastLoginDate(LocalDate.parse(lastLoginDate));
                    }
                    activity.setLoginDays(Integer.parseInt(loginDaysStr));
                    activity.calculateScore();
                    userActivityMapper.updateById(activity);
                }
            }
        } catch (Exception e) {
            log.error("异步保存登录天数失败: userId={}", userId, e);
        }
    }
    
    @Override
    public void recordInteraction(Long userId) {
        try {
            // 使用 Redis + 分布式锁 + Lua 脚本原子递增
            incrementInteractionWithRedis(userId);
            
            // 异步更新数据库
            asyncSaveInteractionToDb(userId);
            
            // 更新 Redis 缓存并触发粉丝分类更新
            UserActivity activity = userActivityMapper.selectByUserId(userId);
            if (activity != null) {
                updateRedisCache(userId, activity);
                triggerFansClassificationUpdate(userId);
            }
            
            log.debug("记录用户互动: userId={}", userId);
            
        } catch (DataAccessException e) {
            log.error("数据库记录用户互动失败: userId={}, 原因: {}", userId, e.getMessage());
        } catch (Exception e) {
            log.error("记录用户互动失败: userId={}, 原因: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 使用 Redis + 分布式锁 + Lua 脚本原子递增互动数
     */
    private void incrementInteractionWithRedis(Long userId) {
        RLock lock = redissonClient != null ? redissonClient.getLock("activity:user:" + userId) : null;
        if (lock != null) {
            lock.lock();
        }
        try {
            LocalDate today = LocalDate.now();
            String dateStr = today.toString();
            
            // 递增总互动数
            String interactionKey = USER_INTERACTION_COUNT_KEY + userId;
            redisTemplate.execute(
                RedisScript.of(INTERACTION_INCREMENT_SCRIPT, Long.class),
                Collections.singletonList(interactionKey)
            );
            
            // 递增今日互动数
            String todayKey = USER_TODAY_INTERACTION_KEY + userId + ":" + dateStr;
            String todayValue = redisTemplate.opsForValue().get(todayKey);
            if (todayValue == null) {
                // 新的一天，重置今日计数
                redisTemplate.opsForValue().set(todayKey, "1");
            } else {
                redisTemplate.execute(
                    RedisScript.of(INTERACTION_INCREMENT_SCRIPT, Long.class),
                    Collections.singletonList(todayKey)
                );
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }
    
    /**
     * 异步保存互动数到数据库
     */
    private void asyncSaveInteractionToDb(Long userId) {
        try {
            // 获取 Redis 中的值
            String interactionCountStr = redisTemplate.opsForValue().get(USER_INTERACTION_COUNT_KEY + userId);
            LocalDate today = LocalDate.now();
            String todayKey = USER_TODAY_INTERACTION_KEY + userId + ":" + today.toString();
            String todayCountStr = redisTemplate.opsForValue().get(todayKey);
            
            if (interactionCountStr != null) {
                UserActivity activity = userActivityMapper.selectByUserId(userId);
                if (activity == null) {
                    activity = new UserActivity();
                    activity.setUserId(userId);
                    activity.setLoginDays(0);
                    activity.setInteractionCount(Integer.parseInt(interactionCountStr));
                    activity.setTodayInteractionCount(todayCountStr != null ? Integer.parseInt(todayCountStr) : 0);
                    activity.setTodayInteractionDate(today);
                    activity.setLastLoginDate(null);
                    activity.setActivityScore(5.0);
                    userActivityMapper.insert(activity);
                } else {
                    // 判断是否需要重置今日计数
                    if (activity.getTodayInteractionDate() == null || !activity.getTodayInteractionDate().equals(today)) {
                        activity.setTodayInteractionCount(todayCountStr != null ? Integer.parseInt(todayCountStr) : 1);
                        activity.setTodayInteractionDate(today);
                    }
                    activity.setInteractionCount(Integer.parseInt(interactionCountStr));
                    activity.calculateScore();
                    userActivityMapper.updateById(activity);
                }
            }
} catch (Exception e) {
            log.error("异步保存互动数失败: userId={}", userId, e);
        }
    }
    
    @Override
    public double getActivityScore(Long userId) {
        try {
            String key = ACTIVITY_KEY_PREFIX + userId;
            
            String scoreStr = redisTemplate.opsForValue().get(key + ":" + ACTIVITY_SCORE_FIELD);
            if (scoreStr != null) {
                return Double.parseDouble(scoreStr);
            }
            
            UserActivity activity = userActivityMapper.selectByUserId(userId);
            if (activity != null && activity.getActivityScore() != null) {
                updateRedisCache(userId, activity);
                return activity.getActivityScore();
            }
            
            return 5.0;
            
        } catch (NumberFormatException e) {
            log.warn("解析活跃分数失败，从数据库查询: userId={}", userId);
            UserActivity activity = userActivityMapper.selectByUserId(userId);
            return activity != null && activity.getActivityScore() != null ? activity.getActivityScore() : 5.0;
        } catch (Exception e) {
            log.warn("获取活跃分数失败，从数据库查询: userId={}, 原因: {}", userId, e.getMessage());
            UserActivity activity = userActivityMapper.selectByUserId(userId);
            return activity != null && activity.getActivityScore() != null ? activity.getActivityScore() : 5.0;
        }
    }
    
    @Override
    public Map<Long, Double> getActivityScores(List<Long> userIds) {
        Map<Long, Double> result = new HashMap<>();
        
        try {
            List<UserActivity> activities = userActivityMapper.selectByUserIds(userIds);
            Map<Long, UserActivity> activityMap = new HashMap<>();
            for (UserActivity activity : activities) {
                activityMap.put(activity.getUserId(), activity);
            }
            
            for (Long userId : userIds) {
                UserActivity activity = activityMap.get(userId);
                if (activity != null && activity.getActivityScore() != null) {
                    result.put(userId, activity.getActivityScore());
                } else {
                    result.put(userId, 5.0);
                }
            }
            
        } catch (DataAccessException e) {
            log.error("数据库批量获取活跃分数失败: 原因: {}", e.getMessage());
            for (Long userId : userIds) {
                result.put(userId, 5.0);
            }
        } catch (Exception e) {
            log.error("批量获取活跃分数失败: 原因: {}", e.getMessage(), e);
            for (Long userId : userIds) {
                result.put(userId, 5.0);
            }
        }
        
        return result;
    }
    
    @Override
    public void syncToDatabase() {
        log.info("开始同步Redis活跃度数据到数据库...");
        try {
            log.info("Redis同步完成（当前采用DB实时写入模式）");
        } catch (Exception e) {
            log.error("同步Redis数据到数据库失败: 原因: {}", e.getMessage(), e);
        }
    }
    
    @Override
    @Scheduled(cron = "0 0 4 * * ?")
    public void resetDailyInteractionCount() {
        log.info("开始重置每日互动计数...");
        try {
            int updated = userActivityMapper.resetTodayInteractionCount();
            log.info("重置每日互动计数完成: 更新{}条", updated);
        } catch (DataAccessException e) {
            log.error("数据库重置每日互动计数失败: 原因: {}", e.getMessage());
        } catch (Exception e) {
            log.error("重置每日互动计数失败: 原因: {}", e.getMessage(), e);
        }
    }
    
    @Override
    @Scheduled(cron = "0 0 0 * * ?")
    public void decayActivityAndRecalculate() {
        log.info("开始每日活跃度衰减和粉丝分类重算...");
        long startTime = System.currentTimeMillis();
        try {
            List<UserActivity> allActivities = userActivityMapper.selectList(null);
            if (allActivities.isEmpty()) {
                log.info("无活跃度数据，跳过");
                return;
            }
            
            List<Long> activeUserIds = new ArrayList<>();
            for (UserActivity activity : allActivities) {
                if (activity.getActivityScore() != null && activity.getActivityScore() > 0) {
                    double newScore = activity.getActivityScore() * decayRate;
                    activity.setActivityScore(newScore);
                    activeUserIds.add(activity.getUserId());
                }
            }
            
            // 分批更新数据库，避免SQL过长
            int totalSize = allActivities.size();
            for (int i = 0; i < totalSize; i += dbBatchSize) {
                int end = Math.min(i + dbBatchSize, totalSize);
                List<UserActivity> batch = allActivities.subList(i, end);
                userActivityMapper.batchUpdateScore(batch);
                log.debug("活跃度衰减批次: {}/{}", (i / dbBatchSize + 1), (totalSize + dbBatchSize - 1) / dbBatchSize);
            }
            
            for (UserActivity activity : allActivities) {
                updateRedisCache(activity.getUserId(), activity);
            }
            
log.info("活跃度衰减完成: 更新{}条, 耗时{}ms", allActivities.size(), System.currentTimeMillis() - startTime);
            
            // 对大博主（粉丝数>10万）重新计算粉丝分类
            List<Long> bigBloggerIds = followMapper.selectBloggerIdsByFollowerCount((long) bigBloggerThreshold);
            for (Long authorId : bigBloggerIds) {
                updateFansActivityRank(authorId);
            }
            log.info("大博主粉丝分类更新完成，共{}个", bigBloggerIds.size());
            
        } catch (DataAccessException e) {
            log.error("数据库操作失败，每日活跃度衰减和粉丝分类重算失败: 原因: {}", e.getMessage());
        } catch (Exception e) {
            log.error("每日活跃度衰减和粉丝分类重算失败: 原因: {}", e.getMessage(), e);
        }
    }
    
    @Override
    @Scheduled(cron = "0 30 * * * ?")
    public void hourlySyncFansClassification() {
        log.info("开始每小时粉丝分类增量更新...");
        long startTime = System.currentTimeMillis();
        try {
            LocalDate today = LocalDate.now();
            List<UserActivity> todayActiveUsers = userActivityMapper.selectTodayActiveUsers(today);
            
            if (todayActiveUsers.isEmpty()) {
                log.info("今日无互动用户，跳过");
                return;
            }
            
            Set<Long> authorIds = new HashSet<>();
            for (UserActivity activity : todayActiveUsers) {
                Long userId = activity.getUserId();
                
                updateRedisCache(userId, activity);
                
                LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Follow::getFollowerId, userId);
                List<Follow> follows = followMapper.selectList(wrapper);
                for (Follow follow : follows) {
                    authorIds.add(follow.getFollowingId());
                }
            }
            
            for (Long authorId : authorIds) {
                updateFansActivityRank(authorId);
            }
            
            log.info("每小时粉丝分类增量更新完成: 活跃用户{}个, 涉及博主{}个, 耗时{}ms", 
                todayActiveUsers.size(), authorIds.size(), System.currentTimeMillis() - startTime);
            
        } catch (DataAccessException e) {
            log.error("数据库操作失败，每小时粉丝分类增量更新失败: 原因: {}", e.getMessage());
        } catch (Exception e) {
            log.error("每小时粉丝分类增量更新失败: 原因: {}", e.getMessage(), e);
        }
    }
    
    private void updateRedisCache(Long userId, UserActivity activity) {
        try {
            String key = ACTIVITY_KEY_PREFIX + userId;
            
            redisTemplate.opsForHash().put(key, LOGIN_DAYS_FIELD, String.valueOf(activity.getLoginDays()));
            redisTemplate.opsForHash().put(key, INTERACTION_COUNT_FIELD, String.valueOf(activity.getInteractionCount()));
            redisTemplate.opsForHash().put(key, ACTIVITY_SCORE_FIELD, String.valueOf(activity.getActivityScore()));
            if (activity.getTodayInteractionCount() != null) {
                redisTemplate.opsForHash().put(key, TODAY_INTERACTION_FIELD, String.valueOf(activity.getTodayInteractionCount()));
            }
            
            if (REDIS_CACHE_EXPIRE_HOURS > 0) {
                redisTemplate.expire(key, REDIS_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            }
            
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis连接失败，跳过缓存更新: userId={}", userId);
        } catch (Exception e) {
            log.warn("更新Redis缓存失败: userId={}, 原因: {}", userId, e.getMessage());
        }
    }

    private void triggerFansClassificationUpdate(Long userId) {
        try {
            LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Follow::getFollowerId, userId);
            List<Follow> follows = followMapper.selectList(wrapper);

            if (follows.isEmpty()) {
                return;
            }

            UserActivity activity = userActivityMapper.selectByUserId(userId);
            double score = activity != null && activity.getActivityScore() != null 
                ? activity.getActivityScore() 
                : 5.0;

            for (Follow follow : follows) {
                Long authorId = follow.getFollowingId();
                incrementalUpdateFansClassification(authorId, userId, score);
            }

log.debug("触发粉丝分类更新: userId={}, 涉及{}个作者", userId, follows.size());
            
        } catch (DataAccessException e) {
            log.warn("数据库操作失败，跳过粉丝分类更新: userId={}", userId);
        } catch (Exception e) {
            log.warn("触发粉丝分类更新失败: userId={}, 原因: {}", userId, e.getMessage());
        }
    }
    
    @Override
    @Scheduled(cron = "0 0 * * * ?")
    public void syncActivityToRedis() {
        long startTime = System.currentTimeMillis();
        log.info("开始每小时同步用户活跃度到Redis...");
        
        try {
            // 1. 批量获取所有用户活跃度
            List<UserActivity> allActivities = userActivityMapper.selectList(null);
            log.info("从数据库加载{}条活跃度数据", allActivities.size());
            
            // 2. 批量写入用户活跃度到Redis (Pipeline)
            final int userBatchSize = 5000;
            int userTotalBatches = (allActivities.size() + userBatchSize - 1) / userBatchSize;
            
            for (int batchIdx = 0; batchIdx < userTotalBatches; batchIdx++) {
                int start = batchIdx * userBatchSize;
                int end = Math.min(start + userBatchSize, allActivities.size());
                List<UserActivity> batchActivities = allActivities.subList(start, end);
                
                final long batchStartTime = System.currentTimeMillis();
                redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    for (UserActivity activity : batchActivities) {
                        Double score = activity.getActivityScore();
                        if (score != null && score > 0) {
                            String key = ACTIVITY_KEY_PREFIX + activity.getUserId();
                            connection.stringCommands().set(key.getBytes(), String.valueOf(score).getBytes());
                            connection.keyCommands().expire(key.getBytes(), REDIS_CACHE_EXPIRE_HOURS * 3600);
                        }
                    }
                    return null;
                });
                
                log.debug("同步用户活跃度: batch={}/{}, cost={}ms", 
                    batchIdx + 1, userTotalBatches, System.currentTimeMillis() - batchStartTime);
            }
            
            log.info("用户活跃度同步完成，耗时{}ms", System.currentTimeMillis() - startTime);
            
            // 3. 优化：只处理大博主的粉丝分类（批量查询，避免N+1）
            syncBigBloggerFansClassification(allActivities);
            
            long costTime = System.currentTimeMillis() - startTime;
            log.info("Redis同步完成，耗时{}ms", costTime);
            
        } catch (RedisConnectionFailureException e) {
            log.error("Redis连接失败，跳过同步", e);
        } catch (Exception e) {
            log.error("同步用户活跃度到Redis失败: 原因: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 同步大博主粉丝分类（优化版：批量查询）
     */
    private void syncBigBloggerFansClassification(List<UserActivity> allActivities) {
        long stepStartTime = System.currentTimeMillis();
        
        // 大博主ID列表（动态查询粉丝数>100000的用户）
        List<Long> bigBloggerIds = followMapper.selectBloggerIdsByFollowerCount(100000L);
        
        // 构建用户ID到活跃分数的映射
        Map<Long, Double> userScoreMap = new HashMap<>();
        for (UserActivity activity : allActivities) {
            Double score = activity.getActivityScore();
            if (score != null && score > 0) {
                userScoreMap.put(activity.getUserId(), score);
            }
        }
        
        log.info("开始同步大博主粉丝分类，共{}个", bigBloggerIds.size());
        
        for (Long authorId : bigBloggerIds) {
            // 批量查询该作者的所有粉丝ID
            List<Long> followerIds = followMapper.selectFollowerIdsByAuthorId(authorId);
            if (followerIds == null || followerIds.isEmpty()) {
                continue;
            }
            
            log.info("作者{}有{}个粉丝", authorId, followerIds.size());
            
            // 分类
            List<String> activeFanIds = new ArrayList<>();
            List<String> normalFanIds = new ArrayList<>();
            
            for (Long fanId : followerIds) {
                Double score = userScoreMap.getOrDefault(fanId, 5.0);
                if (score >= activeThreshold) {
                    activeFanIds.add(fanId.toString());
                } else if (score > normalThreshold) {
                    normalFanIds.add(fanId.toString());
                }
            }
            
            // 写入Redis
            String activeKey = FANS_ACTIVE_PREFIX + authorId + FANS_ACTIVE_SUFFIX;
            String normalKey = FANS_ACTIVE_PREFIX + authorId + FANS_NORMAL_SUFFIX;
            
            redisTemplate.delete(activeKey);
            redisTemplate.delete(normalKey);
            
            if (!activeFanIds.isEmpty()) {
                redisTemplate.opsForSet().add(activeKey, activeFanIds.toArray(new String[0]));
            }
            if (!normalFanIds.isEmpty()) {
                redisTemplate.opsForSet().add(normalKey, normalFanIds.toArray(new String[0]));
            }
            
            if (FANS_CLASS_EXPIRE_HOURS > 0) {
                redisTemplate.expire(activeKey, FANS_CLASS_EXPIRE_HOURS, TimeUnit.HOURS);
                redisTemplate.expire(normalKey, FANS_CLASS_EXPIRE_HOURS, TimeUnit.HOURS);
            }
            
            log.info("作者{}粉丝分类完成：活跃粉丝={}, 普通粉丝={}", 
                authorId, activeFanIds.size(), normalFanIds.size());
        }
        
        log.info("大博主粉丝分类同步完成，耗时{}ms", System.currentTimeMillis() - stepStartTime);
    }
    
    @Override
    public void updateFansActivityRank(Long authorId) {
        long startTime = System.currentTimeMillis();
        
        try {
            LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Follow::getFollowingId, authorId);
            List<Follow> followers = followMapper.selectList(wrapper);
            
            if (followers.isEmpty()) {
                return;
            }
            
            List<Long> fanIds = followers.stream()
                .map(Follow::getFollowerId)
                .collect(Collectors.toList());
            
            Map<Long, Double> scores = getActivityScores(fanIds);
            
            String activeKey = FANS_ACTIVE_PREFIX + authorId + FANS_ACTIVE_SUFFIX;
            String normalKey = FANS_ACTIVE_PREFIX + authorId + FANS_NORMAL_SUFFIX;
            
            // 清空旧的 Set
            redisTemplate.delete(activeKey);
            redisTemplate.delete(normalKey);
            
            // 分类写入
            List<String> activeFanIds = new ArrayList<>();
            List<String> normalFanIds = new ArrayList<>();
            
            for (Map.Entry<Long, Double> entry : scores.entrySet()) {
                Double score = entry.getValue();
                if (score != null) {
                    if (score >= activeThreshold) {
                        activeFanIds.add(entry.getKey().toString());
                    } else if (score > normalThreshold) {
                        normalFanIds.add(entry.getKey().toString());
                    }
                    // 僵尸粉丝 (score <= 20) 不存储
                }
            }
            
            // 批量写入活跃粉丝 Set
            if (!activeFanIds.isEmpty()) {
                try {
                    redisTemplate.opsForSet().add(activeKey, activeFanIds.toArray(new String[0]));
                } catch (RedisConnectionFailureException e) {
                    log.warn("Redis连接失败，写入活跃粉丝Set失败: key={}, size={}", 
                        activeKey, activeFanIds.size());
                } catch (Exception e) {
                    log.warn("写入活跃粉丝Set失败: key={}, size={}, error={}", 
                        activeKey, activeFanIds.size(), e.getMessage());
                }
            }
            
            // 批量写入普通粉丝 Set
            if (!normalFanIds.isEmpty()) {
                try {
                    redisTemplate.opsForSet().add(normalKey, normalFanIds.toArray(new String[0]));
                } catch (RedisConnectionFailureException e) {
                    log.warn("Redis连接失败，写入普通粉丝Set失败: key={}, size={}", 
                        normalKey, normalFanIds.size());
                } catch (Exception e) {
                    log.warn("写入普通粉丝Set失败: key={}, size={}, error={}", 
                        normalKey, normalFanIds.size(), e.getMessage());
                }
            }
            
            // 设置过期时间
            if (FANS_CLASS_EXPIRE_HOURS > 0) {
                redisTemplate.expire(activeKey, FANS_CLASS_EXPIRE_HOURS, TimeUnit.HOURS);
                redisTemplate.expire(normalKey, FANS_CLASS_EXPIRE_HOURS, TimeUnit.HOURS);
            }
            
            long costTime = System.currentTimeMillis() - startTime;
            log.info("更新作者{}的粉丝分类完成，活跃粉丝={}, 普通粉丝={}, 耗时{}ms", 
                authorId, activeFanIds.size(), normalFanIds.size(), costTime);
            
        } catch (RedisConnectionFailureException e) {
            log.error("Redis连接失败，跳过粉丝分类更新: authorId={}", authorId);
        } catch (DataAccessException e) {
            log.error("数据库操作失败，更新粉丝分类失败: authorId={}, 原因: {}", authorId, e.getMessage());
        } catch (Exception e) {
            log.error("更新粉丝分类失败: authorId={}, 原因: {}", authorId, e.getMessage(), e);
        }
    }
    
    // 互动类型常量
    public static final int ACTION_LIKE = 1;
    public static final int ACTION_FAVORITE = 2;
    public static final int ACTION_COMMENT = 3;
    public static final int ACTION_FOLLOW = 4;
    
    @Override
    public void incrementActivityScore(Long userId, int actionType) {
        int scoreIncrement;
        switch (actionType) {
            case ACTION_LIKE: scoreIncrement = 3; break;
            case ACTION_FAVORITE: scoreIncrement = 3; break;
            case ACTION_COMMENT: scoreIncrement = 5; break;
            case ACTION_FOLLOW: scoreIncrement = 2; break;
            default: return;
        }

        try {
            LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Follow::getFollowerId, userId);
            List<Follow> follows = followMapper.selectList(wrapper);

            if (follows.isEmpty()) {
                return;
            }

            UserActivity activity = userActivityMapper.selectByUserId(userId);
            double newScore = activity != null && activity.getActivityScore() != null 
                ? activity.getActivityScore() 
                : 5.0;

            for (Follow follow : follows) {
                Long authorId = follow.getFollowingId();
                incrementalUpdateFansClassification(authorId, userId, newScore);
            }

            log.debug("增量更新用户{}的活跃度分数: actionType={}, increment={}",
                userId, actionType, scoreIncrement);

        } catch (DataAccessException e) {
            log.error("数据库操作失败，增量更新用户活跃度失败: userId={}, actionType={}", userId, actionType);
        } catch (Exception e) {
            log.error("增量更新用户活跃度失败: userId={}, actionType={}, 原因: {}", userId, actionType, e.getMessage(), e);
        }
    }

    private void incrementalUpdateFansClassification(Long authorId, Long fanId, double newScore) {
        String activeKey = FANS_ACTIVE_PREFIX + authorId + FANS_ACTIVE_SUFFIX;
        String normalKey = FANS_ACTIVE_PREFIX + authorId + FANS_NORMAL_SUFFIX;

        boolean wasActive = redisTemplate.opsForSet().isMember(activeKey, fanId.toString());
        boolean wasNormal = redisTemplate.opsForSet().isMember(normalKey, fanId.toString());

        boolean nowActive = newScore >= activeThreshold;
        boolean nowNormal = newScore > normalThreshold && newScore < activeThreshold;

        if (wasActive && !nowActive) {
            redisTemplate.opsForSet().remove(activeKey, fanId.toString());
            if (nowNormal) {
                redisTemplate.opsForSet().add(normalKey, fanId.toString());
            }
        } else if (wasNormal && !nowNormal) {
            redisTemplate.opsForSet().remove(normalKey, fanId.toString());
            if (nowActive) {
                redisTemplate.opsForSet().add(activeKey, fanId.toString());
            }
        } else if (!wasActive && !wasNormal && (nowActive || nowNormal)) {
            if (nowActive) {
                redisTemplate.opsForSet().add(activeKey, fanId.toString());
            } else {
                redisTemplate.opsForSet().add(normalKey, fanId.toString());
            }
        }

        if (FANS_CLASS_EXPIRE_HOURS > 0) {
            redisTemplate.expire(activeKey, FANS_CLASS_EXPIRE_HOURS, TimeUnit.HOURS);
            redisTemplate.expire(normalKey, FANS_CLASS_EXPIRE_HOURS, TimeUnit.HOURS);
        }
    }
}
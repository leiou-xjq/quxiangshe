package com.quxiangshe.backend.scheduler;

import com.quxiangshe.backend.mapper.FollowMapper;
import com.quxiangshe.backend.mapper.NoteMapper;
import com.quxiangshe.backend.entity.Note;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quxiangshe.backend.entity.User;
import com.quxiangshe.backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Redis 旧 Key 清理任务
 * 清理旧格式的 Key 和数据，确保使用新的存储格式
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisKeyCleanupScheduler {

    private final StringRedisTemplate redisTemplate;
    private final NoteMapper noteMapper;
    private final FollowMapper followMapper;
    private final UserMapper userMapper;

    private static final String FOLLOWER_COUNT_KEY = "feed:follower:count:";
    private static final int SCAN_BATCH_SIZE = 100;
    
    @org.springframework.beans.factory.annotation.Autowired
    public void setMonitoringAlertScheduler(MonitoringAlertScheduler monitoringAlertScheduler) {
        this.monitoringAlertScheduler = monitoringAlertScheduler;
    }
    private static final int BATCH_SIZE = 50;

    // ========== 需要清理的旧 Key 模式 ==========
    private static final String[] OLD_KEY_PATTERNS = {
        "feed:follower:count:*",       // Java 序列化格式的旧粉丝数缓存
        "feed:following:*",             // 旧的关注列表缓存
        "feed:user:*",                  // 旧的用户 Feed 缓存
        "note:hot:v2",                  // 旧的热点榜
    };

    private MonitoringAlertScheduler monitoringAlertScheduler;

    /**
     * 每天凌晨 3 点执行旧 Key 清理
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldKeys() {
        log.info("开始清理旧 Redis Key...");
        long startTime = System.currentTimeMillis();
        int totalCleaned = 0;

        for (String pattern : OLD_KEY_PATTERNS) {
            int cleaned = cleanupKeysByPattern(pattern);
            totalCleaned += cleaned;
            log.info("清理模式 {}: 删除 {} 个 Key", pattern, cleaned);
        }

        long costTime = System.currentTimeMillis() - startTime;
        log.info("旧 Key 清理完成: 共删除 {} 个 Key, 耗时 {}ms", totalCleaned, costTime);
        
        // 记录执行时间供监控检查
        if (monitoringAlertScheduler != null) {
            monitoringAlertScheduler.recordCleanupExecution();
        }
    }

    /**
     * 根据模式清理 Key
     */
    private int cleanupKeysByPattern(String pattern) {
        int count = 0;
        Set<String> keys = new HashSet<>();
        
        try {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(100)
                    .build();
            
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }

            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                count = keys.size();
            }
        } catch (Exception e) {
            log.error("清理 Key 失败: pattern={}", pattern, e);
        }

        return count;
    }

    /**
     * 手动触发清理 - 清理粉丝数缓存中的 Java 序列化数据
     * API: POST /admin/cleanup/follower-cache
     */
    public int cleanupFollowerCache() {
        log.info("开始清理粉丝数缓存中的旧格式数据...");
        
        Set<String> oldKeys = new HashSet<>();
        
        // 查找 feed:follower:count:* 模式的 key
        ScanOptions options = ScanOptions.scanOptions()
                .match("feed:follower:count:*")
                .count(100)
                .build();
        
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                // 检查是否为 Java 序列化格式（以 \xac\xed 开头）
                String value = redisTemplate.opsForValue().get(key);
                if (value != null && (value.startsWith("\u0000") || value.contains("java.lang.Long"))) {
                    oldKeys.add(key);
                }
            }
        } catch (Exception e) {
            log.error("扫描粉丝数缓存失败", e);
        }

        if (!oldKeys.isEmpty()) {
            redisTemplate.delete(oldKeys);
            log.info("清理粉丝数旧缓存完成: 删除 {} 个 Key", oldKeys.size());
        }

        return oldKeys.size();
    }

    /**
     * 重建粉丝数缓存 - 从数据库同步
     * API: POST /admin/rebuild/follower-cache
     */
    public int rebuildFollowerCache() {
        log.info("开始重建粉丝数缓存...");
        long startTime = System.currentTimeMillis();
        int totalRebuilt = 0;
        
        try {
            // 1. 获取所有用户
            List<User> users = userMapper.selectList(null);
            log.info("共有 {} 个用户需要重建粉丝数缓存", users.size());
            
            // 2. 批量处理每个用户的粉丝数
            List<Long> userIds = users.stream().map(User::getId).toList();
            
            for (int i = 0; i < userIds.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, userIds.size());
                List<Long> batchUserIds = userIds.subList(i, end);
                
                for (Long userId : batchUserIds) {
                    try {
                        // 查询该用户的粉丝数
                        Long followerCount = followMapper.selectCount(
                            new LambdaQueryWrapper<com.quxiangshe.backend.entity.Follow>()
                                .eq(com.quxiangshe.backend.entity.Follow::getFollowingId, userId)
                        );
                        
                        // 写入 Redis（使用 String 格式）
                        redisTemplate.opsForValue().set(
                            FOLLOWER_COUNT_KEY + userId,
                            String.valueOf(followerCount != null ? followerCount : 0)
                        );
                        totalRebuilt++;
                    } catch (Exception e) {
                        log.warn("重建粉丝数缓存失败: userId={}", userId, e);
                    }
                }
            }
            
            long costTime = System.currentTimeMillis() - startTime;
            log.info("粉丝数缓存重建完成: 共重建 {} 个用户, 耗时 {}ms", totalRebuilt, costTime);
            
        } catch (Exception e) {
            log.error("粉丝数缓存重建失败", e);
        }
        
        return totalRebuilt;
    }
}
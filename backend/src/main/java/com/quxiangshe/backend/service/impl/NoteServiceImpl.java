package com.quxiangshe.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quxiangshe.backend.config.RabbitMQConfig;
import com.quxiangshe.backend.dto.CreateNoteRequest;
import com.quxiangshe.backend.dto.NotificationMessage;
import com.quxiangshe.backend.dto.ReviewTaskMessage;
import com.quxiangshe.backend.entity.*;
import com.quxiangshe.backend.exception.BusinessException;
import com.quxiangshe.backend.component.FeedPusher;
import com.quxiangshe.backend.component.SnowflakeIdGenerator;
import com.quxiangshe.backend.mapper.*;
import com.quxiangshe.backend.service.IActivityService;
import com.quxiangshe.backend.service.IFeedService;
import com.quxiangshe.backend.service.INotificationService;
import com.quxiangshe.backend.service.IOssService;
import org.springframework.context.annotation.Lazy;
import com.quxiangshe.backend.service.INoteService;
import static com.quxiangshe.backend.service.impl.ActivityServiceImpl.ACTION_LIKE;
import static com.quxiangshe.backend.service.impl.ActivityServiceImpl.ACTION_FAVORITE;
import com.quxiangshe.backend.service.ISearchService;
import com.quxiangshe.backend.service.sort.FullSortStrategy;
import com.quxiangshe.backend.task.ReviewAsyncTask;
import com.quxiangshe.backend.vo.NoteVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 笔记服务实现类
 * 
 * 核心职责：笔记的发布/查询/删除、点赞/收藏/转发/浏览计数、热门榜单维护、AI内容审核触发
 * 业务模块：笔记模块（核心业务域）
 * 设计思路：
 *   1. Redis作为实时计数器 + 数据库作为真相源，DB-first保证最终一致性
 *   2. 所有计数操作使用 Redis Lua脚本 保证原子性，异步写回数据库
 *   3. 点赞/收藏 使用 Redisson分布式锁 + Double-Check 防并发重复操作
 *   4. 异步审核通过 MQ(消息队列) 解耦，审核通过后自动触发Feed分发
 *   5. 热门榜单使用 Redis ZSet 维护，热度 = like×1 + comment×2 + favorite×3 + forward×5
 *   6. 发现页使用 DB RAND()随机排序，配合已展示ID列表实现游标翻页
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements INoteService {
    
    private final NoteMapper noteMapper;
    private final NoteLikeMapper noteLikeMapper;
    private final NoteFavoriteMapper noteFavoriteMapper;
    private final ForwardMapper forwardMapper;
    private final UserMapper userMapper;
    private final FeedPusher feedPusher;
    private final ObjectMapper objectMapper;
    private final ISearchService searchService;
    @Lazy
    @Autowired
    private IActivityService activityService;
    
    @Lazy
    @Autowired
    private IFeedService feedService;
    @Lazy
    @Autowired
    private IOssService ossService;
    @Lazy
    @Autowired
    private INotificationService notificationService;
    @Lazy
    @Autowired
    private NoteReviewService noteReviewService;
    @Lazy
    @Autowired
    private ReviewAsyncTask reviewAsyncTask;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${review.async-enabled:true}")
    private boolean asyncReviewEnabled;
    
    private final StringRedisTemplate redisTemplate;
    private final FullSortStrategy fullSortStrategy;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    @Autowired(required = false)
    private RedissonClient redissonClient;
    
    // ========== 点赞相关常量 ==========
    private static final String NOTE_LIKE_COUNT_KEY = "note:like:count:";
    private static final String NOTE_LIKED_USERS_KEY = "note:liked:";
    private static final String USER_LIKED_NOTES_KEY = "user:liked:";
    
    // 点赞 Lua 脚本 - 原子递增并添加用户关联
    private static final String LIKE_SCRIPT = 
            "local current = redis.call('INCR', KEYS[1]) " +
            "redis.call('SADD', KEYS[2], ARGV[1]) " +
            "redis.call('SADD', KEYS[3], KEYS[1]) " +
            "return current";
    
    // 取消点赞 Lua 脚本 - 修复DECR竞态：先GET检查再DECR
    private static final String UNLIKE_SCRIPT =
            "local current = redis.call('GET', KEYS[1]) " +
            "if current == false or tonumber(current) <= 0 then " +
            "  return 0 " +
            "end " +
            "current = redis.call('DECR', KEYS[1]) " +
            "redis.call('SREM', KEYS[2], ARGV[1]) " +
            "return current";
    
    // ========== 收藏相关常量 ==========
    private static final String NOTE_FAVORITE_COUNT_KEY = "note:favorite:count:";
    private static final String NOTE_FAVORITE_USERS_KEY = "note:favorited:";
    private static final String USER_FAVORITED_NOTES_KEY = "user:favorited:";
    
    // 收藏 Lua 脚本
    private static final String FAVORITE_SCRIPT = 
            "local current = redis.call('INCR', KEYS[1]) " +
            "redis.call('SADD', KEYS[2], ARGV[1]) " +
            "redis.call('SADD', KEYS[3], KEYS[1]) " +
            "return current";
    
    // 取消收藏 Lua 脚本 - 修复DECR竞态
    private static final String UNFAVORITE_SCRIPT =
            "local current = redis.call('GET', KEYS[1]) " +
            "if current == false or tonumber(current) <= 0 then " +
            "  return 0 " +
            "end " +
            "current = redis.call('DECR', KEYS[1]) " +
            "redis.call('SREM', KEYS[2], ARGV[1]) " +
            "return current";
    
    // ========== 浏览量相关常量 ==========
    private static final String NOTE_VIEW_COUNT_KEY = "note:view:count:";
    
    // 浏览量递增 Lua 脚本
    private static final String VIEW_INCREMENT_SCRIPT = 
            "local current = redis.call('INCR', KEYS[1]) " +
            "return current";
    
    // ========== 转发量相关常量 ==========
    private static final String NOTE_FORWARD_COUNT_KEY = "note:forward:count:";
    
    // 转发量递增 Lua 脚本
    private static final String FORWARD_INCREMENT_SCRIPT = 
            "local current = redis.call('INCR', KEYS[1]) " +
            "return current";
    
    private static final String HOT_RANK_KEY = "note:hot";
    private static final String HOT_BLOCK_KEY = "note:hot:block";
    private static final String HOT_LAST_REFRESH_KEY = "note:hot:last_refresh";
    private static final String HOT_CACHED_KEY = "note:hot:cached";
    private static final long HOT_REFRESH_INTERVAL = 60000; // 1分钟
    private static final int HOT_REFRESH_SIZE = 100; // 每次刷新前100条
    private static final int MAX_HOT_NOTES = 10000; // 热门榜单最多保留条数
    private static final String USER_ACTION_KEY_PREFIX = "user:action:";
    private static final int HOT_SCORE_EXPIRE_DAYS = 7;
    
    private static final String VIEWED_NOTES_KEY_PREFIX = "user:viewed:";
    private static final int VIEWED_NOTES_EXPIRE_HOURS = 24;
    
    private static final String DISCOVER_RANDOM_KEY = "discover:random:";
    
    /**
     * 发布笔记（支持同步审核和异步审核两种模式）
     * 
     * 完整流程：
     *   1. 构建笔记实体，初始化计数为0
     *   2. 根据配置决定审核模式：异步模式状态=0(待审核)，同步模式状态=1(发布)
     *   3. 序列化图片/标签字段，保存到数据库
     *   4. 异步模式：事务提交后通过MQ投递审核任务（避免事务回滚时审核已被触发）
     *   5. 同步模式：事务提交后直接触发Feed分发和粉丝标记更新
     *   6. 同步笔记到ES搜索引擎
     * 
     * @param userId 发布者ID
     * @param request 笔记创建请求（含标题、内容、图片、标签、视频等）
     * @return 笔记VO（含审核状态）
     * @throws BusinessException 同步审核不通过时抛出 400 错误
     */
    @Override
    @Transactional
    public NoteVO createNote(Long userId, CreateNoteRequest request) {
        // 构建笔记实体
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setLocation(request.getLocation());
        
        // 根据审核模式设置初始状态
        log.info("创建笔记检查: asyncReviewEnabled={}, reviewAsyncTask={}, review.enabled={}", 
            asyncReviewEnabled, reviewAsyncTask, reviewAsyncTask != null ? "loaded" : "null");
        if (asyncReviewEnabled && reviewAsyncTask != null) {
            // 异步审核模式：先入库，状态为待审核，由MQ消费者异步审核
            note.setStatus(0);  // 0=待审核
            log.info("发布笔记（异步审核模式）: noteId={}, status=0", note.getId());
        } else {
            // 同步审核模式：入库前先进行AI内容审核
            if (noteReviewService != null) {
                NoteReviewService.ReviewResponse reviewResponse = noteReviewService.reviewNote(
                    null, // noteId暂时为null，审核记录会在之后更新关联
                    userId,
                    request
                );
                
                // 审核未通过则直接拒绝发布，抛出业务异常
                if (!reviewResponse.isPassed()) {
                    throw new BusinessException(400, reviewResponse.getMessage());
                }
            }
            note.setStatus(1); // 1=正常状态，审核通过
        }
        
        note.setLikeCount(0);
        note.setCommentCount(0);
        note.setFavoriteCount(0);
        note.setViewCount(0);
        note.setForwardCount(0);
        note.setStableRandom(generateStableRandom());
        
        // 将图片列表和标签列表序列化为JSON字符串存储
        try {
            if (request.getImages() != null && !request.getImages().isEmpty()) {
                note.setImages(objectMapper.writeValueAsString(request.getImages()));
            }
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                note.setTags(objectMapper.writeValueAsString(request.getTags()));
            }
            note.setVideo(request.getVideo());
            note.setVideoCover(request.getVideoCover());
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "数据序列化失败");
        }
        
        // 插入数据库（获取自增ID）
        noteMapper.insert(note);

        // 异步审核模式：注册事务同步器，在事务成功提交后投递MQ审核任务
        // NOTE: 必须在afterCommit中投递，否则若事务回滚而MQ消息已发出会导致重复审核
        if (asyncReviewEnabled && reviewAsyncTask != null) {
            final Long noteId = note.getId();
            final Long authorId = userId;
            final String title = request.getTitle();
            final String content = request.getContent();
            final List<String> images = request.getImages();

            // 事务提交后异步投递审核任务到MQ
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("投递审核任务到MQ: noteId={}, authorId={}, title={}, images={}", noteId, authorId, title,
                        images != null ? images.size() : 0);
                    ReviewTaskMessage message = ReviewTaskMessage.builder()
                            .noteId(noteId)
                            .userId(authorId)
                            .title(title)
                            .content(content)
                            .imageUrls(images)
                            .submitTime(System.currentTimeMillis())
                            .build();
                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.REVIEW_EXCHANGE,
                            RabbitMQConfig.REVIEW_ROUTING_KEY,
                            message
                    );
                }
            });
        } else {
            log.warn("未触发异步审核: asyncReviewEnabled={}, reviewAsyncTask={}", asyncReviewEnabled, reviewAsyncTask);
            // 同步审核模式：事务提交后直接触发Feed分发 + 设置粉丝关注更新标记
            final Long noteId = note.getId();
            final Long authorId = userId;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 推送笔记到Feed流
                    feedPusher.pushNote(noteId, authorId);
                    if (feedService != null) {
                        // 标记粉丝的关注Tab有更新（红点提示）
                        feedService.setFollowUpdateForFans(authorId, noteId);
                    }
                }
            });
        }
        
        // 同步笔记到Elasticsearch搜索引擎
        searchService.syncNote(note.getId());
        
        // 返回笔记视图对象
        return buildNoteVO(note, userId);
    }
    
    /**
     * 获取笔记分页列表
     * 从数据库分页查询后批量加载用户信息，避免N+1查询
     * 
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @param userId 当前用户ID（用于判断点赞/收藏状态）
     * @return 笔记VO列表
     */
    @Override
    public List<NoteVO> getNoteList(int page, int size, Long userId) {
        int offset = (page - 1) * size;
        List<Note> notes = noteMapper.selectNoteList(size, offset);
        
        if (notes.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 批量收集用户ID，一次查询所有作者信息，避免N+1问题
        List<Long> userIds = notes.stream().map(Note::getUserId).distinct().collect(java.util.stream.Collectors.toList());
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User user : users) {
                userMap.put(user.getId(), user);
            }
        }
        
        List<NoteVO> voList = new ArrayList<>();
        for (Note note : notes) {
            voList.add(buildNoteVO(note, userId, userMap));
        }
        return voList;
    }
    
    /**
     * 获取笔记详情（含浏览数自增）
     * 
     * 1. 查询数据库笔记（过滤已删除和非正常状态的笔记）
     * 2. 使用Redis分布式锁 + Lua脚本原子递增浏览数
     * 3. 异步写回数据库，浏览数优先从Redis读取
     * 
     * @param noteId 笔记ID
     * @param userId 当前用户ID
     * @return 笔记VO（含最新浏览数、点赞/收藏状态）
     * @throws BusinessException 笔记不存在时抛出404
     */
    @Override
    public NoteVO getNoteDetail(Long noteId, Long userId) {
        Note note = noteMapper.selectById(noteId);
        // 笔记不存在或已下架/待审核则不返回
        if (note == null || note.getStatus() != 1) {
            throw new BusinessException(404, "笔记不存在");
        }
        
        // 原子递增浏览数（Redis + 分布式锁 + Lua脚本）
        incrementViewCountWithRedis(noteId);
        
        return buildNoteVO(note, userId);
    }
    
    /**
     * 使用Redis分布式锁 + Lua脚本原子递增浏览数
     * 
     * 设计要点：
     *   - 分布式锁防止并发写覆盖，锁超时30秒避免死锁
     *   - 获取锁失败时静默跳过（浏览数是非核心指标，不影响主流程）
     *   - Lua脚本保证INCR原子性
     *   - 异步写回数据库降低响应延迟
     * 
     * @param noteId 笔记ID
     */
    private void incrementViewCountWithRedis(Long noteId) {
        // 尝试获取分布式锁（带5秒等待超时、30秒持有超时）
        RLock lock = redissonClient != null ? redissonClient.getLock("view:note:" + noteId) : null;
        boolean lockAcquired = false;
        if (lock != null) {
            try {
                lockAcquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (!lockAcquired) {
            // 获取锁失败静默跳过，浏览数不允许阻塞主流程
            log.warn("获取浏览量锁失败（静默跳过）: noteId={}", noteId);
            return;
        }
        try {
            String viewCountKey = NOTE_VIEW_COUNT_KEY + noteId;
            // 执行Lua脚本：原子INCR (INCR命令本身就是原子的，Lua脚本用于扩展)
            redisTemplate.execute(
                RedisScript.of(VIEW_INCREMENT_SCRIPT, Long.class),
                Collections.singletonList(viewCountKey)
            );
            // 异步刷新Redis中的浏览数到数据库
            asyncSaveViewCountToDb(noteId);
        } finally {
            // 确保锁一定被释放（仅当锁由当前线程持有时）
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    /**
     * 异步保存浏览数到数据库
     */
    private void asyncSaveViewCountToDb(Long noteId) {
        try {
            String viewCountStr = redisTemplate.opsForValue().get(NOTE_VIEW_COUNT_KEY + noteId);
            if (viewCountStr != null) {
                Note note = new Note();
                note.setId(noteId);
                note.setViewCount(Integer.parseInt(viewCountStr));
                noteMapper.updateById(note);
            }
        } catch (Exception e) {
            log.error("异步保存浏览数失败: noteId={}", noteId, e);
        }
    }
    
    /**
     * 删除笔记（软删除模式）
     * 
     * 1. 校验笔记存在性
     * 2. 校验操作权限（只能删除自己的笔记）
     * 3. 软删除：设置deletedAt时间戳，标记status=2(下架)，不物理删除
     * 4. 从ES搜索引擎同步删除
     * 
     * @param noteId 笔记ID
     * @param userId 操作者用户ID
     * @return true=删除成功
     * @throws BusinessException 笔记不存在(404)或无权限(403)
     */
    @Override
    @Transactional
    public boolean deleteNote(Long noteId, Long userId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException(404, "笔记不存在");
        }
        // 权限校验：只能删除自己发布的笔记
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限删除此笔记");
        }
        
        // 软删除：仅标记删除时间和下架状态，保留数据库记录
        note.setDeletedAt(java.time.LocalDateTime.now());
        note.setStatus(2); // 2=已下架
        noteMapper.updateById(note);
        
        // 从Elasticsearch搜索引擎中删除索引
        searchService.deleteNote(noteId);
        
        return true;
    }
    
    /**
     * 点赞/取消点赞（Toggle模式）
     * 
     * 如果用户已点赞则取消点赞，未点赞则点赞。使用以下并发安全策略：
     *   1. 快速检查（Redis SMEMBER无锁查询）判断当前状态
     *   2. 分布式锁 + Double-Check模式防止并发重复操作
     *   3. Lua脚本原子执行 INCR/DECR + SADD/SREM
     *   4. 异步写回数据库，不阻塞响应
     *   5. 点赞后增量更新热度 +1，取消点赞 -1
     *   6. 通过MQ异步发送点赞通知
     * 
     * @param noteId 笔记ID
     * @param userId 点赞用户ID
     * @return Map包含liked(是否已点赞)和likeCount(最新点赞数)
     * @throws BusinessException 笔记不存在(404)或系统繁忙(500)
     */
    @Override
    public Map<String, Object> likeNote(Long noteId, Long userId) {
        // 第一层快速检查：通过Redis Set判断是否已点赞（无锁，可能存在误判但不会遗漏）
        Boolean hasLiked = redisTemplate.opsForSet().isMember(NOTE_LIKED_USERS_KEY + noteId, userId.toString());
        if (Boolean.TRUE.equals(hasLiked)) {
            // 已点赞则取消点赞
            unlikeNote(noteId, userId);
            Long count = getRedisLikeCount(noteId);
            return Map.of("liked", false, "likeCount", count != null ? count.intValue() : 0);
        }

        // 获取分布式锁（Redisson，5秒等待超时，30秒持有超时）
        RLock lock = redissonClient != null ? redissonClient.getLock("like:note:" + noteId) : null;
        boolean lockAcquired = false;
        if (lock != null) {
            try {
                lockAcquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
                if (!lockAcquired) {
                    log.warn("获取点赞锁失败: noteId={}, userId={}", noteId, userId);
                    throw new BusinessException(500, "系统繁忙，请稍后重试");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(500, "系统繁忙，请稍后重试");
            }
        } else {
            throw new BusinessException(500, "系统繁忙，请稍后重试");
        }
        try {
            // 第二层Double-Check：获取锁后再次确认是否已点赞，防止并发下重复操作
            hasLiked = redisTemplate.opsForSet().isMember(NOTE_LIKED_USERS_KEY + noteId, userId.toString());
            if (Boolean.TRUE.equals(hasLiked)) {
                Long count = getRedisLikeCount(noteId);
                return Map.of("liked", false, "likeCount", count != null ? count.intValue() : 0);
            }

            // 检查笔记是否存在且状态正常
            Note note = noteMapper.selectById(noteId);
            if (note == null || note.getStatus() != 1) {
                throw new BusinessException(404, "笔记不存在");
            }

            // 执行Lua脚本：原子完成 INCR点赞数 + SADD已点赞用户 + SADD用户已点赞列表
            String likeCountKey = NOTE_LIKE_COUNT_KEY + noteId;
            String likedUsersKey = NOTE_LIKED_USERS_KEY + noteId;
            String userLikedKey = USER_LIKED_NOTES_KEY + userId;

            Long newCount = redisTemplate.execute(
                RedisScript.of(LIKE_SCRIPT, Long.class),
                Arrays.asList(likeCountKey, likedUsersKey, userLikedKey),
                userId.toString()
            );

            // 异步保存点赞关系到数据库（不阻塞响应）
            asyncSaveLikeRelationToDb(noteId, userId);

            // 记录用户互动活跃度，增量更新活动分数
            activityService.recordInteraction(userId);
            activityService.incrementActivityScore(userId, ACTION_LIKE);
            // 点赞热度 +1
            incrementHotScore(noteId, 1);

            // 发送点赞通知到MQ（异步），点赞自己的笔记不通知
            if (rabbitTemplate != null && !note.getUserId().equals(userId)) {
                NotificationMessage msg = NotificationMessage.builder()
                        .type(NotificationMessage.TYPE_LIKE)
                        .userId(note.getUserId())
                        .fromUserId(userId)
                        .noteId(noteId)
                        .timestamp(LocalDateTime.now())
                        .build();
                rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_EXCHANGE,
                        RabbitMQConfig.NOTIFICATION_ROUTING_KEY, msg);
            }

            return Map.of("liked", true, "likeCount", newCount != null ? newCount.intValue() : 1);
        } finally {
            // 确保释放锁
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 取消点赞
     * 
     * 使用与点赞相同的并发安全策略：快速检查 → 分布式锁 → Double-Check → Lua脚本原子递减
     * Lua脚本（UNLIKE_SCRIPT）修复了DECR竞态：先GET检查当前值，仅在>0时执行DECR
     * 
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return Map包含liked(false)和likeCount(最新点赞数)
     * @throws BusinessException 未点赞(400)或系统繁忙(500)
     */
    @Override
    public Map<String, Object> unlikeNote(Long noteId, Long userId) {
        // 快速检查：未点赞直接返回错误
        Boolean hasLiked = redisTemplate.opsForSet().isMember(NOTE_LIKED_USERS_KEY + noteId, userId.toString());
        if (!Boolean.TRUE.equals(hasLiked)) {
            throw new BusinessException(400, "未点赞过该笔记");
        }

        // 获取分布式锁
        RLock lock = redissonClient != null ? redissonClient.getLock("like:note:" + noteId) : null;
        boolean lockAcquired = false;
        if (lock != null) {
            try {
                lockAcquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
                if (!lockAcquired) {
                    log.warn("获取点赞锁失败: noteId={}, userId={}", noteId, userId);
                    throw new BusinessException(500, "系统繁忙，请稍后重试");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(500, "系统繁忙，请稍后重试");
            }
        } else {
            throw new BusinessException(500, "系统繁忙，请稍后重试");
        }
        try {
            // Double-Check：锁内再次确认
            hasLiked = redisTemplate.opsForSet().isMember(NOTE_LIKED_USERS_KEY + noteId, userId.toString());
            if (!Boolean.TRUE.equals(hasLiked)) {
                throw new BusinessException(400, "未点赞过该笔记");
            }

            // 执行Lua脚本：先GET检查 > 0，再DECR + SREM
            String likeCountKey = NOTE_LIKE_COUNT_KEY + noteId;
            String likedUsersKey = NOTE_LIKED_USERS_KEY + noteId;
            String userLikedKey = USER_LIKED_NOTES_KEY + userId;

            Long newCount = redisTemplate.execute(
                RedisScript.of(UNLIKE_SCRIPT, Long.class),
                Arrays.asList(likeCountKey, likedUsersKey, userLikedKey),
                userId.toString()
            );

            // 异步删除数据库中的点赞记录
            asyncDeleteLikeRelationFromDb(noteId, userId);

            // 扣减热度 -1
            incrementHotScore(noteId, -1);

            return Map.of("liked", false, "likeCount", newCount != null ? newCount.intValue() : 0);
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    /**
     * 异步保存点赞关系到数据库
     */
    private void asyncSaveLikeRelationToDb(Long noteId, Long userId) {
        try {
            NoteLike like = new NoteLike();
            like.setNoteId(noteId);
            like.setUserId(userId);
            noteLikeMapper.insert(like);
        } catch (Exception e) {
            log.error("异步保存点赞关系失败: noteId={}, userId={}", noteId, userId, e);
        }
    }
    
    /**
     * 异步删除点赞关系
     */
    private void asyncDeleteLikeRelationFromDb(Long noteId, Long userId) {
        try {
            LambdaQueryWrapper<NoteLike> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NoteLike::getNoteId, noteId).eq(NoteLike::getUserId, userId);
            noteLikeMapper.delete(wrapper);
        } catch (Exception e) {
            log.error("异步删除点赞关系失败: noteId={}, userId={}", noteId, userId, e);
        }
    }
    
    /**
     * 收藏笔记
     * 已收藏则静默返回当前收藏状态。使用与点赞相同的并发安全策略。
     * 收藏热度权重为3（高于点赞的1）。
     * 
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return Map包含favorited(是否已收藏)和favoriteCount(最新收藏数)
     * @throws BusinessException 笔记不存在(404)或系统繁忙(500)
     */
    @Override
    public Map<String, Object> favoriteNote(Long noteId, Long userId) {
        // 快速检查：已收藏则静默返回（不取消收藏）
        Boolean hasFavorited = redisTemplate.opsForSet().isMember(NOTE_FAVORITE_USERS_KEY + noteId, userId.toString());
        if (Boolean.TRUE.equals(hasFavorited)) {
            Long count = getRedisFavoriteCount(noteId);
            return Map.of("favorited", true, "favoriteCount", count != null ? count.intValue() : 0);
        }

        // 获取分布式锁
        RLock lock = redissonClient != null ? redissonClient.getLock("favorite:note:" + noteId) : null;
        boolean lockAcquired = false;
        if (lock != null) {
            try {
                lockAcquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
                if (!lockAcquired) {
                    log.warn("获取收藏锁失败: noteId={}, userId={}", noteId, userId);
                    throw new BusinessException(500, "系统繁忙，请稍后重试");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(500, "系统繁忙，请稍后重试");
            }
        } else {
            throw new BusinessException(500, "系统繁忙，请稍后重试");
        }
        try {
            // Double-Check：锁内再次确认
            hasFavorited = redisTemplate.opsForSet().isMember(NOTE_FAVORITE_USERS_KEY + noteId, userId.toString());
            if (Boolean.TRUE.equals(hasFavorited)) {
                Long count = getRedisFavoriteCount(noteId);
                return Map.of("favorited", true, "favoriteCount", count != null ? count.intValue() : 0);
            }

            // 检查笔记是否存在
            Note note = noteMapper.selectById(noteId);
            if (note == null || note.getStatus() != 1) {
                throw new BusinessException(404, "笔记不存在");
            }

            // 执行Lua脚本：原子 INCR + SADD
            String favoriteCountKey = NOTE_FAVORITE_COUNT_KEY + noteId;
            String favoritedUsersKey = NOTE_FAVORITE_USERS_KEY + noteId;
            String userFavoritedKey = USER_FAVORITED_NOTES_KEY + userId;

            Long newCount = redisTemplate.execute(
                RedisScript.of(FAVORITE_SCRIPT, Long.class),
                Arrays.asList(favoriteCountKey, favoritedUsersKey, userFavoritedKey),
                userId.toString()
            );

            // 异步保存收藏关系到数据库
            asyncSaveFavoriteRelationToDb(noteId, userId);

            // 记录用户活跃度，收藏权重更高
            activityService.recordInteraction(userId);
            activityService.incrementActivityScore(userId, ACTION_FAVORITE);
            // 收藏热度 +3
            incrementHotScore(noteId, 3);

            return Map.of("favorited", true, "favoriteCount", newCount != null ? newCount.intValue() : 1);
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 取消收藏
     * 使用与取消点赞相同的并发安全策略
     * 
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return Map包含favorited(false)和favoriteCount(最新收藏数)
     */
    @Override
    public Map<String, Object> unfavoriteNote(Long noteId, Long userId) {
        // 快速检查
        Boolean hasFavorited = redisTemplate.opsForSet().isMember(NOTE_FAVORITE_USERS_KEY + noteId, userId.toString());
        if (!Boolean.TRUE.equals(hasFavorited)) {
            Long count = getRedisFavoriteCount(noteId);
            return Map.of("favorited", false, "favoriteCount", count != null ? count.intValue() : 0);
        }

        // 获取分布式锁
        RLock lock = redissonClient != null ? redissonClient.getLock("favorite:note:" + noteId) : null;
        boolean lockAcquired = false;
        if (lock != null) {
            try {
                lockAcquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
                if (!lockAcquired) {
                    log.warn("获取收藏锁失败: noteId={}, userId={}", noteId, userId);
                    throw new BusinessException(500, "系统繁忙，请稍后重试");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(500, "系统繁忙，请稍后重试");
            }
        } else {
            throw new BusinessException(500, "系统繁忙，请稍后重试");
        }
        try {
            // Double-Check
            hasFavorited = redisTemplate.opsForSet().isMember(NOTE_FAVORITE_USERS_KEY + noteId, userId.toString());
            if (!Boolean.TRUE.equals(hasFavorited)) {
                Long count = getRedisFavoriteCount(noteId);
                return Map.of("favorited", false, "favoriteCount", count != null ? count.intValue() : 0);
            }

            // 执行Lua脚本：原子递减收藏数
            String favoriteCountKey = NOTE_FAVORITE_COUNT_KEY + noteId;
            String favoritedUsersKey = NOTE_FAVORITE_USERS_KEY + noteId;
            String userFavoritedKey = USER_FAVORITED_NOTES_KEY + userId;

            Long newCount = redisTemplate.execute(
                RedisScript.of(UNFAVORITE_SCRIPT, Long.class),
                Arrays.asList(favoriteCountKey, favoritedUsersKey, userFavoritedKey),
                userId.toString()
            );

            // 异步删除收藏关系
            asyncDeleteFavoriteRelationFromDb(noteId, userId);

            return Map.of("favorited", false, "favoriteCount", newCount != null ? newCount.intValue() : 0);
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    /**
     * 异步保存收藏关系到数据库
     */
    private void asyncSaveFavoriteRelationToDb(Long noteId, Long userId) {
        try {
            NoteFavorite favorite = new NoteFavorite();
            favorite.setNoteId(noteId);
            favorite.setUserId(userId);
            noteFavoriteMapper.insert(favorite);
        } catch (Exception e) {
            log.error("异步保存收藏关系失败: noteId={}, userId={}", noteId, userId, e);
        }
    }
    
    /**
     * 异步删除收藏关系
     */
    private void asyncDeleteFavoriteRelationFromDb(Long noteId, Long userId) {
        try {
            LambdaQueryWrapper<NoteFavorite> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NoteFavorite::getNoteId, noteId).eq(NoteFavorite::getUserId, userId);
            noteFavoriteMapper.delete(wrapper);
        } catch (Exception e) {
            log.error("异步删除收藏关系失败: noteId={}, userId={}", noteId, userId, e);
        }
    }
    
    /**
     * 获取点赞数：优先从Redis缓存获取，缓存未命中则从数据库加载并回写Redis
     * 
     * @param noteId 笔记ID
     * @param defaultCount 数据库中的默认值（缓存缺失时作为兜底）
     * @return 点赞数
     */
    private int getLikeCountFromCache(Long noteId, int defaultCount) {
        try {
            String likeCountStr = redisTemplate.opsForValue().get(NOTE_LIKE_COUNT_KEY + noteId);
            if (likeCountStr != null) {
                return Integer.parseInt(likeCountStr);
            }
            // Redis无缓存，从数据库读取并回灌Redis（缓存预热）
            Note note = noteMapper.selectById(noteId);
            if (note != null && note.getLikeCount() != null) {
                redisTemplate.opsForValue().set(NOTE_LIKE_COUNT_KEY + noteId, String.valueOf(note.getLikeCount()));
                return note.getLikeCount();
            }
        } catch (Exception e) {
            log.debug("获取点赞数缓存失败: noteId={}", noteId, e);
        }
        return defaultCount;
    }
    
    /**
     * 获取收藏数：优先从Redis缓存获取，缓存未命中则从数据库加载并回写Redis
     * 读取失败静默降级到数据库值，保证服务可用
     */
    private int getFavoriteCountFromCache(Long noteId, int defaultCount) {
        try {
            String favoriteCountStr = redisTemplate.opsForValue().get(NOTE_FAVORITE_COUNT_KEY + noteId);
            if (favoriteCountStr != null) {
                return Integer.parseInt(favoriteCountStr);
            }
            Note note = noteMapper.selectById(noteId);
            if (note != null && note.getFavoriteCount() != null) {
                redisTemplate.opsForValue().set(NOTE_FAVORITE_COUNT_KEY + noteId, String.valueOf(note.getFavoriteCount()));
                return note.getFavoriteCount();
            }
        } catch (Exception e) {
            log.debug("获取收藏数缓存失败: noteId={}", noteId, e);
        }
        return defaultCount;
    }
    
    /**
     * 获取浏览数：Redis优先，降级数据库
     */
    private int getViewCountFromCache(Long noteId, int defaultCount) {
        try {
            String viewCountStr = redisTemplate.opsForValue().get(NOTE_VIEW_COUNT_KEY + noteId);
            if (viewCountStr != null) {
                return Integer.parseInt(viewCountStr);
            }
            Note note = noteMapper.selectById(noteId);
            if (note != null && note.getViewCount() != null) {
                redisTemplate.opsForValue().set(NOTE_VIEW_COUNT_KEY + noteId, String.valueOf(note.getViewCount()));
                return note.getViewCount();
            }
        } catch (Exception e) {
            log.debug("获取浏览数缓存失败: noteId={}", noteId, e);
        }
        return defaultCount;
    }
    
    /**
     * 获取转发数：Redis优先，降级数据库
     */
    private int getForwardCountFromCache(Long noteId, int defaultCount) {
        try {
            String forwardCountStr = redisTemplate.opsForValue().get(NOTE_FORWARD_COUNT_KEY + noteId);
            if (forwardCountStr != null) {
                return Integer.parseInt(forwardCountStr);
            }
            Note note = noteMapper.selectById(noteId);
            if (note != null && note.getForwardCount() != null) {
                redisTemplate.opsForValue().set(NOTE_FORWARD_COUNT_KEY + noteId, String.valueOf(note.getForwardCount()));
                return note.getForwardCount();
            }
        } catch (Exception e) {
            log.debug("获取转发数缓存失败: noteId={}", noteId, e);
        }
        return defaultCount;
    }
    
    /**
     * 构建笔记VO（无用户缓存）
     */
    private NoteVO buildNoteVO(Note note, Long userId) {
        return buildNoteVO(note, userId, null);
    }
    
    /**
     * 构建笔记VO（含用户缓存Map，批量查询优化）
     * 
     * 策略：
     *   - 评论数从FullSortStrategy获取（Redis计算值优先）
     *   - 点赞/收藏/浏览/转发数：Redis优先，降级数据库
     *   - 点赞/收藏状态：Redis Set优先，降级数据库查询
     *   - 用户信息：优先使用传入的userMap（批量查询缓存），减少DB查询
     *   - 视频URL：生成OSS签名URL（临时授权访问）
     * 
     * @param note 笔记实体
     * @param userId 当前用户ID（null表示未登录）
     * @param userMap 用户信息缓存Map（批量查询优化用）
     * @return 笔记VO
     */
    private NoteVO buildNoteVO(Note note, Long userId, Map<Long, User> userMap) {
        NoteVO vo = new NoteVO();
        vo.setId(note.getId());
        vo.setUserId(note.getUserId());
        vo.setTitle(note.getTitle());
        vo.setContent(note.getContent());
        vo.setLocation(note.getLocation());
        
        // 各计数优先从Redis缓存获取（实时性好），缺缓存时从数据库回灌
        int likeCount = getLikeCountFromCache(note.getId(), note.getLikeCount());
        vo.setLikeCount(likeCount);
        
        // 评论数：使用全文排序策略中维护的Redis评论计数（更实时）
        long redisCommentCount = fullSortStrategy.getCommentCount(note.getId());
        vo.setCommentCount(redisCommentCount > 0 ? (int) redisCommentCount : note.getCommentCount());
        
        int favoriteCount = getFavoriteCountFromCache(note.getId(), note.getFavoriteCount());
        vo.setFavoriteCount(favoriteCount);
        
        int viewCount = getViewCountFromCache(note.getId(), note.getViewCount());
        vo.setViewCount(viewCount);
        
        int forwardCount = getForwardCountFromCache(note.getId(), note.getForwardCount());
        vo.setForwardCount(forwardCount);
        vo.setStableRandom(note.getStableRandom());
        vo.setCreatedAt(note.getCreatedAt());
        
        // 用户信息：优先使用批量查询缓存的userMap，避免N+1查询
        User user = null;
        if (userMap != null && userMap.containsKey(note.getUserId())) {
            user = userMap.get(note.getUserId());
        } else {
            user = userMapper.selectById(note.getUserId());
        }
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }
        
        // 反序列化图片列表和标签列表（JSON字符串 → List<String>）
        try {
            if (note.getImages() != null) {
                vo.setImages(objectMapper.readValue(note.getImages(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
            }
            if (note.getTags() != null) {
                vo.setTags(objectMapper.readValue(note.getTags(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
            }
        } catch (JsonProcessingException e) {
            // 序列化异常静默忽略，不影响页面展示
        }
        
        // 视频URL生成OSS签名URL（私有bucket需要临时授权）
        String videoUrl = note.getVideo();
        if (videoUrl != null && !videoUrl.isEmpty()) {
            try {
                videoUrl = ossService.getSignedUrl(videoUrl);
            } catch (Exception e) {
                log.warn("生成视频签名URL失败，使用原始URL: {}", e.getMessage());
            }
        }
        vo.setVideo(videoUrl);
        vo.setVideoCover(note.getVideoCover());
        
        // 设置当前用户的点赞和收藏状态
        if (userId != null) {
            try {
                // 优先从Redis Set判断（实时性好）
                Boolean liked = redisTemplate.opsForSet().isMember(NOTE_LIKED_USERS_KEY + note.getId(), userId.toString());
                Boolean favorited = redisTemplate.opsForSet().isMember(NOTE_FAVORITE_USERS_KEY + note.getId(), userId.toString());
                vo.setLiked(Boolean.TRUE.equals(liked));
                vo.setFavorited(Boolean.TRUE.equals(favorited));
            } catch (Exception e) {
                // Redis查询失败，降级到数据库查询
                vo.setLiked(noteLikeMapper.checkUserLiked(note.getId(), userId));
                vo.setFavorited(noteFavoriteMapper.checkUserFavorited(note.getId(), userId));
            }
        } else {
            vo.setLiked(false);
            vo.setFavorited(false);
        }
        
        vo.setStatus(note.getStatus());
        
        return vo;
    }
    
    /**
     * 获取指定用户的笔记列表（个人主页）
     * 
     * 权限控制：
     *   - 本人查看时：返回所有状态的笔记（包括待审核、违规），方便用户管理
     *   - 他人查看时：只返回正常状态(status=1)的笔记
     * 
     * @param userId 目标用户ID（笔记作者）
     * @param page 页码
     * @param size 每页数量
     * @param currentUserId 当前登录用户ID（用于判断是否为本人）
     * @return 笔记VO列表
     */
    @Override
    public List<NoteVO> getMyNotes(Long userId, int page, int size, Long currentUserId) {
        int offset = (page - 1) * size;
        // Mapper层根据currentUserId与userId是否相同决定查询范围
        List<Note> notes = noteMapper.selectUserNotes(userId, currentUserId, size, offset);
        
        List<NoteVO> list = new ArrayList<>();
        for (Note note : notes) {
            list.add(buildNoteVO(note, currentUserId));
        }
        return list;
    }
    
    /**
     * 获取当前用户的收藏列表
     * 先查收藏关系表获取笔记ID列表，再批量查询笔记详情和用户信息
     * 
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @param currentUserId 当前用户ID
     * @return 笔记VO列表（仅正常状态笔记）
     */
    @Override
    public List<NoteVO> getMyFavorites(Long userId, int page, int size, Long currentUserId) {
        int offset = (page - 1) * size;
        List<NoteFavorite> favorites = noteFavoriteMapper.selectByUserId(userId, size, offset);
        
        if (favorites == null || favorites.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 收集收藏的笔记ID
        List<Long> noteIds = favorites.stream()
                .map(NoteFavorite::getNoteId)
                .collect(java.util.stream.Collectors.toList());
        
        // 批量查询笔记详情，只查询正常状态的笔记
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Note::getId, noteIds)
               .eq(Note::getStatus, 1);
        List<Note> notes = noteMapper.selectList(wrapper);
        
        // 按收藏顺序构建VO（保持收藏时间排序）
        List<NoteVO> list = new ArrayList<>();
        for (NoteFavorite fav : favorites) {
            for (Note note : notes) {
                if (fav.getNoteId().equals(note.getId())) {
                    list.add(buildNoteVO(note, currentUserId));
                    break;
                }
            }
        }
        return list;
    }
    
    /**
     * 获取当前用户的获赞总数（个人主页展示用）
     * 
     * @param userId 用户ID
     * @return 该用户所有笔记的点赞数之和
     */
    @Override
    public long getMyLikesCount(Long userId) {
        Long count = noteMapper.selectSumLikeCountByUserId(userId);
        return count != null ? count : 0L;
    }
    
    /**
     * 发现页：动态随机展示笔记（每次刷新都不同）
     * 
     * 分页方案：使用已展示ID列表（cursor）实现游标翻页，配合DB RAND()函数
     * cursor格式：逗号分隔的已展示笔记ID列表，如 "123,456,789"
     * 
     * @param cursor 游标（已展示的笔记ID列表）
     * @param size 每页数量
     * @param userId 当前用户ID
     * @return 笔记VO列表
     */
    @Override
    public List<NoteVO> getDiscoverNotes(String cursor, int size, Long userId) {
        List<Note> notes;
        
        // 解析cursor：提取已展示的笔记ID列表，用于翻页时排除
        List<Long> excludeIds = new ArrayList<>();
        if (cursor != null && !cursor.isEmpty()) {
            try {
                String[] idArr = cursor.split(",");
                for (String idStr : idArr) {
                    excludeIds.add(Long.parseLong(idStr.trim()));
                }
            } catch (NumberFormatException e) {
                // cursor格式错误，清空重置，从头开始
                excludeIds.clear();
            }
        }
        
        if (excludeIds.isEmpty()) {
            // 首次加载：使用RAND()随机获取
            notes = noteMapper.selectDiscoverNotes(size);
        } else {
            // 翻页：排除已展示的笔记后随机获取
            notes = noteMapper.selectDiscoverNotesWithExclude(excludeIds, size);
        }
        
        List<NoteVO> voList = new ArrayList<>();
        for (Note note : notes) {
            voList.add(buildNoteVO(note, userId));
        }
        
        return voList;
    }
    
    /**
     * 过滤用户已浏览的笔记 - 使用Redis Set记录，避免重复展示
     * 
     * @param userId 用户ID
     * @param notes 待过滤的笔记列表
     * @return 过滤后的笔记列表
     */
    private List<Note> filterViewedNotes(Long userId, List<Note> notes) {
        String key = VIEWED_NOTES_KEY_PREFIX + userId;
        
        Set<String> viewedIds = redisTemplate.opsForSet().members(key);
        
        if (viewedIds == null || viewedIds.isEmpty()) {
            return notes;
        }
        
        // 排除已在浏览记录中的笔记
        return notes.stream()
            .filter(note -> !viewedIds.contains(note.getId().toString()))
            .collect(Collectors.toList());
    }
    
    /**
     * 记录用户浏览笔记 - 使用Redis Set，最多保留最近1000条
     * 
     * @param userId 用户ID
     * @param notes 浏览的笔记列表
     */
    private void recordViewedNotes(Long userId, List<Note> notes) {
        String key = VIEWED_NOTES_KEY_PREFIX + userId;
        
        // 使用Redis Set记录，同一笔记多次浏览只记录一次
        for (Note note : notes) {
            redisTemplate.opsForSet().add(key, note.getId().toString());
        }
        
        // 设置24小时过期
        redisTemplate.expire(key, VIEWED_NOTES_EXPIRE_HOURS, TimeUnit.HOURS);
        
        // 限制Set大小为最近1000条（LRU近似）
        Long size = redisTemplate.opsForSet().size(key);
        if (size != null && size > 1000) {
            Set<String> allMembers = redisTemplate.opsForSet().members(key);
            int toRemove = (int) (size - 1000);
            if (toRemove > 0 && allMembers != null) {
                // 转为列表后删除前面的元素（近似LRU，前N个较旧）
                List<String> membersList = new ArrayList<>(allMembers);
                // NOTE: Set本身无序，此处LRU为近似策略，精确LRU需改用ZSet
                List<String> toRemoveList = membersList.subList(0, toRemove);
                redisTemplate.opsForSet().remove(key, toRemoveList.toArray());
            }
        }
    }
    
    /**
     * 生成稳定的伪随机数（float0-1, 16位小数精度）
     * 
     * 用途：发现页随机展示的排序参考值，同一笔记多次请求结果不变
     * 实现：以雪花算法ID为种子，对10^15取模后归一化到[0,1]
     * 
     * @return BigDecimal [0, 1) 范围的稳定随机数
     */
    private BigDecimal generateStableRandom() {
        long seed = snowflakeIdGenerator.nextId();
        // 雪花ID对10^15取模作为随机种子
        return BigDecimal.valueOf(seed % 1000000000000000L)
            .divide(BigDecimal.valueOf(1000000000000000L), 16, RoundingMode.DOWN);
    }
    
    /**
     * 热门页：按热度排序展示笔记（Redis ZSet）  
     * 
     * 分页方式：使用数字offset游标（ZREVRANGE按索引分页）
     * 同时过滤已被拉黑的笔记（HOT_BLOCK_KEY Set）
     * 
     * @param cursor 游标（数字offset）
     * @param size 每页数量
     * @param userId 当前用户ID
     * @return 笔记VO列表（按热度降序）
     */
    @Override
    public List<NoteVO> getPopularNotes(String cursor, int size, Long userId) {
        long startIndex = 0;
        long endIndex = size - 1;
        
        // 解析游标：数字格式的offset
        if (cursor != null && !cursor.isEmpty()) {
            try {
                startIndex = Long.parseLong(cursor);
                endIndex = startIndex + size - 1;
            } catch (NumberFormatException e) {
                startIndex = 0;
                endIndex = size - 1;
            }
        }
        
        // 从Redis ZSet按热度降序获取笔记ID
        Set<String> noteIds = redisTemplate.opsForZSet()
            .reverseRange(HOT_RANK_KEY, startIndex, endIndex);
        
        if (noteIds == null || noteIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 过滤黑名单笔记（违规下架的笔记被拉入黑名单）
        Set<String> blockedIds = redisTemplate.opsForSet().members(HOT_BLOCK_KEY);
        if (blockedIds != null && !blockedIds.isEmpty()) {
            noteIds = noteIds.stream()
                .filter(id -> !blockedIds.contains(id))
                .collect(Collectors.toSet());
        }
        
        if (noteIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 批量查询笔记详情
        List<Long> ids = noteIds.stream()
            .map(Long::parseLong)
            .collect(Collectors.toList());
        List<Note> notes = noteMapper.selectByIds(ids);
        
        // 按热度排序（ZSet已有序，这里保持原顺序）
        Map<Long, Note> noteMap = notes.stream()
            .collect(Collectors.toMap(Note::getId, n -> n));
        
        List<NoteVO> voList = new ArrayList<>();
        for (String noteId : noteIds) {
            Note note = noteMap.get(Long.parseLong(noteId));
            if (note != null) {
                voList.add(buildNoteVO(note, userId));
            }
        }
        
        return voList;
    }
    
    /**
     * 定时刷新热门笔记热度（全局限流1分钟）
     * 
     * 刷新策略：
     *   - 1分钟内只刷新一次（HOT_LAST_REFRESH_KEY控制频率）
     *   - 仅刷新热门榜单前100条笔记的热度
     *   - 热度公式：like×1 + comment×2 + favorite×3 + forward×5
     *   - 只刷新正常状态的笔记
     * 
     * 调用时机：用户访问热门Tab时由Controller触发
     */
    public void refreshHotScoreIfNeeded() {
        try {
            long now = System.currentTimeMillis();
            String lastRefreshStr = redisTemplate.opsForValue().get(HOT_LAST_REFRESH_KEY);
            
            // 限流检查：1分钟内如果已刷新则跳过
            if (lastRefreshStr != null) {
                long lastRefresh = Long.parseLong(lastRefreshStr);
                if (now - lastRefresh < HOT_REFRESH_INTERVAL) {
                    log.debug("热度刷新在限流期内，跳过");
                    return;
                }
            }
            
            // 获取热门榜单前100条笔记ID
            Set<String> noteIds = redisTemplate.opsForZSet().reverseRange(HOT_RANK_KEY, 0, HOT_REFRESH_SIZE - 1);
            if (noteIds == null || noteIds.isEmpty()) {
                return;
            }
            
            // 根据数据库最新计数重新计算热度并更新Redis ZSet
            List<Long> ids = noteIds.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            List<Note> notes = noteMapper.selectByIds(ids);
            
            for (Note note : notes) {
                if (note.getStatus() == 1) { // 仅刷新正常状态的笔记
                    double hotScore = calculateHotScore(note);
                    redisTemplate.opsForZSet().add(HOT_RANK_KEY, note.getId().toString(), hotScore);
                }
            }
            
            // 更新最后刷新时间戳（作为限流标记）
            redisTemplate.opsForValue().set(HOT_LAST_REFRESH_KEY, String.valueOf(now));
            
            log.info("刷新热门笔记热度完成，刷新数量: {}", ids.size());
        } catch (Exception e) {
            log.error("刷新热门笔记热度失败: {}", e.getMessage());
        }
    }
    
    /**
     * 计算笔记热度分数
     * 
     * 公式：like×1.0 + comment×2.0 + favorite×3.0 + forward×5.0
     * 权重设计理由：转发>收藏>评论>点赞，体现用户互动的深度差异
     * 
     * @param note 笔记实体
     * @return 热度分数
     */
    private double calculateHotScore(Note note) {
        int likeCount = note.getLikeCount() != null ? note.getLikeCount() : 0;
        int commentCount = note.getCommentCount() != null ? note.getCommentCount() : 0;
        int favoriteCount = note.getFavoriteCount() != null ? note.getFavoriteCount() : 0;
        int forwardCount = note.getForwardCount() != null ? note.getForwardCount() : 0;
        
        // 转发权重最高(5.0)，因为转发意味着更强的内容认同
        return likeCount * 1.0 + commentCount * 2.0 + favoriteCount * 3.0 + forwardCount * 5.0;
    }
    
    /**
     * 根据ID查询笔记
     * 
     * @param noteId 笔记ID
     * @return 笔记实体（null表示不存在）
     */
    @Override
    public Note getNoteById(Long noteId) {
        return noteMapper.selectById(noteId);
    }
    
    /**
     * 转发笔记
     * 
     * 1. 校验笔记存在且状态正常
     * 2. 创建转发记录（包含转发者ID、原始笔记ID、转发附言）
     * 3. 原子递增转发数（Redis + 分布式锁 + Lua脚本）
     * 4. 增量更新热度 +5（转发权重最高）
     * 5. 记录用户活跃度
     * 
     * @param noteId 笔记ID
     * @param userId 转发用户ID
     * @param content 转发附言
     * @return true=转发成功
     * @throws BusinessException 笔记不存在(404)
     */
    @Override
    public boolean forwardNote(Long noteId, Long userId, String content) {
        Note note = noteMapper.selectById(noteId);
        if (note == null || note.getStatus() != 1) {
            throw new BusinessException(404, "笔记不存在");
        }
        
        // 记录转发（一次转发一条记录）
        Forward forward = new Forward();
        forward.setOriginalNoteId(noteId);
        forward.setUserId(userId);
        forward.setContent(content);
        forwardMapper.insert(forward);
        
        // Redis + 分布式锁原子递增转发数
        incrementForwardCountWithRedis(noteId);
        
        // 转发热度 +5（权重最高）
        incrementHotScore(noteId, 5);
        
        // 记录用户活跃度
        activityService.recordInteraction(userId);
        activityService.incrementActivityScore(userId, ActivityServiceImpl.ACTION_FOLLOW);
        
        return true;
    }
    
    /**
     * Redis分布式锁 + Lua脚本原子递增转发数
     * 与浏览数递增逻辑相同，锁失败时静默跳过（转发数非核心数据）
     * 
     * @param noteId 笔记ID
     */
    private void incrementForwardCountWithRedis(Long noteId) {
        RLock lock = redissonClient != null ? redissonClient.getLock("forward:note:" + noteId) : null;
        boolean lockAcquired = false;
        if (lock != null) {
            try {
                lockAcquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (!lockAcquired) {
            // 锁获取失败静默跳过，转发数允许最终一致性
            log.warn("获取转发量锁失败（静默跳过）: noteId={}", noteId);
            return;
        }
        try {
            String forwardCountKey = NOTE_FORWARD_COUNT_KEY + noteId;
            // Lua脚本原子执行INCR（简洁可靠）
            redisTemplate.execute(
                RedisScript.of(FORWARD_INCREMENT_SCRIPT, Long.class),
                Collections.singletonList(forwardCountKey)
            );
            // 异步刷新Redis计数到数据库
            asyncSaveForwardCountToDb(noteId);
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    /**
     * 异步保存转发数到数据库
     * 从Redis读取当前转发数，写回数据库（最终一致性）
     * 
     * @param noteId 笔记ID
     */
    private void asyncSaveForwardCountToDb(Long noteId) {
        try {
            String forwardCountStr = redisTemplate.opsForValue().get(NOTE_FORWARD_COUNT_KEY + noteId);
            if (forwardCountStr != null) {
                Note note = new Note();
                note.setId(noteId);
                note.setForwardCount(Integer.parseInt(forwardCountStr));
                noteMapper.updateById(note);
            }
        } catch (Exception e) {
            log.error("异步保存转发数失败: noteId={}", noteId, e);
        }
    }
    
    /**
     * 增量更新笔记热度（同步双写：DB + Redis）
     * 
     * 更新策略：
     *   1. 分布式锁保证并发安全
     *   2. 先更新数据库（主数据源，hotScore字段）
     *   3. 再同步更新Redis ZSet（实时展示用）
     *   4. 控制ZSet容量上限（MAX_HOT_NOTES条）
     *   5. 7天过期（超时自动清理冷数据）
     *   6. 黑名单笔记跳过（违规笔记不参与热度排名）
     *   7. 非正常状态笔记从榜单移除
     * 
     * @param noteId 笔记ID
     * @param increment 热度增量（正数增加，负数减少）
     */
    @Override
    public void incrementHotScore(Long noteId, int increment) {
        if (noteId == null) return;

        try {
            // 1. 获取分布式锁（后台任务，失败静默跳过不阻塞主流程）
            RLock lock = redissonClient != null ?
                redissonClient.getLock("hotScore:" + noteId) : null;
            boolean lockAcquired = false;
            if (lock != null) {
                try {
                    lockAcquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (!lockAcquired) {
                log.warn("获取热度锁失败（静默跳过）: noteId={}", noteId);
                return;
            }

            try {
                // 2. 检查黑名单：已拉黑的笔记不更新热度
                Boolean isBlocked = redisTemplate.opsForSet().isMember(HOT_BLOCK_KEY, noteId.toString());
                if (Boolean.TRUE.equals(isBlocked)) {
                    return;
                }

                // 3. 从数据库查询笔记（获取最新状态和当前热度）
                Note note = noteMapper.selectById(noteId);
                if (note == null || note.getStatus() != 1) {
                    // 非正常状态的笔记从ZSet中移除
                    redisTemplate.opsForZSet().remove(HOT_RANK_KEY, noteId.toString());
                    return;
                }

                // 4. 计算新热度值（基于数据库当前值 + 增量），不允许负数
                double currentScore = note.getHotScore() != null ? note.getHotScore() : 0;
                double newScore = currentScore + increment;
                if (newScore < 0) newScore = 0;

                // 5. 同步写入数据库（主数据源，保证最终一致性）
                note.setHotScore(newScore);
                noteMapper.updateById(note);

                // 6. 同步更新Redis ZSet（用于实时查询热门榜单）
                redisTemplate.opsForZSet().add(HOT_RANK_KEY, noteId.toString(), newScore);

                // 7. 维护热门榜单大小：超出上限则删除最低分记录
                Long size = redisTemplate.opsForZSet().size(HOT_RANK_KEY);
                if (size != null && size > MAX_HOT_NOTES) {
                    redisTemplate.opsForZSet().removeRange(HOT_RANK_KEY, 0, size - MAX_HOT_NOTES - 1);
                }

                // 8. 设置过期时间（7天），避免历史数据永久占用内存
                redisTemplate.expire(HOT_RANK_KEY, HOT_SCORE_EXPIRE_DAYS, java.util.concurrent.TimeUnit.DAYS);

            } finally {
                if (lock != null && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (RedisConnectionFailureException e) {
            // Redis不可用时静默跳过，不失业务功能
            log.warn("Redis连接失败，跳过热度更新: noteId={}", noteId);
        } catch (Exception e) {
            log.warn("更新热度失败: noteId={}, increment={}, 原因: {}", noteId, increment, e.getMessage());
        }
    }

    /**
     * 获取Redis中的点赞数（用于返回权威计数）
     */
    private Long getRedisLikeCount(Long noteId) {
        try {
            String countStr = redisTemplate.opsForValue().get(NOTE_LIKE_COUNT_KEY + noteId);
            return countStr != null ? Long.parseLong(countStr) : 0L;
        } catch (Exception e) {
            log.warn("获取Redis点赞数失败: noteId={}", noteId);
            return 0L;
        }
    }

    /**
     * 获取Redis中的收藏数（用于返回权威计数）
     */
    private Long getRedisFavoriteCount(Long noteId) {
        try {
            String countStr = redisTemplate.opsForValue().get(NOTE_FAVORITE_COUNT_KEY + noteId);
            return countStr != null ? Long.parseLong(countStr) : 0L;
        } catch (Exception e) {
            log.warn("获取Redis收藏数失败: noteId={}", noteId);
            return 0L;
        }
    }
}

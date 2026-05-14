package com.quxiangshe.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quxiangshe.backend.config.RabbitMQConfig;
import com.quxiangshe.backend.dto.CreateNoteRequest;
import com.quxiangshe.backend.dto.NotificationMessage;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 笔记服务实现类
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
    
    // 取消点赞 Lua 脚本 - 原子递减并移除用户关联
    private static final String UNLIKE_SCRIPT = 
            "local current = redis.call('DECR', KEYS[1]) " +
            "if current < 0 then " +
            "  redis.call('SET', KEYS[1], 0) " +
            "  return 0 " +
            "end " +
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
    
    // 取消收藏 Lua 脚本
    private static final String UNFAVORITE_SCRIPT = 
            "local current = redis.call('DECR', KEYS[1]) " +
            "if current < 0 then " +
            "  redis.call('SET', KEYS[1], 0) " +
            "  return 0 " +
            "end " +
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
     * 发布笔记
     * 支持同步审核和异步审核两种模式
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
        
        // 根据审核模式设置状态
        log.info("创建笔记检查: asyncReviewEnabled={}, reviewAsyncTask={}, review.enabled={}", 
            asyncReviewEnabled, reviewAsyncTask, reviewAsyncTask != null ? "loaded" : "null");
        if (asyncReviewEnabled && reviewAsyncTask != null) {
            // 异步审核模式：状态为"审核中"
            note.setStatus(0);  // 待审核
            log.info("发布笔记（异步审核模式）: noteId={}, status=0", note.getId());
        } else {
            // 同步审核模式：先进行AI内容审核
            if (noteReviewService != null) {
                NoteReviewService.ReviewResponse reviewResponse = noteReviewService.reviewNote(
                    null, // noteId暂时为null，审核记录会后更新
                    userId,
                    request
                );
                
                // 如果审核未通过（违规），禁止发布
                if (!reviewResponse.isPassed()) {
                    throw new BusinessException(400, reviewResponse.getMessage());
                }
            }
            note.setStatus(1); // 正常状态
        }
        
        note.setLikeCount(0);
        note.setCommentCount(0);
        note.setFavoriteCount(0);
        note.setViewCount(0);
        note.setForwardCount(0);
        note.setStableRandom(generateStableRandom());
        
        // 序列化图片和标签，设置视频
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
        
        // 插入数据库
        noteMapper.insert(note);

        // 异步审核模式：触发异步审核任务
        if (asyncReviewEnabled && reviewAsyncTask != null) {
            final Long noteId = note.getId();
            final Long authorId = userId;
            final String title = request.getTitle();
            final String content = request.getContent();
            final List<String> images = request.getImages();

            // 事务提交后异步执行审核
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("触发异步审核: noteId={}, authorId={}, title={}, images={}", noteId, authorId, title,
                        images != null ? images.size() : 0);
                    reviewAsyncTask.asyncReview(noteId, authorId, title, content, images);
                }
            });
        } else {
            log.warn("未触发异步审核: asyncReviewEnabled={}, reviewAsyncTask={}", asyncReviewEnabled, reviewAsyncTask);
            // 同步审核模式：正常触发Feed推送
            final Long noteId = note.getId();
            final Long authorId = userId;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    feedPusher.pushNote(noteId, authorId);
                    if (feedService != null) {
                        feedService.setFollowUpdateForFans(authorId, noteId);
                    }
                }
            });
        }
        
        // 同步到ES
        searchService.syncNote(note.getId());
        
        // 返回VO
        return buildNoteVO(note, userId);
    }
    
    /**
     * 获取笔记列表
     */
    @Override
    public List<NoteVO> getNoteList(int page, int size, Long userId) {
        int offset = (page - 1) * size;
        List<Note> notes = noteMapper.selectNoteList(size, offset);
        
        if (notes.isEmpty()) {
            return new ArrayList<>();
        }
        
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
     * 获取笔记详情
     * 使用 Redis + 分布式锁 + Lua 脚本原子递增浏览数
     */
    @Override
    public NoteVO getNoteDetail(Long noteId, Long userId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null || note.getStatus() != 1) {
            throw new BusinessException(404, "笔记不存在");
        }
        
        // 使用 Redis + 分布式锁原子递增浏览数
        incrementViewCountWithRedis(noteId);
        
        return buildNoteVO(note, userId);
    }
    
    /**
     * 使用 Redis + 分布式锁 + Lua 脚本原子递增浏览数
     */
    private void incrementViewCountWithRedis(Long noteId) {
        RLock lock = redissonClient != null ? redissonClient.getLock("view:note:" + noteId) : null;
        if (lock != null) {
            lock.lock();
        }
        try {
            String viewCountKey = NOTE_VIEW_COUNT_KEY + noteId;
            redisTemplate.execute(
                RedisScript.of(VIEW_INCREMENT_SCRIPT, Long.class),
                Collections.singletonList(viewCountKey)
            );
            // 异步更新数据库
            asyncSaveViewCountToDb(noteId);
        } finally {
            if (lock != null) {
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
     * 删除笔记
     */
    @Override
    @Transactional
    public boolean deleteNote(Long noteId, Long userId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException(404, "笔记不存在");
        }
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限删除此笔记");
        }
        
        // 软删除
        note.setDeletedAt(java.time.LocalDateTime.now());
        note.setStatus(2); // 标记为下架
        noteMapper.updateById(note);
        
        // 从ES删除
        searchService.deleteNote(noteId);
        
        return true;
    }
    
    /**
     * 点赞笔记
     * 如果已点赞，则取消点赞
     * 使用 Redis + 分布式锁 + Lua 脚本保证原子性
     * @return 包含isLiked(布尔)和likeCount(整数)的Map
     */
    @Override
    public Map<String, Object> likeNote(Long noteId, Long userId) {
        // 快速检查 Redis Set 中是否已点赞
        Boolean hasLiked = redisTemplate.opsForSet().isMember(NOTE_LIKED_USERS_KEY + noteId, userId.toString());
        if (Boolean.TRUE.equals(hasLiked)) {
            unlikeNote(noteId, userId);
            Long count = getRedisLikeCount(noteId);
            return Map.of("liked", false, "likeCount", count != null ? count.intValue() : 0);
        }

        // 获取分布式锁（如果Redisson不可用则跳过）
        RLock lock = redissonClient != null ? redissonClient.getLock("like:note:" + noteId) : null;
        if (lock != null) {
            lock.lock();
        }
        try {
            // 双重检查（防止并发）
            hasLiked = redisTemplate.opsForSet().isMember(NOTE_LIKED_USERS_KEY + noteId, userId.toString());
            if (Boolean.TRUE.equals(hasLiked)) {
                Long count = getRedisLikeCount(noteId);
                return Map.of("liked", false, "likeCount", count != null ? count.intValue() : 0);
            }

            // 检查笔记是否存在
            Note note = noteMapper.selectById(noteId);
            if (note == null || note.getStatus() != 1) {
                throw new BusinessException(404, "笔记不存在");
            }

            // 执行 Lua 脚本 - 原子递增点赞数
            String likeCountKey = NOTE_LIKE_COUNT_KEY + noteId;
            String likedUsersKey = NOTE_LIKED_USERS_KEY + noteId;
            String userLikedKey = USER_LIKED_NOTES_KEY + userId;

            Long newCount = redisTemplate.execute(
                RedisScript.of(LIKE_SCRIPT, Long.class),
                Arrays.asList(likeCountKey, likedUsersKey, userLikedKey),
                userId.toString()
            );

            // 异步更新数据库
            asyncSaveLikeRelationToDb(noteId, userId);

            // 记录用户互动活跃度
            activityService.recordInteraction(userId);
            activityService.incrementActivityScore(userId, ACTION_LIKE);
            // 增量更新热度
            incrementHotScore(noteId, 1);

            // 发送点赞通知 (异步MQ)
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
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    /**
     * 取消点赞
     * 使用 Redis + 分布式锁 + Lua 脚本保证原子性
     * @return 包含isLiked(布尔)和likeCount(整数)的Map
     */
    @Override
    public Map<String, Object> unlikeNote(Long noteId, Long userId) {
        // 快速检查
        Boolean hasLiked = redisTemplate.opsForSet().isMember(NOTE_LIKED_USERS_KEY + noteId, userId.toString());
        if (!Boolean.TRUE.equals(hasLiked)) {
            throw new BusinessException(400, "未点赞过该笔记");
        }

        // 获取分布式锁（如果Redisson不可用则跳过）
        RLock lock = redissonClient != null ? redissonClient.getLock("like:note:" + noteId) : null;
        if (lock != null) {
            lock.lock();
        }
        try {
            // 双重检查
            hasLiked = redisTemplate.opsForSet().isMember(NOTE_LIKED_USERS_KEY + noteId, userId.toString());
            if (!Boolean.TRUE.equals(hasLiked)) {
                throw new BusinessException(400, "未点赞过该笔记");
            }

            // 执行 Lua 脚本 - 原子递减点赞数
            String likeCountKey = NOTE_LIKE_COUNT_KEY + noteId;
            String likedUsersKey = NOTE_LIKED_USERS_KEY + noteId;
            String userLikedKey = USER_LIKED_NOTES_KEY + userId;

            Long newCount = redisTemplate.execute(
                RedisScript.of(UNLIKE_SCRIPT, Long.class),
                Arrays.asList(likeCountKey, likedUsersKey, userLikedKey),
                userId.toString()
            );

            // 异步删除数据库记录
            asyncDeleteLikeRelationFromDb(noteId, userId);

            // 扣减热度
            incrementHotScore(noteId, -1);

            return Map.of("liked", false, "likeCount", newCount != null ? newCount.intValue() : 0);
        } finally {
            if (lock != null) {
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
     * 使用 Redis + 分布式锁 + Lua 脚本保证原子性
     * @return 包含isFavorited(布尔)和favoriteCount(整数)的Map
     */
    @Override
    public Map<String, Object> favoriteNote(Long noteId, Long userId) {
        // 快速检查 Redis Set 中是否已收藏
        Boolean hasFavorited = redisTemplate.opsForSet().isMember(NOTE_FAVORITE_USERS_KEY + noteId, userId.toString());
        if (Boolean.TRUE.equals(hasFavorited)) {
            Long count = getRedisFavoriteCount(noteId);
            return Map.of("favorited", true, "favoriteCount", count != null ? count.intValue() : 0);
        }

        // 获取分布式锁
        RLock lock = redissonClient != null ? redissonClient.getLock("favorite:note:" + noteId) : null;
        if (lock != null) {
            lock.lock();
        }
        try {
            // 双重检查
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

            // 执行 Lua 脚本 - 原子递增收藏数
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

            // 记录用户互动活跃度
            activityService.recordInteraction(userId);
            activityService.incrementActivityScore(userId, ACTION_FAVORITE);
            // 增量更新热度 +3
            incrementHotScore(noteId, 3);

            return Map.of("favorited", true, "favoriteCount", newCount != null ? newCount.intValue() : 1);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    /**
     * 取消收藏
     * 使用 Redis + 分布式锁 + Lua 脚本保证原子性
     * @return 包含isFavorited(布尔)和favoriteCount(整数)的Map
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
        if (lock != null) {
            lock.lock();
        }
        try {
            // 双重检查
            hasFavorited = redisTemplate.opsForSet().isMember(NOTE_FAVORITE_USERS_KEY + noteId, userId.toString());
            if (!Boolean.TRUE.equals(hasFavorited)) {
                Long count = getRedisFavoriteCount(noteId);
                return Map.of("favorited", false, "favoriteCount", count != null ? count.intValue() : 0);
            }

            // 执行 Lua 脚本 - 原子递减收藏数
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
            if (lock != null) {
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
     * 获取点赞数：优先从Redis，没有则从数据库并写入Redis
     */
    private int getLikeCountFromCache(Long noteId, int defaultCount) {
        try {
            String likeCountStr = redisTemplate.opsForValue().get(NOTE_LIKE_COUNT_KEY + noteId);
            if (likeCountStr != null) {
                return Integer.parseInt(likeCountStr);
            }
            // Redis没有，从数据库获取并写入Redis
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
     * 获取收藏数：优先从Redis，没有则从数据库并写入Redis
     */
    private int getFavoriteCountFromCache(Long noteId, int defaultCount) {
        try {
            String favoriteCountStr = redisTemplate.opsForValue().get(NOTE_FAVORITE_COUNT_KEY + noteId);
            if (favoriteCountStr != null) {
                return Integer.parseInt(favoriteCountStr);
            }
            // Redis没有，从数据库获取并写入Redis
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
     * 获取浏览数：优先从Redis，没有则从数据库并写入Redis
     */
    private int getViewCountFromCache(Long noteId, int defaultCount) {
        try {
            String viewCountStr = redisTemplate.opsForValue().get(NOTE_VIEW_COUNT_KEY + noteId);
            if (viewCountStr != null) {
                return Integer.parseInt(viewCountStr);
            }
            // Redis没有，从数据库获取并写入Redis
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
     * 获取转发数：优先从Redis，没有则从数据库并写入Redis
     */
    private int getForwardCountFromCache(Long noteId, int defaultCount) {
        try {
            String forwardCountStr = redisTemplate.opsForValue().get(NOTE_FORWARD_COUNT_KEY + noteId);
            if (forwardCountStr != null) {
                return Integer.parseInt(forwardCountStr);
            }
            // Redis没有，从数据库获取并写入Redis
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
     * 构建笔记VO（带用户缓存Map）
     */
    private NoteVO buildNoteVO(Note note, Long userId, Map<Long, User> userMap) {
        NoteVO vo = new NoteVO();
        vo.setId(note.getId());
        vo.setUserId(note.getUserId());
        vo.setTitle(note.getTitle());
        vo.setContent(note.getContent());
        vo.setLocation(note.getLocation());
        
        // 获取点赞数：优先从Redis，没有则从数据库并写入Redis
        int likeCount = getLikeCountFromCache(note.getId(), note.getLikeCount());
        vo.setLikeCount(likeCount);
        
        // 获取评论数：优先从Redis，没有则从数据库
        long redisCommentCount = fullSortStrategy.getCommentCount(note.getId());
        vo.setCommentCount(redisCommentCount > 0 ? (int) redisCommentCount : note.getCommentCount());
        
        // 获取收藏数：优先从Redis，没有则从数据库并写入Redis
        int favoriteCount = getFavoriteCountFromCache(note.getId(), note.getFavoriteCount());
        vo.setFavoriteCount(favoriteCount);
        
        // 获取浏览数：优先从Redis，没有则从数据库并写入Redis
        int viewCount = getViewCountFromCache(note.getId(), note.getViewCount());
        vo.setViewCount(viewCount);
        
        // 获取转发数：优先从Redis，没有则从数据库并写入Redis
        int forwardCount = getForwardCountFromCache(note.getId(), note.getForwardCount());
        vo.setForwardCount(forwardCount);
        vo.setStableRandom(note.getStableRandom());
        vo.setCreatedAt(note.getCreatedAt());
        
        // 获取用户信息
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
        
        // 反序列化图片和标签
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
            // 忽略序列化错误
        }
        
        // 设置视频信息（使用签名URL）
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
        
        // 设置点赞收藏状态（优先从 Redis 获取，失败则降级到数据库）
        if (userId != null) {
            try {
                Boolean liked = redisTemplate.opsForSet().isMember(NOTE_LIKED_USERS_KEY + note.getId(), userId.toString());
                Boolean favorited = redisTemplate.opsForSet().isMember(NOTE_FAVORITE_USERS_KEY + note.getId(), userId.toString());
                vo.setLiked(Boolean.TRUE.equals(liked));
                vo.setFavorited(Boolean.TRUE.equals(favorited));
            } catch (Exception e) {
                // Redis 失败则降级到数据库查询
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
     * 获取当前用户的笔记列表
     * 如果查询者是笔记作者本人，返回所有状态（包括待审核、违规）
     * 如果查询者是其他用户，只返回正常状态的笔记
     */
    @Override
    public List<NoteVO> getMyNotes(Long userId, int page, int size, Long currentUserId) {
        int offset = (page - 1) * size;
        List<Note> notes = noteMapper.selectUserNotes(userId, currentUserId, size, offset);
        
        List<NoteVO> list = new ArrayList<>();
        for (Note note : notes) {
            list.add(buildNoteVO(note, currentUserId));
        }
        return list;
    }
    
    /**
     * 获取当前用户的收藏列表
     */
    @Override
    public List<NoteVO> getMyFavorites(Long userId, int page, int size, Long currentUserId) {
        int offset = (page - 1) * size;
        List<NoteFavorite> favorites = noteFavoriteMapper.selectByUserId(userId, size, offset);
        
        if (favorites == null || favorites.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取笔记ID列表
        List<Long> noteIds = favorites.stream()
                .map(NoteFavorite::getNoteId)
                .collect(java.util.stream.Collectors.toList());
        
        // 查询笔记详情
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Note::getId, noteIds)
               .eq(Note::getStatus, 1);
        List<Note> notes = noteMapper.selectList(wrapper);
        
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
     * 获取当前用户的获赞数
     */
    @Override
    public long getMyLikesCount(Long userId) {
        Long count = noteMapper.selectSumLikeCountByUserId(userId);
        return count != null ? count : 0L;
    }
    
    /**
     * 发现精彩 - 动态随机展示笔记（每次刷新都不同）
     * 使用 RAND() 实现随机排序，配合已展示ID列表实现翻页
     * 
     * cursor 格式：已展示的笔记ID列表（逗号分隔）
     * 例如：123,456,789
     */
    @Override
    public List<NoteVO> getDiscoverNotes(String cursor, int size, Long userId) {
        List<Note> notes;
        
        // 解析已展示的笔记ID
        List<Long> excludeIds = new ArrayList<>();
        if (cursor != null && !cursor.isEmpty()) {
            try {
                String[] idArr = cursor.split(",");
                for (String idStr : idArr) {
                    excludeIds.add(Long.parseLong(idStr.trim()));
                }
            } catch (NumberFormatException e) {
                // cursor格式错误，从头开始
                excludeIds.clear();
            }
        }
        
        if (excludeIds.isEmpty()) {
            // 首次加载：随机获取
            notes = noteMapper.selectDiscoverNotes(size);
        } else {
            // 翻页：排除已展示的笔记后随机获取
            notes = noteMapper.selectDiscoverNotesWithExclude(excludeIds, size);
        }
        
        // 构建VO
        List<NoteVO> voList = new ArrayList<>();
        for (Note note : notes) {
            voList.add(buildNoteVO(note, userId));
        }
        
        return voList;
    }
    
    /**
     * 过滤用户已浏览的笔记 - 使用 Redis Set 读取
     */
    private List<Note> filterViewedNotes(Long userId, List<Note> notes) {
        String key = VIEWED_NOTES_KEY_PREFIX + userId;
        
        Set<String> viewedIds = redisTemplate.opsForSet().members(key);
        
        if (viewedIds == null || viewedIds.isEmpty()) {
            return notes;
        }
        
        return notes.stream()
            .filter(note -> !viewedIds.contains(note.getId().toString()))
            .collect(Collectors.toList());
    }
    
    /**
     * 记录用户浏览笔记 - 使用 Redis Set 避免并发问题
     */
    private void recordViewedNotes(Long userId, List<Note> notes) {
        String key = VIEWED_NOTES_KEY_PREFIX + userId;
        
        // 使用 Redis Set 替代字符串拼接，避免并发覆盖
        for (Note note : notes) {
            redisTemplate.opsForSet().add(key, note.getId().toString());
        }
        
        // 设置过期时间
        redisTemplate.expire(key, VIEWED_NOTES_EXPIRE_HOURS, TimeUnit.HOURS);
        
        // 限制 Set 大小为最近 1000 条
        Long size = redisTemplate.opsForSet().size(key);
        if (size != null && size > 1000) {
            // 删除多余的元素（保留最新的1000条）
            Set<String> allMembers = redisTemplate.opsForSet().members(key);
            int toRemove = (int) (size - 1000);
            if (toRemove > 0 && allMembers != null) {
                // 将元素转为列表并排序（较旧的在前面）
                List<String> membersList = new ArrayList<>(allMembers);
                // 由于 Set 无序，直接取前面的元素删除
                List<String> toRemoveList = membersList.subList(0, toRemove);
                redisTemplate.opsForSet().remove(key, toRemoveList.toArray());
            }
        }
    }
    
    /**
     * 生成稳定随机数 stable_random
     * 使用雪花ID作为种子，保证唯一性和随机性
     * 范围：0-1，16位小数精度
     */
    private BigDecimal generateStableRandom() {
        long seed = snowflakeIdGenerator.nextId();
        // 将雪花ID转为0-1之间的随机数
        return BigDecimal.valueOf(seed % 1000000000000000L)
            .divide(BigDecimal.valueOf(1000000000000000L), 16, RoundingMode.DOWN);
    }
    
    /**
     * 热门 - 热度排序笔记（Redis ZSet）
     * 使用 ZREVRANGE 按热度降序获取
     */
    @Override
    public List<NoteVO> getPopularNotes(String cursor, int size, Long userId) {
        long startIndex = 0;
        long endIndex = size - 1;
        
        // 解析游标（数字offset）
        if (cursor != null && !cursor.isEmpty()) {
            try {
                startIndex = Long.parseLong(cursor);
                endIndex = startIndex + size - 1;
            } catch (NumberFormatException e) {
                startIndex = 0;
                endIndex = size - 1;
            }
        }
        
        // 从Redis获取热度榜单
        Set<String> noteIds = redisTemplate.opsForZSet()
            .reverseRange(HOT_RANK_KEY, startIndex, endIndex);
        
        if (noteIds == null || noteIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 过滤黑名单
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
        
        // 按热度排序
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
     * 刷新热门笔记热度（全局限流1分钟）
     * 访问热门Tab时调用
     */
    public void refreshHotScoreIfNeeded() {
        try {
            long now = System.currentTimeMillis();
            String lastRefreshStr = redisTemplate.opsForValue().get(HOT_LAST_REFRESH_KEY);
            
            // 检查是否在限流期内
            if (lastRefreshStr != null) {
                long lastRefresh = Long.parseLong(lastRefreshStr);
                if (now - lastRefresh < HOT_REFRESH_INTERVAL) {
                    log.debug("热度刷新在限流期内，跳过");
                    return;
                }
            }
            
            // 获取热门榜单前N条笔记ID
            Set<String> noteIds = redisTemplate.opsForZSet().reverseRange(HOT_RANK_KEY, 0, HOT_REFRESH_SIZE - 1);
            if (noteIds == null || noteIds.isEmpty()) {
                return;
            }
            
            // 刷新这些笔记的热度
            List<Long> ids = noteIds.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            List<Note> notes = noteMapper.selectByIds(ids);
            
            for (Note note : notes) {
                if (note.getStatus() == 1) {
                    double hotScore = calculateHotScore(note);
                    redisTemplate.opsForZSet().add(HOT_RANK_KEY, note.getId().toString(), hotScore);
                }
            }
            
            // 更新时间戳
            redisTemplate.opsForValue().set(HOT_LAST_REFRESH_KEY, String.valueOf(now));
            
            log.info("刷新热门笔记热度完成，刷新数量: {}", ids.size());
        } catch (Exception e) {
            log.error("刷新热门笔记热度失败: {}", e.getMessage());
        }
    }
    
    /**
     * 计算笔记热度
     */
    private double calculateHotScore(Note note) {
        int likeCount = note.getLikeCount() != null ? note.getLikeCount() : 0;
        int commentCount = note.getCommentCount() != null ? note.getCommentCount() : 0;
        int favoriteCount = note.getFavoriteCount() != null ? note.getFavoriteCount() : 0;
        int forwardCount = note.getForwardCount() != null ? note.getForwardCount() : 0;
        
        return likeCount * 1.0 + commentCount * 2.0 + favoriteCount * 3.0 + forwardCount * 5.0;
    }
    
    @Override
    public Note getNoteById(Long noteId) {
        return noteMapper.selectById(noteId);
    }
    
    /**
     * 转发笔记
     * 使用 Redis + 分布式锁 + Lua 脚本原子递增转发数
     */
    @Override
    public boolean forwardNote(Long noteId, Long userId, String content) {
        Note note = noteMapper.selectById(noteId);
        if (note == null || note.getStatus() != 1) {
            throw new BusinessException(404, "笔记不存在");
        }
        
        // 记录转发
        Forward forward = new Forward();
        forward.setOriginalNoteId(noteId);
        forward.setUserId(userId);
        forward.setContent(content);
        forwardMapper.insert(forward);
        
        // 使用 Redis + 分布式锁原子递增转发数
        incrementForwardCountWithRedis(noteId);
        
        // 增量更新热度 +5
        incrementHotScore(noteId, 5);
        
        // 记录用户互动活跃度
        activityService.recordInteraction(userId);
        activityService.incrementActivityScore(userId, ActivityServiceImpl.ACTION_FOLLOW);
        
        return true;
    }
    
    /**
     * 使用 Redis + 分布式锁 + Lua 脚本原子递增转发数
     */
    private void incrementForwardCountWithRedis(Long noteId) {
        RLock lock = redissonClient != null ? redissonClient.getLock("forward:note:" + noteId) : null;
        if (lock != null) {
            lock.lock();
        }
        try {
            String forwardCountKey = NOTE_FORWARD_COUNT_KEY + noteId;
            redisTemplate.execute(
                RedisScript.of(FORWARD_INCREMENT_SCRIPT, Long.class),
                Collections.singletonList(forwardCountKey)
            );
            // 异步更新数据库
            asyncSaveForwardCountToDb(noteId);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }
    
    /**
     * 异步保存转发数到数据库
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
     * 增量更新笔记热度 - 同步双写保证数据一致性
     * 热度 = like×1 + comment×2 + favorite×3 + forward×5
     * 
     * 更新策略：先更新数据库（主数据源），再更新Redis（缓存/展示）
     * 使用分布式锁保证并发安全
     */
    @Override
    public void incrementHotScore(Long noteId, int increment) {
        if (noteId == null) return;
        
        try {
            // 1. 获取分布式锁，保证并发安全
            RLock lock = redissonClient != null ? 
                redissonClient.getLock("hotScore:" + noteId) : null;
            if (lock != null) {
                lock.lock();
            }
            
            try {
                // 2. 检查黑名单
                Boolean isBlocked = redisTemplate.opsForSet().isMember(HOT_BLOCK_KEY, noteId.toString());
                if (Boolean.TRUE.equals(isBlocked)) {
                    return;
                }
                
                // 3. 查询当前笔记（从数据库获取）
                Note note = noteMapper.selectById(noteId);
                if (note == null || note.getStatus() != 1) {
                    // 笔记不存在或已删除，移除Redis
                    redisTemplate.opsForZSet().remove(HOT_RANK_KEY, noteId.toString());
                    return;
                }
                
                // 4. 计算新的热度值（基于数据库当前值 + 增量）
                double currentScore = note.getHotScore() != null ? note.getHotScore() : 0;
                double newScore = currentScore + increment;
                if (newScore < 0) newScore = 0;
                
                // 5. 同步写入数据库（主数据源）
                note.setHotScore(newScore);
                noteMapper.updateById(note);
                
                // 6. 同步更新Redis（实时展示）
                redisTemplate.opsForZSet().add(HOT_RANK_KEY, noteId.toString(), newScore);
                
                // 7. 维护热门榜单大小，删除最低分记录
                Long size = redisTemplate.opsForZSet().size(HOT_RANK_KEY);
                if (size != null && size > MAX_HOT_NOTES) {
                    redisTemplate.opsForZSet().removeRange(HOT_RANK_KEY, 0, size - MAX_HOT_NOTES - 1);
                }
                
                // 8. 设置过期时间（7天）
                redisTemplate.expire(HOT_RANK_KEY, HOT_SCORE_EXPIRE_DAYS, java.util.concurrent.TimeUnit.DAYS);
                
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
            
        } catch (RedisConnectionFailureException e) {
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

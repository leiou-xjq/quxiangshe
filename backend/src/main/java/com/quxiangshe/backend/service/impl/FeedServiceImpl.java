package com.quxiangshe.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.quxiangshe.backend.component.SnowflakeIdGenerator;
import com.quxiangshe.backend.entity.Follow;
import com.quxiangshe.backend.entity.Note;
import com.quxiangshe.backend.entity.User;
import com.quxiangshe.backend.mapper.FollowMapper;
import com.quxiangshe.backend.mapper.NoteMapper;
import com.quxiangshe.backend.mapper.NoteLikeMapper;
import com.quxiangshe.backend.mapper.NoteFavoriteMapper;
import com.quxiangshe.backend.mapper.UserMapper;
import com.quxiangshe.backend.mapper.FeedPushLogMapper;
import com.quxiangshe.backend.entity.FeedPushLog;
import com.quxiangshe.backend.service.IActivityService;
import com.quxiangshe.backend.service.IBlacklistService;
import com.quxiangshe.backend.service.IFeedService;
import com.quxiangshe.backend.service.INoteService;
import com.quxiangshe.backend.service.IOssService;
import com.quxiangshe.backend.vo.NoteVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.serializer.RedisSerializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Feed流服务实现类
 * 实现推模式、拉模式、推拉结合三种推荐策略
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements IFeedService {
    
    private final FollowMapper followMapper;
    private final NoteMapper noteMapper;
    private final UserMapper userMapper;
    private final NoteLikeMapper noteLikeMapper;
    private final NoteFavoriteMapper noteFavoriteMapper;
    private final FeedPushLogMapper feedPushLogMapper;
    private INoteService noteService;
    private IBlacklistService blacklistService;
    
    @Lazy
    @Autowired
    public void setNoteService(INoteService noteService) {
        this.noteService = noteService;
    }
    
    @Lazy
    @Autowired
    public void setBlacklistService(IBlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }
    
    private IOssService ossService;
    
    @Lazy
    @Autowired
    public void setOssService(IOssService ossService) {
        this.ossService = ossService;
    }
    
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final IActivityService activityService;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    // 本地缓存 - 热点Feed数据 (5分钟过期, 最大1000条)
    private static final Cache<Long, List<NoteVO>> LOCAL_FEED_CACHE = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, java.util.concurrent.TimeUnit.MINUTES)
            .build();
    
    // 本地缓存 - 热点用户基本信息 (10分钟过期, 最大500条)
    private static final Cache<Long, User> LOCAL_USER_CACHE = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(10, java.util.concurrent.TimeUnit.MINUTES)
            .build();
    
    // 推模式收件箱Key前缀 (粉丝的收件箱)
    private static final String PUSH_INBOX_PREFIX = "feed:inbox:push:";
    // 拉模式发件箱Key前缀 (作者的发件箱)
    private static final String PULL_OUTBOX_PREFIX = "feed:outbox:pull:";
    // 用户活跃度Key前缀
    private static final String ACTIVITY_PREFIX = "user:activity:";
    // 作者粉丝活跃度排名Key前缀
    private static final String FANS_ACTIVITY_RANK_PREFIX = "author:";
    private static final String FANS_ACTIVITY_RANK_SUFFIX = ":active_fans";
    private static final String FANS_NORMAL_FANS_SUFFIX = ":normal_fans";
    
    // Redis缓存Key前缀
    private static final String FOLLOWER_COUNT_KEY = "feed:follower:count:";
    private static final String FOLLOWING_KEY = "feed:following:";
    private static final String USER_FEED_KEY = "feed:user:";
    
    // 关注Tab红点标记Key前缀
    private static final String FOLLOW_UPDATE_KEY_PREFIX = "feed:follow:update:";
    // 红点标记过期时间（24小时）
    private static final long FOLLOW_UPDATE_EXPIRE_SECONDS = 86400;
    // 批量设置红点的批次大小
    private static final int BATCH_SET_FOLLOW_UPDATE_SIZE = 1000;
    
    // 查询并清除红点的Lua脚本（原子操作）
    private static final String CHECK_AND_CLEAR_SCRIPT = 
            "local exists = redis.call('EXISTS', KEYS[1]) " +
            "if exists == 1 then " +
            "  redis.call('DEL', KEYS[1]) " +
            "  return 1 " +
            "end " +
            "return 0";
    
    // 批量设置红点的Lua脚本
    private static final String BATCH_SET_FOLLOW_UPDATE_SCRIPT = 
            "for i = 1, #KEYS do " +
            "  redis.call('SET', KEYS[i], '1', 'EX', ARGV[1]) " +
            "end " +
            "return #KEYS";
    
    @Value("${feed.blogger.small:1000}")
    private long smallBloggerThreshold;
    @Value("${feed.blogger.medium:100000}")
    private long mediumBloggerThreshold;
    
    // 小博主平滑切换配置（可后续配置化）
    private static final int PRE_DUAL_WRITE_THRESHOLD = 950;
    private static final int SWITCH_TO_PULL_THRESHOLD = 1000;
    
    // ==================== 缓存配置优化 ====================
    // 关注列表缓存 - 1天（频繁读取，少量写入）
    private static final Duration FOLLOWING_CACHE_EXPIRE = Duration.ofDays(1);
    // 粉丝数缓存 - 6小时（相对稳定）
    private static final Duration FOLLOWER_COUNT_CACHE_EXPIRE = Duration.ofHours(6);
    // 拉模式发件箱缓存过期时间 (24小时)
    private static final Duration PULL_CACHE_EXPIRE = Duration.ofHours(24);
    // 用户Feed缓存 - 15分钟（高频更新）
    private static final long FEED_CACHE_EXPIRE_SECONDS = 15 * 60;
    // 旧缓存过期时间 (兼容旧代码)
    private static final long CACHE_EXPIRE_SECONDS = FEED_CACHE_EXPIRE_SECONDS;
    
    // 粉丝活跃度阈值
    private static final double ACTIVE_FAN_THRESHOLD = 120.0;   // 活跃粉丝：>=120
    private static final double NORMAL_FAN_THRESHOLD = 20.0;    // 普通粉丝：>20
    // 僵尸粉丝：<=20 不推送
    
    // Score序列号位数
    private static final long SCORE_SEQUENCE_BITS = 1024L;
    
    
    
    // 旧格式Score阈值 (小于此值为旧格式: 单纯时间戳)
    private static final double OLD_SCORE_THRESHOLD = 1_000_000_000_000L;
    
    // ==================== 新增方法：Score生成与解析 ====================
    
    /**
     * 生成复合Score (时间戳 * 1024 + 序列号)
     * 支持亿级并发，避免同毫秒多条笔记Score冲突
     * 
     * @param timestamp 时间戳
     * @return 复合Score
     */
    private long generateCompositeScore(double timestamp) {
        long sequence = snowflakeIdGenerator.nextId() & 1023;
        return ((long) timestamp * SCORE_SEQUENCE_BITS) + sequence;
    }
    
    /**
     * 判断Score是否为新格式
     * 新格式: timestamp * 1024 + sequence > 1万亿
     * 旧格式: 单纯时间戳 (如 1712645678000)
     * 
     * @param score Score值
     * @return true=新格式, false=旧格式
     */
    private boolean isNewFormatScore(double score) {
        return score > OLD_SCORE_THRESHOLD;
    }
    
    /**
     * 从Score中提取时间戳（兼容新旧格式）
     * 
     * @param score Score值
     * @return 时间戳
     */
    private long extractTimestampFromScore(double score) {
        if (isNewFormatScore(score)) {
            return (long) (score / SCORE_SEQUENCE_BITS);
        }
        return (long) score;
    }
    
    @Override
    public List<NoteVO> getFeed(Long userId, String cursor, int size) {
        // 尝试从缓存获取，失败时降级到数据库
        try {
            return getFeedFromCache(userId, cursor, size);
        } catch (Exception e) {
            log.warn("Feed获取失败，降级到数据库: userId={}, error={}", userId, e.getMessage());
            return getFeedFromDatabaseFallback(userId, cursor, size);
        }
    }
    
    /**
     * 从缓存获取Feed（核心逻辑）
     * 三级缓存：本地缓存 → Redis → 数据库
     */
    private List<NoteVO> getFeedFromCache(Long userId, String cursor, int size) {
        // 1. 先从本地缓存获取
        List<NoteVO> localCache = LOCAL_FEED_CACHE.getIfPresent(userId);
        if (localCache != null && localCache.get(0) != null && cursor == null) {
            log.debug("本地缓存命中: userId={}, size={}", userId, localCache.size());
            return processCursorPagination(localCache, cursor, size);
        }
        
        // 2. 获取关注列表（带缓存）
        List<Long> followingIds = getFollowingIdsCached(userId);
        
        if (followingIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 2. 过滤黑名单用户和用户自己（不显示自己发布的笔记）
        List<Long> blockedIds = blacklistService.getBlockedUserIds(userId);
        blockedIds.add(userId);  // 排除自己
        followingIds = followingIds.stream()
                .filter(id -> !blockedIds.contains(id))
                .collect(Collectors.toList());
        
        if (followingIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 3. 按博主粉丝量分类（小/中/大）
        Map<String, List<Long>> bloggerGroups = classifyBloggersByFollowerCount(followingIds);
        
        List<NoteVO> allNotes = new ArrayList<>();
        
        // 4. 并行获取各类型博主的笔记
        List<NoteVO> smallNotes = new ArrayList<>();
        List<NoteVO> mediumNotes = new ArrayList<>();
        List<NoteVO> largeNotes = new ArrayList<>();
        
        try {
            // 小博主：推模式，从收件箱获取
            if (!bloggerGroups.get("small").isEmpty()) {
                for (Long authorId : bloggerGroups.get("small")) {
                    List<NoteVO> notes = getFromPushInbox(authorId, userId);
                    smallNotes.addAll(notes);
                }
            }
            
            // 中博主：拉模式，分片聚合获取
            if (!bloggerGroups.get("medium").isEmpty()) {
                mediumNotes = getFromPullOutboxSharded(bloggerGroups.get("medium"), userId);
            }
            
            // 大博主：推拉结合模式
            if (!bloggerGroups.get("large").isEmpty()) {
                for (Long authorId : bloggerGroups.get("large")) {
                    List<NoteVO> notes = getFromHybridMode(authorId, userId);
                    largeNotes.addAll(notes);
                }
            }
        } catch (RedisConnectionFailureException e) {
            log.error("Redis连接失败，无法获取Feed: userId={}, 原因: {}", userId, e.getMessage());
        } catch (DataAccessException e) {
            log.error("数据库访问失败，无法获取Feed: userId={}, 原因: {}", userId, e.getMessage());
        } catch (Exception e) {
            log.error("获取Feed失败: userId={}, 原因: {}", userId, e.getMessage(), e);
        }
        
// 5. 合并所有笔记
        allNotes.addAll(smallNotes);
        allNotes.addAll(mediumNotes);
        allNotes.addAll(largeNotes);
        
        // 5.1 去重（根据noteId）
        allNotes = allNotes.stream()
                .collect(Collectors.toMap(
                        NoteVO::getId,
                        n -> n,
                        (existing, replacement) -> existing))
                .values()
                .stream()
                .collect(Collectors.toList());
        
        // 6. 按时间排序
        allNotes.sort((a, b) -> {
            if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        
        // 7. 写入本地缓存
        if (cursor == null) {
            LOCAL_FEED_CACHE.put(userId, allNotes);
        }
        
        // 8. 游标分页
        return processCursorPagination(allNotes, cursor, size);
    }
    
    /**
     * 按博主粉丝量分类
     */
    private Map<String, List<Long>> classifyBloggersByFollowerCount(List<Long> authorIds) {
        Map<String, List<Long>> groups = new HashMap<>();
        groups.put("small", new ArrayList<>());
        groups.put("medium", new ArrayList<>());
        groups.put("large", new ArrayList<>());
        
        for (Long authorId : authorIds) {
            long followerCount = getFollowerCount(authorId);
            
            if (followerCount < smallBloggerThreshold) {
                groups.get("small").add(authorId);
            } else if (followerCount < mediumBloggerThreshold) {
                groups.get("medium").add(authorId);
            } else {
                groups.get("large").add(authorId);
            }
        }
        
        return groups;
    }
    
    /**
     * 根据博主粉丝数决定读取策略（兼容旧逻辑，用于单个作者）
     */
    private List<NoteVO> getNotesFromAuthor(Long authorId, Long currentUserId, long followerCount) {
        try {
            if (followerCount < smallBloggerThreshold) {
                // 小博主：推模式，从粉丝收件箱获取
                log.debug("使用推模式获取笔记: authorId={}, followerCount={}", authorId, followerCount);
                return getFromPushInbox(authorId, currentUserId);
            } else if (followerCount < mediumBloggerThreshold) {
                // 中博主：拉模式，从作者发件箱获取
                log.debug("使用拉模式获取笔记: authorId={}, followerCount={}", authorId, followerCount);
                return getFromPullOutbox(authorId, currentUserId);
            } else {
                // 大博主：推拉结合模式
                log.debug("使用推拉结合模式获取笔记: authorId={}, followerCount={}", authorId, followerCount);
                return getFromHybridMode(authorId, currentUserId);
            }
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis连接失败，降级到数据库获取笔记: authorId={}", authorId);
            return getFromDatabaseFallback(authorId, currentUserId);
        } catch (DataAccessException e) {
            log.warn("数据库访问失败，降级到数据库获取笔记: authorId={}, 原因: {}", authorId, e.getMessage());
            return getFromDatabaseFallback(authorId, currentUserId);
        } catch (Exception e) {
            log.error("获取笔记失败，降级到数据库: authorId={}, 原因: {}", authorId, e.getMessage(), e);
            return getFromDatabaseFallback(authorId, currentUserId);
        }
    }
    
    /**
     * 推模式：从粉丝收件箱获取
     * 收件箱为空时，从数据库查询该作者的笔记并返回
     */
    private List<NoteVO> getFromPushInbox(Long authorId, Long userId) {
        String key = PUSH_INBOX_PREFIX + userId;
        
        // 1. 先查Redis收件箱
        Set<String> noteIds = redisTemplate.opsForZSet().reverseRange(key, 0, -1);
        
        if (noteIds != null && !noteIds.isEmpty()) {
            // 收件箱有数据，转换为Long并获取详情
            List<Long> ids = noteIds.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            return getNoteDetails(ids, userId);
        }
        
        // 2. 缓存未命中，从数据库查询该作者的最新笔记
        return getFromDatabaseFallback(authorId, userId);
    }
    
    /**
     * 拉模式：从作者发件箱获取
     * 发件箱为空时，从数据库查询并写入发件箱缓存
     */
    private List<NoteVO> getFromPullOutbox(Long authorId, Long userId) {
        String key = PULL_OUTBOX_PREFIX + authorId;
        
        // 1. 先查Redis发件箱
        Set<String> noteIds = redisTemplate.opsForZSet().reverseRange(key, 0, -1);
        
        if (noteIds != null && !noteIds.isEmpty()) {
            // 发件箱有数据
            List<Long> ids = noteIds.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            return getNoteDetails(ids, userId);
        }
        
        // 2. 缓存未命中，从数据库查询并写入发件箱缓存
        return getFromPullOutboxCacheAndReturn(authorId, userId);
    }
    
    /**
     * 拉模式：查数据库并写入发件箱缓存
     */
    private List<NoteVO> getFromPullOutboxCacheAndReturn(Long authorId, Long userId) {
        List<Note> notes = getLatestNotesByAuthor(authorId, 50);
        
        if (notes.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 写入发件箱缓存
        String key = PULL_OUTBOX_PREFIX + authorId;
        for (Note note : notes) {
            double score = note.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
            redisTemplate.opsForZSet().add(key, note.getId().toString(), score);
        }
        redisTemplate.expire(key, PULL_CACHE_EXPIRE);
        
        List<Long> noteIds = notes.stream().map(Note::getId).collect(Collectors.toList());
        return getNoteDetails(noteIds, userId);
    }
    
    /**
     * 拉模式：分片聚合拉取（中博主优化）
     * 1. 按作者ID哈希分片
     * 2. Redis缓存检查（30分钟过期）
     * 3. 批量获取未缓存的作者发件箱
     */
    private List<NoteVO> getFromPullOutboxSharded(List<Long> authorIds, Long userId) {
        if (authorIds == null || authorIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<NoteVO> result = new ArrayList<>();
        
        // 1. Redis缓存检查
        List<NoteVO> cachedNotes = getFromRedisCache(userId);
        Map<Long, List<NoteVO>> cachedByAuthor = cachedNotes.stream()
            .collect(Collectors.groupingBy(NoteVO::getUserId));
        
        // 2. 按分片聚合作者ID
        Map<String, List<Long>> shards = splitAuthorsByShard(authorIds);
        
        // 3. 逐分片获取
        for (Map.Entry<String, List<Long>> shard : shards.entrySet()) {
            List<Long> shardAuthors = shard.getValue();
            
            for (Long authorId : shardAuthors) {
                // 跳过已有缓存的作者
                if (cachedByAuthor.containsKey(authorId)) {
                    result.addAll(cachedByAuthor.get(authorId));
                    continue;
                }
                
// 从Redis发件箱获取
                try {
                    String key = PULL_OUTBOX_PREFIX + authorId;
                    Set<String> noteIds = redisTemplate.opsForZSet().reverseRange(key, 0, 49);
                    
                    if (noteIds != null && !noteIds.isEmpty()) {
                        List<Long> ids = noteIds.stream()
                            .map(Long::parseLong)
                            .collect(Collectors.toList());
                        List<NoteVO> notes = getNoteDetails(ids, userId);
                        result.addAll(notes);
                    }
                } catch (NumberFormatException e) {
                    log.warn("解析发件箱笔记ID失败: authorId={}, 原因: {}", authorId, e.getMessage());
                } catch (RedisConnectionFailureException e) {
                    log.warn("Redis连接失败: authorId={}", authorId);
                } catch (Exception e) {
                    log.warn("从发件箱获取笔记失败: authorId={}, 原因: {}", authorId, e.getMessage());
                }
            }
        }
        
        // 4. 更新Redis缓存
        updateRedisCache(userId, result);
        
        return result;
    }
    
    /**
     * 推拉结合模式：优先从收件箱获取，收件箱为空则从发件箱获取
     */
    private List<NoteVO> getFromHybridMode(Long authorId, Long currentUserId) {
        // 1. 优先从收件箱获取（推模式）
        List<NoteVO> notes = getFromPushInbox(authorId, currentUserId);
        
        // 2. 收件箱为空，则从发件箱获取（拉模式）
        if (notes == null || notes.isEmpty()) {
            notes = getFromPullOutbox(authorId, currentUserId);
        }
        
return notes;
    }
    
    /**
     * 从Redis缓存获取用户的关注列表
     */
    private List<Long> getFollowingIdsCached(Long userId) {
        String key = FOLLOWING_KEY + userId;
        
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null && !json.isEmpty()) {
                return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
            }
        } catch (JsonProcessingException e) {
            log.warn("解析关注列表缓存失败: userId={}, 原因: {}", userId, e.getMessage());
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis连接失败，降级到数据库获取关注列表: userId={}", userId);
        } catch (Exception e) {
            log.warn("获取关注列表缓存失败: userId={}, 原因: {}", userId, e.getMessage());
        }
        
        // 缓存未命中，从数据库查询
        return getFollowingIdsFromDB(userId);
    }
    
    /**
     * 从数据库获取用户的关注列表
     */
    private List<Long> getFollowingIdsFromDB(Long userId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowerId, userId);
        List<Follow> follows = followMapper.selectList(wrapper);
        
        List<Long> followingIds = follows.stream()
                .map(Follow::getFollowingId)
                .collect(Collectors.toList());
        
        // 写入缓存
        cacheFollowingIds(userId, followingIds);
        
        return followingIds;
    }
    
    /**
     * 缓存用户关注列表
     */
    private void cacheFollowingIds(Long userId, List<Long> followingIds) {
        String key = FOLLOWING_KEY + userId;
        try {
            String json = objectMapper.writeValueAsString(followingIds);
            redisTemplate.opsForValue().set(key, json, FOLLOWING_CACHE_EXPIRE.toHours(), TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.warn("序列化关注列表失败: userId={}, 原因: {}", userId, e.getMessage());
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis连接失败，无法缓存关注列表: userId={}", userId);
        } catch (Exception e) {
            log.warn("缓存关注列表失败: userId={}, 原因: {}", userId, e.getMessage());
        }
    }
    
    /**
     * 从Redis缓存获取粉丝数
     */
    private long getFollowerCountCached(Long userId) {
        String key = FOLLOWER_COUNT_KEY + userId;
        
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null && !value.isEmpty()) {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            log.warn("解析粉丝数缓存失败: userId={}, value异常, 原因: {}", userId, e.getMessage());
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis连接失败，降级到数据库获取粉丝数: userId={}", userId);
        } catch (Exception e) {
            log.warn("获取粉丝数缓存失败: userId={}, 原因: {}", userId, e.getMessage());
        }
        
        // 缓存未命中，从数据库获取
        long count = getFollowerCountFromDB(userId);
        cacheFollowerCount(userId, count);
        
        return count;
    }
    
    /**
     * 缓存粉丝数
     */
    private void cacheFollowerCount(Long userId, long count) {
        String key = FOLLOWER_COUNT_KEY + userId;
        try {
            redisTemplate.opsForValue().set(key, String.valueOf(count), FOLLOWER_COUNT_CACHE_EXPIRE.toHours(), TimeUnit.HOURS);
        } catch (RedisConnectionFailureException e) {
            log.debug("Redis连接失败，跳过缓存粉丝数: userId={}", userId);
        } catch (Exception e) {
            log.warn("缓存粉丝数失败: userId={}, 原因: {}", userId, e.getMessage());
        }
    }
    
    /**
     * 从数据库查询该作者的笔记并返回（降级方案）
     */
    private List<NoteVO> getFromDatabaseFallback(Long authorId, Long userId) {
        List<Note> notes = getLatestNotesByAuthor(authorId, 20);
        if (notes.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> noteIds = notes.stream().map(Note::getId).collect(Collectors.toList());
        return getNoteDetails(noteIds, userId);
    }
    
    /**
     * 完全降级：从数据库获取用户Feed（Redis不可用时）
     */
    private List<NoteVO> getFeedFromDatabaseFallback(Long userId, String cursor, int size) {
        log.info("使用数据库降级方案获取Feed: userId={}", userId);
        
        try {
            // 1. 从数据库获取关注列表
            List<Long> followingIds = getFollowingIdsFromDB(userId);
            
            if (followingIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 2. 过滤黑名单
            List<Long> blockedIds = blacklistService.getBlockedUserIds(userId);
            blockedIds.add(userId);
            followingIds = followingIds.stream()
                    .filter(id -> !blockedIds.contains(id))
                    .collect(Collectors.toList());
            
            if (followingIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 3. 查询这些用户的最新笔记
            LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(Note::getUserId, followingIds);
            wrapper.eq(Note::getStatus, 1);
            wrapper.orderByDesc(Note::getCreatedAt);
            wrapper.last("LIMIT " + (size * 2));  // 多查一些用于分页
            
            List<Note> notes = noteMapper.selectList(wrapper);
            
            if (notes.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 4. 获取笔记详情
            List<Long> noteIds = notes.stream().map(Note::getId).collect(Collectors.toList());
            List<NoteVO> noteVOs = getNoteDetails(noteIds, userId);
            
            // 5. 游标分页
            return processCursorPagination(noteVOs, cursor, size);
            
        } catch (DataAccessException e) {
            log.error("数据库访问异常，降级方案失败: userId={}, 原因: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("获取Feed失败: userId={}, 原因: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 从数据库获取该作者的最新笔记
     */
    private List<Note> getLatestNotesByAuthor(Long authorId, int limit) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getUserId, authorId);
        wrapper.orderByDesc(Note::getCreatedAt);
        wrapper.last("LIMIT " + limit);
        return noteMapper.selectList(wrapper);
    }
    
    /**
     * 按分片将作者列表分组
     */
    private Map<String, List<Long>> splitAuthorsByShard(List<Long> authorIds) {
        Map<String, List<Long>> result = new HashMap<>();
        
        // 简单的分片策略：每10个作者为一个分片
        int shardSize = 10;
        for (int i = 0; i < authorIds.size(); i += shardSize) {
            int end = Math.min(i + shardSize, authorIds.size());
            String shardKey = "shard_" + (i / shardSize);
            result.put(shardKey, authorIds.subList(i, end));
        }
        
        return result;
    }
    
    /**
     * 从Redis缓存获取用户Feed
     */
    private List<NoteVO> getFromRedisCache(Long userId) {
        String key = USER_FEED_KEY + userId;
        
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null && !json.isEmpty()) {
                return objectMapper.readValue(json, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, NoteVO.class));
            }
        } catch (JsonProcessingException e) {
            log.warn("解析用户Feed缓存失败: userId={}, 原因: {}", userId, e.getMessage());
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis连接失败，跳过缓存获取: userId={}", userId);
        } catch (Exception e) {
            log.warn("获取用户Feed缓存失败: userId={}, 原因: {}", userId, e.getMessage());
        }
        
        return new ArrayList<>();
    }

    /**
     * 更新Redis缓存
     */
private void updateRedisCache(Long userId, List<NoteVO> notes) {
        if (notes.isEmpty()) {
            return;
        }
        
        String key = USER_FEED_KEY + userId;
        try {
            String json = objectMapper.writeValueAsString(notes);
            redisTemplate.opsForValue().set(key, json, CACHE_EXPIRE_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("序列化用户Feed失败: userId={}, 原因: {}", userId, e.getMessage());
        } catch (RedisConnectionFailureException e) {
            log.debug("Redis连接失败，跳过缓存更新: userId={}", userId);
        } catch (Exception e) {
            log.warn("更新用户Feed缓存失败: userId={}, 原因: {}", userId, e.getMessage());
        }
    }
    
    @Override
    public long getFollowerCount(Long userId) {
        return getFollowerCountCached(userId);
    }
    
    @Override
    public void evictFollowingCache(Long userId) {
        String key = FOLLOWING_KEY + userId;
        redisTemplate.delete(key);
        log.debug("清除关注列表缓存: userId={}", userId);
    }
    
    @Override
    public void evictFollowerCache(Long userId) {
        String key = FOLLOWER_COUNT_KEY + userId;
        redisTemplate.delete(key);
        log.debug("清除粉丝数缓存: userId={}", userId);
    }
    
    @Override
    public void evictUserFeedCache(Long userId) {
        String key = USER_FEED_KEY + userId;
        redisTemplate.delete(key);
        log.debug("清除用户Feed缓存: userId={}", userId);
    }
    
    @Override
    public void evictAllCachesByAuthor(Long authorId) {
        // 只清除粉丝数缓存，保留粉丝分类数据（推拉结合的依据）
        evictFollowerCache(authorId);
        log.debug("清除作者粉丝数缓存: authorId={}", authorId);
    }
    
    /**
     * 分批推送笔记到活跃粉丝收件箱（推拉结合模式）
     */
    @Override
    public void pushNoteInBatch(Long noteId, Long authorId, int batchNum, int totalBatches) {
        long startTime = System.currentTimeMillis();
        
        // 获取笔记信息
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            log.warn("笔记不存在，跳过推送: noteId={}", noteId);
            return;
        }
        
        double score = note.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        
        // 计算当前批次的粉丝范围
        long totalFollowers = getFollowerCount(authorId);
        long fansPerBatch = totalFollowers / totalBatches;
        long startIdx = batchNum * fansPerBatch;
        long endIdx = (batchNum == totalBatches - 1) ? totalFollowers : (batchNum + 1) * fansPerBatch;
        
        log.info("分批推送-第{}/{}批: noteId={}, authorId={}, startIdx={}, endIdx={}", 
            batchNum + 1, totalBatches, noteId, authorId, startIdx, endIdx);
        
        // 获取当前批次的粉丝（带活跃度筛选）
        List<Follow> batchFollowers = getActiveFollowersByRank(authorId, startIdx, endIdx);
        
        if (batchFollowers.isEmpty()) {
            log.info("分批推送-第{}/{}批: 无活跃粉丝, noteId={}", batchNum + 1, totalBatches, noteId);
            return;
        }
        
        // 生成复合Score
        long compositeScore = generateCompositeScore(score);
        
        // 批量写入收件箱（Redis Pipeline）
        pushToActiveFansInBatch(batchFollowers, noteId, authorId, score);
        
        // 记录推送日志
        recordPushLogs(batchFollowers, noteId, authorId, FeedPushLog.PUSH_MODE_HYBRID);
        
        long costTime = System.currentTimeMillis() - startTime;
        log.info("分批推送-第{}/{}批完成: noteId={}, 推送数={}, cost={}ms", 
            batchNum + 1, totalBatches, noteId, batchFollowers.size(), costTime);
    }
    
    /**
     * 按排名范围获取活跃粉丝
     */
    private List<Follow> getActiveFollowersByRank(Long authorId, long startIdx, long endIdx) {
        // 查询该范围的粉丝
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowingId, authorId);
        wrapper.last("LIMIT " + startIdx + ", " + (endIdx - startIdx));
        List<Follow> followers = followMapper.selectList(wrapper);
        
        if (followers.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取这些粉丝的活跃度，筛选活跃的
        List<Long> fanIds = followers.stream().map(Follow::getFollowerId).collect(Collectors.toList());
        Map<Long, Double> activityScores = activityService.getActivityScores(fanIds);
        
        List<Follow> activeFollows = new ArrayList<>();
        for (Follow follow : followers) {
            Double score = activityScores.getOrDefault(follow.getFollowerId(), 0.0);
            if (score >= ACTIVE_FAN_THRESHOLD) {
                activeFollows.add(follow);
            }
        }
        
        return activeFollows;
    }
    
    /**
     * 从数据库获取粉丝数
     */
    private long getFollowerCountFromDB(Long userId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowingId, userId);
        return followMapper.selectCount(wrapper);
    }
    
    /**
     * 批量获取笔记详情（集成本地缓存）
     */
    private List<NoteVO> getNoteDetails(List<Long> noteIds, Long userId) {
        if (noteIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 1. 批量获取笔记
        List<Note> notes = noteMapper.selectByIds(noteIds);
        if (notes.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 2. 收集所有用户ID，从本地缓存获取 + 数据库批量查询
        Set<Long> userIdSet = notes.stream()
                .map(Note::getUserId)
                .collect(Collectors.toSet());
        List<Long> userIds = new ArrayList<>(userIdSet);
        Map<Long, User> userMap = new HashMap<>();
        
        if (!userIds.isEmpty()) {
            // 先从本地缓存获取
            List<Long> missingUserIds = new ArrayList<>();
            for (Long userIdItem : userIds) {
                User cachedUser = LOCAL_USER_CACHE.getIfPresent(userIdItem);
                if (cachedUser != null) {
                    userMap.put(userIdItem, cachedUser);
                } else {
                    missingUserIds.add(userIdItem);
                }
            }
            
            // 本地缓存未命中，从数据库批量查询
            if (!missingUserIds.isEmpty()) {
                List<User> users = userMapper.selectByIds(missingUserIds);
                for (User user : users) {
                    userMap.put(user.getId(), user);
                    // 放入本地缓存
                    LOCAL_USER_CACHE.put(user.getId(), user);
                }
            }
        }
        
        // 3. 批量查询当前用户的点赞和收藏状态
        Set<Long> likedNoteIds = new HashSet<>();
        Set<Long> favoritedNoteIds = new HashSet<>();
        
        if (userId != null) {
            // 查询点赞状态
            List<Long> likedIds = noteLikeMapper.selectLikedNoteIds(userId, noteIds);
            likedNoteIds = new HashSet<>(likedIds);
            
            // 查询收藏状态
            List<Long> favoritedIds = noteFavoriteMapper.selectFavoritedNoteIds(userId, noteIds);
            favoritedNoteIds = new HashSet<>(favoritedIds);
        }
        
        // 4. 组装VO
        List<NoteVO> result = new ArrayList<>();
        for (Note note : notes) {
            try {
                NoteVO vo = buildNoteVO(note, userMap, likedNoteIds, favoritedNoteIds);
                result.add(vo);
            } catch (Exception e) {
                log.error("构建笔记VO失败: noteId={}, 原因: {}", note.getId(), e.getMessage(), e);
            }
        }
        
        return result;
    }
    
    /**
     * 构建笔记VO
     */
    private NoteVO buildNoteVO(Note note, Map<Long, User> userMap, Set<Long> likedNoteIds, Set<Long> favoritedNoteIds) {
        NoteVO vo = new NoteVO();
        vo.setId(note.getId());
        vo.setUserId(note.getUserId());
        vo.setTitle(note.getTitle());
        vo.setContent(note.getContent());
        vo.setLocation(note.getLocation());
        vo.setLikeCount(note.getLikeCount());
        vo.setCommentCount(note.getCommentCount());
        vo.setFavoriteCount(note.getFavoriteCount());
        vo.setViewCount(note.getViewCount());
        vo.setCreatedAt(note.getCreatedAt());
        
        // 用户信息
        User user = userMap.get(note.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }
        
        // 设置点赞和收藏状态
        if (likedNoteIds != null) {
            vo.setLiked(likedNoteIds.contains(note.getId()));
        }
        if (favoritedNoteIds != null) {
            vo.setFavorited(favoritedNoteIds.contains(note.getId()));
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
        } catch (Exception e) {
            // 忽略序列化错误
        }
        
        // 设置视频信息（使用签名URL）
        String videoUrl = note.getVideo();
        if (videoUrl != null && !videoUrl.isEmpty() && ossService != null) {
            try {
                videoUrl = ossService.getSignedUrl(videoUrl);
            } catch (Exception e) {
                log.warn("生成视频签名URL失败: {}", e.getMessage());
            }
        }
        vo.setVideo(videoUrl);
        vo.setVideoCover(note.getVideoCover());
        
        return vo;
    }
    
    /**
     * 游标分页处理
     */
    private List<NoteVO> processCursorPagination(List<NoteVO> notes, String cursor, int size) {
        int startIndex = 0;
        
        if (cursor != null && !cursor.isEmpty()) {
            String[] parts = cursor.split("_");
            if (parts.length == 2) {
                long timestamp = Long.parseLong(parts[0]);
                long noteId = Long.parseLong(parts[1]);
                
                for (int i = 0; i < notes.size(); i++) {
                    NoteVO note = notes.get(i);
                    if (note.getCreatedAt() != null && 
                        note.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli() == timestamp &&
                        note.getId() == noteId) {
                        startIndex = i + 1;
                        break;
                    }
                }
            }
        }
        
        int endIndex = Math.min(startIndex + size, notes.size());
        if (startIndex >= notes.size()) {
            return new ArrayList<>();
        }
        
        return notes.subList(startIndex, endIndex);
    }
    
    @Override
    public void pushNoteToFeed(Long noteId, Long authorId) {
        try {
            pushNoteToFeedInternal(noteId, authorId);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis连接失败，推送笔记加入重试队列: noteId={}, authorId={}", noteId, authorId, e);
            saveToRetryQueue(noteId, authorId);
        } catch (Exception e) {
            log.error("推送笔记失败，加入重试队列: noteId={}, authorId={}, 原因: {}", noteId, authorId, e.getMessage(), e);
            saveToRetryQueue(noteId, authorId);
        }
    }
    
    /**
     * 缓存预热：笔记发布后主动刷新粉丝的Feed缓存
     */
    public void warmCacheOnNotePublished(Long authorId) {
        try {
            List<Long> followerIds = getFollowersFromDB(authorId);
            for (Long followerId : followerIds) {
                String feedKey = USER_FEED_KEY + followerId;
                redisTemplate.delete(feedKey);
            }
            log.info("缓存预热完成: authorId={}, 刷新粉丝数={}", authorId, followerIds.size());
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis连接失败，跳过缓存预热: authorId={}", authorId);
        } catch (Exception e) {
            log.warn("缓存预热失败: authorId={}, 原因: {}", authorId, e.getMessage());
        }
    }
    
    /**
     * 缓存预热：关注变更时刷新缓存
     */
    public void warmCacheOnFollowChanged(Long userId, Long followTargetUserId, boolean isFollow) {
        try {
            // 刷新关注者的关注列表缓存
            redisTemplate.delete(FOLLOWING_KEY + userId);
            // 刷新被关注者的粉丝数缓存
            redisTemplate.delete(FOLLOWER_COUNT_KEY + followTargetUserId);
            log.info("关注变更缓存刷新: userId={}, target={}, isFollow={}", userId, followTargetUserId, isFollow);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis连接失败，跳过缓存刷新: userId={}", userId);
        } catch (Exception e) {
            log.warn("关注变更缓存刷新失败: userId={}, 原因: {}", userId, e.getMessage());
        }
    }
    
    /**
     * 获取粉丝列表（从数据库）
     */
    private List<Long> getFollowersFromDB(Long authorId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowingId, authorId);
        List<Follow> follows = followMapper.selectList(wrapper);
        return follows.stream().map(Follow::getFollowerId).collect(Collectors.toList());
    }
    
    /**
     * 推送笔记内部实现
     * 统一异步推送策略，避免阻塞主流程
     */
    private void pushNoteToFeedInternal(Long noteId, Long authorId) {
        long startTime = System.currentTimeMillis();
        
        // 获取博主粉丝数量（带缓存）
        long followerCount = getFollowerCount(authorId);
        
        log.info("开始推送笔记: noteId={}, authorId={}, followerCount={}", noteId, authorId, followerCount);
        
        // 统一异步执行，根据粉丝数选择策略
        CompletableFuture.runAsync(() -> {
            try {
                // 验证笔记存在
                Note note = noteMapper.selectById(noteId);
                if (note == null) {
                    log.warn("笔记不存在，跳过推送: noteId={}", noteId);
                    return;
                }
                
                if (followerCount < 100) {
                    // 微博主 (<100)：直接同步推送
                    pushBySmallBlogger(noteId, authorId, (int) followerCount);
                } else if (followerCount < smallBloggerThreshold) {
                    // 小博主 (100-1000)：推模式异步
                    pushBySmallBlogger(noteId, authorId, (int) followerCount);
                } else if (followerCount <= mediumBloggerThreshold) {
                    // 中博主 (1000-10万)：拉模式（发件箱，无需等待）
                    pushByMediumBlogger(noteId, authorId);
                } else {
                    // 大博主 (>10万)：推拉结合异步
                    pushByLargeBlogger(noteId, authorId);
                }
                
                log.info("推送完成: noteId={}, authorId={}, totalCost={}ms", 
                    noteId, authorId, System.currentTimeMillis() - startTime);
                    
            } catch (Exception e) {
                log.error("推送笔记异常: noteId={}, authorId={}", noteId, authorId, e);
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    /**
     * 保存到重试队列（Redis List）
     */
    private void saveToRetryQueue(Long noteId, Long authorId) {
        try {
            String key = "feed:push:retry";
            String value = noteId + ":" + authorId + ":" + System.currentTimeMillis();
            redisTemplate.opsForList().leftPush(key, value);
            // 24小时后过期
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
            log.info("已加入重试队列: noteId={}, authorId={}", noteId, authorId);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis连接失败，无法加入重试队列: noteId={}, authorId={}", noteId, authorId, e);
        } catch (Exception e) {
            log.error("加入重试队列失败: noteId={}, authorId={}, 原因: {}", noteId, authorId, e.getMessage());
        }
    }
    
    /**
     * 小博主推送 (<1000粉丝)：纯推模式
     * 支持平滑切换：950粉预双写，1000粉切换到纯拉
     */
    private void pushBySmallBlogger(Long noteId, Long authorId, int followerCount) {
        Note note = noteMapper.selectById(noteId);
        double score = note.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        long compositeScore = generateCompositeScore(score);
        
        List<Follow> followers = getFollowers(authorId);
        if (followers.isEmpty()) {
            return;
        }
        
        if (followerCount >= PRE_DUAL_WRITE_THRESHOLD && followerCount < SWITCH_TO_PULL_THRESHOLD) {
            // 950-1000粉：双写模式（同时写收件箱+发件箱）
            log.info("双写模式: authorId={}, followerCount={}", authorId, followerCount);
            
            // 写收件箱
            pushToAllFollowersInBatch(followers, noteId, authorId);
            
            // 写发件箱
            pushToAuthorOutbox(noteId, authorId);
        } else {
            // <950粉：纯推模式
            log.info("纯推模式: authorId={}, followerCount={}", authorId, followerCount);
            pushToAllFollowersInBatch(followers, noteId, authorId);
        }
    }
    
    /**
     * 中博主推送 (1000-10万粉丝)：纯拉模式
     * 仅写入作者发件箱
     */
    private void pushByMediumBlogger(Long noteId, Long authorId) {
        pushToAuthorOutbox(noteId, authorId);
    }
    
    /**
     * 大博主推送 (≥10万粉丝)：推拉结合模式
     * 异步执行，不阻塞发布笔记主流程
     */
    private void pushByLargeBlogger(Long noteId, Long authorId) {
        final Long finalNoteId = noteId;
        final Long finalAuthorId = authorId;
        
        // 异步执行推拉结合模式，不阻塞当前请求
        CompletableFuture.runAsync(() -> {
            try {
                pushToHybridModeByRedisAsync(finalNoteId, finalAuthorId);
            } catch (RedisConnectionFailureException e) {
                log.warn("Redis连接失败，推拉结合模式降级到纯拉模式: authorId={}", finalAuthorId);
                pushToAuthorOutbox(finalNoteId, finalAuthorId);
            } catch (Exception e) {
                log.warn("推拉结合模式异常，降级到纯拉模式: authorId={}, error={}", finalAuthorId, e.getMessage());
                pushToAuthorOutbox(finalNoteId, finalAuthorId);
            }
        }, executor);
        
        log.info("大博主推拉结合模式已触发异步执行: noteId={}, authorId={}", noteId, authorId);
    }
    
    /**
     * 推拉结合模式异步执行
     */
    private void pushToHybridModeByRedisAsync(Long noteId, Long authorId) {
        long stepStartTime = System.currentTimeMillis();
        
        String activeKey = FANS_ACTIVITY_RANK_PREFIX + authorId + FANS_ACTIVITY_RANK_SUFFIX;
        String normalKey = FANS_ACTIVITY_RANK_PREFIX + authorId + FANS_NORMAL_FANS_SUFFIX;
        
        // 检查Redis数据是否存在
        Long activeCount = redisTemplate.opsForSet().size(activeKey);
        Long normalCount = redisTemplate.opsForSet().size(normalKey);
        
        if ((activeCount == null || activeCount == 0) && (normalCount == null || normalCount == 0)) {
            log.info("Redis无粉丝分类数据，触发后台异步初始化: authorId={}", authorId);
            final Long asyncAuthorId = authorId;
            CompletableFuture.runAsync(() -> {
                try {
                    activityService.updateFansActivityRank(asyncAuthorId);
                } catch (Exception e) {
                    log.error("后台异步初始化粉丝分类失败: authorId={}, 原因: {}", asyncAuthorId, e.getMessage());
                }
            }, executor);
            // 降级到纯拉模式
            pushToAuthorOutbox(noteId, authorId);
            return;
        }
        
        // 获取笔记（增加重试机制，等待主事务提交）
        Note note = retrySelectNote(noteId);
        if (note == null) {
            log.error("笔记不存在，跳过推送: noteId={}", noteId);
            return;
        }
        
        double score = note.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        long compositeScore = generateCompositeScore(score);
        
        // 获取活跃粉丝 Set
        Set<String> activeFanIds = redisTemplate.opsForSet().members(activeKey);
        
        // 获取普通粉丝 Set (不需要推送到收件箱，走拉模式)
        Set<String> normalFanIds = redisTemplate.opsForSet().members(normalKey);
        
        log.info("推拉结合模式-粉丝分类: noteId={}, 活跃粉丝数={}, 普通粉丝数={}", 
            noteId, 
            activeFanIds != null ? activeFanIds.size() : 0,
            normalFanIds != null ? normalFanIds.size() : 0);
        
        // 活跃粉丝 -> 收件箱 (异步Pipeline批量写入)
        if (activeFanIds != null && !activeFanIds.isEmpty()) {
            final Set<String> finalActiveFanIds = activeFanIds;
            final long finalCompositeScore = compositeScore;
            CompletableFuture.runAsync(() -> {
                pushActiveFansToInboxByPipeline(finalActiveFanIds, noteId, authorId, finalCompositeScore);
                log.info("推拉结合模式-活跃粉丝推送完成: noteId={}, count={}, cost={}ms", 
                    noteId, finalActiveFanIds.size(), System.currentTimeMillis() - stepStartTime);
            }, executor);
        }
        
        // 普通粉丝 -> 写入作者发件箱 (异步)
        if (normalFanIds != null && !normalFanIds.isEmpty()) {
            pushToAuthorOutbox(noteId, authorId);
            log.info("推拉结合模式-普通粉丝已写入发件箱: noteId={}, count={}, cost={}ms", 
                noteId, normalFanIds.size(), System.currentTimeMillis() - stepStartTime);
        }
        
        log.info("推拉结合模式-处理完成: noteId={}, authorId={}, totalCost={}ms", 
            noteId, authorId, System.currentTimeMillis() - stepStartTime);
    }
    
    /**
     * 推拉结合模式：通过Redis Set获取粉丝，按活跃度分类处理 (同步版本，保留兼容)
     */
    private void pushToHybridModeByRedis(Long noteId, Long authorId) {
        long stepStartTime = System.currentTimeMillis();
        
        String activeKey = FANS_ACTIVITY_RANK_PREFIX + authorId + FANS_ACTIVITY_RANK_SUFFIX;
        String normalKey = FANS_ACTIVITY_RANK_PREFIX + authorId + FANS_NORMAL_FANS_SUFFIX;
        
        // 检查Redis数据是否存在
        Long activeCount = redisTemplate.opsForSet().size(activeKey);
        Long normalCount = redisTemplate.opsForSet().size(normalKey);
        
        if ((activeCount == null || activeCount == 0) && (normalCount == null || normalCount == 0)) {
            log.info("Redis无粉丝分类数据，触发后台异步初始化: authorId={}", authorId);
            // 异步初始化，不阻塞当前请求
            final Long asyncAuthorId = authorId;
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    activityService.updateFansActivityRank(asyncAuthorId);
                } catch (Exception e) {
                    log.error("后台异步初始化粉丝分类失败: authorId={}, error={}", asyncAuthorId, e.getMessage());
                }
            });
            // 立即降级到纯拉模式，不等待初始化完成
            pushByMediumBlogger(noteId, authorId);
            return;
        }
        
        Note note = noteMapper.selectById(noteId);
        double score = note.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        long compositeScore = generateCompositeScore(score);
        
        // 1. 获取活跃粉丝 Set
        Set<String> activeFanIds = redisTemplate.opsForSet().members(activeKey);
        
        // 2. 获取普通粉丝 Set
        Set<String> normalFanIds = redisTemplate.opsForSet().members(normalKey);
        
        log.info("推拉结合模式-粉丝分类: noteId={}, 活跃粉丝数={}, 普通粉丝数={}", 
            noteId, 
            activeFanIds != null ? activeFanIds.size() : 0,
            normalFanIds != null ? normalFanIds.size() : 0);
        
        // 粉丝分类为空时，降级到纯拉模式并记录日志
        if ((activeFanIds == null || activeFanIds.isEmpty()) && 
            (normalFanIds == null || normalFanIds.isEmpty())) {
            log.warn("粉丝分类数据为空，降级到纯拉模式: authorId={}", authorId);
            pushToAuthorOutbox(noteId, authorId);
            return;
        }
        
        // 3. 活跃粉丝 -> 收件箱 (Pipeline批量写入)
        if (activeFanIds != null && !activeFanIds.isEmpty()) {
            pushActiveFansToInboxByPipeline(activeFanIds, noteId, authorId, compositeScore);
            log.info("推拉结合模式-活跃粉丝推送完成: noteId={}, count={}, cost={}ms", 
                noteId, activeFanIds.size(), System.currentTimeMillis() - stepStartTime);
        }
        
        // 4. 普通粉丝 -> 作者发件箱 (Pipeline批量写入)
        if (normalFanIds != null && !normalFanIds.isEmpty()) {
            pushNormalFansToOutboxByPipeline(normalFanIds, noteId, authorId, score);
            log.info("推拉结合模式-普通粉丝推送完成: noteId={}, count={}, cost={}ms", 
                noteId, normalFanIds.size(), System.currentTimeMillis() - stepStartTime);
        }
        
        // 5. 僵尸粉丝不推送
        log.info("推拉结合模式-处理完成: noteId={}, authorId={}", noteId, authorId);
    }
    
    /**
     * 活跃粉丝批量推送到收件箱 (使用Redis Pipeline + 异步日志)
     */
    private void pushActiveFansToInboxByPipeline(Set<String> fanIds, Long noteId, Long authorId, long compositeScore) {
        long startTime = System.currentTimeMillis();
        
        // 使用 Pipeline 批量写入 Redis (分批处理避免单次数据量过大)
        List<String> fanIdList = new ArrayList<>(fanIds);
        int batchSize = 5000;
        
        for (int i = 0; i < fanIdList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, fanIdList.size());
            List<String> batch = fanIdList.subList(i, end);
            
            final String noteIdStr = noteId.toString();
            final long score = compositeScore;
            
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (String fanId : batch) {
                    String key = PUSH_INBOX_PREFIX + fanId;
                    
                    // 写入收件箱
                    connection.zSetCommands().zAdd(key.getBytes(), score, noteIdStr.getBytes());
                    // 清理超过200条的数据 (使用 zRemRange)
                    connection.zSetCommands().zRemRange(key.getBytes(), 0, -201);
                    // 设置过期时间
                    connection.keyCommands().expire(key.getBytes(), 7 * 24 * 3600);
                }
                return null;
            });
        }
        
        // 异步记录推送日志
        asyncRecordPushLogs(fanIds, noteId, authorId, FeedPushLog.PUSH_MODE_HYBRID);
        
        log.info("活跃粉丝收件箱推送完成: noteId={}, fans={}, cost={}ms", 
            noteId, fanIds.size(), System.currentTimeMillis() - startTime);
    }
    
    /**
     * 异步记录推送日志
     */
    private void asyncRecordPushLogs(Set<String> fanIds, Long noteId, Long authorId, int pushMode) {
        executor.submit(() -> {
            try {
                List<FeedPushLog> logs = new ArrayList<>(fanIds.size());
                for (String fanId : fanIds) {
                    FeedPushLog log = new FeedPushLog();
                    log.setNoteId(noteId);
                    log.setAuthorId(authorId);
                    log.setTargetUserId(Long.parseLong(fanId));
                    log.setPushMode(pushMode);
                    log.setPushStatus(FeedPushLog.PUSH_STATUS_SUCCESS);
                    logs.add(log);
                }
                feedPushLogMapper.batchInsert(logs);
            } catch (Exception e) {
                log.error("异步记录推送日志失败: noteId={}", noteId, e);
            }
        });
    }
    
    /**
     * 普通粉丝批量写入作者发件箱 (使用Redis Pipeline)
     */
    private void pushNormalFansToOutboxByPipeline(Set<String> fanIds, Long noteId, Long authorId, double score) {
        long startTime = System.currentTimeMillis();
        
        // 写入作者发件箱（所有普通粉丝都能通过拉模式获取）
        String outboxKey = PULL_OUTBOX_PREFIX + authorId;
        long compositeScore = generateCompositeScore(score);
        
        // 写入作者发件箱
        redisTemplate.opsForZSet().add(outboxKey, noteId.toString(), compositeScore);
        redisTemplate.opsForZSet().removeRange(outboxKey, 0, -101);
        redisTemplate.expire(outboxKey, PULL_CACHE_EXPIRE);
        
        // 异步记录普通粉丝的拉模式日志
        asyncRecordPushLogs(fanIds, noteId, authorId, FeedPushLog.PUSH_MODE_PULL);
        
        log.info("普通粉丝发件箱写入完成: noteId={}, fans={}, cost={}ms", 
            noteId, fanIds.size(), System.currentTimeMillis() - startTime);
    }
    
    /**
     * 获取粉丝列表（带缓存）
     */
    private List<Follow> getFollowers(Long userId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowingId, userId);
        return followMapper.selectList(wrapper);
    }
    
    /**
     * 推模式：批量推送到粉丝收件箱（使用Pipeline优化）
     */
    private void pushToAllFollowersInBatch(List<Follow> followers, Long noteId, Long authorId) {
        long startTime = System.currentTimeMillis();
        
        // 获取笔记发布时间作为Score
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            log.warn("笔记不存在，跳过推送: noteId={}", noteId);
            return;
        }
        double score = note.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        
        // 生成复合Score (使用雪花ID避免并发冲突)
        long compositeScore = generateCompositeScore(score);
        
        // 使用Pipeline批量写入（优化性能）
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Follow follow : followers) {
                String key = PUSH_INBOX_PREFIX + follow.getFollowerId();
                byte[] keyBytes = key.getBytes();
                byte[] valueBytes = String.valueOf(noteId).getBytes();
                connection.zAdd(keyBytes, compositeScore, valueBytes);
            }
            return null;
        });
        
        // 使用Pipeline批量设置过期时间
        final long finalScore = compositeScore;
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Follow follow : followers) {
                String key = PUSH_INBOX_PREFIX + follow.getFollowerId();
                byte[] keyBytes = key.getBytes();
                byte[] valueBytes = String.valueOf(noteId).getBytes();
                connection.zAdd(keyBytes, finalScore, valueBytes);
                // 设置7天过期
                connection.expire(keyBytes, 7 * 24 * 60 * 60);
            }
            return null;
        });
        
        // 记录推送日志（异步）
        CompletableFuture.runAsync(() -> {
            try {
                recordPushLogs(followers, noteId, authorId, FeedPushLog.PUSH_MODE_PUSH);
            } catch (Exception e) {
                log.warn("记录推送日志失败: noteId={}", noteId, e);
            }
        }, executor);
        
        log.info("推模式批量推送完成(Pipeline): noteId={}, fans={}, cost={}ms", 
            noteId, followers.size(), System.currentTimeMillis() - startTime);
    }
    
    /**
     * 拉模式：写入作者发件箱
     */
    private void pushToAuthorOutbox(Long noteId, Long authorId) {
        // 获取笔记发布时间作为Score
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            log.warn("笔记不存在，跳过推送: noteId={}", noteId);
            return;
        }
        double score = note.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        
        // 生成复合Score (使用雪花ID避免并发冲突)
        long compositeScore = generateCompositeScore(score);
        
        String key = PULL_OUTBOX_PREFIX + authorId;
        redisTemplate.opsForZSet().add(key, noteId.toString(), compositeScore);
        redisTemplate.opsForZSet().removeRange(key, 0, -101);
        redisTemplate.expire(key, PULL_CACHE_EXPIRE);
        
        // 记录推送日志（拉模式：写入发件箱，target_user_id为作者自己）
        FeedPushLog pushLog = new FeedPushLog();
        pushLog.setNoteId(noteId);
        pushLog.setAuthorId(authorId);
        pushLog.setTargetUserId(authorId);
        pushLog.setPushMode(FeedPushLog.PUSH_MODE_PULL);
        pushLog.setPushStatus(FeedPushLog.PUSH_STATUS_SUCCESS);
        feedPushLogMapper.insert(pushLog);
        
        log.info("拉模式写入发件箱完成: authorId={}, noteId={}", authorId, noteId);
    }
    
    /**
     * 推拉结合模式：批量处理，保留活跃度区分
     */
    private void pushToHybridModeInBatch(List<Follow> followers, Long authorId, Long noteId) {
        long startTime = System.currentTimeMillis();
        
        // 获取笔记发布时间作为Score
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            log.warn("笔记不存在，跳过推送: noteId={}", noteId);
            return;
        }
        double score = note.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        
        // 1. 写入作者发件箱（所有粉丝都能通过拉模式获取）
        pushToAuthorOutbox(noteId, authorId);
        
        // 2. 收集所有粉丝ID
        List<Long> fanIds = followers.stream()
            .map(Follow::getFollowerId)
            .collect(Collectors.toList());
        
        // 3. 批量获取所有粉丝的活跃度
        Map<Long, Double> activityScores = getActivityScoresInBatch(fanIds);
        
        // 4. 分类：活跃粉丝
        List<Follow> activeFollows = new ArrayList<>();
        for (Follow follow : followers) {
            Double userScore = activityScores.getOrDefault(follow.getFollowerId(), 0.0);
            if (userScore >= ACTIVE_FAN_THRESHOLD) {
                activeFollows.add(follow);
            }
        }
        
        // 5. 只推送给活跃粉丝（使用Pipeline批量）
        if (!activeFollows.isEmpty()) {
            pushToActiveFansInBatch(activeFollows, noteId, authorId, score);
        }
        
        log.info("推拉结合模式完成: noteId={}, 总粉丝={}, 活跃粉丝={}, cost={}ms", 
            noteId, followers.size(), activeFollows.size(), System.currentTimeMillis() - startTime);
    }
    
    /**
     * 批量获取粉丝活跃度
     */
    private Map<Long, Double> getActivityScoresInBatch(List<Long> fanIds) {
        if (fanIds == null || fanIds.isEmpty()) {
            return new HashMap<>();
        }
        return activityService.getActivityScores(fanIds);
    }
    
    /**
     * 批量推送给活跃粉丝
     */
    private void pushToActiveFansInBatch(List<Follow> activeFollows, Long noteId, Long authorId, double score) {
        if (activeFollows == null || activeFollows.isEmpty()) {
            return;
        }
        
        long compositeScore = generateCompositeScore(score);
        
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Follow follow : activeFollows) {
                String key = PUSH_INBOX_PREFIX + follow.getFollowerId();
                byte[] keyBytes = key.getBytes();
                byte[] valueBytes = String.valueOf(noteId).getBytes();
                connection.zAdd(keyBytes, compositeScore, valueBytes);
            }
            return null;
        });
        
        for (Follow follow : activeFollows) {
            String key = PUSH_INBOX_PREFIX + follow.getFollowerId();
            redisTemplate.opsForZSet().removeRange(key, 0, -201);
            redisTemplate.expire(key, Duration.ofDays(7));
        }
        
        // 记录推送日志
        recordPushLogs(activeFollows, noteId, authorId, FeedPushLog.PUSH_MODE_HYBRID);
        
        log.info("活跃粉丝批量推送完成: noteId={}, activeFans={}", noteId, activeFollows.size());
    }
    
    @Override
    public double calculateActivityScore(Long userId) {
        return activityService.getActivityScore(userId);
    }
    
    /**
     * 记录推送日志（批量插入 - 异步）
     */
    private void recordPushLogs(List<Follow> followers, Long noteId, Long authorId, int pushMode) {
        if (followers == null || followers.isEmpty()) {
            return;
        }
        
        // 转换为 Set 后异步记录
        Set<String> fanIds = followers.stream()
            .map(f -> f.getFollowerId().toString())
            .collect(Collectors.toSet());
        asyncRecordPushLogs(fanIds, noteId, authorId, pushMode);
    }
    
    /**
     * 重试查询笔记（等待主事务提交）
     */
    private Note retrySelectNote(Long noteId) {
        int maxRetries = 5;
        long retryIntervalMs = 100;
        
        for (int i = 0; i < maxRetries; i++) {
            Note note = noteMapper.selectById(noteId);
            if (note != null) {
                return note;
            }
            log.warn("笔记查询为空，重试 {}/{}: noteId={}", i + 1, maxRetries, noteId);
            try {
                Thread.sleep(retryIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }
    
    // ==================== 关注Tab红点管理 ====================
    
    @Override
    public boolean hasFollowUpdate(Long userId) {
        if (userId == null) {
            return false;
        }
        
        String key = FOLLOW_UPDATE_KEY_PREFIX + userId;
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis连接失败，降级处理: userId={}", userId);
            return false;
        } catch (Exception e) {
            log.warn("查询关注更新标记失败: userId={}, 原因: {}", userId, e.getMessage());
            return false;
        }
    }
    
    @Override
    public void clearFollowUpdate(Long userId) {
        if (userId == null) {
            return;
        }
        
        String key = FOLLOW_UPDATE_KEY_PREFIX + userId;
        
        try {
            // 使用Lua脚本原子检查并删除
            RedisScript<Long> script = RedisScript.of(CHECK_AND_CLEAR_SCRIPT, Long.class);
            redisTemplate.execute(script, Collections.singletonList(key));
            log.debug("清除关注更新标记: userId={}", userId);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis连接失败，跳过清除: userId={}", userId);
        } catch (Exception e) {
            log.warn("清除关注更新标记失败: userId={}, 原因: {}", userId, e.getMessage());
        }
    }
    
    @Override
    public void setFollowUpdateForFans(Long authorId, Long noteId) {
        if (authorId == null || noteId == null) {
            return;
        }
        
        log.info("开始设置粉丝关注更新标记: authorId={}, noteId={}", authorId, noteId);
        
        // 异步执行，不阻塞主流程
        CompletableFuture.runAsync(() -> {
            try {
                setFollowUpdateForFansInternal(authorId);
            } catch (Exception e) {
                log.error("设置粉丝关注更新标记失败: authorId={}, noteId={}, 原因: {}", 
                    authorId, noteId, e.getMessage(), e);
            }
        }, executor);
    }
    
    /**
     * 内部实现：设置所有粉丝的关注更新标记（分批处理）
     */
    private void setFollowUpdateForFansInternal(Long authorId) {
        long startTime = System.currentTimeMillis();
        
        // 获取粉丝总数
        long followerCount = getFollowerCount(authorId);
        
        if (followerCount == 0) {
            log.info("作者无粉丝，跳过设置红点: authorId={}", authorId);
            return;
        }
        
        log.info("开始分批设置红点: authorId={}, 总粉丝数={}", authorId, followerCount);
        
        // 计算批次数
        int totalBatches = (int) Math.ceil((double) followerCount / BATCH_SET_FOLLOW_UPDATE_SIZE);
        
        int totalSet = 0;
        
        for (int batch = 0; batch < totalBatches; batch++) {
            long startIdx = (long) batch * BATCH_SET_FOLLOW_UPDATE_SIZE;
            long batchSize = Math.min(BATCH_SET_FOLLOW_UPDATE_SIZE, followerCount - startIdx);
            
            // 获取当前批次的粉丝ID
            List<Long> fanIds = getFanIdsByRange(authorId, startIdx, (int) batchSize);
            
            if (fanIds.isEmpty()) {
                continue;
            }
            
            // 批量设置红点
            int setCount = batchSetFollowUpdate(fanIds);
            totalSet += setCount;
            
            log.info("设置红点第{}/{}批完成: authorId={}, 设置数={}", 
                batch + 1, totalBatches, authorId, setCount);
            
            // 每批间隔100ms，避免Redis压力过大
            if (batch < totalBatches - 1) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        long costTime = System.currentTimeMillis() - startTime;
        log.info("设置粉丝红点完成: authorId={}, 总设置数={}, cost={}ms", 
            authorId, totalSet, costTime);
    }
    
    /**
     * 获取指定范围的粉丝ID
     */
    private List<Long> getFanIdsByRange(Long authorId, long offset, int limit) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowingId, authorId);
        wrapper.last("LIMIT " + offset + ", " + limit);
        
        List<Follow> follows = followMapper.selectList(wrapper);
        return follows.stream()
                .map(Follow::getFollowerId)
                .collect(Collectors.toList());
    }
    
    /**
     * 批量设置关注更新标记（使用Lua脚本）
     */
    private int batchSetFollowUpdate(List<Long> fanIds) {
        if (fanIds == null || fanIds.isEmpty()) {
            return 0;
        }
        
        // 构建Key列表
        List<String> keys = fanIds.stream()
                .map(id -> FOLLOW_UPDATE_KEY_PREFIX + id)
                .collect(Collectors.toList());
        
        try {
            // 使用Lua脚本批量设置
            RedisScript<Long> script = RedisScript.of(BATCH_SET_FOLLOW_UPDATE_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, keys, String.valueOf(FOLLOW_UPDATE_EXPIRE_SECONDS));
            return result != null ? result.intValue() : 0;
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis连接失败，批量设置红点失败: fanIds.size={}", fanIds.size());
            return 0;
        } catch (Exception e) {
            log.warn("批量设置关注更新标记失败: 原因: {}", e.getMessage());
            return 0;
        }
    }
}
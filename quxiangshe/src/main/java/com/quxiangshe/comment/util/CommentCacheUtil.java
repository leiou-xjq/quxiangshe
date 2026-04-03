package com.quxiangshe.comment.util;

import com.quxiangshe.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 评论Redis缓存工具类
 * 用于缓存评论数、点赞数等热点数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentCacheUtil {

    /**
     * Redis工具类
     */
    private final RedisUtil redisUtil;

    // ==================== 缓存Key前缀 ====================

    /**
     * 评论数缓存Key前缀
     */
    private static final String COMMENT_COUNT_KEY_PREFIX = "comment:count:";

    /**
     * 点赞数缓存Key前缀
     */
    private static final String LIKE_COUNT_KEY_PREFIX = "comment:like:";

    /**
     * 评论点赞用户Key前缀
     */
    private static final String COMMENT_LIKE_USERS_KEY_PREFIX = "comment:like:users:";

    /**
     * 缓存过期时间（秒）
     */
    private static final long CACHE_EXPIRE_TIME = 3600; // 1小时

    // ==================== 评论数缓存 ====================

    /**
     * 缓存评论数
     *
     * @param articleId 文章ID
     * @param count 评论数
     */
    public void cacheCommentCount(Long articleId, Long count) {
        String key = COMMENT_COUNT_KEY_PREFIX + articleId;
        redisUtil.set(key, count.toString(), CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        log.debug("缓存文章评论数: articleId={}, count={}", articleId, count);
    }

    /**
     * 获取缓存的评论数
     *
     * @param articleId 文章ID
     * @return 评论数，如果缓存不存在返回null
     */
    public Long getCommentCount(Long articleId) {
        String key = COMMENT_COUNT_KEY_PREFIX + articleId;
        Object value = redisUtil.get(key);
        if (value != null) {
            return Long.parseLong(value.toString());
        }
        return null;
    }

    /**
     * 删除评论数缓存
     *
     * @param articleId 文章ID
     */
    public void deleteCommentCount(Long articleId) {
        String key = COMMENT_COUNT_KEY_PREFIX + articleId;
        redisUtil.delete(key);
    }

    // ==================== 点赞数缓存 ====================

    /**
     * 缓存点赞数
     *
     * @param commentId 评论ID
     * @param count 点赞数
     */
    public void cacheLikeCount(Long commentId, Long count) {
        String key = LIKE_COUNT_KEY_PREFIX + commentId;
        redisUtil.set(key, count.toString(), CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存的点赞数
     *
     * @param commentId 评论ID
     * @return 点赞数，如果缓存不存在返回null
     */
    public Long getLikeCount(Long commentId) {
        String key = LIKE_COUNT_KEY_PREFIX + commentId;
        Object value = redisUtil.get(key);
        if (value != null) {
            return Long.parseLong(value.toString());
        }
        return null;
    }

    /**
     * 增加点赞数缓存
     *
     * @param commentId 评论ID
     * @return 新的点赞数
     */
    public Long incrementLikeCount(Long commentId) {
        String key = LIKE_COUNT_KEY_PREFIX + commentId;
        Long newCount = redisUtil.increment(key);
        redisUtil.expire(key, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        return newCount;
    }

    /**
     * 减少点赞数缓存
     *
     * @param commentId 评论ID
     * @return 新的点赞数
     */
    public Long decrementLikeCount(Long commentId) {
        String key = LIKE_COUNT_KEY_PREFIX + commentId;
        Long newCount = redisUtil.decrement(key);
        if (newCount < 0) {
            redisUtil.set(key, "0", CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
            return 0L;
        }
        redisUtil.expire(key, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        return newCount;
    }

    // ==================== 用户点赞状态缓存 ====================

    /**
     * 缓存用户点赞状态
     *
     * @param commentId 评论ID
     * @param userId 用户ID
     */
    public void cacheUserLikeStatus(Long commentId, Long userId) {
        String key = COMMENT_LIKE_USERS_KEY_PREFIX + commentId;
        redisUtil.sAdd(key, userId.toString());
        redisUtil.expire(key, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    /**
     * 检查用户是否点赞
     *
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return true表示已点赞
     */
    public Boolean isUserLiked(Long commentId, Long userId) {
        String key = COMMENT_LIKE_USERS_KEY_PREFIX + commentId;
        return redisUtil.sIsMember(key, userId.toString());
    }

    /**
     * 移除用户点赞状态
     *
     * @param commentId 评论ID
     * @param userId 用户ID
     */
    public void removeUserLikeStatus(Long commentId, Long userId) {
        String key = COMMENT_LIKE_USERS_KEY_PREFIX + commentId;
        redisUtil.sRemove(key, userId.toString());
    }
}

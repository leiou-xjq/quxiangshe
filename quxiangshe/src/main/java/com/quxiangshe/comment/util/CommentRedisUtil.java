package com.quxiangshe.comment.util;

import com.quxiangshe.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 评论专用Redis工具类
 * 提供评论队列、热榜、点赞等操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentRedisUtil {

    private final RedisUtil redisUtil;

    /**
     * 评论队列Key
     */
    private static final String COMMENT_QUEUE_KEY = "comment:queue";

    /**
     * 动态评论列表Key前缀
     */
    private static final String POST_COMMENTS_KEY_PREFIX = "comment:post:";

    /**
     * 热门评论Key前缀
     */
    private static final String HOT_COMMENTS_KEY_PREFIX = "comment:hot:";

    /**
     * 评论点赞Key前缀
     */
    private static final String COMMENT_LIKE_KEY_PREFIX = "comment:like:";

    /**
     * 队列过期时间（秒）
     */
    private static final long QUEUE_TTL = 3600;

    // ==================== 评论队列操作 ====================

    /**
     * 推入评论队列
     * 用于异步批量写入MySQL
     *
     * @param commentData 评论数据（JSON格式）
     */
    public void pushToQueue(String commentData) {
        try {
            redisUtil.rPush(COMMENT_QUEUE_KEY, commentData);
            log.debug("评论推入队列: {}", commentData);
        } catch (Exception e) {
            log.error("推入评论队列失败", e);
            throw e;
        }
    }

    /**
     * 从队列弹出评论
     *
     * @return 评论数据
     */
    public String popFromQueue() {
        return (String) redisUtil.lPop(COMMENT_QUEUE_KEY);
    }

    /**
     * 获取队列长度
     *
     * @return 队列长度
     */
    public Long getQueueSize() {
        return redisUtil.lSize(COMMENT_QUEUE_KEY);
    }

    /**
     * 批量从队列获取评论
     *
     * @param count 数量
     * @return 评论数据列表
     */
    public List<String> batchPopFromQueue(int count) {
        List<String> result = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            String comment = popFromQueue();
            if (comment == null) {
                break;
            }
            result.add(comment);
        }
        return result;
    }

    // ==================== 动态评论列表操作 ====================

    /**
     * 缓存动态的评论列表
     *
     * @param postId    动态ID
     * @param comments  评论列表
     */
    public void cachePostComments(Long postId, List<String> comments) {
        String key = POST_COMMENTS_KEY_PREFIX + postId;
        try {
            // 先删除旧缓存
            redisUtil.delete(key);
            // 添加新缓存
            for (String comment : comments) {
                redisUtil.rPush(key, comment);
            }
            redisUtil.expire(key, 10 * 60L, TimeUnit.SECONDS); // 10分钟过期
            log.debug("缓存动态评论列表: postId={}, count={}", postId, comments.size());
        } catch (Exception e) {
            log.error("缓存动态评论列表失败: postId={}", postId, e);
        }
    }

    /**
     * 获取动态评论列表
     *
     * @param postId 动态ID
     * @return 评论列表
     */
    public List<String> getPostComments(Long postId) {
        String key = POST_COMMENTS_KEY_PREFIX + postId;
        List<Object> result = redisUtil.lRange(key, 0, -1);
        if (result == null || result.isEmpty()) {
            return Collections.emptyList();
        }
        return result.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    /**
     * 添加评论到动态列表
     *
     * @param postId    动态ID
     * @param comment   评论数据
     */
    public void addCommentToPost(Long postId, String comment) {
        String key = POST_COMMENTS_KEY_PREFIX + postId;
        // 从左侧推入（最新的在前面）
        redisUtil.lPush(key, comment);
    }

    // ==================== 热门评论操作 ====================

    /**
     * 增加评论热度
     * 用于热门评论排行
     *
     * @param postId    动态ID
     * @param commentId 评论ID
     * @param score     热度增量
     */
    public void incrementCommentHot(Long postId, Long commentId, double score) {
        String key = HOT_COMMENTS_KEY_PREFIX + postId;
        redisUtil.zAdd(key, commentId.toString(), score);
        redisUtil.expire(key, 60 * 60L, TimeUnit.SECONDS); // 1小时过期
    }

    /**
     * 获取热门评论列表
     *
     * @param postId 动态ID
     * @param limit  数量
     * @return 评论ID列表
     */
    public List<Long> getHotComments(Long postId, int limit) {
        String key = HOT_COMMENTS_KEY_PREFIX + postId;
        Set<Object> result = redisUtil.zReverseRange(key, 0, limit - 1);
        if (result == null || result.isEmpty()) {
            return Collections.emptyList();
        }
        return result.stream()
                .map(obj -> Long.parseLong(obj.toString()))
                .collect(Collectors.toList());
    }

    // ==================== 评论点赞操作 ====================

    /**
     * 点赞评论
     *
     * @param commentId 评论ID
     * @param userId    用户ID
     */
    public void likeComment(Long commentId, Long userId) {
        String key = COMMENT_LIKE_KEY_PREFIX + commentId;
        redisUtil.sAdd(key, userId.toString());
        redisUtil.expire(key, 30 * 60L, TimeUnit.SECONDS); // 30分钟过期
    }

    /**
     * 取消点赞评论
     *
     * @param commentId 评论ID
     * @param userId    用户ID
     */
    public void unlikeComment(Long commentId, Long userId) {
        String key = COMMENT_LIKE_KEY_PREFIX + commentId;
        redisUtil.sRemove(key, userId.toString());
    }

    /**
     * 检查是否点赞评论
     *
     * @param commentId 评论ID
     * @param userId    用户ID
     * @return true表示已点赞
     */
    public Boolean isCommentLiked(Long commentId, Long userId) {
        String key = COMMENT_LIKE_KEY_PREFIX + commentId;
        return redisUtil.sIsMember(key, userId.toString());
    }

    /**
     * 获取评论点赞数
     *
     * @param commentId 评论ID
     * @return 点赞数
     */
    public Long getCommentLikeCount(Long commentId) {
        String key = COMMENT_LIKE_KEY_PREFIX + commentId;
        return redisUtil.sSize(key);
    }
}

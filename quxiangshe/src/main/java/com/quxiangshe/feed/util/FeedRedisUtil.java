package com.quxiangshe.feed.util;

import com.quxiangshe.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Feed流专用Redis工具类
 * 提供收件箱、点赞、收藏等Feed相关操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedRedisUtil {

    private final RedisUtil redisUtil;

    /**
     * 收件箱Key前缀
     */
    private static final String INBOX_KEY_PREFIX = "feed:inbox:";

    /**
     * 时间线Key前缀
     */
    private static final String TIMELINE_KEY_PREFIX = "feed:timeline:";

    /**
     * 动态点赞Key前缀
     */
    private static final String POST_LIKE_KEY_PREFIX = "post:like:";

    /**
     * 用户动态Key前缀
     */
    private static final String USER_POST_KEY_PREFIX = "post:user:";

    /**
     * 收件箱最大容量
     */
    @Value("${feed.inbox-size:200}")
    private int inboxSize;

    /**
     * 收件箱过期时间（天）
     */
    @Value("${feed.inbox-ttl:7}")
    private int inboxTtl;

    // ==================== 收件箱操作 ====================

    /**
     * 添加动态到用户收件箱
     * 使用LPUSH，从左侧推入（最新的在前面）
     *
     * @param userId 用户ID
     * @param postId 动态ID
     */
    public void pushToInbox(Long userId, Long postId) {
        String key = INBOX_KEY_PREFIX + userId;
        try {
            // 从左侧推入
            redisUtil.lPush(key, postId.toString());
            // 设置过期时间
            redisUtil.expire(key, inboxTtl * 24L * 3600L, TimeUnit.SECONDS);
            // 限制收件箱大小
            trimInbox(key);
            log.debug("添加动态到收件箱: userId={}, postId={}", userId, postId);
        } catch (Exception e) {
            log.error("添加动态到收件箱失败: userId={}, postId={}", userId, postId, e);
        }
    }

    /**
     * 从收件箱获取动态列表
     *
     * @param userId   用户ID
     * @param offset   偏移量
     * @param limit    数量
     * @return 动态ID列表
     */
    public List<Long> getInbox(Long userId, int offset, int limit) {
        String key = INBOX_KEY_PREFIX + userId;
        List<Object> result = redisUtil.lRange(key, offset, offset + limit - 1);
        if (result == null || result.isEmpty()) {
            return Collections.emptyList();
        }
        return result.stream()
                .map(obj -> Long.parseLong(obj.toString()))
                .collect(Collectors.toList());
    }

    /**
     * 获取收件箱大小
     *
     * @param userId 用户ID
     * @return 收件箱大小
     */
    public Long getInboxSize(Long userId) {
        String key = INBOX_KEY_PREFIX + userId;
        return redisUtil.lSize(key);
    }

    /**
     * 检查收件箱是否存在数据
     *
     * @param userId 用户ID
     * @return true表示存在
     */
    public Boolean hasInboxData(Long userId) {
        String key = INBOX_KEY_PREFIX + userId;
        Long size = redisUtil.lSize(key);
        return size != null && size > 0;
    }

    /**
     * 限制收件箱大小
     * 超过容量时删除旧数据
     */
    private void trimInbox(String key) {
        Long size = redisUtil.lSize(key);
        if (size != null && size > inboxSize) {
            redisUtil.lTrim(key, 0, inboxSize - 1);
        }
    }

    // ==================== 时间线操作 ====================

    /**
     * 添加动态到用户时间线
     * 使用ZSet，按创建时间排序
     *
     * @param userId    用户ID
     * @param postId    动态ID
     * @param timestamp 时间戳（秒）
     */
    public void addToTimeline(Long userId, Long postId, long timestamp) {
        String key = TIMELINE_KEY_PREFIX + userId;
        try {
            // 添加到ZSet，score为时间戳（用于排序）
            redisUtil.zAdd(key, postId.toString(), timestamp);
            // 设置过期时间
            redisUtil.expire(key, inboxTtl * 24L * 3600L, TimeUnit.SECONDS);
            log.debug("添加动态到时间线: userId={}, postId={}, timestamp={}", userId, postId, timestamp);
        } catch (Exception e) {
            log.error("添加动态到时间线失败: userId={}, postId={}", userId, postId, e);
        }
    }

    /**
     * 从时间线获取动态列表
     * 按时间倒序
     *
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit  数量
     * @return 动态ID列表
     */
    public List<Long> getTimeline(Long userId, int offset, int limit) {
        String key = TIMELINE_KEY_PREFIX + userId;
        Set<Object> result = redisUtil.zReverseRange(key, offset, offset + limit - 1);
        if (result == null || result.isEmpty()) {
            return Collections.emptyList();
        }
        return result.stream()
                .map(obj -> Long.parseLong(obj.toString()))
                .collect(Collectors.toList());
    }

    /**
     * 获取时间线大小
     */
    public Long getTimelineSize(Long userId) {
        String key = TIMELINE_KEY_PREFIX + userId;
        return redisUtil.zSize(key);
    }

    // ==================== 动态点赞操作 ====================

    /**
     * 点赞动态
     *
     * @param postId 动态ID
     * @param userId 用户ID
     */
    public void likePost(Long postId, Long userId) {
        String key = POST_LIKE_KEY_PREFIX + postId;
        redisUtil.sAdd(key, userId.toString());
        redisUtil.expire(key, 30 * 60L, TimeUnit.SECONDS); // 30分钟过期
    }

    /**
     * 取消点赞
     *
     * @param postId 动态ID
     * @param userId 用户ID
     */
    public void unlikePost(Long postId, Long userId) {
        String key = POST_LIKE_KEY_PREFIX + postId;
        redisUtil.sRemove(key, userId.toString());
    }

    /**
     * 检查是否点赞
     *
     * @param postId 动态ID
     * @param userId 用户ID
     * @return true表示已点赞
     */
    public Boolean isLiked(Long postId, Long userId) {
        String key = POST_LIKE_KEY_PREFIX + postId;
        return redisUtil.sIsMember(key, userId.toString());
    }

    /**
     * 获取动态点赞数
     *
     * @param postId 动态ID
     * @return 点赞数
     */
    public Long getLikeCount(Long postId) {
        String key = POST_LIKE_KEY_PREFIX + postId;
        return redisUtil.sSize(key);
    }
}

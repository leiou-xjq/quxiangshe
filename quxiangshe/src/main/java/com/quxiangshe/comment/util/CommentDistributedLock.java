package com.quxiangshe.comment.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 评论分布式锁工具类
 * 用于保证并发场景下的数据一致性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentDistributedLock {

    /**
     * StringRedisTemplate（用于分布式锁）
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 分布式锁Key前缀
     */
    private static final String LOCK_KEY_PREFIX = "comment:lock:";

    /**
     * 默认锁超时时间（秒）
     */
    private static final long DEFAULT_LOCK_EXPIRE = 10;

    /**
     * 获取分布式锁
     * 使用SET NX EX实现原子性加锁
     *
     * @param lockKey 锁的Key
     * @return true表示获取锁成功
     */
    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_LOCK_EXPIRE, TimeUnit.SECONDS);
    }

    /**
     * 获取分布式锁
     *
     * @param lockKey 锁的Key
     * @param expire 过期时间
     * @param unit 时间单位
     * @return true表示获取锁成功
     */
    public boolean tryLock(String lockKey, long expire, TimeUnit unit) {
        String key = LOCK_KEY_PREFIX + lockKey;
        try {
            // 使用setIfAbsent实现NX功能
            Boolean result = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, "1", expire, unit);
            return result != null && result;
        } catch (Exception e) {
            log.error("获取分布式锁失败: lockKey={}", lockKey, e);
            return false;
        }
    }

    /**
     * 释放分布式锁
     *
     * @param lockKey 锁的Key
     */
    public void unlock(String lockKey) {
        String key = LOCK_KEY_PREFIX + lockKey;
        stringRedisTemplate.delete(key);
        log.debug("释放分布式锁: lockKey={}", lockKey);
    }

    /**
     * 获取评论数更新锁
     * 用于保证并发更新评论数的一致性
     *
     * @param articleId 文章ID
     * @return true表示获取锁成功
     */
    public boolean tryLockCommentCount(Long articleId) {
        return tryLock("count:" + articleId);
    }

    /**
     * 获取点赞数更新锁
     *
     * @param commentId 评论ID
     * @return true表示获取锁成功
     */
    public boolean tryLockLikeCount(Long commentId) {
        return tryLock("like:" + commentId);
    }
}

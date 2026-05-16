package com.quxiangshe.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis滑动窗口限流器
 * 使用Lua脚本实现原子操作
 * 支持分布式锁，防止多实例并发
 */
@Slf4j
@Component
public class SlidingWindowRateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private RedissonClient redissonClient;

    /**
     * 滑动窗口限流Lua脚本 - 原子操作
     * 1. 删除过期记录
     * 2. 计数
     * 3. 判断并添加
     */
    private static final String SLIDING_WINDOW_SCRIPT =
            "local key = KEYS[1] " +
            "local now = tonumber(ARGV[1]) " +
            "local window = tonumber(ARGV[2]) " +
            "local limit = tonumber(ARGV[3]) " +
            "local windowStart = now - window " +
            "redis.call('ZREMRANGEBYSCORE', key, 0, windowStart) " +
            "local count = redis.call('ZCARD', key) " +
            "if count < limit then " +
            "  redis.call('ZADD', key, now, now .. ':' .. math.random()) " +
            "  redis.call('EXPIRE', key, math.ceil(window / 1000) + 1) " +
            "  return 1 " +
            "end " +
            "return 0";

    public SlidingWindowRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试获取令牌
     * 使用Lua脚本保证原子性
     *
     * @param key 限流key
     * @param maxRequests 最大请求数
     * @param windowSeconds 时间窗口（秒）
     * @return true-允许请求，false-被限流
     */
    public boolean tryAcquire(String key, int maxRequests, int windowSeconds) {
        String fullKey = getFullKey(key);
        long nowMillis = System.currentTimeMillis();
        long windowMillis = windowSeconds * 1000L;

        try {
            Long result = redisTemplate.execute(
                    RedisScript.of(SLIDING_WINDOW_SCRIPT, Long.class),
                    List.of(fullKey),
                    String.valueOf(nowMillis),
                    String.valueOf(windowMillis),
                    String.valueOf(maxRequests)
            );

            boolean allowed = result != null && result == 1;
            if (!allowed) {
                log.debug("滑动窗口限流触发: key={}, maxRequests={}, windowSeconds={}",
                        key, maxRequests, windowSeconds);
            } else {
                log.debug("滑动窗口限流通过: key={}, maxRequests={}, windowSeconds={}",
                        key, maxRequests, windowSeconds);
            }
            return allowed;
        } catch (Exception e) {
            log.warn("滑动窗口限流异常: key={}, error={}", key, e.getMessage());
            return true;
        }
    }

    /**
     * 获取分布式锁（带超时）
     */
    private RLock getLock(String key) {
        if (redissonClient == null) {
            return null;
        }
        return redissonClient.getLock("lock:" + key);
    }

    /**
     * 获取当前窗口内的请求数
     */
    public Long getCurrentCount(String key, int windowSeconds) {
        String fullKey = getFullKey(key);
        long now = System.currentTimeMillis();
        long windowStart = now - windowSeconds * 1000L;

        try {
            redisTemplate.opsForZSet().removeRangeByScore(fullKey, 0, windowStart);
            Long count = redisTemplate.opsForZSet().zCard(fullKey);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("获取限流计数失败: key={}, error={}", key, e.getMessage());
            return 0L;
        }
    }

    /**
     * 重置限流计数
     */
    public void reset(String key) {
        String fullKey = getFullKey(key);
        try {
            redisTemplate.delete(fullKey);
        } catch (Exception e) {
            log.warn("重置限流计数失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 获取完整的Redis key
     */
    private String getFullKey(String key) {
        return "rate_limit:sliding:" + key;
    }
}

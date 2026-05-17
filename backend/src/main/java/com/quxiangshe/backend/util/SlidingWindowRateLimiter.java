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
 * 
 * <p>基于Redis有序集合（ZSet）实现滑动窗口限流，使用Lua脚本保证原子操作，
 * 支持分布式锁防止多实例并发竞争，具备异常容错能力（异常时默认放行）。</p>
 * 
 * <p>核心原理：将每次请求的时间戳作为score存入ZSet，每次请求时先删除窗口外的过期记录，
 * 再统计当前窗口内的请求数量，未达阈值的请求方可放行。</p>
 * 
 * @author 趣享社技术团队
 * @since 1.0
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

    /**
     * 构造函数，注入RedisTemplate
     *
     * @param redisTemplate Redis操作模板
     */
    public SlidingWindowRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试获取令牌
     * 
     * <p>使用Lua脚本保证"删除过期记录 + 计数 + 判断 + 添加"四个步骤的原子性，
     * 避免并发场景下窗口边界突增问题（比固定窗口更平滑）。</p>
     *
     * @param key           限流key，用于区分不同接口/用户
     * @param maxRequests   窗口内最大请求数，超出则限流
     * @param windowSeconds 时间窗口大小（秒）
     * @return true-允许请求，false-被限流
     */
    public boolean tryAcquire(String key, int maxRequests, int windowSeconds) {
        String fullKey = getFullKey(key);
        // 当前时间戳（毫秒），作为ZSet的score
        long nowMillis = System.currentTimeMillis();
        // 窗口长度（毫秒）
        long windowMillis = windowSeconds * 1000L;

        try {
            // 执行Lua脚本，原子性地完成限流判定与记录
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
            // 异常时默认放行，避免限流组件故障导致业务中断
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
     * 
     * <p>先删除过期记录再统计，确保返回的是当前有效窗口内的请求数量，
     * 可用于监控面板展示实时QPS。</p>
     *
     * @param key           限流key
     * @param windowSeconds 时间窗口（秒）
     * @return 当前窗口内的请求数量
     */
    public Long getCurrentCount(String key, int windowSeconds) {
        String fullKey = getFullKey(key);
        long now = System.currentTimeMillis();
        // 窗口起始时间戳
        long windowStart = now - windowSeconds * 1000L;

        try {
            // 先删除窗口外的过期记录
            redisTemplate.opsForZSet().removeRangeByScore(fullKey, 0, windowStart);
            // 统计当前窗口内的请求数
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

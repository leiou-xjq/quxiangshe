package com.quxiangshe.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis滑动窗口限流器
 * 使用Redis ZSet实现标准滑动窗口限流
 * 支持分布式锁，防止多实例并发
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Component
public class SlidingWindowRateLimiter {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Autowired(required = false)
    private RedissonClient redissonClient;
    
    public SlidingWindowRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 尝试获取令牌
     * 使用分布式锁保证多实例安全
     * 
     * @param key 限流key
     * @param maxRequests 最大请求数
     * @param windowSeconds 时间窗口（秒）
     * @return true-允许请求，false-被限流
     */
    public boolean tryAcquire(String key, int maxRequests, int windowSeconds) {
        String fullKey = getFullKey(key);
        
        // 获取分布式锁
        RLock lock = getLock(fullKey);
        if (lock != null) {
            lock.lock();
        }
        
        try {
            long now = Instant.now().toEpochMilli();
            long windowStart = now - windowSeconds * 1000L;
            
            // 移除窗口外的请求记录
            ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
            zSetOps.removeRangeByScore(fullKey, 0, windowStart);
            
            // 获取当前窗口内的请求数
            Long currentCount = zSetOps.zCard(fullKey);
            
            // 检查是否超过限制
            if (currentCount != null && currentCount >= maxRequests) {
                log.debug("滑动窗口限流触发: key={}, currentCount={}, maxRequests={}", 
                        fullKey, currentCount, maxRequests);
                return false;
            }
            
            // 添加当前请求记录
            String uniqueMember = now + ":" + Math.random();
            zSetOps.add(fullKey, uniqueMember, now);
            
            // 设置过期时间（窗口时间 + 1秒缓冲）
            redisTemplate.expire(fullKey, windowSeconds + 1, TimeUnit.SECONDS);
            
            log.debug("滑动窗口限流检查: key={}, currentCount={}, maxRequests={}, allowed=true", 
                    fullKey, currentCount, maxRequests);
            
            return true;
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    /**
     * 获取分布式锁
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
     * @param key 限流key
     * @param windowSeconds 时间窗口（秒）
     * @return 当前请求数
     */
    public Long getCurrentCount(String key, int windowSeconds) {
        String fullKey = getFullKey(key);
        long now = Instant.now().toEpochMilli();
        long windowStart = now - windowSeconds * 1000L;
        
        // 移除窗口外的请求记录
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        zSetOps.removeRangeByScore(fullKey, 0, windowStart);
        
        Long count = zSetOps.zCard(fullKey);
        return count != null ? count : 0L;
    }
    
    /**
     * 重置限流计数
     * 
     * @param key 限流key
     */
    public void reset(String key) {
        String fullKey = getFullKey(key);
        redisTemplate.delete(fullKey);
    }
    
    /**
     * 获取完整的Redis key
     */
    private String getFullKey(String key) {
        return "rate_limit:sliding:" + key;
    }
}
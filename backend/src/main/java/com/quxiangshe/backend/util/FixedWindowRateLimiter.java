package com.quxiangshe.backend.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis固定窗口限流器
 * 使用Redis INCR实现固定窗口限流
 * 支持分布式锁，防止多实例并发
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Component
public class FixedWindowRateLimiter {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Autowired(required = false)
    private RedissonClient redissonClient;
    
    public FixedWindowRateLimiter(RedisTemplate<String, Object> redisTemplate) {
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
            // 增加计数器
            Long count = redisTemplate.opsForValue().increment(fullKey);
            
            // 首次设置过期时间
            if (count != null && count == 1) {
                redisTemplate.expire(fullKey, windowSeconds, TimeUnit.SECONDS);
            }
            
            // 检查是否超过限制
            boolean allowed = count != null && count <= maxRequests;
            
            log.debug("FixedWindow限流检查: key={}, count={}, maxRequests={}, allowed={}", 
                    fullKey, count, maxRequests, allowed);
            
            return allowed;
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
     * @return 当前请求数
     */
    public Long getCurrentCount(String key) {
        String fullKey = getFullKey(key);
        Object value = redisTemplate.opsForValue().get(fullKey);
        return value != null ? Long.parseLong(value.toString()) : 0L;
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
        return "rate_limit:fixed:" + key;
    }
}
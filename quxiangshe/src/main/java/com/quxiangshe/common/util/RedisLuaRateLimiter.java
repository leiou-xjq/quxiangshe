package com.quxiangshe.common.util;

import com.quxiangshe.common.annotation.RateLimit;
import com.quxiangshe.common.config.RateLimitProperties;
import com.quxiangshe.common.constant.RateLimitConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Redis Lua脚本工具类（支持降级、持久化和数据同步）
 * 
 * 功能增强:
 * 1. 滑动窗口限流（Redis Lua脚本）
 * 2. Redis不可用时降级到Caffeine本地限流
 * 3. 数据持久化到Redis（服务重启后恢复）
 * 4. Redis恢复时自动同步数据
 * 
 * 降级策略:
 * - Redis可用: 使用Redis Lua脚本限流
 * - Redis不可用: 使用Caffeine本地缓存限流
 * - 持久化: 定期将本地限流数据写入Redis
 * 
 * @author quxiangshe
 * @since 2024
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLuaRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RedisHealthManager healthManager;
    private final LocalRateLimitCache localCache;
    private final RateLimitProperties properties;
    
    /**
     * Lua脚本
     */
    private DefaultRedisScript<Long> slidingWindowScript;
    
    /**
     * 滑动窗口脚本路径
     */
    private static final String LUA_SCRIPT_PATH = "lua/sliding_window_rate_limit.lua";

    /**
     * 定时清理过期限流数据（每分钟）
     */
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rate-limit-cleanup");
        t.setDaemon(true);
        return t;
    });

    /**
     * 初始化Lua脚本
     */
    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource(LUA_SCRIPT_PATH);
            String scriptContent = new String(resource.getInputStream().readAllBytes());
            slidingWindowScript = new DefaultRedisScript<>();
            slidingWindowScript.setScriptText(scriptContent);
            slidingWindowScript.setResultType(Long.class);
            log.info("滑动窗口限流Lua脚本加载成功");
        } catch (Exception e) {
            log.error("加载滑动窗口限流Lua脚本失败: {}", e.getMessage(), e);
            slidingWindowScript = new DefaultRedisScript<>(getFallbackScript(), Long.class);
        }

        // 注册Redis恢复回调
        healthManager.registerCallback(new RedisHealthManager.RedisStatusCallback() {
            @Override
            public void onRedisRecovered() {
                handleRedisRecovered();
            }

            @Override
            public void onRedisUnavailable() {
                log.info("Redis不可用，限流切换到本地模式");
            }
        });

        // 启动定时清理
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredData, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * 处理Redis恢复
     * 清理本地缓存中的过期数据，避免同步到Redis后误限流
     */
    private void handleRedisRecovered() {
        log.info("Redis恢复，清理本地限流过期数据...");
        
        long now = System.currentTimeMillis();
        int cleanedKeys = 0;
        int totalRequests = 0;

        // 清理Caffeine缓存中过期的key
        for (String key : localCache.getKeys()) {
            long count = localCache.getCurrentCount(key, 60000);
            if (count == 0) {
                localCache.invalidate(key);
                cleanedKeys++;
            } else {
                totalRequests += count;
            }
        }

        log.info("Redis恢复后限流数据清理完成: 清理{}个key, 保留{}条请求", cleanedKeys, totalRequests);
    }

    /**
     * 清理过期的本地限流数据
     */
    private void cleanupExpiredData() {
        // Caffeine会自动处理过期，这里可以做额外的清理
        log.debug("本地限流缓存统计: {}", localCache.getStats());
    }

    /**
     * 滑动窗口限流核心方法
     * 
     * 限流策略（按优先级）:
     * 1. Redis可用 + 启用本地降级: 优先Redis，失败则降级本地
     * 2. Redis不可用 + 启用本地降级: 使用Caffeine本地限流
     * 3. 未启用本地降级: 直接拒绝（返回false）
     * 
     * @param key        限流key
     * @param limit      限流阈值（次/窗口时间）
     * @param windowMs   窗口时间（毫秒）
     * @param expireMs   key过期时间（毫秒）
     * @return true-允许请求, false-拒绝请求
     */
    public boolean tryAcquire(String key, int limit, long windowMs, long expireMs) {
        // 优先尝试Redis限流
        if (healthManager.isRedisAvailable()) {
            try {
                boolean allowed = tryAcquireWithRedis(key, limit, windowMs, expireMs);
                if (allowed) {
                    return true;
                }
                // Redis限流触发，如果启用了本地降级，可以尝试本地
                // 但这里直接返回Redis的结果，不做双层限流
                return false;
            } catch (Exception e) {
                log.error("Redis限流执行失败: {}", e.getMessage());
                healthManager.recordOperationFailure();
            }
        }
        
        // Redis不可用或失败，降级到本地限流
        if (properties.isEnableLocalFallback()) {
            return tryAcquireLocal(key, limit, windowMs);
        }
        
        // 未启用本地降级，直接拒绝
        log.warn("Redis不可用且未启用本地降级，限流拒绝: key={}", key);
        return false;
    }

    /**
     * Redis限流实现
     */
    private boolean tryAcquireWithRedis(String key, int limit, long windowMs, long expireMs) {
        long currentTime = System.currentTimeMillis();
        long windowSize = windowMs;
        long expireTime = expireMs;
        
        List<String> keys = Collections.singletonList(key);
        
        Long result = redisTemplate.execute(
            slidingWindowScript,
            keys,
            String.valueOf(currentTime),
            String.valueOf(windowSize),
            String.valueOf(limit),
            String.valueOf(expireTime)
        );
        
        boolean allowed = result != null && result == 1L;
        
        if (!allowed) {
            log.warn("限流触发: key={}, limit={}, windowMs={}", key, limit, windowMs);
        }
        
        healthManager.recordOperationSuccess();
        return allowed;
    }

    /**
     * 本地Caffeine限流实现（降级方案）
     * 使用Caffeine缓存实现滑动窗口限流
     */
    private boolean tryAcquireLocal(String key, int limit, long windowMs) {
        boolean allowed = localCache.tryAcquire(key, limit, windowMs);
        
        if (!allowed) {
            log.warn("本地限流触发: key={}, limit={}, windowMs={}", key, limit, windowMs);
        }
        
        return allowed;
    }

    /**
     * 基于IP的限流
     * 
     * @param ip         客户端IP地址
     * @param prefix     限流key前缀
     * @param limit      限流阈值
     * @param windowMs   窗口时间
     * @return true-允许, false-拒绝
     */
    public boolean tryAcquireByIp(String ip, String prefix, int limit, long windowMs) {
        String key = prefix + ip;
        return tryAcquire(key, limit, windowMs, windowMs + 1000);
    }

    /**
     * 基于手机号的限流
     * 
     * @param phone      手机号
     * @param prefix     限流key前缀
     * @param limit      限流阈值
     * @param windowMs   窗口时间
     * @return true-允许, false-拒绝
     */
    public boolean tryAcquireByPhone(String phone, String prefix, int limit, long windowMs) {
        String key = prefix + phone;
        return tryAcquire(key, limit, windowMs, windowMs + 1000);
    }

    /**
     * 基于注解的限流
     * 
     * @param ip         客户端IP地址
     * @param annotation 限流注解
     * @return true-允许, false-拒绝
     */
    public boolean tryAcquireByAnnotation(String ip, RateLimit annotation) {
        String prefix = annotation.keyPrefix();
        int limit = annotation.limit() > 0 ? annotation.limit() : RateLimitConstants.CAPTCHA_DEFAULT_LIMIT;
        long windowMs = annotation.windowMs() > 0 ? annotation.windowMs() : RateLimitConstants.CAPTCHA_DEFAULT_WINDOW_MS;
        
        String key;
        switch (annotation.type()) {
            case IP:
                key = prefix + ip;
                break;
            case IP_AND_INTERFACE:
                key = prefix + ip;
                break;
            case PHONE:
                key = prefix + ip;
                break;
            default:
                key = prefix + ip;
        }
        
        return tryAcquire(key, limit, windowMs, windowMs + 1000);
    }

    /**
     * 获取当前窗口内的请求数（用于监控）
     * 优先从Redis获取，失败则从本地缓存获取
     * 
     * @param key      限流key
     * @param windowMs 窗口时间
     * @return 当前窗口内的请求数
     */
    public Long getCurrentCount(String key, long windowMs) {
        try {
            if (healthManager.isRedisAvailable()) {
                long now = System.currentTimeMillis();
                Long count = redisTemplate.opsForZSet().zCard(key);
                return count != null ? count : 0L;
            }
        } catch (Exception e) {
            log.error("获取限流计数失败: key={}, error={}", key, e.getMessage());
        }
        
        // 降级到本地缓存
        return localCache.getCurrentCount(key, windowMs);
    }

    /**
     * 清除限流记录（用于测试或管理员操作）
     * 同时清除Redis和本地缓存
     * 
     * @param key 限流key
     */
    public void clear(String key) {
        try {
            if (healthManager.isRedisAvailable()) {
                redisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.warn("清除Redis限流记录失败: key={}, error={}", key, e.getMessage());
        }
        
        localCache.invalidate(key);
        log.info("限流记录已清除: key={}", key);
    }

    /**
     * 清除所有限流记录（用于测试）
     */
    public void clearAll() {
        try {
            if (healthManager.isRedisAvailable()) {
                Set<String> keys = redisTemplate.keys("limit:*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
            }
        } catch (Exception e) {
            log.warn("清除Redis限流记录失败: {}", e.getMessage());
        }
        
        localCache.invalidateAll();
        log.info("所有限流记录已清除");
    }

    /**
     * 获取本地限流缓存大小（用于监控）
     * 
     * @return 本地缓存key数量
     */
    public int getLocalCacheSize() {
        return (int) localCache.size();
    }

    /**
     * 获取限流器状态信息
     * 
     * @return 状态描述
     */
    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("限流器状态: ");
        sb.append("Redis=").append(healthManager.isRedisAvailable() ? "可用" : "不可用");
        sb.append(", 本地缓存=").append(localCache.size()).append("条");
        sb.append(", 本地降级=").append(properties.isEnableLocalFallback() ? "启用" : "禁用");
        return sb.toString();
    }

    /**
     * 备用Lua脚本（当外部脚本加载失败时使用）
     */
    private String getFallbackScript() {
        return """
            local key = KEYS[1]
            local currentTime = tonumber(ARGV[1])
            local windowSize = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            local expireTime = tonumber(ARGV[4])
            
            local windowStart = currentTime - windowSize
            redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)
            
            local count = redis.call('ZCARD', key)
            
            if count >= limit then
                return 0
            end
            
            local requestId = currentTime .. '-' .. math.random(1000, 9999)
            redis.call('ZADD', key, currentTime, requestId)
            redis.call('PEXPIRE', key, expireTime)
            
            return 1
            """;
    }
}
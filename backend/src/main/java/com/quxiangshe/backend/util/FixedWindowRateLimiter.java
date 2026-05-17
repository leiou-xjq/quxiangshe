package com.quxiangshe.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis固定窗口限流器
 * 
 * <p>基于Redis String类型的INCR命令实现固定窗口限流，使用Lua脚本保证"递增 + 设过期 + 判断"
 * 操作的原子性。相比滑动窗口实现更简单、性能更高，但在窗口边界处可能存在突增问题。</p>
 * 
 * <p>适用场景：对流量精度要求不高的批量操作接口（如后台管理），或在测试环境中使用。</p>
 * 
 * @author 趣享社技术团队
 * @since 1.0
 */
@Slf4j
@Component
public class FixedWindowRateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private RedissonClient redissonClient;

    /**
     * 固定窗口限流Lua脚本 - 原子操作
     * 1. INCR计数
     * 2. 判断是否首次，若是则设置过期时间
     */
    private static final String FIXED_WINDOW_SCRIPT =
            "local key = KEYS[1] " +
            "local limit = tonumber(ARGV[1]) " +
            "local window = tonumber(ARGV[2]) " +
            "local count = redis.call('INCR', key) " +
            "if count == 1 then " +
            "  redis.call('EXPIRE', key, window) " +
            "end " +
            "if count <= limit then " +
            "  return 1 " +
            "end " +
            "return 0";

    /**
     * 构造函数，注入RedisTemplate
     *
     * @param redisTemplate Redis操作模板
     */
    public FixedWindowRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试获取令牌
     * 
     * <p>使用Lua脚本原子性地完成：INCR递增计数器 → 首次访问时设置过期时间 → 
     * 判断当前计数是否超过阈值。Lua脚本内完成所有操作，避免网络往返带来的竞态。</p>
     *
     * @param key           限流key
     * @param maxRequests   窗口内最大请求数
     * @param windowSeconds 时间窗口大小（秒）
     * @return true-允许请求，false-被限流
     */
    public boolean tryAcquire(String key, int maxRequests, int windowSeconds) {
        String fullKey = getFullKey(key);

        try {
            // 执行Lua脚本：原子递增并判断是否超限
            Long result = redisTemplate.execute(
                    RedisScript.of(FIXED_WINDOW_SCRIPT, Long.class),
                    Collections.singletonList(fullKey),
                    String.valueOf(maxRequests),
                    String.valueOf(windowSeconds)
            );

            boolean allowed = result != null && result == 1;
            if (!allowed) {
                log.debug("固定窗口限流触发: key={}, maxRequests={}, windowSeconds={}",
                        key, maxRequests, windowSeconds);
            }
            return allowed;
        } catch (Exception e) {
            // 异常时默认放行，避免限流组件异常导致服务不可用
            log.warn("固定窗口限流异常: key={}, error={}", key, e.getMessage());
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
     * <p>直接读取Redis中的计数器当前值。固定窗口到期后key自动过期，
     * 因此读到0表示上一窗口已结束。</p>
     *
     * @param key 限流key
     * @return 当前窗口内的请求数量，key不存在时返回0
     */
    public Long getCurrentCount(String key) {
        String fullKey = getFullKey(key);
        try {
            // 读取Redis中的计数器值
            Object value = redisTemplate.opsForValue().get(fullKey);
            return value != null ? Long.parseLong(value.toString()) : 0L;
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
        return "rate_limit:fixed:" + key;
    }
}

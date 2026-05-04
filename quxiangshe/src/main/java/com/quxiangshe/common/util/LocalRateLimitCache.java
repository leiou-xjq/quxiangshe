package com.quxiangshe.common.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.quxiangshe.common.config.RateLimitProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 本地限流缓存（基于Caffeine）
 * 
 * 功能:
 * 1. 作为Redis不可用时的降级兜底
 * 2. 支持滑动窗口限流算法
 * 3. 数据持久化备份（服务重启时恢复）
 * 4. 自动过期清理
 * 
 * 数据结构:
 * - Key: 限流key（如 limit:captcha:127.0.0.1）
 * - Value: 请求时间戳列表（SortedSet，按时间排序）
 * 
 * @author quxiangshe
 * @since 2024
 */
@Slf4j
@Component
public class LocalRateLimitCache {

    private final RateLimitProperties properties;
    
    /**
     * 本地限流缓存
     * Key: 限流key
     * Value: 请求时间戳列表（按时间升序排列）
     */
    private Cache<String, List<Long>> rateLimitCache;

    /**
     * 缓存Key的过期时间映射
     * 用于跟踪每个key的过期时间
     */
    private final Map<String, Long> keyExpirationTime = new java.util.concurrent.ConcurrentHashMap<>();

    public LocalRateLimitCache(RateLimitProperties properties) {
        this.properties = properties;
    }

    /**
     * 初始化Caffeine缓存
     */
    @PostConstruct
    public void init() {
        rateLimitCache = Caffeine.newBuilder()
                .maximumSize(properties.getLocalCacheMaxSize())
                .expireAfterAccess(properties.getLocalCacheExpireMinutes(), TimeUnit.MINUTES)
                .recordStats()
                .build();

        log.info("本地限流缓存初始化完成: maxSize={}, expireMinutes={}", 
                properties.getLocalCacheMaxSize(), 
                properties.getLocalCacheExpireMinutes());
    }

    /**
     * 尝试获取限流令牌
     * 
     * 滑动窗口算法:
     * 1. 获取当前时间窗口的请求列表
     * 2. 移除窗口外的请求
     * 3. 检查是否超过限制
     * 4. 添加新请求到窗口
     * 
     * @param key 限流key
     * @param limit 限流阈值
     * @param windowMs 窗口时间（毫秒）
     * @return true-允许, false-拒绝
     */
    public boolean tryAcquire(String key, int limit, long windowMs) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowMs;

        // 获取或创建缓存条目
        List<Long> timestamps = rateLimitCache.get(key, k -> new ArrayList<>());
        
        // 使用同步块保证线程安全
        synchronized (timestamps) {
            // 清理窗口外的请求
            timestamps.removeIf(ts -> ts < windowStart);

            // 检查是否超限
            if (timestamps.size() >= limit) {
                log.debug("本地限流触发: key={}, count={}, limit={}", key, timestamps.size(), limit);
                return false;
            }

            // 添加新请求时间戳
            timestamps.add(now);
            
            // 更新过期时间
            keyExpirationTime.put(key, now + windowMs + 60000); // 多保留1分钟缓冲

            return true;
        }
    }

    /**
     * 获取当前窗口内的请求数
     * 
     * @param key 限流key
     * @param windowMs 窗口时间
     * @return 请求数
     */
    public long getCurrentCount(String key, long windowMs) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowMs;

        List<Long> timestamps = rateLimitCache.getIfPresent(key);
        if (timestamps == null || timestamps.isEmpty()) {
            return 0;
        }

        // 过滤出窗口内的请求
        return timestamps.stream().filter(ts -> ts >= windowStart).count();
    }

    /**
     * 检查key是否存在
     * 
     * @param key 限流key
     * @return true-存在, false-不存在
     */
    public boolean containsKey(String key) {
        return rateLimitCache.getIfPresent(key) != null;
    }

    /**
     * 清除指定key的限流记录
     * 
     * @param key 限流key
     */
    public void invalidate(String key) {
        rateLimitCache.invalidate(key);
        keyExpirationTime.remove(key);
        log.debug("本地限流记录已清除: key={}", key);
    }

    /**
     * 清除所有限流记录
     */
    public void invalidateAll() {
        rateLimitCache.invalidateAll();
        keyExpirationTime.clear();
        log.info("本地限流记录已全部清除");
    }

    /**
     * 获取缓存大小
     * 
     * @return 当前缓存条目数
     */
    public long size() {
        return rateLimitCache.estimatedSize();
    }

    /**
     * 获取缓存统计信息
     * 
     * @return 统计信息描述
     */
    public String getStats() {
        var stats = rateLimitCache.stats();
        return String.format("hits=%d, misses=%d, evictions=%d, size=%d",
                stats.hitCount(), stats.missCount(), stats.evictionCount(), size());
    }

    /**
     * 获取所有缓存Key
     * 
     * @return Key集合
     */
    public Set<String> getKeys() {
        return rateLimitCache.asMap().keySet();
    }

    /**
     * 导出所有活跃的限流数据（用于持久化）
     * 
     * @param windowMs 窗口时间（用于计算是否过期）
     * @return 限流数据列表
     */
    public List<RateLimitData> exportActiveData(long windowMs) {
        List<RateLimitData> result = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Map.Entry<String, List<Long>> entry : rateLimitCache.asMap().entrySet()) {
            String key = entry.getKey();
            List<Long> timestamps = entry.getValue();
            
            if (timestamps == null || timestamps.isEmpty()) {
                continue;
            }

            // 过滤窗口内的请求
            long windowStart = now - windowMs;
            List<Long> validTimestamps = new ArrayList<>();
            for (Long ts : timestamps) {
                if (ts >= windowStart) {
                    validTimestamps.add(ts);
                }
            }

            if (!validTimestamps.isEmpty()) {
                Long oldest = validTimestamps.stream().min(Long::compareTo).orElse(now);
                long remainingMs = (oldest + windowMs) - now;
                result.add(new RateLimitData(key, validTimestamps, Math.max(remainingMs, 0)));
            }
        }

        return result;
    }

    /**
     * 导入限流数据（用于服务重启后恢复）
     * 
     * @param dataList 限流数据列表
     */
    public void importData(List<RateLimitData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        for (RateLimitData data : dataList) {
            try {
                if (data.getTimestamps() == null || data.getTimestamps().isEmpty()) {
                    continue;
                }

                // 过滤未过期的请求
                long now = System.currentTimeMillis();
                List<Long> validTimestamps = new ArrayList<>();
                for (Long ts : data.getTimestamps()) {
                    if (ts > now - data.getWindowMs()) {
                        validTimestamps.add(ts);
                    }
                }

                if (!validTimestamps.isEmpty()) {
                    rateLimitCache.put(data.getKey(), validTimestamps);
                    keyExpirationTime.put(data.getKey(), now + data.getRemainingMs());
                }
            } catch (Exception e) {
                log.warn("导入限流数据失败: key={}, error={}", data.getKey(), e.getMessage());
            }
        }

        log.info("本地限流数据导入完成: {}条", dataList.size());
    }

    /**
     * 限流数据结构
     */
    public static class RateLimitData {
        private String key;
        private List<Long> timestamps;
        private long windowMs;
        private long remainingMs;

        public RateLimitData() {}

        public RateLimitData(String key, List<Long> timestamps, long remainingMs) {
            this.key = key;
            this.timestamps = timestamps;
            this.remainingMs = remainingMs;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public List<Long> getTimestamps() {
            return timestamps;
        }

        public void setTimestamps(List<Long> timestamps) {
            this.timestamps = timestamps;
        }

        public long getWindowMs() {
            return windowMs;
        }

        public void setWindowMs(long windowMs) {
            this.windowMs = windowMs;
        }

        public long getRemainingMs() {
            return remainingMs;
        }

        public void setRemainingMs(long remainingMs) {
            this.remainingMs = remainingMs;
        }
    }
}

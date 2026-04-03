package com.quxiangshe.common.util;

import com.quxiangshe.common.util.RedisHealthManager.RedisStatusCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * IP黑名单工具类（支持Redis降级和数据同步）
 * 优先使用Redis存储，黑名单数据双写（Redis + 本地缓存）
 * 当Redis不可用时，自动降级到本地缓存继续提供防护
 * Redis恢复时，自动同步本地缓存数据到Redis
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistUtil {

    private final StringRedisTemplate redisTemplate;
    private final LocalBlacklistCache localCache;
    private final RedisHealthManager healthManager;

    /**
     * 黑名单Key前缀
     */
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:ip:";

    /**
     * 黑名单有效期（10分钟）
     */
    private static final long BLACKLIST_DURATION_MINUTES = 10;

    /**
     * 黑名单有效期（毫秒）
     */
    private static final long BLACKLIST_DURATION_MS = BLACKLIST_DURATION_MINUTES * 60 * 1000L;

    /**
     * 初始化：注册Redis状态回调
     */
    @PostConstruct
    public void init() {
        healthManager.registerCallback(new RedisStatusCallback() {
            @Override
            public void onRedisRecovered() {
                syncToRedis();
            }

            @Override
            public void onRedisUnavailable() {
                log.info("Redis不可用，黑名单切换到本地缓存模式");
            }
        });
    }

    /**
     * 同步本地黑名单数据到Redis
     */
    private void syncToRedis() {
        try {
            var entries = localCache.getAllActiveEntries();
            if (entries.isEmpty()) {
                log.info("本地黑名单为空，无需同步");
                return;
            }

            log.info("开始同步本地黑名单到Redis，共{}条", entries.size());
            int successCount = 0;
            int failCount = 0;

            for (var entry : entries) {
                try {
                    String key = BLACKLIST_KEY_PREFIX + entry.getIp();
                    Long remainingSeconds = entry.getRemainingSeconds();
                    
                    if (remainingSeconds > 0) {
                        redisTemplate.opsForValue().set(
                            key, 
                            "1", 
                            remainingSeconds, 
                            TimeUnit.SECONDS
                        );
                        successCount++;
                    } else {
                        // 已过期，从本地缓存移除
                        localCache.removeFromBlacklist(entry.getIp());
                    }
                } catch (Exception e) {
                    failCount++;
                    log.warn("同步黑名单失败: ip={}, error={}", entry.getIp(), e.getMessage());
                }
            }

            log.info("黑名单同步完成: 成功{}条, 失败{}条", successCount, failCount);
        } catch (Exception e) {
            log.error("同步本地黑名单到Redis失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查IP是否在黑名单中
     * 双检：先检查Redis，Redis不可用时检查本地缓存
     * 
     * @param ip 客户端IP
     * @return true-在黑名单中, false-不在黑名单
     */
    public boolean isBlacklisted(String ip) {
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }

        try {
            // Redis可用时，优先检查Redis
            if (healthManager.isRedisAvailable()) {
                String key = BLACKLIST_KEY_PREFIX + ip;
                Boolean hasKey = redisTemplate.hasKey(key);
                if (Boolean.TRUE.equals(hasKey)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Redis检查黑名单失败，降级到本地缓存: {}", e.getMessage());
            healthManager.recordOperationFailure();
        }

        // Redis不可用或查询失败时，检查本地缓存
        return localCache.isBlacklisted(ip);
    }

    /**
     * 将IP加入黑名单
     * 双写：同时写入Redis和本地缓存
     * 
     * @param ip 客户端IP
     */
    public void addToBlacklist(String ip) {
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            return;
        }

        String key = BLACKLIST_KEY_PREFIX + ip;

        try {
            // 尝试写入Redis
            if (healthManager.isRedisAvailable()) {
                redisTemplate.opsForValue().set(key, "1", BLACKLIST_DURATION_MINUTES, TimeUnit.MINUTES);
                healthManager.recordOperationSuccess();
                log.info("IP已加入Redis黑名单: ip={}, 有效期{}分钟", ip, BLACKLIST_DURATION_MINUTES);
            }
        } catch (Exception e) {
            log.warn("Redis加入黑名单失败，使用本地缓存: {}", e.getMessage());
            healthManager.recordOperationFailure();
        }

        // 无论Redis是否成功，都写入本地缓存作为备份
        localCache.addToBlacklist(ip, BLACKLIST_DURATION_MS);
    }

    /**
     * 将IP移出黑名单
     * 双删：同时删除Redis和本地缓存
     * 
     * @param ip 客户端IP
     */
    public void removeFromBlacklist(String ip) {
        if (ip == null || ip.isEmpty()) {
            return;
        }

        String key = BLACKLIST_KEY_PREFIX + ip;

        // 删除Redis
        try {
            if (healthManager.isRedisAvailable()) {
                redisTemplate.delete(key);
                healthManager.recordOperationSuccess();
            }
        } catch (Exception e) {
            log.warn("Redis删除黑名单失败: {}", e.getMessage());
            healthManager.recordOperationFailure();
        }

        // 删除本地缓存
        localCache.removeFromBlacklist(ip);
        log.info("IP已移出黑名单: ip={}", ip);
    }

    /**
     * 获取IP在黑名单中的剩余时间（秒）
     * 
     * @param ip 客户端IP
     * @return 剩余时间（秒），-1表示不在黑名单
     */
    public long getRemainingTime(String ip) {
        if (ip == null || ip.isEmpty()) {
            return -1;
        }

        try {
            if (healthManager.isRedisAvailable()) {
                String key = BLACKLIST_KEY_PREFIX + ip;
                Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                if (expire != null && expire > 0) {
                    return expire;
                }
            }
        } catch (Exception e) {
            log.warn("Redis获取剩余时间失败: {}", e.getMessage());
            healthManager.recordOperationFailure();
        }

        // 降级到本地缓存
        return localCache.getRemainingTime(ip);
    }

    /**
     * 获取黑名单Key
     * 
     * @param ip 客户端IP
     * @return Redis key
     */
    public String getBlacklistKey(String ip) {
        return BLACKLIST_KEY_PREFIX + ip;
    }

    /**
     * 获取本地黑名单大小（用于监控）
     * 
     * @return 本地缓存黑名单数量
     */
    public int getLocalCacheSize() {
        return localCache.size();
    }

    /**
     * 清理本地缓存中的过期数据
     */
    public void cleanupLocalCache() {
        localCache.cleanupExpired();
    }
}
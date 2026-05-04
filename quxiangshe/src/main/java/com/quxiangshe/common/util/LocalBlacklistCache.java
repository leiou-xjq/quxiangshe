package com.quxiangshe.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 本地黑名单缓存
 * 当Redis不可用时，作为本地备份继续提供黑名单防护
 * 使用ConcurrentHashMap保证线程安全
 * 支持数据同步，回传剩余时间给Redis
 */
@Slf4j
@Component
public class LocalBlacklistCache {

    /**
     * 本地黑名单缓存
     * Key: IP地址
     * Value: 过期时间戳（毫秒）
     */
    private final ConcurrentHashMap<String, Long> blacklistCache = new ConcurrentHashMap<>();

    /**
     * 定期清理过期数据（每分钟）
     */
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "blacklist-cleanup");
        t.setDaemon(true);
        return t;
    });

    /**
     * 初始化定时清理
     */
    public LocalBlacklistCache() {
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpired, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * 检查IP是否在本地黑名单中
     * 
     * @param ip 客户端IP
     * @return true-在黑名单中, false-不在黑名单
     */
    public boolean isBlacklisted(String ip) {
        Long expireTime = blacklistCache.get(ip);
        if (expireTime == null) {
            return false;
        }
        
        // 检查是否过期
        if (System.currentTimeMillis() > expireTime) {
            // 已过期，移除
            blacklistCache.remove(ip);
            return false;
        }
        
        return true;
    }

    /**
     * 将IP加入本地黑名单
     * 
     * @param ip 客户端IP
     * @param durationMs 有效期（毫秒）
     */
    public void addToBlacklist(String ip, long durationMs) {
        long expireTime = System.currentTimeMillis() + durationMs;
        blacklistCache.put(ip, expireTime);
        log.info("IP加入本地黑名单: ip={}, 有效期{}ms", ip, durationMs);
    }

    /**
     * 将IP移出本地黑名单
     * 
     * @param ip 客户端IP
     */
    public void removeFromBlacklist(String ip) {
        blacklistCache.remove(ip);
    }

    /**
     * 获取本地黑名单大小（用于监控）
     * 
     * @return 黑名单数量
     */
    public int size() {
        return blacklistCache.size();
    }

    /**
     * 清理过期数据
     */
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        int removed = 0;
        for (java.util.Map.Entry<String, Long> entry : blacklistCache.entrySet()) {
            if (now > entry.getValue()) {
                if (blacklistCache.remove(entry.getKey()) != null) {
                    removed++;
                }
            }
        }
        if (removed > 0) {
            log.info("清理本地黑名单过期数据: {}条", removed);
        }
    }

    /**
     * 清理所有数据
     */
    public void clear() {
        blacklistCache.clear();
    }

    /**
     * 获取IP剩余时间（秒）
     * 
     * @param ip 客户端IP
     * @return 剩余时间（秒），-1表示不在黑名单
     */
    public long getRemainingTime(String ip) {
        Long expireTime = blacklistCache.get(ip);
        if (expireTime == null) {
            return -1;
        }
        
        long remaining = expireTime - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000 : -1;
    }

    /**
     * 获取所有未过期的黑名单IP及其剩余时间
     * 用于Redis恢复时同步到Redis
     * 
     * @return IP和剩余时间（秒）的映射
     */
    public List<BlacklistEntry> getAllActiveEntries() {
        List<BlacklistEntry> entries = new ArrayList<>();
        long now = System.currentTimeMillis();
        
        for (java.util.Map.Entry<String, Long> entry : blacklistCache.entrySet()) {
            if (entry.getValue() > now) {
                long remainingSeconds = (entry.getValue() - now) / 1000;
                entries.add(new BlacklistEntry(entry.getKey(), remainingSeconds));
            }
        }
        
        return entries;
    }

    /**
     * 黑名单条目
     */
    public static class BlacklistEntry {
        private final String ip;
        private final long remainingSeconds;

        public BlacklistEntry(String ip, long remainingSeconds) {
            this.ip = ip;
            this.remainingSeconds = remainingSeconds;
        }

        public String getIp() {
            return ip;
        }

        public long getRemainingSeconds() {
            return remainingSeconds;
        }
    }
}
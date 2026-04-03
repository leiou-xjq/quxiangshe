package com.quxiangshe.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quxiangshe.common.util.RedisHealthManager.RedisStatusCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 黑名单数据持久化服务
 * 
 * 功能:
 * 1. 定期将本地黑名单数据持久化到Redis（防止服务重启丢失黑名单）
 * 2. 服务启动时从Redis恢复黑名单数据
 * 3. Redis恢复时同步黑名单数据
 * 
 * 持久化策略:
 * - 定时持久化：每30秒执行一次
 * - 只持久化未过期的黑名单
 * - 过期时间作为Redis Key的TTL
 * 
 * @author quxiangshe
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlacklistPersistenceService {

    private final StringRedisTemplate redisTemplate;
    private final LocalBlacklistCache localCache;
    private final RedisHealthManager healthManager;
    private final ObjectMapper objectMapper;

    /**
     * 持久化调度器
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "blacklist-persistence");
        t.setDaemon(true);
        return t;
    });

    /**
     * 黑名单Key前缀
     */
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:ip:";

    /**
     * 持久化Key
     */
    private static final String PERSISTENCE_KEY = "blacklist:persistence:data";

    /**
     * 黑名单有效期（分钟）
     */
    private static final long BLACKLIST_DURATION_MINUTES = 10;

    /**
     * 持久化间隔（秒）
     */
    private static final int PERSISTENCE_INTERVAL = 30;

    /**
     * 初始化：启动定时持久化和恢复
     */
    @PostConstruct
    public void init() {
        // 启动时恢复数据
        recoverFromRedis();

        // 注册Redis恢复回调
        healthManager.registerCallback(new RedisStatusCallback() {
            @Override
            public void onRedisRecovered() {
                syncToRedis();
            }

            @Override
            public void onRedisUnavailable() {
                log.debug("Redis不可用，跳过黑名单持久化");
            }
        });

        // 启动定时持久化
        scheduler.scheduleAtFixedRate(
                this::persistToRedis,
                PERSISTENCE_INTERVAL,
                PERSISTENCE_INTERVAL,
                TimeUnit.SECONDS
        );

        log.info("黑名单持久化服务已启动: interval={}s", PERSISTENCE_INTERVAL);
    }

    /**
     * 从Redis恢复黑名单数据
     * 服务启动时调用
     */
    public void recoverFromRedis() {
        if (!healthManager.isRedisAvailable()) {
            log.debug("Redis不可用，跳过黑名单数据恢复");
            return;
        }

        try {
            String json = redisTemplate.opsForValue().get(PERSISTENCE_KEY);
            
            if (json == null || json.isEmpty()) {
                log.info("Redis中无黑名单持久化数据，跳过恢复");
                return;
            }

            List<BlacklistData> dataList = objectMapper.readValue(json, new TypeReference<List<BlacklistData>>() {});
            
            if (dataList != null && !dataList.isEmpty()) {
                int successCount = 0;
                long now = System.currentTimeMillis();
                
                for (BlacklistData data : dataList) {
                    long remainingMs = data.getExpireTime() - now;
                    if (remainingMs > 0) {
                        localCache.addToBlacklist(data.getIp(), remainingMs);
                        successCount++;
                    }
                }
                
                log.info("黑名单数据恢复成功: {}条", successCount);
            }
        } catch (Exception e) {
            log.error("从Redis恢复黑名单数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 持久化到Redis
     * 定时任务调用
     */
    public void persistToRedis() {
        if (!healthManager.isRedisAvailable()) {
            log.debug("Redis不可用，跳过持久化");
            return;
        }

        try {
            List<LocalBlacklistCache.BlacklistEntry> entries = localCache.getAllActiveEntries();
            
            if (entries.isEmpty()) {
                log.debug("本地无活跃黑名单数据，跳过持久化");
                return;
            }

            List<BlacklistData> dataList = new ArrayList<>();
            long now = System.currentTimeMillis();
            
            for (LocalBlacklistCache.BlacklistEntry entry : entries) {
                long expireTime = now + (entry.getRemainingSeconds() * 1000);
                dataList.add(new BlacklistData(entry.getIp(), expireTime));
            }

            String json = objectMapper.writeValueAsString(dataList);
            // 持久化数据有效期 = 黑名单有效期 + 缓冲时间
            redisTemplate.opsForValue().set(PERSISTENCE_KEY, json, BLACKLIST_DURATION_MINUTES + 1, TimeUnit.MINUTES);
            
            log.debug("黑名单数据持久化完成: {}条", dataList.size());
        } catch (JsonProcessingException e) {
            log.error("序列化黑名单数据失败: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("持久化黑名单数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 同步到Redis
     * Redis恢复时调用
     */
    public void syncToRedis() {
        if (!healthManager.isRedisAvailable()) {
            log.debug("Redis不可用，跳过同步");
            return;
        }

        try {
            List<LocalBlacklistCache.BlacklistEntry> entries = localCache.getAllActiveEntries();
            
            if (entries.isEmpty()) {
                log.info("本地无活跃黑名单数据，无需同步");
                return;
            }

            int successCount = 0;
            for (LocalBlacklistCache.BlacklistEntry entry : entries) {
                try {
                    String key = BLACKLIST_KEY_PREFIX + entry.getIp();
                    redisTemplate.opsForValue().set(key, "1", entry.getRemainingSeconds(), TimeUnit.SECONDS);
                    successCount++;
                } catch (Exception e) {
                    log.warn("同步黑名单失败: ip={}, error={}", entry.getIp(), e.getMessage());
                }
            }

            log.info("黑名单数据同步到Redis完成: {}条", successCount);
        } catch (Exception e) {
            log.error("同步黑名单数据到Redis失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 手动触发持久化
     */
    public void triggerPersist() {
        log.info("手动触发黑名单数据持久化");
        persistToRedis();
    }

    /**
     * 手动触发恢复
     */
    public void triggerRecover() {
        log.info("手动触发黑名单数据恢复");
        recoverFromRedis();
    }

    /**
     * 清理持久化数据
     */
    public void clearPersistence() {
        try {
            redisTemplate.delete(PERSISTENCE_KEY);
            log.info("黑名单持久化数据已清除");
        } catch (Exception e) {
            log.error("清除持久化数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取持久化状态
     */
    public String getStatus() {
        try {
            String json = redisTemplate.opsForValue().get(PERSISTENCE_KEY);
            if (json != null) {
                List<BlacklistData> dataList = objectMapper.readValue(json, new TypeReference<List<BlacklistData>>() {});
                return String.format("黑名单持久化: Redis中数据 %d条, 本地缓存 %d条", 
                        dataList != null ? dataList.size() : 0, localCache.size());
            }
            return String.format("黑名单持久化: Redis中无数据, 本地缓存 %d条", localCache.size());
        } catch (Exception e) {
            return "黑名单持久化状态获取失败: " + e.getMessage();
        }
    }

    /**
     * 黑名单数据结构
     */
    public static class BlacklistData {
        private String ip;
        private long expireTime;

        public BlacklistData() {}

        public BlacklistData(String ip, long expireTime) {
            this.ip = ip;
            this.expireTime = expireTime;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public long getExpireTime() {
            return expireTime;
        }

        public void setExpireTime(long expireTime) {
            this.expireTime = expireTime;
        }
    }
}

package com.quxiangshe.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quxiangshe.common.config.RateLimitProperties;
import com.quxiangshe.common.util.LocalRateLimitCache.RateLimitData;
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
 * 限流数据持久化服务
 * 
 * 功能:
 * 1. 定期将本地限流数据持久化到Redis（防止服务重启丢失数据）
 * 2. 服务启动时从Redis恢复限流数据
 * 3. Redis恢复时同步数据到Redis
 * 
 * 持久化策略:
 * - 定时持久化：每30秒执行一次
 * - 增量更新：只持久化有数据的key
 * - 批量处理：每次最多100条
 * 
 * @author quxiangshe
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitPersistenceService {

    private final StringRedisTemplate redisTemplate;
    private final LocalRateLimitCache localCache;
    private final RedisHealthManager healthManager;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 持久化调度器
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ratelimit-persistence");
        t.setDaemon(true);
        return t;
    });

    /**
     * 默认窗口时间（毫秒）- 用于计算过期时间
     */
    private static final long DEFAULT_WINDOW_MS = 60000;

    /**
     * 初始化：启动定时持久化和恢复
     */
    @PostConstruct
    public void init() {
        if (properties.isEnablePersistence()) {
            // 启动时恢复数据
            recoverFromRedis();

            // 注册Redis恢复回调
            healthManager.registerCallback(new RedisStatusCallback() {
                @Override
                public void onRedisRecovered() {
                    if (properties.isSyncOnRedisRecover()) {
                        syncToRedis();
                    }
                }

                @Override
                public void onRedisUnavailable() {
                    log.debug("Redis不可用，跳过持久化");
                }
            });

            // 启动定时持久化
            scheduler.scheduleAtFixedRate(
                    this::persistToRedis,
                    properties.getPersistenceIntervalSeconds(),
                    properties.getPersistenceIntervalSeconds(),
                    TimeUnit.SECONDS
            );

            log.info("限流持久化服务已启动: interval={}s", properties.getPersistenceIntervalSeconds());
        } else {
            log.info("限流持久化已禁用");
        }
    }

    /**
     * 从Redis恢复限流数据
     * 服务启动时调用
     */
    public void recoverFromRedis() {
        if (!properties.isEnablePersistence() || !healthManager.isRedisAvailable()) {
            log.debug("跳过数据恢复: enablePersistence={}, redisAvailable={}", 
                    properties.isEnablePersistence(), healthManager.isRedisAvailable());
            return;
        }

        try {
            String key = properties.getPersistenceKeyPrefix() + "data";
            String json = redisTemplate.opsForValue().get(key);

            if (json == null || json.isEmpty()) {
                log.info("Redis中无持久化数据，跳过恢复");
                return;
            }

            List<RateLimitData> dataList = objectMapper.readValue(json, new TypeReference<List<RateLimitData>>() {});
            
            if (dataList != null && !dataList.isEmpty()) {
                // 为每条数据设置默认窗口时间
                for (RateLimitData data : dataList) {
                    data.setWindowMs(DEFAULT_WINDOW_MS);
                }
                
                localCache.importData(dataList);
                log.info("限流数据恢复成功: {}条", dataList.size());
            }
        } catch (Exception e) {
            log.error("从Redis恢复限流数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 持久化到Redis
     * 定时任务调用
     */
    public void persistToRedis() {
        if (!properties.isEnablePersistence()) {
            return;
        }

        // 检查Redis是否可用
        if (!healthManager.isRedisAvailable()) {
            log.debug("Redis不可用，跳过持久化");
            return;
        }

        try {
            // 导出本地活跃数据
            List<RateLimitData> dataList = localCache.exportActiveData(DEFAULT_WINDOW_MS);
            
            if (dataList.isEmpty()) {
                log.debug("本地无活跃限流数据，跳过持久化");
                return;
            }

            // 批量处理
            int batchSize = properties.getPersistenceBatchSize();
            int totalBatches = (dataList.size() + batchSize - 1) / batchSize;
            
            for (int i = 0; i < totalBatches; i++) {
                int fromIndex = i * batchSize;
                int toIndex = Math.min(fromIndex + batchSize, dataList.size());
                List<RateLimitData> batch = dataList.subList(fromIndex, toIndex);
                
                persistBatch(batch);
            }

            log.debug("限流数据持久化完成: {}条", dataList.size());
        } catch (Exception e) {
            log.error("持久化限流数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 批量持久化
     * 
     * @param dataList 数据列表
     */
    private void persistBatch(List<RateLimitData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        try {
            String key = properties.getPersistenceKeyPrefix() + "data";
            String json = objectMapper.writeValueAsString(dataList);
            
            // 持久化数据有效期 = 窗口时间 + 额外缓冲时间
            long expireSeconds = 120; // 2分钟
            redisTemplate.opsForValue().set(key, json, expireSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("序列化限流数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 同步到Redis
     * Redis恢复时调用
     */
    public void syncToRedis() {
        if (!properties.isEnablePersistence()) {
            return;
        }

        if (!healthManager.isRedisAvailable()) {
            log.debug("Redis不可用，跳过同步");
            return;
        }

        try {
            List<RateLimitData> dataList = localCache.exportActiveData(DEFAULT_WINDOW_MS);
            
            if (dataList.isEmpty()) {
                log.info("本地无活跃数据，无需同步");
                return;
            }

            persistBatch(dataList);
            log.info("限流数据同步到Redis完成: {}条", dataList.size());
        } catch (Exception e) {
            log.error("同步限流数据到Redis失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 手动触发持久化（用于管理接口）
     */
    public void triggerPersist() {
        log.info("手动触发限流数据持久化");
        persistToRedis();
    }

    /**
     * 手动触发恢复（用于管理接口）
     */
    public void triggerRecover() {
        log.info("手动触发限流数据恢复");
        recoverFromRedis();
    }

    /**
     * 清理持久化数据（用于测试或管理员操作）
     */
    public void clearPersistence() {
        try {
            String key = properties.getPersistenceKeyPrefix() + "data";
            redisTemplate.delete(key);
            log.info("持久化数据已清除");
        } catch (Exception e) {
            log.error("清除持久化数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取持久化状态信息
     * 
     * @return 状态描述
     */
    public String getStatus() {
        if (!properties.isEnablePersistence()) {
            return "持久化已禁用";
        }
        
        try {
            String key = properties.getPersistenceKeyPrefix() + "data";
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                List<RateLimitData> dataList = objectMapper.readValue(json, new TypeReference<List<RateLimitData>>() {});
                return String.format("持久化已启用, Redis中数据: %d条", dataList.size());
            }
            return "持久化已启用, Redis中无数据";
        } catch (Exception e) {
            return "持久化状态获取失败: " + e.getMessage();
        }
    }
}

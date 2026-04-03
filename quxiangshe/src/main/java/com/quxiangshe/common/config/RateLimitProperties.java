package com.quxiangshe.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 限流模块配置属性
 * 
 * 配置说明:
 * - enableLocalFallback: 是否启用本地降级限流（Redis不可用时）
 * - localCacheExpireMinutes: 本地缓存过期时间（分钟）
 * - enablePersistence: 是否启用Redis持久化
 * - persistenceIntervalSeconds: 持久化间隔（秒）
 * - syncOnRedisRecover: Redis恢复时是否同步数据
 * 
 * @author quxiangshe
 * @since 2024
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ratelimit.config")
public class RateLimitProperties {

    /**
     * 是否启用本地降级限流
     * 当Redis不可用时，使用本地Caffeine缓存兜底
     * 默认: true
     */
    private boolean enableLocalFallback = true;

    /**
     * 本地缓存过期时间（分钟）
     * 默认: 10分钟
     */
    private int localCacheExpireMinutes = 10;

    /**
     * 本地缓存最大条目数
     * 超过此数量后，按LRU淘汰
     * 默认: 10000
     */
    private int localCacheMaxSize = 10000;

    /**
     * 是否启用Redis持久化
     * 服务重启后恢复限流数据
     * 默认: true
     */
    private boolean enablePersistence = true;

    /**
     * 持久化间隔（秒）
     * 定期将本地限流数据写入Redis
     * 默认: 30秒
     */
    private int persistenceIntervalSeconds = 30;

    /**
     * Redis恢复时是否同步数据
     * 将本地缓存数据同步到Redis
     * 默认: true
     */
    private boolean syncOnRedisRecover = true;

    /**
     * 持久化Key前缀
     */
    private String persistenceKeyPrefix = "ratelimit:persistence:";

    /**
     * 持久化批量大小
     * 每次持久化的最大条目数
     * 默认: 100
     */
    private int persistenceBatchSize = 100;
}

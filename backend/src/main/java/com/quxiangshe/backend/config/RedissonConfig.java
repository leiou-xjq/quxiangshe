package com.quxiangshe.backend.config;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.redisson.Redisson;
import org.redisson.config.Config;

/**
 * Redisson分布式锁配置
 * <p>手动创建RedissonClient单机模式实例，与Spring Data Redis的RedisConnectionFactory
 * 解耦，避免两者自动配置冲突。Redisson用于分布式锁（@RateLimit注解AOP、热门刷新等）。
 * 连接失败时返回null，由调用方降级处理。</p>
 * 
 * @author 趣享社技术团队
 */
@Configuration
@ConditionalOnClass(RedissonClient.class)
public class RedissonConfig {

    /**
     * 创建RedissonClient单机模式实例
     * <p>连接池最小1连接、最大2连接，避免占用过多Redis连接资源。</p>
     * 
     * @return RedissonClient实例，连接失败返回null
     */
    @Bean
    public RedissonClient redissonClient() {
        try {
            Config config = new Config();
            // 单机模式：直接连接本地Redis
            config.useSingleServer()
                    .setAddress("redis://localhost:6379")
                    .setDatabase(0)
                    .setConnectionMinimumIdleSize(1)    // 最小空闲连接
                    .setConnectionPoolSize(2);          // 最大连接数
            return Redisson.create(config);
        } catch (Exception e) {
            // 连接失败返回null，由调用方判断并降级处理（跳过限流/锁逻辑）
            return null;
        }
    }
}
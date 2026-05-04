package com.quxiangshe.backend.config;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.redisson.Redisson;
import org.redisson.config.Config;

/**
 * Redisson配置类
 * 手动配置 Redisson，避免与 RedisConnectionFactory 冲突
 */
@Configuration
@ConditionalOnClass(RedissonClient.class)
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        try {
            Config config = new Config();
            config.useSingleServer()
                    .setAddress("redis://localhost:6379")
                    .setDatabase(0)
                    .setConnectionMinimumIdleSize(1)
                    .setConnectionPoolSize(2);
            return Redisson.create(config);
        } catch (Exception e) {
            // Redisson 连接失败时返回 null，由使用方处理
            return null;
        }
    }
}
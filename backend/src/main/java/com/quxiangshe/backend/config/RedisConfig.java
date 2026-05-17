package com.quxiangshe.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 * <p>定义全局RedisTemplate序列化规则：
 * <ul>
 *   <li>Key使用StringRedisSerializer（可读性好，便于排查）</li>
 *   <li>Value使用GenericJackson2JsonRedisSerializer（支持任意Java对象JSON序列化）</li>
 * </ul>
 * 此配置确保存入Redis的数据在Redis Desktop Manager等工具中可直接阅读，
 * 同时避免JDK默认序列化导致的跨语言兼容问题。</p>
 * 
 * @author 趣享社技术团队
 */
@Configuration
public class RedisConfig {
    
    /**
     * 创建RedisTemplate实例
     * <p>序列化策略：Key用StringRedisSerializer，Value用GenericJackson2JsonRedisSerializer。
     * 原因：String key便于运维查看，JSON value兼容异构系统。</p>
     * 
     * @param connectionFactory Redis连接工厂（由Spring Boot自动配置注入）
     * @return 配置好序列化器的RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Key序列化器：StringRedisSerializer，确保key在Redis客户端中可读
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // Value序列化器：GenericJackson2JsonRedisSerializer，支持泛型和复杂嵌套对象
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
}

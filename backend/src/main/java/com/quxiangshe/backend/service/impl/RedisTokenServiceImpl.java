package com.quxiangshe.backend.service.impl;

import com.quxiangshe.backend.service.IRedisTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis令牌服务实现类
 * 使用Redis存储refreshToken，与用户ID绑定
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTokenServiceImpl implements IRedisTokenService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Redis中refreshToken的key前缀
     */
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";
    
    /**
     * 默认refreshToken有效期：7天
     */
    private static final long DEFAULT_EXPIRE_SECONDS = 7 * 24 * 60 * 60;
    
    /**
     * 存储refreshToken到Redis
     */
    @Override
    public void storeRefreshToken(Long userId, String refreshToken, long expireSeconds) {
        String key = getKey(userId);
        // 存储到Redis，设置过期时间
        redisTemplate.opsForValue().set(key, refreshToken, expireSeconds, TimeUnit.SECONDS);
        log.debug("存储refreshToken到Redis: userId={}, 有效期={}秒", userId, expireSeconds);
    }
    
    /**
     * 获取Redis中的refreshToken
     */
    @Override
    public String getRefreshToken(Long userId) {
        String key = getKey(userId);
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 删除Redis中的refreshToken
     */
    @Override
    public void removeRefreshToken(Long userId) {
        String key = getKey(userId);
        redisTemplate.delete(key);
        log.debug("删除Redis中的refreshToken: userId={}", userId);
    }
    
    /**
     * 验证refreshToken是否有效
     */
    @Override
    public boolean validateRefreshToken(Long userId, String refreshToken) {
        String storedToken = getRefreshToken(userId);
        if (storedToken == null) {
            log.debug("refreshToken不存在: userId={}", userId);
            return false;
        }
        boolean valid = storedToken.equals(refreshToken);
        if (!valid) {
            log.debug("refreshToken不匹配: userId={}", userId);
        }
        return valid;
    }
    
    /**
     * 刷新Token过期时间
     */
    @Override
    public void expireRefreshToken(Long userId, long expireSeconds) {
        String key = getKey(userId);
        redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
        log.debug("刷新refreshToken过期时间: userId={}, 有效期={}秒", userId, expireSeconds);
    }
    
    /**
     * 生成Redis key
     */
    private String getKey(Long userId) {
        return REFRESH_TOKEN_KEY_PREFIX + userId;
    }
}

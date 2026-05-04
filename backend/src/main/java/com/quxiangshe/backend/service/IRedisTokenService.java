package com.quxiangshe.backend.service;

import java.util.concurrent.TimeUnit;

/**
 * Redis令牌服务接口
 * 用于管理Redis中的refreshToken
 * 
 * @author 趣享社技术团队
 */
public interface IRedisTokenService {
    
    /**
     * 存储refreshToken到Redis
     * @param userId 用户ID
     * @param refreshToken 刷新令牌
     * @param expireSeconds 过期时间（秒）
     */
    void storeRefreshToken(Long userId, String refreshToken, long expireSeconds);
    
    /**
     * 获取Redis中的refreshToken
     * @param userId 用户ID
     * @return refreshToken
     */
    String getRefreshToken(Long userId);
    
    /**
     * 删除Redis中的refreshToken
     * @param userId 用户ID
     */
    void removeRefreshToken(Long userId);
    
    /**
     * 验证refreshToken是否有效
     * @param userId 用户ID
     * @param refreshToken 待验证的token
     * @return 是否有效
     */
    boolean validateRefreshToken(Long userId, String refreshToken);
    
    /**
     * 刷新Token过期时间
     * @param userId 用户ID
     * @param expireSeconds 新的过期时间
     */
    void expireRefreshToken(Long userId, long expireSeconds);
}

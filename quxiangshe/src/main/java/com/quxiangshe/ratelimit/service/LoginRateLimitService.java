package com.quxiangshe.ratelimit.service;

import com.quxiangshe.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录限流服务
 * 使用Redis ZSet实现滑动窗口限流
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginRateLimitService {

    private final RedisUtil redisUtil;

    /**
     * 登录限流Key前缀
     */
    private static final String LOGIN_RATE_LIMIT_KEY = "login:ratelimit:";

    /**
     * 滑动窗口大小（秒）
     */
    @Value("${login-ratelimit.window-size:1}")
    private int windowSize;

    /**
     * 每秒最大登录请求数
     */
    @Value("${login-ratelimit.max-request:20}")
    private int maxRequest;

    /**
     * 检查登录请求是否允许
     * 
     * @param identifier 用户标识（用户名/手机号/邮箱/IP）
     * @return true表示允许，false表示被限流
     */
    public boolean tryAcquire(String identifier) {
        String key = LOGIN_RATE_LIMIT_KEY + identifier;
        long now = System.currentTimeMillis();
        long windowMs = windowSize * 1000L;
        long windowStart = now - windowMs;

        // 删除窗口外的过期数据
        redisUtil.zRemoveRangeByScore(key, 0, windowStart);

        // 检查当前窗口内的请求数
        Long count = redisUtil.zSize(key);
        if (count != null && count >= maxRequest) {
            log.warn("登录限流触发: identifier={}, count={}, max={}", identifier, count, maxRequest);
            return false;
        }

        // 添加新请求
        String requestId = now + ":" + Math.random();
        redisUtil.zAdd(key, requestId, now);
        redisUtil.expire(key, windowMs + 1000, TimeUnit.MILLISECONDS);

        log.debug("登录请求通过: identifier={}, count={}", identifier, count != null ? count + 1 : 1);
        return true;
    }

    /**
     * 获取当前窗口内的登录请求数
     * 
     * @param identifier 用户标识
     * @return 当前请求数
     */
    public Long getCurrentCount(String identifier) {
        String key = LOGIN_RATE_LIMIT_KEY + identifier;
        long now = System.currentTimeMillis();
        long windowMs = windowSize * 1000L;
        long windowStart = now - windowMs;

        redisUtil.zRemoveRangeByScore(key, 0, windowStart);
        return redisUtil.zSize(key);
    }

    /**
     * 清除限流记录
     * 
     * @param identifier 用户标识
     */
    public void clear(String identifier) {
        String key = LOGIN_RATE_LIMIT_KEY + identifier;
        redisUtil.delete(key);
    }
}
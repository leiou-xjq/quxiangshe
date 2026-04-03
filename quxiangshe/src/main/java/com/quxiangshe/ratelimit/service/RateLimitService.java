package com.quxiangshe.ratelimit.service;

import com.quxiangshe.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 限流服务
 * 使用Redis ZSet实现滑动窗口限流
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisUtil redisUtil;

    /**
     * 限流窗口大小（秒）
     */
    @Value("${ratelimit.window-size:1}")
    private int windowSize;

    /**
     * 最大请求数
     */
    @Value("${ratelimit.max-request:100}")
    private int maxRequest;

    /**
     * 请求去重窗口（秒）
     */
    @Value("${ratelimit.dedup-window:5}")
    private int dedupWindow;

    /**
     * 限流Key前缀
     */
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    /**
     * 请求去重Key前缀
     */
    private static final String DEDUP_KEY_PREFIX = "dedup:";

    /**
     * 滑动窗口限流检查
     *
     * @param userId     用户ID
     * @param requestId  请求唯一标识
     * @return true表示通过，false表示限流
     */
    public boolean checkRateLimit(Long userId, String requestId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        long windowMs = windowSize * 1000L;

        // 生成唯一请求ID（防止重复请求）
        String uniqueRequestId = requestId + ":" + System.currentTimeMillis();

        // 先检查去重
        String dedupKey = DEDUP_KEY_PREFIX + userId;
        Boolean isDuplicated = redisUtil.hasKey(dedupKey + ":" + requestId);
        if (Boolean.TRUE.equals(isDuplicated)) {
            log.debug("请求去重: userId={}, requestId={}", userId, requestId);
            return false;
        }

        // 执行滑动窗口限流
        boolean allowed = redisUtil.slideWindowRateLimit(key, windowMs, maxRequest, uniqueRequestId);

        // 如果通过，设置去重标记
        if (allowed) {
            redisUtil.setEx(dedupKey + ":" + requestId, "1", dedupWindow);
        }

        return allowed;
    }

    /**
     * 获取当前用户剩余请求次数
     *
     * @param userId 用户ID
     * @return 剩余请求次数
     */
    public long getRemainingRequests(Long userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSize * 1000L);

        // 删除窗口外的过期数据
        redisUtil.zRemoveRangeByScore(key, 0, windowStart);

        Long count = redisUtil.zSize(key);
        if (count == null) {
            count = 0L;
        }

        return Math.max(0, maxRequest - count);
    }

    /**
     * 重置限流计数器
     *
     * @param userId 用户ID
     */
    public void resetRateLimit(Long userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        redisUtil.delete(key);
    }
}

package com.quxiangshe.backend.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 缓存工具类
 * 提供TTL抖动等防护机制
 */
public class CacheUtil {

    /**
     * 添加随机抖动到TTL，防止缓存雪崩
     * 抖动范围：baseTtl ± 10%
     *
     * @param baseTtlSeconds 基础TTL（秒）
     * @return 带抖动的TTL（秒）
     */
    public static long jitterTtl(long baseTtlSeconds) {
        if (baseTtlSeconds <= 0) {
            return baseTtlSeconds;
        }
        long jitter = (long) (baseTtlSeconds * 0.1);
        long jitterValue = ThreadLocalRandom.current().nextLong(-jitter, jitter + 1);
        return Math.max(1, baseTtlSeconds + jitterValue);
    }

    /**
     * 添加固定百分比抖动
     *
     * @param baseTtlSeconds 基础TTL（秒）
     * @param jitterPercent 抖动百分比（如10表示±10%）
     * @return 带抖动的TTL（秒）
     */
    public static long jitterTtl(long baseTtlSeconds, int jitterPercent) {
        if (baseTtlSeconds <= 0) {
            return baseTtlSeconds;
        }
        long jitter = (long) (baseTtlSeconds * jitterPercent / 100.0);
        long jitterValue = ThreadLocalRandom.current().nextLong(-jitter, jitter + 1);
        return Math.max(1, baseTtlSeconds + jitterValue);
    }
}

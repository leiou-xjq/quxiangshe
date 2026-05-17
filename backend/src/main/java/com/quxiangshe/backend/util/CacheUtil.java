package com.quxiangshe.backend.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 缓存工具类
 * 
 * <p>提供缓存TTL（过期时间）抖动机制，防止大量缓存在同一时间过期导致缓存雪崩。
 * 通过在基础TTL上叠加上下浮动10%的随机偏移量，使得不同key的过期时间分散在不同时间点，
 * 避免缓存同时失效时对数据库造成瞬时巨大压力。</p>
 * 
 * @author 趣享社技术团队
 * @since 1.0
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
            // 非正数TTL无意义，直接返回
            return baseTtlSeconds;
        }
        // 计算10%的抖动范围
        long jitter = (long) (baseTtlSeconds * 0.1);
        // 在[-jitter, +jitter]范围内随机偏移
        long jitterValue = ThreadLocalRandom.current().nextLong(-jitter, jitter + 1);
        // 确保最终TTL至少为1秒
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
        // 根据指定百分比计算抖动范围
        long jitter = (long) (baseTtlSeconds * jitterPercent / 100.0);
        // 在[-jitter, +jitter]范围内随机偏移
        long jitterValue = ThreadLocalRandom.current().nextLong(-jitter, jitter + 1);
        // 确保最终TTL至少为1秒
        return Math.max(1, baseTtlSeconds + jitterValue);
    }
}

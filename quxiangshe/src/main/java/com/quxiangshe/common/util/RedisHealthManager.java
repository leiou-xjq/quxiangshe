package com.quxiangshe.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redis健康检查与降级管理器
 * 监控Redis连接状态，在Redis不可用时自动降级到本地缓存
 * 支持状态变化回调，通知其他组件进行数据同步
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthManager {

    private final StringRedisTemplate redisTemplate;

    /**
     * Redis是否可用
     */
    private volatile boolean redisAvailable = true;

    /**
     * 连续失败次数
     */
    private final AtomicInteger failureCount = new AtomicInteger(0);

    /**
     * 最大允许失败次数
     */
    private static final int MAX_FAILURE_COUNT = 3;

    /**
     * 健康检查间隔（秒）
     */
    private static final int CHECK_INTERVAL_SECONDS = 10;

    /**
     * 连续成功次数（用于确认Redis真正恢复）
     */
    private static final int SUCCESS_COUNT_THRESHOLD = 2;
    private final AtomicInteger successCount = new AtomicInteger(0);

    /**
     * 状态变化回调列表
     */
    private final List<RedisStatusCallback> callbacks = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "redis-health-check");
        t.setDaemon(true);
        return t;
    });

    /**
     * Redis状态回调接口
     */
    public interface RedisStatusCallback {
        /**
         * 当Redis从不可用变为可用时触发
         */
        void onRedisRecovered();

        /**
         * 当Redis从可用变为不可用时触发
         */
        void onRedisUnavailable();
    }

    /**
     * 注册状态变化回调
     * 
     * @param callback 回调实现
     */
    public void registerCallback(RedisStatusCallback callback) {
        callbacks.add(callback);
        log.debug("注册Redis状态回调: {}", callback.getClass().getSimpleName());
    }

    /**
     * 初始化健康检查
     */
    @PostConstruct
    public void init() {
        // 首次检查
        checkRedisHealth();
        
        scheduler.scheduleAtFixedRate(this::checkRedisHealth, 
            CHECK_INTERVAL_SECONDS, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("Redis健康检查已启动，检查间隔{}秒", CHECK_INTERVAL_SECONDS);
    }

    /**
     * 检查Redis健康状态
     */
    private void checkRedisHealth() {
        try {
            String result = redisTemplate.execute((RedisCallback<String>) connection -> {
                return connection.ping();
            });

            if ("PONG".equals(result)) {
                int success = successCount.incrementAndGet();
                
                if (!redisAvailable) {
                    // Redis刚恢复，需要连续多次成功才认为真正恢复
                    if (success >= SUCCESS_COUNT_THRESHOLD) {
                        handleRedisRecovered();
                    }
                } else {
                    successCount.set(0);
                }
                failureCount.set(0);
            }
        } catch (Exception e) {
            int failures = failureCount.incrementAndGet();
            successCount.set(0);
            log.warn("Redis健康检查失败: {}, 连续失败{}次", e.getMessage(), failures);
            
            if (failures >= MAX_FAILURE_COUNT && redisAvailable) {
                handleRedisUnavailable();
            }
        }
    }

    /**
     * 处理Redis恢复
     */
    private synchronized void handleRedisRecovered() {
        if (!redisAvailable) {
            log.info("Redis连接已恢复，准备同步数据...");
            redisAvailable = true;
            successCount.set(0);
            
            // 通知所有回调
            for (RedisStatusCallback callback : callbacks) {
                try {
                    callback.onRedisRecovered();
                } catch (Exception e) {
                    log.error("执行Redis恢复回调失败: {}", e.getMessage());
                }
            }
            log.info("Redis状态回调执行完成");
        }
    }

    /**
     * 处理Redis不可用
     */
    private synchronized void handleRedisUnavailable() {
        if (redisAvailable) {
            log.error("Redis不可用，切换到本地缓存降级模式");
            redisAvailable = false;
            successCount.set(0);
            
            // 通知所有回调
            for (RedisStatusCallback callback : callbacks) {
                try {
                    callback.onRedisUnavailable();
                } catch (Exception e) {
                    log.error("执行Redis不可用回调失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 检查Redis是否可用
     * 
     * @return true-可用, false-不可用
     */
    public boolean isRedisAvailable() {
        return redisAvailable;
    }

    /**
     * Redis降级时记录一次操作失败
     */
    public void recordOperationFailure() {
        failureCount.incrementAndGet();
    }

    /**
     * Redis降级时记录一次操作成功
     */
    public void recordOperationSuccess() {
        if (failureCount.get() > 0) {
            failureCount.decrementAndGet();
        }
    }

    /**
     * 手动触发健康检查
     */
    public void triggerHealthCheck() {
        checkRedisHealth();
    }

    /**
     * 获取当前状态描述
     * 
     * @return 状态描述
     */
    public String getStatus() {
        if (redisAvailable) {
            return "Redis正常";
        } else {
            return "Redis不可用（降级模式），连续失败" + failureCount.get() + "次";
        }
    }

    /**
     * 获取失败次数
     */
    public int getFailureCount() {
        return failureCount.get();
    }

    /**
     * 强制设置Redis状态（用于测试或管理员操作）
     * 
     * @param available 是否可用
     */
    public void setRedisAvailable(boolean available) {
        if (available && !redisAvailable) {
            handleRedisRecovered();
        } else if (!available && redisAvailable) {
            handleRedisUnavailable();
        }
    }
}
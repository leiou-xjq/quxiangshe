package com.quxiangshe.backend.util;

import com.quxiangshe.backend.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁工具类
 * 统一锁获取逻辑，避免重复代码
 */
@Slf4j
public class DistributedLockUtil {

    private static final long DEFAULT_WAIT_TIME = 5;
    private static final long DEFAULT_LEASE_TIME = 30;

    /**
     * 获取分布式锁，成功后执行操作，失败则抛异常
     *
     * @param client  Redisson客户端
     * @param lockKey 锁key
     * @param action  持有锁后要执行的操作
     * @param <T>    返回值类型
     * @return 操作返回值
     */
    public static <T> T executeWithLock(RedissonClient client, String lockKey, Supplier<T> action) {
        return executeWithLock(client, lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, action);
    }

    /**
     * 获取分布式锁，成功后执行操作，失败则抛异常
     *
     * @param client     Redisson客户端
     * @param lockKey    锁key
     * @param waitTime   等待时间（秒）
     * @param leaseTime  持有时间（秒）
     * @param action     持有锁后要执行的操作
     * @param <T>       返回值类型
     * @return 操作返回值
     */
    public static <T> T executeWithLock(RedissonClient client, String lockKey,
                                       long waitTime, long leaseTime, Supplier<T> action) {
        if (client == null) {
            log.warn("RedissonClient为空，无法获取分布式锁: {}", lockKey);
            throw new BusinessException(500, "系统繁忙，请稍后重试");
        }

        RLock lock = client.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("获取分布式锁失败: lockKey={}, waitTime={}s", lockKey, waitTime);
                throw new BusinessException(500, "系统繁忙，请稍后重试");
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断: lockKey={}", lockKey, e);
            throw new BusinessException(500, "系统繁忙，请稍后重试");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 获取分布式锁，失败时静默跳过（用于后台任务）
     *
     * @param client  Redisson客户端
     * @param lockKey 锁key
     * @param action  持有锁后要执行的操作
     */
    public static void executeWithLockSilently(RedissonClient client, String lockKey, Runnable action) {
        executeWithLockSilently(client, lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, action);
    }

    /**
     * 获取分布式锁，失败时静默跳过
     */
    public static void executeWithLockSilently(RedissonClient client, String lockKey,
                                              long waitTime, long leaseTime, Runnable action) {
        if (client == null) {
            log.warn("RedissonClient为空，跳过锁操作: {}", lockKey);
            return;
        }

        RLock lock = client.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("获取分布式锁失败（静默跳过）: lockKey={}", lockKey);
                return;
            }
            action.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断（静默跳过）: lockKey={}", lockKey, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 尝试获取锁，不阻塞
     *
     * @param client  Redisson客户端
     * @param lockKey 锁key
     * @return 是否获取成功
     */
    public static boolean tryLock(RedissonClient client, String lockKey) {
        if (client == null) {
            return false;
        }
        RLock lock = client.getLock(lockKey);
        try {
            return lock.tryLock(0, DEFAULT_LEASE_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放锁
     *
     * @param lock 锁对象
     */
    public static void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}

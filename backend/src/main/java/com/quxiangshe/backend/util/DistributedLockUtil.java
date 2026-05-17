package com.quxiangshe.backend.util;

import com.quxiangshe.backend.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁工具类
 * 
 * <p>基于Redisson封装分布式锁的获取、释放逻辑，提供"获取锁→执行→释放"的模板化操作，
 * 消除项目中重复的锁获取样板代码。支持两种失败处理策略：抛异常抛出和静默跳过。</p>
 * 
 * <p>使用场景：防止多实例并发操作共享资源（如库存扣减、缓存重建、定时任务互斥）。</p>
 * 
 * @author 趣享社技术团队
 * @since 1.0
 */
@Slf4j
public class DistributedLockUtil {

    /** 默认等待获取锁的超时时间（秒），超过则获取失败 */
    private static final long DEFAULT_WAIT_TIME = 5;
    /** 默认锁持有时间（秒），超时自动释放，防止死锁 */
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
            // 尝试获取锁，最长等待waitTime秒
            acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!acquired) {
                // 等待超时，说明当前有其他实例持有锁
                log.warn("获取分布式锁失败: lockKey={}, waitTime={}s", lockKey, waitTime);
                throw new BusinessException(500, "系统繁忙，请稍后重试");
            }
            // 成功获取锁，执行业务操作
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断: lockKey={}", lockKey, e);
            throw new BusinessException(500, "系统繁忙，请稍后重试");
        } finally {
            // 仅当当前线程持锁时才释放，防止误释放其他线程的锁
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 获取分布式锁，失败时静默跳过（用于后台任务）
     * 
     * <p>适用于定时任务或后台清理任务等非关键操作，获取锁失败直接放弃，
     * 不抛出异常，由其他实例或下次调度继续处理。</p>
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
     * 
     * @param client    Redisson客户端
     * @param lockKey   锁key
     * @param waitTime  等待时间（秒）
     * @param leaseTime 持有时间（秒）
     * @param action    持有锁后要执行的操作
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
            // 尝试获取锁，失败不抛异常直接返回
            acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("获取分布式锁失败（静默跳过）: lockKey={}", lockKey);
                return;
            }
            // 获取锁成功，执行任务
            action.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断（静默跳过）: lockKey={}", lockKey, e);
        } finally {
            // 安全释放：仅释放当前线程持有的锁
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 尝试获取锁，不阻塞（waitTime=0）
     * 
     * <p>仅尝试一次，无论是否获取到锁都立即返回，不会阻塞当前线程，
     * 适用于需要快速判断资源是否被占用的场景。</p>
     *
     * @param client  Redisson客户端
     * @param lockKey 锁key
     * @return true-获取成功，false-获取失败（锁被占用或client为空）
     */
    public static boolean tryLock(RedissonClient client, String lockKey) {
        if (client == null) {
            return false;
        }
        RLock lock = client.getLock(lockKey);
        try {
            // waitTime=0 表示不等待，立即返回结果
            return lock.tryLock(0, DEFAULT_LEASE_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放分布式锁
     * 
     * <p>安全释放：先校验锁是否由当前线程持有，避免释放其他线程/进程持有的锁，
     * 防止锁语义被破坏。</p>
     *
     * @param lock 锁对象，可为null
     */
    public static void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}

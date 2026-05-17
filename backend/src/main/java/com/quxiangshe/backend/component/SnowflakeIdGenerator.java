package com.quxiangshe.backend.component;

import org.springframework.stereotype.Component;

/**
 * 雪花算法（Snowflake）分布式ID生成器
 * 
 * <p>基于Twitter Snowflake算法实现，生成全局唯一的64位长整型ID。
 * 简化了标准Snowflake中的机器ID部分，重点支持单机高并发下的序列号自增。</p>
 * 
 * <p>ID结构（63位）：<br>
 * 41位时间戳（相对2021-01-01的毫秒差，可用约69年）+<br>
 * 10位序列号（每毫秒最多1024个ID）+<br>
 * 12位保留（可为机器ID预留）</p>
 * 
 * <p>性能特性：单机每秒可生成约100万ID，适合亿级流量场景。</p>
 * 
 * @author 趣享社技术团队
 * @since 1.0
 */
@Component
public class SnowflakeIdGenerator {
    
    /** 纪元起始时间：2021-01-01 00:00:00（UTC+8），减少时间戳位数 */
    private static final long EPOCH = 1609459200000L;
    /** 序列号占用位数 */
    private static final long SEQUENCE_BITS = 10L;
    /** 序列号掩码：用于取低10位，最大值1023 */
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;
    
    /** 当前毫秒内的序列号（0-1023） */
    private long sequence = 0;
    /** 上一次生成ID的时间戳，用于判断是否同一毫秒 */
    private long lastTimestamp = -1;
    
    /**
     * 生成下一个全局唯一雪花ID
     * 
     * <p>synchronized保证线程安全，同一毫秒内序列号递增，
     * 序列号溢出时阻塞等待到下一毫秒。时钟回拨时直接抛异常，
     * 拒绝生成可能重复的ID。</p>
     *
     * @return 64位长整型雪花ID，全局唯一且趋势递增
     * @throws RuntimeException 检测到时钟回拨时抛出
     */
    public synchronized long nextId() {
        // 相对于纪元的毫秒时间戳，减小数值范围
        long timestamp = System.currentTimeMillis() - EPOCH;
        
        // 时钟回拨检测：时间戳不能小于上一次记录值
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }
        
        if (timestamp == lastTimestamp) {
            // 同一毫秒内，序列号+1
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 序列号溢出（超过1023），等待下一毫秒
            if (sequence == 0) {
                timestamp = waitNextMillis();
            }
        } else {
            // 进入新的一毫秒，序列号从0开始
            sequence = 0;
        }
        
        lastTimestamp = timestamp;
        
        // 组合：时间戳左移10位，低10位放序列号
        return (timestamp << SEQUENCE_BITS) | sequence;
    }
    
    /**
     * 从雪花ID中提取序列号部分
     * 
     * <p>Retention阶段使用序列号作为Feed收件箱的Score，
     * 避免ID数值过大导致精度丢失。</p>
     *
     * @param snowflakeId 雪花ID
     * @return 序列号，范围0-1023
     */
    public long getSequence(long snowflakeId) {
        // 按位与掩码，提取低10位序列号
        return snowflakeId & SEQUENCE_MASK;
    }
    
    /**
     * 自旋等待到下一毫秒
     * 
     * <p>当当前毫秒内序列号耗尽时，忙等待直到系统时间进入下一毫秒，
     * 确保生成的ID在时间维度上严格递增。</p>
     *
     * @return 下一毫秒的时间戳（相对于纪元）
     */
    private long waitNextMillis() {
        long timestamp = System.currentTimeMillis() - EPOCH;
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis() - EPOCH;
        }
        return timestamp;
    }
}
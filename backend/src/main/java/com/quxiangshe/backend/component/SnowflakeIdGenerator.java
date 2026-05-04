package com.quxiangshe.backend.component;

import org.springframework.stereotype.Component;

/**
 * 雪花ID生成器
 * 支持亿级并发流量
 * 
 * 结构: 41位时间戳 + 10位序列号 + 12位机器ID (共63位)
 * 
 * @author 趣享社技术团队
 */
@Component
public class SnowflakeIdGenerator {
    
    private static final long EPOCH = 1609459200000L; // 2021-01-01 00:00:00
    private static final long SEQUENCE_BITS = 10L;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;
    
    private long sequence = 0;
    private long lastTimestamp = -1;
    
    /**
     * 生成下一个雪花ID
     * @return 雪花ID
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis() - EPOCH;
        
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }
        
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitNextMillis();
            }
        } else {
            sequence = 0;
        }
        
        lastTimestamp = timestamp;
        
        return (timestamp << SEQUENCE_BITS) | sequence;
    }
    
    /**
     * 获取序列号部分（用于Score计算）
     * @param snowflakeId 雪花ID
     * @return 序列号 (0-1023)
     */
    public long getSequence(long snowflakeId) {
        return snowflakeId & SEQUENCE_MASK;
    }
    
    private long waitNextMillis() {
        long timestamp = System.currentTimeMillis() - EPOCH;
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis() - EPOCH;
        }
        return timestamp;
    }
}
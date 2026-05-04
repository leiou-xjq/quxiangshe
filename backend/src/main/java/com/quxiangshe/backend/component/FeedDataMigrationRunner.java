package com.quxiangshe.backend.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Feed流Redis数据迁移组件
 * 启动时自动迁移旧格式Score数据
 * 
 * 旧格式: score = 时间戳 (如 1712645678000)
 * 新格式: score = 时间戳 * 1024 + 序列号 (如 1752999999999999...)
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedDataMigrationRunner implements CommandLineRunner {
    
    private final StringRedisTemplate redisTemplate;
    
    // Score序列号位数
    private static final long SCORE_SEQUENCE_BITS = 1024L;
    // 旧格式Score阈值
    private static final double OLD_SCORE_THRESHOLD = 1_000_000_000_000L;
    
    @Override
    public void run(String... args) {
        log.info("开始迁移Feed流Redis数据...");
        long startTime = System.currentTimeMillis();
        
        int migratedCount = 0;
        
        // 1. 迁移发件箱数据
        migratedCount += migrateDataByPattern("feed:outbox:pull:*");
        
        // 2. 迁移收件箱数据
        migratedCount += migrateDataByPattern("feed:inbox:push:*");
        
        long costTime = System.currentTimeMillis() - startTime;
        log.info("Feed流Redis数据迁移完成: 迁移{}条, 耗时{}ms", migratedCount, costTime);
    }
    
    /**
     * 按模式迁移数据
     */
    private int migrateDataByPattern(String pattern) {
        Set<String> keys = scanKeys(pattern);
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        
        int totalMigrated = 0;
        for (String key : keys) {
            try {
                int count = migrateZSetData(key);
                if (count > 0) {
                    totalMigrated += count;
                    log.info("迁移完成: {}, 迁移{}条", key, count);
                }
            } catch (Exception e) {
                log.error("迁移失败: {}, 错误: {}", key, e.getMessage());
            }
        }
        
        return totalMigrated;
    }
    
    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        try {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }
        } catch (Exception e) {
            log.warn("扫描Key失败: {}, 原因: {}", pattern, e.getMessage());
        }
        return keys;
    }
    
    /**
     * 迁移单个ZSet数据
     */
    private int migrateZSetData(String key) {
        Set<ZSetOperations.TypedTuple<String>> data = 
            redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
        
        if (data == null || data.isEmpty()) {
            return 0;
        }
        
        // 检查是否需要迁移
        boolean needMigration = false;
        for (ZSetOperations.TypedTuple<String> tuple : data) {
            if (tuple.getScore() != null && tuple.getScore() < OLD_SCORE_THRESHOLD) {
                needMigration = true;
                break;
            }
        }
        
        if (!needMigration) {
            return 0;
        }
        
        // 迁移数据
        int count = 0;
        long sequence = 0;
        
        // 先删除所有旧数据
        Set<String> values = redisTemplate.opsForZSet().range(key, 0, -1);
        if (values != null && !values.isEmpty()) {
            for (String value : values) {
                redisTemplate.opsForZSet().remove(key, value);
            }
        }
        
        // 重新写入新格式数据
        for (ZSetOperations.TypedTuple<String> tuple : data) {
            Double oldScore = tuple.getScore();
            String value = tuple.getValue();
            
            if (value == null || oldScore == null) {
                continue;
            }
            
            // 转换为新格式: 时间戳 * 1024 + 递增序列号
            long timestamp = oldScore.longValue();
            long newScore = timestamp * SCORE_SEQUENCE_BITS + (sequence++);
            
            redisTemplate.opsForZSet().add(key, value, newScore);
            count++;
        }
        
        // 设置过期时间
        redisTemplate.expire(key, 7, TimeUnit.DAYS);
        
        return count;
    }
}
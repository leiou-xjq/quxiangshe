package com.quxiangshe.backend.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/**
 * 监控告警调度器
 * 监控关键任务执行状态，记录告警日志
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringAlertScheduler {

    private final StringRedisTemplate redisTemplate;

    private static final String LAST_RECONCILIATION_KEY = "monitor:last:reconciliation";
    private static final String LAST_CLEANUP_KEY = "monitor:last:cleanup";
    private static final String LAST_HOT_DECAY_KEY = "monitor:last:hot_decay";
    private static final int ALERT_THRESHOLD_MINUTES = 10; // 超过10分钟未执行则告警

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 每分钟检查关键任务状态
     */
    @Scheduled(fixedRate = 60 * 1000)
    public void checkTaskHealth() {
        log.debug("开始检查任务健康状态...");
        
        checkReconciliationTask();
        checkCleanupTask();
        checkHotDecayTask();
        
        log.debug("任务健康检查完成");
    }

    /**
     * 检查对账任务
     */
    private void checkReconciliationTask() {
        try {
            String lastTime = redisTemplate.opsForValue().get(LAST_RECONCILIATION_KEY);
            if (lastTime == null) {
                log.warn("[告警] 对账任务尚未执行过");
                return;
            }
            
            LocalDateTime lastExecution = LocalDateTime.parse(lastTime, FORMATTER);
            long minutesSinceLast = java.time.Duration.between(lastExecution, LocalDateTime.now()).toMinutes();
            
            if (minutesSinceLast > ALERT_THRESHOLD_MINUTES) {
                log.error("[严重告警] 对账任务超过" + ALERT_THRESHOLD_MINUTES + "分钟未执行! 上次执行时间: " + lastTime);
            } else {
                log.info("对账任务状态正常，上次执行: {}, {}分钟前", lastTime, minutesSinceLast);
            }
        } catch (Exception e) {
            log.warn("检查对账任务状态失败", e);
        }
    }

    /**
     * 检查清理任务
     */
    private void checkCleanupTask() {
        try {
            String lastTime = redisTemplate.opsForValue().get(LAST_CLEANUP_KEY);
            if (lastTime == null) {
                log.info("清理任务尚未执行过（可能未到执行时间）");
                return;
            }
            
            LocalDateTime lastExecution = LocalDateTime.parse(lastTime, FORMATTER);
            long minutesSinceLast = java.time.Duration.between(lastExecution, LocalDateTime.now()).toMinutes();
            
            if (minutesSinceLast > ALERT_THRESHOLD_MINUTES + 120) { // 清理任务每天凌晨3点执行，多给2小时
                log.warn("[告警] 清理任务超过" + ((minutesSinceLast + 60) / 60) + "小时未执行!");
            }
        } catch (Exception e) {
            log.warn("检查清理任务状态失败", e);
        }
    }

    /**
     * 检查热度衰减任务
     */
    private void checkHotDecayTask() {
        try {
            String lastTime = redisTemplate.opsForValue().get(LAST_HOT_DECAY_KEY);
            if (lastTime == null) {
                log.info("热度衰减任务尚未执行过");
                return;
            }
            
            LocalDateTime lastExecution = LocalDateTime.parse(lastTime, FORMATTER);
            long minutesSinceLast = java.time.Duration.between(lastExecution, LocalDateTime.now()).toMinutes();
            
            if (minutesSinceLast > 24 * 60 + 30) { // 每天凌晨执行，超过24.5小时则告警
                log.warn("[告警] 热度衰减任务超过24小时未执行!");
            }
        } catch (Exception e) {
            log.warn("检查热度衰减任务状态失败", e);
        }
    }

    /**
     * 记录任务执行时间 - 供对账任务调用
     */
    public void recordReconciliationExecution() {
        redisTemplate.opsForValue().set(LAST_RECONCILIATION_KEY, 
            LocalDateTime.now().format(FORMATTER));
    }

    /**
     * 记录任务执行时间 - 供清理任务调用
     */
    public void recordCleanupExecution() {
        redisTemplate.opsForValue().set(LAST_CLEANUP_KEY, 
            LocalDateTime.now().format(FORMATTER));
    }

    /**
     * 记录任务执行时间 - 供热度衰减任务调用
     */
    public void recordHotDecayExecution() {
        redisTemplate.opsForValue().set(LAST_HOT_DECAY_KEY, 
            LocalDateTime.now().format(FORMATTER));
    }
    
    /**
     * 记录任务失败 - 供各任务捕获异常后调用
     * @param taskName 任务名称
     * @param errorMessage 错误信息
     */
    public void recordTaskFailure(String taskName, String errorMessage) {
        String key = "monitor:failed:" + taskName;
        redisTemplate.opsForValue().set(key, errorMessage);
        log.error("[任务失败告警] {}: {}", taskName, errorMessage);
    }
    
    /**
     * 检查是否有未恢复的任务失败
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void checkUnrecoveredFailures() {
        try {
            // 使用 SCAN 替代 KEYS
            Set<String> keys = scanKeys("monitor:failed:*");
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    String error = redisTemplate.opsForValue().get(key);
                    String taskName = key.replace("monitor:failed:", "");
                    log.error("[未恢复的失败] 任务: {}, 错误: {}", taskName, error);
                }
            }
        } catch (Exception e) {
            log.warn("检查未恢复失败失败", e);
        }
    }
    
    /**
     * 使用 SCAN 遍历 Key
     */
    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build();
        
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        } catch (Exception e) {
            log.warn("SCAN 遍历失败: pattern={}", pattern, e);
        }
        return keys;
    }
}
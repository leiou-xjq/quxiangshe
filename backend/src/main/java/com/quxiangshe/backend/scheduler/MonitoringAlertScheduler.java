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
 * <p>监控关键定时任务的执行状态，当任务超过阈值时间未执行时记录告警日志。
 * <p>监控目标：
 * <ul>
 *   <li>对账任务（ReconciliationScheduler）：超过10分钟未执行 → 严重告警</li>
 *   <li>清理任务（RedisKeyCleanupScheduler）：超过2小时未执行 → 告警</li>
 *   <li>热度衰减任务（HotScoreScheduler）：超过24.5小时未执行 → 告警</li>
 * </ul>
 * <p>各任务执行完毕后调用对应的 record*Execution 方法记录时间戳到Redis
 *
 * @author 趣享社技术团队
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringAlertScheduler {

    private final StringRedisTemplate redisTemplate;

    /** 各关键任务的上次执行时间Key */
    private static final String LAST_RECONCILIATION_KEY = "monitor:last:reconciliation";
    private static final String LAST_CLEANUP_KEY = "monitor:last:cleanup";
    private static final String LAST_HOT_DECAY_KEY = "monitor:last:hot_decay";
    /** 告警阈值：超过此分钟数未执行则触发告警 */
    private static final int ALERT_THRESHOLD_MINUTES = 10; // 超过10分钟未执行则告警

    /** 时间格式化器，用于Redis中存储可读的时间字符串 */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 每分钟检查关键任务健康状态
     * <p>依次检查对账、清理、热度衰减三个关键任务的最后执行时间
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
     * <p>对账任务每5分钟执行一次，超过10分钟未执行则触发严重告警
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
     * <p>清理任务每天凌晨3点执行，阈值放宽至2小时以上
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
            
            // 清理任务每天凌晨3点执行，多给2小时缓冲
            if (minutesSinceLast > ALERT_THRESHOLD_MINUTES + 120) { // 清理任务每天凌晨3点执行，多给2小时
                log.warn("[告警] 清理任务超过" + ((minutesSinceLast + 60) / 60) + "小时未执行!");
            }
        } catch (Exception e) {
            log.warn("检查清理任务状态失败", e);
        }
    }

    /**
     * 检查热度衰减任务
     * <p>热度衰减每天凌晨00:05执行，超过24.5小时未执行则告警
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
            
            // 每天凌晨执行，超过24.5小时则告警
            if (minutesSinceLast > 24 * 60 + 30) { // 每天凌晨执行，超过24.5小时则告警
                log.warn("[告警] 热度衰减任务超过24小时未执行!");
            }
        } catch (Exception e) {
            log.warn("检查热度衰减任务状态失败", e);
        }
    }

    /**
     * 记录对账任务执行时间
     * <p>供 {@link ReconciliationScheduler} 在每次执行完毕后调用
     */
    public void recordReconciliationExecution() {
        redisTemplate.opsForValue().set(LAST_RECONCILIATION_KEY, 
            LocalDateTime.now().format(FORMATTER));
    }

    /**
     * 记录清理任务执行时间
     * <p>供 {@link RedisKeyCleanupScheduler} 在每次执行完毕后调用
     */
    public void recordCleanupExecution() {
        redisTemplate.opsForValue().set(LAST_CLEANUP_KEY, 
            LocalDateTime.now().format(FORMATTER));
    }

    /**
     * 记录热度衰减任务执行时间
     * <p>供 {@link HotScoreScheduler} 在每次执行完毕后调用
     */
    public void recordHotDecayExecution() {
        redisTemplate.opsForValue().set(LAST_HOT_DECAY_KEY, 
            LocalDateTime.now().format(FORMATTER));
    }
    
    /**
     * 记录任务失败
     * <p>供各定时任务在捕获异常后调用，将失败信息写入Redis并记录告警日志
     *
     * @param taskName     任务名称
     * @param errorMessage 错误信息
     */
    public void recordTaskFailure(String taskName, String errorMessage) {
        String key = "monitor:failed:" + taskName;
        redisTemplate.opsForValue().set(key, errorMessage);
        log.error("[任务失败告警] {}: {}", taskName, errorMessage);
    }
    
    /**
     * 每5分钟检查是否有未恢复的任务失败
     * <p>扫描 monitor:failed:* Key，若存在则记录告警日志
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void checkUnrecoveredFailures() {
        try {
            // 使用 SCAN 替代 KEYS，避免阻塞 Redis
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
     * 使用SCAN命令遍历匹配的Redis Key
     * <p>相比KEYS命令，SCAN以游标方式分批迭代，不会阻塞Redis服务线程
     *
     * @param pattern Key匹配模式
     * @return 匹配到的Key集合
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
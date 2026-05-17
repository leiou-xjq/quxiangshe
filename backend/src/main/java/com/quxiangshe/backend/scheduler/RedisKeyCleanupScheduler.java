package com.quxiangshe.backend.scheduler;

import com.quxiangshe.backend.mapper.FollowMapper;
import com.quxiangshe.backend.mapper.NoteMapper;
import com.quxiangshe.backend.entity.Note;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quxiangshe.backend.entity.User;
import com.quxiangshe.backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Redis旧Key清理定时任务
 * <p>负责清理旧格式的Redis缓存Key，确保系统使用的Key格式统一升级。
 * 同时提供粉丝数缓存的手动清理与重建能力，用于修复因序列化格式变更导致的数据问题。
 *
 * <p>清理模式包括：
 * <ul>
 *   <li>feed:follower:count:* — Java序列化格式的旧粉丝数缓存</li>
 *   <li>feed:following:*    — 旧的关注列表缓存</li>
 *   <li>feed:user:*         — 旧的用户Feed缓存</li>
 *   <li>note:hot:v2         — 旧版热度榜单（临时Key残留）</li>
 * </ul>
 *
 * @author 趣享社技术团队
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisKeyCleanupScheduler {

    private final StringRedisTemplate redisTemplate;
    private final NoteMapper noteMapper;
    private final FollowMapper followMapper;
    private final UserMapper userMapper;

    /** 新格式的粉丝数缓存Key前缀 */
    private static final String FOLLOWER_COUNT_KEY = "feed:follower:count:";
    private static final int SCAN_BATCH_SIZE = 100;
    
    /** 通过Setter注入监控告警调度器，用于记录清理任务的执行时间 */
    @org.springframework.beans.factory.annotation.Autowired
    public void setMonitoringAlertScheduler(MonitoringAlertScheduler monitoringAlertScheduler) {
        this.monitoringAlertScheduler = monitoringAlertScheduler;
    }
    private static final int BATCH_SIZE = 50;

    // ========== 需要清理的旧 Key 模式 ==========
    private static final String[] OLD_KEY_PATTERNS = {
        "feed:follower:count:*",       // Java 序列化格式的旧粉丝数缓存
        "feed:following:*",             // 旧的关注列表缓存
        "feed:user:*",                  // 旧的用户 Feed 缓存
        "note:hot:v2",                  // 旧的热点榜
    };

    private MonitoringAlertScheduler monitoringAlertScheduler;

    /**
     * 每天凌晨3点执行旧Key清理
     * <p>遍历所有预设的旧Key模式，使用SCAN命令安全删除
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldKeys() {
        log.info("开始清理旧 Redis Key...");
        long startTime = System.currentTimeMillis();
        int totalCleaned = 0;

        // 按模式逐一清理，分别记录删除数量
        for (String pattern : OLD_KEY_PATTERNS) {
            int cleaned = cleanupKeysByPattern(pattern);
            totalCleaned += cleaned;
            log.info("清理模式 {}: 删除 {} 个 Key", pattern, cleaned);
        }

        long costTime = System.currentTimeMillis() - startTime;
        log.info("旧 Key 清理完成: 共删除 {} 个 Key, 耗时 {}ms", totalCleaned, costTime);
        
        // 记录执行时间供监控检查
        if (monitoringAlertScheduler != null) {
            monitoringAlertScheduler.recordCleanupExecution();
        }
    }

    /**
     * 根据模式清理Key
     * <p>使用SCAN命令遍历匹配的Key，收集后批量删除，避免阻塞Redis
     *
     * @param pattern Key匹配模式
     * @return 删除的Key数量
     */
    private int cleanupKeysByPattern(String pattern) {
        int count = 0;
        Set<String> keys = new HashSet<>();
        
        try {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(100)
                    .build();
            
            // 使用SCAN游标迭代，避免在大量Key场景下阻塞Redis
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }

            // 收集完毕后一次性批量删除
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                count = keys.size();
            }
        } catch (Exception e) {
            log.error("清理 Key 失败: pattern={}", pattern, e);
        }

        return count;
    }

    /**
     * 手动触发清理 — 清理粉丝数缓存中的Java序列化旧格式数据
     * <p>Java序列化数据以特定的二进制魔数（\xac\xed）开头，value中会包含"java.lang.Long"等类型信息。
     * 此方法扫描所有粉丝数缓存Key，将值为Java序列化格式的Key标记为待删除
     *
     * <p>API: POST /admin/cleanup/follower-cache
     *
     * @return 清理的Key数量
     */
    public int cleanupFollowerCache() {
        log.info("开始清理粉丝数缓存中的旧格式数据...");
        
        Set<String> oldKeys = new HashSet<>();
        
        // 查找 feed:follower:count:* 模式的 key
        ScanOptions options = ScanOptions.scanOptions()
                .match("feed:follower:count:*")
                .count(100)
                .build();
        
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                // 检测是否为Java序列化格式：以NULL字符(\u0000)开头或包含序列化类名
                String value = redisTemplate.opsForValue().get(key);
                if (value != null && (value.startsWith("\u0000") || value.contains("java.lang.Long"))) {
                    oldKeys.add(key);
                }
            }
        } catch (Exception e) {
            log.error("扫描粉丝数缓存失败", e);
        }

        if (!oldKeys.isEmpty()) {
            redisTemplate.delete(oldKeys);
            log.info("清理粉丝数旧缓存完成: 删除 {} 个 Key", oldKeys.size());
        }

        return oldKeys.size();
    }

    /**
     * 重建粉丝数缓存 — 从数据库全量同步粉丝数到Redis
     * <p>批量遍历所有用户，查询每个用户的粉丝数，以String格式写入Redis。
     * 适用于粉丝数缓存大面积损坏或格式迁移后的数据重建场景
     *
     * <p>API: POST /admin/rebuild/follower-cache
     *
     * @return 重建的用户数量
     */
    public int rebuildFollowerCache() {
        log.info("开始重建粉丝数缓存...");
        long startTime = System.currentTimeMillis();
        int totalRebuilt = 0;
        
        try {
            // 1. 获取所有用户
            List<User> users = userMapper.selectList(null);
            log.info("共有 {} 个用户需要重建粉丝数缓存", users.size());
            
            // 2. 批量处理每个用户的粉丝数，每批BATCH_SIZE个用户
            List<Long> userIds = users.stream().map(User::getId).toList();
            
            for (int i = 0; i < userIds.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, userIds.size());
                List<Long> batchUserIds = userIds.subList(i, end);
                
                for (Long userId : batchUserIds) {
                    try {
                        // 查询该用户的粉丝数
                        Long followerCount = followMapper.selectCount(
                            new LambdaQueryWrapper<com.quxiangshe.backend.entity.Follow>()
                                .eq(com.quxiangshe.backend.entity.Follow::getFollowingId, userId)
                        );
                        
                        // 写入 Redis（使用 String 格式，兼容新架构）
                        redisTemplate.opsForValue().set(
                            FOLLOWER_COUNT_KEY + userId,
                            String.valueOf(followerCount != null ? followerCount : 0)
                        );
                        totalRebuilt++;
                    } catch (Exception e) {
                        log.warn("重建粉丝数缓存失败: userId={}", userId, e);
                    }
                }
            }
            
            long costTime = System.currentTimeMillis() - startTime;
            log.info("粉丝数缓存重建完成: 共重建 {} 个用户, 耗时 {}ms", totalRebuilt, costTime);
            
        } catch (Exception e) {
            log.error("粉丝数缓存重建失败", e);
        }
        
        return totalRebuilt;
    }
}
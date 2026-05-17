package com.quxiangshe.backend.scheduler;

import com.quxiangshe.backend.mapper.NoteMapper;
import com.quxiangshe.backend.entity.Note;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 定时对账任务
 * 用于同步 Redis 缓存中的计数值与数据库，确保数据一致性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationScheduler {

    private final StringRedisTemplate redisTemplate;
    private final NoteMapper noteMapper;
    private final MonitoringAlertScheduler monitoringAlertScheduler;

    private static final String NOTE_LIKE_COUNT_KEY = "note:like:count:";
    private static final String NOTE_FAVORITE_COUNT_KEY = "note:favorite:count:";
    private static final String NOTE_VIEW_COUNT_KEY = "note:view:count:";
    private static final String NOTE_FORWARD_COUNT_KEY = "note:forward:count:";
    private static final String NOTE_COMMENT_COUNT_KEY = "note:comment:count:";

    private static final int SCAN_BATCH_SIZE = 100;
    private static final int DB_BATCH_SIZE = 50;

    /**
     * 每5分钟执行一次计数器对账
     * <p>依次对账点赞、收藏、浏览、转发、评论五种计数，
     * 将Redis中的计数值同步回数据库，确保缓存与数据库最终一致
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void reconcileNoteCounts() {
        log.info("开始笔记计数器对账任务...");
        long startTime = System.currentTimeMillis();
        
        // 分别对各类型计数器执行对账
        int likeCountReconciled = reconcileCount(NOTE_LIKE_COUNT_KEY, "likeCount");
        int favoriteCountReconciled = reconcileCount(NOTE_FAVORITE_COUNT_KEY, "favoriteCount");
        int viewCountReconciled = reconcileCount(NOTE_VIEW_COUNT_KEY, "viewCount");
        int forwardCountReconciled = reconcileCount(NOTE_FORWARD_COUNT_KEY, "forwardCount");
        int commentCountReconciled = reconcileCount(NOTE_COMMENT_COUNT_KEY, "commentCount");
        
        long costTime = System.currentTimeMillis() - startTime;
        log.info("笔记计数器对账完成: 点赞{}条, 收藏{}条, 浏览{}条, 转发{}条, 评论{}条, 耗时{}ms",
            likeCountReconciled, favoriteCountReconciled, viewCountReconciled, 
            forwardCountReconciled, commentCountReconciled, costTime);
        
        // 记录执行时间供监控检查
        monitoringAlertScheduler.recordReconciliationExecution();
    }

    /**
     * 对账单个计数器类型
     * <p>使用SCAN命令遍历Redis Key（避免KEYS阻塞），
     * 将Redis中的值与数据库做比对，不一致则以Redis为准回写数据库
     *
     * @param keyPrefix      Redis Key前缀，如 "note:like:count:"
     * @param countFieldName 数据库计数字段名，用于反射设值
     * @return 对账修正的笔记数量
     */
    private int reconcileCount(String keyPrefix, String countFieldName) {
        int reconciled = 0;
        List<Note> batchNotes = new ArrayList<>();
        
        try {
            // 使用 SCAN 替代 KEYS，避免阻塞 Redis
            Set<String> keys = scanKeys(keyPrefix + "*");
            if (keys == null || keys.isEmpty()) {
                return 0;
            }

            for (String key : keys) {
                try {
                    // 从Key中解析出笔记ID
                    String noteIdStr = key.substring(keyPrefix.length());
                    Long noteId = Long.parseLong(noteIdStr);
                    
                    // 获取 Redis 中的值
                    String redisValue = redisTemplate.opsForValue().get(key);
                    if (redisValue == null) {
                        continue;
                    }
                    int redisCount = Integer.parseInt(redisValue);
                    
                    // 获取数据库中的值
                    Note note = noteMapper.selectById(noteId);
                    if (note == null) {
                        continue;
                    }
                    
                    // 获取数据库当前值
                    int dbCount = 0;
                    switch (countFieldName) {
                        case "likeCount": dbCount = note.getLikeCount() != null ? note.getLikeCount() : 0; break;
                        case "favoriteCount": dbCount = note.getFavoriteCount() != null ? note.getFavoriteCount() : 0; break;
                        case "viewCount": dbCount = note.getViewCount() != null ? note.getViewCount() : 0; break;
                        case "forwardCount": dbCount = note.getForwardCount() != null ? note.getForwardCount() : 0; break;
                        case "commentCount": dbCount = note.getCommentCount() != null ? note.getCommentCount() : 0; break;
                    }
                    
                    // 如果不一致，收集到批量列表
                    if (redisCount != dbCount) {
                        // 以Redis为准构建更新对象
                        Note updateNote = new Note();
                        updateNote.setId(noteId);
                        switch (countFieldName) {
                            case "likeCount": updateNote.setLikeCount(redisCount); break;
                            case "favoriteCount": updateNote.setFavoriteCount(redisCount); break;
                            case "viewCount": updateNote.setViewCount(redisCount); break;
                            case "forwardCount": updateNote.setForwardCount(redisCount); break;
                            case "commentCount": updateNote.setCommentCount(redisCount); break;
                        }
                        batchNotes.add(updateNote);
                        reconciled++;
                        
                        // 达到批量大小则执行批量更新
                        if (batchNotes.size() >= DB_BATCH_SIZE) {
                            batchUpdateNotes(batchNotes);
                            batchNotes.clear();
                        }
                    }
                } catch (Exception e) {
                    log.warn("对账处理单个key失败: key={}", key, e);
                }
            }
            
            // 处理剩余的批量
            if (!batchNotes.isEmpty()) {
                batchUpdateNotes(batchNotes);
            }
            
        } catch (Exception e) {
            log.error("计数器对账失败: field={}", countFieldName, e);
        }
        return reconciled;
    }
    
    /**
     * 批量更新笔记数据
     */
    private void batchUpdateNotes(List<Note> notes) {
        for (Note note : notes) {
            try {
                noteMapper.updateById(note);
            } catch (Exception e) {
                log.warn("批量更新笔记失败: noteId={}", note.getId(), e);
            }
        }
    }
    
    /**
     * 使用 SCAN 遍历 Redis Key，避免阻塞
     */
    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(SCAN_BATCH_SIZE)
                .build();
        
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        } catch (Exception e) {
            log.error("SCAN 遍历失败: pattern={}", pattern, e);
        }
        return keys;
    }
}
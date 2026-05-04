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
     * 每5分钟执行一次计數器对账
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void reconcileNoteCounts() {
        log.info("开始笔记计数器对账任务...");
        long startTime = System.currentTimeMillis();
        
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
     * 对账单个计数器类型 - 使用 SCAN 避免阻塞 + 批量处理
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
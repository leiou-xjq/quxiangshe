package com.quxiangshe.backend.scheduler;

import com.quxiangshe.backend.mapper.NoteMapper;
import com.quxiangshe.backend.entity.Note;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 热点榜单定时任务
 * 1. 热度衰减：每日凌晨执行，0.9系数（有数据才执行）
 * 2. 增量同步：有新增/更新笔记才执行
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotScoreScheduler {
    
    private final StringRedisTemplate redisTemplate;
    private final NoteMapper noteMapper;
    
    private static final String HOT_RANK_KEY = "note:hot";
    private static final String HOT_RANK_KEY_V2 = "note:hot:v2";
    private static final String HOT_LAST_SYNC_KEY = "note:hot:last_sync";
    private static final double DECAY_FACTOR = 0.9;
    private static final int MAX_HOT_NOTES = 10000; // 热门榜单最多保留条数
    
    /**
     * 每日凌晨00:05执行热度衰减
     * 使用双key保证衰减期间榜单可用
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void decayHotScore() {
        log.info("开始热度衰减，系数: {}", DECAY_FACTOR);
        
        try {
            Long size = redisTemplate.opsForZSet().size(HOT_RANK_KEY);
            if (size == null || size == 0) {
                log.info("热度榜单为空，跳过衰减");
                return;
            }
            
            log.info("热度榜单有{}条数据，执行衰减", size);
            
            Set<String> noteIds = redisTemplate.opsForZSet().range(HOT_RANK_KEY, 0, -1);
            if (noteIds == null || noteIds.isEmpty()) {
                log.info("热度榜单为空，跳过衰减");
                return;
            }
            
            String tempKey = HOT_RANK_KEY_V2;
            redisTemplate.delete(tempKey);
            
            List<String> noteIdList = new ArrayList<>(noteIds);
            List<List<String>> batches = new ArrayList<>();
            for (int i = 0; i < noteIdList.size(); i += 500) {
                batches.add(noteIdList.subList(i, Math.min(i + 500, noteIdList.size())));
            }
            
            for (List<String> batch : batches) {
                List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    for (String noteId : batch) {
                        connection.zSetCommands().zScore(HOT_RANK_KEY.getBytes(), noteId.getBytes());
                    }
                    return null;
                });
                
                int idx = 0;
                for (String noteId : batch) {
                    Double score = (Double) results.get(idx++);
                    if (score != null && score > 0) {
                        double newScore = score * DECAY_FACTOR;
                        redisTemplate.opsForZSet().add(tempKey, noteId, newScore);
                    }
                }
            }
            
            redisTemplate.delete(HOT_RANK_KEY);
            redisTemplate.rename(tempKey, HOT_RANK_KEY);
            redisTemplate.expire(HOT_RANK_KEY, 7, TimeUnit.DAYS);
            
            // 超过最大限制时，删除最低分的记录
            Long finalSize = redisTemplate.opsForZSet().size(HOT_RANK_KEY);
            if (finalSize != null && finalSize > MAX_HOT_NOTES) {
                redisTemplate.opsForZSet().removeRange(HOT_RANK_KEY, 0, finalSize - MAX_HOT_NOTES - 1);
                log.info("热度衰减后清理超量数据，保留{}条，删除{}条", MAX_HOT_NOTES, finalSize - MAX_HOT_NOTES);
            }
            
            log.info("热度衰减完成，处理{}条数据", noteIds.size());
            
        } catch (RedisConnectionFailureException e) {
            log.error("Redis连接失败，跳过热度衰减", e);
        } catch (Exception e) {
            log.error("热度衰减失败: 原因: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 每小时执行增量同步
     */
    @Scheduled(cron = "0 30 * * * ?")
    public void syncHotScoreToRedis() {
        long startTime = System.currentTimeMillis();
        
        try {
            String lastSyncStr = redisTemplate.opsForValue().get(HOT_LAST_SYNC_KEY);
            long lastSyncTime = 0;
            if (lastSyncStr != null) {
                lastSyncTime = Long.parseLong(lastSyncStr);
            }
            
            Long changedCount = noteMapper.countChangedNotes(lastSyncTime);
            
            if (changedCount == 0) {
                log.info("没有新笔记或更新的笔记，跳过同步");
                return;
            }
            
            log.info("开始增量同步热度到Redis，有{}条变化的笔记...", changedCount);
            
            List<Note> changedNotes = noteMapper.selectChangedNotes(lastSyncTime);
            
            if (changedNotes == null || changedNotes.isEmpty()) {
                return;
            }
            
            List<Note> toAdd = new ArrayList<>();
            List<String> toRemove = new ArrayList<>();
            for (Note note : changedNotes) {
                if (note.getStatus() == 1) {
                    toAdd.add(note);
                } else {
                    toRemove.add(note.getId().toString());
                }
            }
            
            if (!toAdd.isEmpty()) {
                redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    for (Note note : toAdd) {
                        double hotScore = calculateHotScore(note);
                        connection.zSetCommands().zAdd(HOT_RANK_KEY.getBytes(), hotScore, note.getId().toString().getBytes());
                    }
                    return null;
                });
            }
            
            if (!toRemove.isEmpty()) {
                redisTemplate.opsForZSet().remove(HOT_RANK_KEY, toRemove.toArray(new String[0]));
            }
            
            redisTemplate.expire(HOT_RANK_KEY, 7, TimeUnit.DAYS);
            redisTemplate.opsForValue().set(HOT_LAST_SYNC_KEY, String.valueOf(System.currentTimeMillis()));
            
            long costTime = System.currentTimeMillis() - startTime;
            log.info("增量同步完成，笔记数: {}, 耗时: {}ms", changedNotes.size(), costTime);
            
        } catch (NumberFormatException e) {
            log.error("同步时间戳解析失败: 原因: {}", e.getMessage());
        } catch (RedisConnectionFailureException e) {
            log.error("Redis连接失败，跳过同步", e);
        } catch (Exception e) {
            log.error("同步热度失败: 原因: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 计算热度
     * hotScore = likeCount × 1 + commentCount × 2 + favoriteCount × 3 + forwardCount × 5
     */
    private double calculateHotScore(Note note) {
        int likeCount = note.getLikeCount() != null ? note.getLikeCount() : 0;
        int commentCount = note.getCommentCount() != null ? note.getCommentCount() : 0;
        int favoriteCount = note.getFavoriteCount() != null ? note.getFavoriteCount() : 0;
        int forwardCount = note.getForwardCount() != null ? note.getForwardCount() : 0;
        
        return likeCount * 1.0 + commentCount * 2.0 + favoriteCount * 3.0 + forwardCount * 5.0;
    }
    
    /**
     * 初始化热点榜单（手动触发）
     */
    public void initHotScore() {
        syncHotScoreToRedis();
    }
}
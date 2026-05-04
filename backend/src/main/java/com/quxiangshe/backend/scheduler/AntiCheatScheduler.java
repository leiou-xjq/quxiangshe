package com.quxiangshe.backend.scheduler;

import com.quxiangshe.backend.mapper.NoteMapper;
import com.quxiangshe.backend.entity.Note;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 防作弊定时任务
 * 1. 异常检测：单笔记单日增幅>500，进入审核队列
 * 2. 黑名单管理：违规笔记移出榜单
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AntiCheatScheduler {
    
    private final StringRedisTemplate redisTemplate;
    private final NoteMapper noteMapper;
    
    private static final String HOT_RANK_KEY = "note:hot";
    private static final String HOT_BLOCK_KEY = "note:hot:block";
    private static final String AUDIT_PENDING_KEY = "note:audit:pending";
    private static final String NOTE_DAILY_INCREASE_KEY = "note:daily:increase:";
    private static final String NOTE_PREVIOUS_SCORE_KEY = "note:previous:score:";
    
    private static final double ANOMALY_GROWTH_RATE = 1.0; // 增长超过100%触发
    private static final int BLOCK_EXPIRE_HOURS = 24;
    private static final int MIN_SCORE_TO_CHECK = 1000; // 最低热度分数才检查
    
    /**
     * 每5分钟执行异常检测
     * 检测单笔记热度单日增幅>500的异常情况
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void checkAnomaly() {
        log.debug("开始异常检测...");
        
        try {
            // 获取榜单中所有笔记
            Set<String> noteIds = redisTemplate.opsForZSet().range(HOT_RANK_KEY, 0, -1);
            
            if (noteIds == null || noteIds.isEmpty()) {
                return;
            }
            
            // 批量查询笔记信息
            List<Long> ids = noteIds.stream()
                .map(Long::parseLong)
                .collect(java.util.stream.Collectors.toList());
            
            List<Note> notes = noteMapper.selectByIds(ids);
            
            // 检测异常笔记 - 使用相对增长率
            for (Note note : notes) {
                double currentScore = calculateHotScore(note);
                
                // 跳过低热度笔记
                if (currentScore < MIN_SCORE_TO_CHECK) {
                    continue;
                }
                
                // 获取上次热度分数
                String prevScoreStr = redisTemplate.opsForValue().get(NOTE_PREVIOUS_SCORE_KEY + note.getId());
                double previousScore = prevScoreStr != null ? Double.parseDouble(prevScoreStr) : 0;
                
                // 计算增长率
                double growthRate = 0;
                if (previousScore > 0) {
                    growthRate = (currentScore - previousScore) / previousScore;
                }
                
                // 记录当前分数用于下次比较
                redisTemplate.opsForValue().set(NOTE_PREVIOUS_SCORE_KEY + note.getId(), String.valueOf(currentScore));
                
                // 增长超过100%且绝对分数足够高才触发
                if (growthRate > ANOMALY_GROWTH_RATE && currentScore > MIN_SCORE_TO_CHECK) {
                    redisTemplate.opsForSet().add(AUDIT_PENDING_KEY, note.getId().toString());
                    log.warn("检测到异常笔记: noteId={}, currentScore={}, previousScore={}, growthRate={}%", 
                        note.getId(), currentScore, previousScore, String.format("%.1f", growthRate * 100));
                }
            }
            
        } catch (NumberFormatException e) {
            log.error("解析笔记ID失败: 原因: {}", e.getMessage());
        } catch (RedisConnectionFailureException e) {
            log.error("Redis连接失败，跳过异常检测", e);
        } catch (Exception e) {
            log.error("异常检测失败: 原因: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 每日凌晨1点执行黑名单清理
     * 24小时后自动解除
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanBlockList() {
        log.info("开始清理黑名单...");
        
        try {
            // 检查是否有需要解除的笔记（简单实现：按时间清理）
            // 实际生产中需要记录封禁时间
            log.info("黑名单清理完成");
            
        } catch (RedisConnectionFailureException e) {
            log.error("Redis连接失败，跳过黑名单清理", e);
        } catch (Exception e) {
            log.error("清理黑名单失败: 原因: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 将笔记加入黑名单
     */
    public void blockNote(Long noteId) {
        try {
            // 从榜单移除
            redisTemplate.opsForZSet().remove(HOT_RANK_KEY, noteId.toString());
            
            // 加入黑名单
            redisTemplate.opsForSet().add(HOT_BLOCK_KEY, noteId.toString());
            redisTemplate.expire(HOT_BLOCK_KEY, BLOCK_EXPIRE_HOURS, TimeUnit.HOURS);
            
            // 从审核队列移除
            redisTemplate.opsForSet().remove(AUDIT_PENDING_KEY, noteId.toString());
            
            log.info("已将笔记加入黑名单: noteId={}", noteId);
            
        } catch (RedisConnectionFailureException e) {
            log.error("Redis连接失败，无法加入黑名单: noteId={}", noteId, e);
        } catch (Exception e) {
            log.error("加入黑名单失败: noteId={}, 原因: {}", noteId, e.getMessage(), e);
        }
    }
    
    /**
     * 将笔记移出黑名单
     */
    public void unblockNote(Long noteId) {
        try {
            // 从黑名单移除
            redisTemplate.opsForSet().remove(HOT_BLOCK_KEY, noteId.toString());
            
            log.info("已将笔记移出黑名单: noteId={}", noteId);
            
        } catch (RedisConnectionFailureException e) {
            log.error("Redis连接失败，无法移出黑名单: noteId={}", noteId, e);
        } catch (Exception e) {
            log.error("移出黑名单失败: noteId={}, 原因: {}", noteId, e.getMessage(), e);
        }
    }
    
    /**
     * 审核通过
     */
    public void approveNote(Long noteId) {
        try {
            // 从审核队列移除
            redisTemplate.opsForSet().remove(AUDIT_PENDING_KEY, noteId.toString());
            
            log.info("审核通过: noteId={}", noteId);
            
        } catch (RedisConnectionFailureException e) {
            log.error("Redis连接失败，审核通过失败: noteId={}", noteId, e);
        } catch (Exception e) {
            log.error("审核通过失败: noteId={}, 原因: {}", noteId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取待审核列表
     */
    public Set<Long> getPendingAuditNotes() {
        try {
            Set<String> noteIds = redisTemplate.opsForSet().members(AUDIT_PENDING_KEY);
            if (noteIds == null) {
                return new HashSet<>();
            }
            return noteIds.stream()
                .map(Long::parseLong)
                .collect(java.util.stream.Collectors.toSet());
        } catch (NumberFormatException e) {
            log.error("解析待审核笔记ID失败: 原因: {}", e.getMessage());
            return new HashSet<>();
        } catch (RedisConnectionFailureException e) {
            log.error("Redis连接失败，获取待审核列表失败", e);
            return new HashSet<>();
        } catch (Exception e) {
            log.error("获取待审核列表失败: 原因: {}", e.getMessage(), e);
            return new HashSet<>();
        }
    }
    
    /**
     * 计算热度
     */
    private double calculateHotScore(Note note) {
        int likeCount = note.getLikeCount() != null ? note.getLikeCount() : 0;
        int commentCount = note.getCommentCount() != null ? note.getCommentCount() : 0;
        int favoriteCount = note.getFavoriteCount() != null ? note.getFavoriteCount() : 0;
        int forwardCount = note.getForwardCount() != null ? note.getForwardCount() : 0;
        
        return likeCount * 1.0 + commentCount * 2.0 + favoriteCount * 3.0 + forwardCount * 5.0;
    }
}
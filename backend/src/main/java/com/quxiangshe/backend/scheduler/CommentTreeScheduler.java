package com.quxiangshe.backend.scheduler;

import com.quxiangshe.backend.mapper.NoteCommentMapper;
import com.quxiangshe.backend.service.sort.FullSortStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quxiangshe.backend.entity.NoteComment;
import com.quxiangshe.backend.entity.Note;
import com.quxiangshe.backend.mapper.NoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 评论树定时同步任务
 * <p>负责将Redis中的评论计数缓存与数据库保持一致。
 * 每5分钟扫描所有笔记的评论数缓存Key（post:*:comment_count），
 * 从数据库查询真实评论数并回写Redis，同时修正数据库中不一致的评论数。
 *
 * @author 趣享社技术团队
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentTreeScheduler {
    
    private final StringRedisTemplate redisTemplate;
    private final FullSortStrategy fullSortStrategy;
    private final NoteCommentMapper noteCommentMapper;
    private final NoteMapper noteMapper;
    
    /**
     * 每5分钟同步Redis评论数与数据库
     * <p>流程：
     * <ol>
     *   <li>扫描所有 post:*:comment_count 格式的Key</li>
     *   <li>从Key中解析noteId，查数据库获取真实评论数</li>
     *   <li>将真实评论数回写Redis缓存</li>
     *   <li>若数据库中comment_count字段不一致则同步修正</li>
     * </ol>
     */
    @Scheduled(fixedRate = 300000)
    public void syncCommentCount() {
        try {
            // 使用KEYS命令扫描评论数缓存Key（pattern: post:*:comment_count）
            Set<String> countKeys = redisTemplate.keys("post:*:comment_count");
            if (countKeys == null || countKeys.isEmpty()) {
                return;
            }
            
            int synced = 0;
            for (String key : countKeys) {
                try {
                    // 从Key格式 "post:{noteId}:comment_count" 中解析笔记ID
                    String noteIdStr = key.split(":")[1];
                    Long noteId = Long.parseLong(noteIdStr);
                    
                    // 从数据库查询真实的正常状态评论数
                    long dbCount = noteCommentMapper.selectCount(
                            new LambdaQueryWrapper<NoteComment>()
                                    .eq(NoteComment::getNoteId, noteId)
                                    .eq(NoteComment::getStatus, 1)
                    );
                    
                    // 以数据库为准回写Redis
                    redisTemplate.opsForValue().set(key, String.valueOf(dbCount));
                    
                    // 若笔记记录的评论数字段与数据库不一致则同步修正
                    Note note = noteMapper.selectById(noteId);
                    if (note != null && note.getCommentCount() != dbCount) {
                        note.setCommentCount((int) dbCount);
                        noteMapper.updateById(note);
                        synced++;
                    }
                } catch (Exception e) {
                    log.warn("同步评论数失败: key={}, error={}", key, e.getMessage());
                }
            }
            
            if (synced > 0) {
                log.info("同步评论数与数据库: {}条笔记", synced);
            }
        } catch (Exception e) {
            log.error("同步评论数失败: {}", e.getMessage());
        }
    }
}
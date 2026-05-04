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
     */
    @Scheduled(fixedRate = 300000)
    public void syncCommentCount() {
        try {
            Set<String> countKeys = redisTemplate.keys("post:*:comment_count");
            if (countKeys == null || countKeys.isEmpty()) {
                return;
            }
            
            int synced = 0;
            for (String key : countKeys) {
                try {
                    String noteIdStr = key.split(":")[1];
                    Long noteId = Long.parseLong(noteIdStr);
                    
                    long dbCount = noteCommentMapper.selectCount(
                            new LambdaQueryWrapper<NoteComment>()
                                    .eq(NoteComment::getNoteId, noteId)
                                    .eq(NoteComment::getStatus, 1)
                    );
                    
                    redisTemplate.opsForValue().set(key, String.valueOf(dbCount));
                    
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
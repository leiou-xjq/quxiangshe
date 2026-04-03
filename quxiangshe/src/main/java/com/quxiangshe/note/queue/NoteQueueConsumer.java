package com.quxiangshe.note.queue;

import com.quxiangshe.note.entity.NoteEntity;
import com.quxiangshe.note.service.NoteSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * 笔记队列消费者
 * 监听笔记队列，异步同步笔记到Elasticsearch
 * 支持失败重试机制
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoteQueueConsumer {

    private final NoteSearchService noteSearchService;

    @RabbitListener(queues = "note.queue")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void handleNoteMessage(NoteEntity note) {
        log.info("收到笔记消息: noteId={}, deleted={}, auditStatus={}, status={}", 
                note.getId(), note.getDeleted(), note.getAuditStatus(), note.getStatus());

        try {
            if (note.getDeleted() == 1 || note.getAuditStatus() != NoteEntity.AUDIT_PASSED) {
                noteSearchService.deleteNoteIndex(note.getId());
                log.info("笔记审核未通过或已删除，从ES删除: noteId={}", note.getId());
            } else if (note.getStatus() == NoteEntity.STATUS_NORMAL) {
                noteSearchService.indexNote(note);
                log.info("笔记索引ES成功: noteId={}", note.getId());
            }
        } catch (Exception e) {
            log.error("笔记消息处理失败，准备重试: noteId={}", note.getId(), e);
            throw e;
        }
    }
}

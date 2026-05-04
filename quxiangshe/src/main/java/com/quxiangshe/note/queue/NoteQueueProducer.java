package com.quxiangshe.note.queue;

import com.quxiangshe.note.entity.NoteEntity;
import com.quxiangshe.common.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 笔记队列生产者
 * 使用RabbitMQ异步同步笔记到ES
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoteQueueProducer {

    /**
     * RabbitMQ模板
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送笔记消息
     * 用于异步同步笔记到Elasticsearch
     *
     * @param note 笔记实体
     */
    public void sendNoteMessage(NoteEntity note) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTE_EXCHANGE,
                    RabbitMQConfig.NOTE_ROUTING_KEY,
                    note
            );
            log.info("笔记消息已发送: noteId={}, deleted={}, status={}", 
                    note.getId(), note.getDeleted(), note.getStatus());
        } catch (Exception e) {
            log.error("发送笔记消息失败: noteId={}", note.getId(), e);
            throw e;
        }
    }
}

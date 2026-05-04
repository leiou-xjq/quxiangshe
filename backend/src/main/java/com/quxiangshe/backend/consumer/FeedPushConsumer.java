package com.quxiangshe.backend.consumer;

import com.quxiangshe.backend.config.RabbitMQConfig;
import com.quxiangshe.backend.dto.FeedPushMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedPushConsumer {
    
    private static final String PUSH_INBOX_PREFIX = "feed:inbox:push:";
    
    private final StringRedisTemplate redisTemplate;
    
    @RabbitListener(queues = RabbitMQConfig.FEED_PUSH_QUEUE)
    public void consumeFeedPush(FeedPushMessage message) {
        log.info("收到Feed推送消息: noteId={}, batch={}/{}", 
                message.getNoteId(), message.getBatchNum() + 1, message.getTotalBatches());
        
        try {
            if (message.getTargetUserIds() == null || message.getTargetUserIds().isEmpty()) {
                log.warn("Feed推送目标用户列表为空: noteId={}", message.getNoteId());
                return;
            }
            
            long score = System.currentTimeMillis();
            String noteIdStr = message.getNoteId().toString();
            
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (Long userId : message.getTargetUserIds()) {
                    String feedKey = PUSH_INBOX_PREFIX + userId;
                    byte[] keyBytes = feedKey.getBytes();
                    byte[] valueBytes = noteIdStr.getBytes();
                    connection.zAdd(keyBytes, score, valueBytes);
                }
                return null;
            });
            
            log.info("Feed推送完成: noteId={}, 推送用户数={}",
                    message.getNoteId(), message.getTargetUserIds().size());
        } catch (Exception e) {
            log.error("Feed推送失败: noteId={}, error={}",
                    message.getNoteId(), e.getMessage());
            throw new RuntimeException("Feed推送失败", e);
        }
    }
    
    @RabbitListener(queues = RabbitMQConfig.FEED_DLX_QUEUE)
    public void consumeDeadLetter(FeedPushMessage message) {
        log.error("Feed推送进入死信队列: noteId={}, batch={}/{}, error: 消息处理失败",
                message.getNoteId(), message.getBatchNum() + 1, message.getTotalBatches());
        
    }
}
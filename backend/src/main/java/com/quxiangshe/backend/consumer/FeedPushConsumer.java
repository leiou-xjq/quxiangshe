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

/**
 * Feed推送消费者
 * 
 * <p>监听Feed推送队列，批量将笔记ID写入目标用户的Feed收件箱（Redis ZSet）。
 * 使用Redis Pipeline实现批量写入，一次网络往返写入多用户数据。
 * 同时监听死信队列，记录推送失败的笔记信息便于人工介入。</p>
 * 
 * @author 趣享社技术团队
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedPushConsumer {
    
    /** Redis中Feed收件箱的key前缀，格式：feed:inbox:push:{userId} */
    private static final String PUSH_INBOX_PREFIX = "feed:inbox:push:";
    
    private final StringRedisTemplate redisTemplate;
    
    /**
     * 消费Feed推送消息，批量写入用户收件箱
     * 
     * <p>使用Redis Pipeline机制，将一批用户的Feed写入操作打包发送，
     * 相比逐条写入减少网络RTT开销，适合大批量粉丝推送场景。</p>
     *
     * @param message Feed推送消息，包含笔记ID和目标用户列表
     */
    @RabbitListener(queues = RabbitMQConfig.FEED_PUSH_QUEUE)
    public void consumeFeedPush(FeedPushMessage message) {
        log.info("收到Feed推送消息: noteId={}, batch={}/{}", 
                message.getNoteId(), message.getBatchNum() + 1, message.getTotalBatches());
        
        try {
            // 防御性检查：目标用户列表为空时直接跳过
            if (message.getTargetUserIds() == null || message.getTargetUserIds().isEmpty()) {
                log.warn("Feed推送目标用户列表为空: noteId={}", message.getNoteId());
                return;
            }
            
            // 使用当前时间戳作为ZSet的score，实现按时间排序
            long score = System.currentTimeMillis();
            String noteIdStr = message.getNoteId().toString();
            
            // Pipeline批量写入：一次网络往返完成所有用户的ZAdd操作
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (Long userId : message.getTargetUserIds()) {
                    // 每个用户的Feed收件箱是一个ZSet：key=feed:inbox:push:{userId}
                    String feedKey = PUSH_INBOX_PREFIX + userId;
                    byte[] keyBytes = feedKey.getBytes();
                    byte[] valueBytes = noteIdStr.getBytes();
                    // score=时间戳，value=笔记ID
                    connection.zAdd(keyBytes, score, valueBytes);
                }
                return null;
            });
            
            log.info("Feed推送完成: noteId={}, 推送用户数={}",
                    message.getNoteId(), message.getTargetUserIds().size());
        } catch (Exception e) {
            log.error("Feed推送失败: noteId={}, error={}",
                    message.getNoteId(), e.getMessage());
            // 抛出异常触发RabbitMQ重试/进入死信队列
            throw new RuntimeException("Feed推送失败", e);
        }
    }
    
    /**
     * 消费死信队列中的Feed推送消息
     * 
     * <p>死信消息（多次重试失败）无法自动恢复，此处记录详细日志，
     * 供运维人员排查后手动重新推送。</p>
     *
     * @param message 进入死信队列的Feed推送消息
     */
    @RabbitListener(queues = RabbitMQConfig.FEED_DLX_QUEUE)
    public void consumeDeadLetter(FeedPushMessage message) {
        log.error("Feed推送进入死信队列: noteId={}, batch={}/{}, error: 消息处理失败",
                message.getNoteId(), message.getBatchNum() + 1, message.getTotalBatches());
        
    }
}
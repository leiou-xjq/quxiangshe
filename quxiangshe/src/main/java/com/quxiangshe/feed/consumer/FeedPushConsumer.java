package com.quxiangshe.feed.consumer;

import com.quxiangshe.common.config.RabbitMQConfig;
import com.quxiangshe.feed.service.FeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Feed推送消费者
 * 监听动态发布事件，触发推模式
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedPushConsumer {

    private final FeedService feedService;

    /**
     * 监听Feed推送队列
     * 消息格式：postId:creatorId
     */
    @RabbitListener(queues = RabbitMQConfig.FEED_PUSH_QUEUE)
    public void consume(String message) {
        log.info("收到Feed推送任务: message={}", message);

        try {
            // 解析消息：postId:creatorId
            String[] parts = message.split(":");
            if (parts.length < 2) {
                log.warn("消息格式错误: {}", message);
                return;
            }

            Long postId = Long.parseLong(parts[0]);
            Long creatorId = Long.parseLong(parts[1]);

            // 触发推模式
            feedService.pushToInbox(postId, creatorId);

            log.info("Feed推送完成: postId={}, creatorId={}", postId, creatorId);

        } catch (Exception e) {
            log.error("Feed推送任务处理失败: message={}, error={}", message, e.getMessage(), e);
        }
    }
}

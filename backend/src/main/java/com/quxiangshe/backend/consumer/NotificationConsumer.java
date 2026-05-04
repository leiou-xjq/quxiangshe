package com.quxiangshe.backend.consumer;

import com.quxiangshe.backend.config.RabbitMQConfig;
import com.quxiangshe.backend.dto.NotificationMessage;
import com.quxiangshe.backend.service.INotificationService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private static final int MAX_RETRY_COUNT = 3;

    private final INotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void consumeNotification(NotificationMessage message, Message amqpMessage, Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("收到通知消息: type={}, userId={}, fromUserId={}",
                message.getType(), message.getUserId(), message.getFromUserId());

        int retryCount = getRetryCount(amqpMessage);

        try {
            handleNotification(message);

            channel.basicAck(deliveryTag, false);
            log.info("通知消息处理成功: type={}, userId={}", message.getType(), message.getUserId());

        } catch (Exception e) {
            log.error("通知消息处理失败: type={}, userId={}, retryCount={}, error={}",
                    message.getType(), message.getUserId(), retryCount, e.getMessage(), e);

            handleFailure(channel, amqpMessage, deliveryTag, retryCount, e);
        }
    }

    private void handleNotification(NotificationMessage message) {
        switch (message.getType()) {
            case NotificationMessage.TYPE_LIKE:
                notificationService.sendLikeNotification(
                        message.getNoteId(), message.getUserId(), message.getFromUserId());
                break;
            case NotificationMessage.TYPE_COMMENT:
                notificationService.sendCommentNotification(
                        message.getNoteId(), message.getCommentId(),
                        message.getUserId(), message.getFromUserId());
                break;
            case NotificationMessage.TYPE_FOLLOW:
                notificationService.sendFollowNotification(
                        message.getUserId(), message.getFromUserId());
                break;
            case NotificationMessage.TYPE_REVIEW_PASSED:
                notificationService.sendReviewPassedNotification(
                        message.getNoteId(), message.getUserId());
                break;
            case NotificationMessage.TYPE_REVIEW_REJECTED:
                notificationService.sendReviewRejectedNotification(
                        message.getNoteId(), message.getUserId(), message.getExtra());
                break;
            default:
                log.warn("未知的通知类型: {}", message.getType());
        }
    }

    private void handleFailure(Channel channel, Message amqpMessage, long deliveryTag,
                               int retryCount, Exception e) {
        try {
            if (retryCount < MAX_RETRY_COUNT) {
                String retryHeader = String.valueOf(retryCount + 1);
                amqpMessage.getMessageProperties().setHeader("x-retry-count", retryHeader);

                channel.basicNack(deliveryTag, false, true);
                log.warn("通知消息重试: retryCount={}", retryCount + 1);
            } else {
                channel.basicNack(deliveryTag, false, false);
                log.error("通知消息超过最大重试次数，丢弃消息: retryCount={}", retryCount);
            }
        } catch (Exception ackEx) {
            log.error("确认消息失败时出现异常: {}", ackEx.getMessage(), ackEx);
        }
    }

    private int getRetryCount(Message message) {
        Object retryHeader = message.getMessageProperties().getHeader("x-retry-count");
        if (retryHeader != null) {
            try {
                return Integer.parseInt(retryHeader.toString());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}
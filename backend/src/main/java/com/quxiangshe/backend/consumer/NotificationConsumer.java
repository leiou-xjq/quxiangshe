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

/**
 * 通知消息消费者
 * 
 * <p>监听RabbitMQ通知队列，消费点赞、评论、关注、审核结果等通知消息。
 * 使用手动ACK模式，支持失败重试（最多3次），超过重试上限后丢弃消息避免死循环。
 * 通过自定义消息头"x-retry-count"记录重试次数。</p>
 * 
 * @author 趣享社技术团队
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    /** 最大重试次数，超过后丢弃消息 */
    private static final int MAX_RETRY_COUNT = 3;

    private final INotificationService notificationService;

    /**
     * 消费通知消息
     * 
     * <p>处理流程：读取重试次数 → 根据通知类型路由到对应Service方法 → 
     * 成功则手动ACK，失败则根据重试次数决定NACK重新入队或丢弃。</p>
     *
     * @param message      通知消息体
     * @param amqpMessage  RabbitMQ原始消息，用于读取重试头
     * @param channel      通道对象，用于发送ACK/NACK
     * @param deliveryTag  投递标签
     */
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

    /**
     * 根据通知类型路由到对应Service处理
     * 
     * <p>switch-case分发，每种通知类型对应不同的业务处理接口，
     * 未来扩展新类型时在此处增加case分支即可。</p>
     *
     * @param message 通知消息
     */
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

    /**
     * 处理消息消费失败的情况
     * 
     * <p>失败分级处理策略：<br>
     * 1. 未超过最大重试次数 → NACK并重新入队，重试计数+1<br>
     * 2. 超过最大重试次数 → NACK且不入队，丢弃消息防止无限重试</p>
     *
     * @param channel     RabbitMQ通道
     * @param amqpMessage 原始消息对象
     * @param deliveryTag 投递标签
     * @param retryCount  当前已重试次数
     * @param e           消费异常
     */
    private void handleFailure(Channel channel, Message amqpMessage, long deliveryTag,
                               int retryCount, Exception e) {
        try {
            if (retryCount < MAX_RETRY_COUNT) {
                // 更新重试计数到头信息中
                String retryHeader = String.valueOf(retryCount + 1);
                amqpMessage.getMessageProperties().setHeader("x-retry-count", retryHeader);

                // 拒绝并重新入队（requeue=true）
                channel.basicNack(deliveryTag, false, true);
                log.warn("通知消息重试: retryCount={}", retryCount + 1);
            } else {
                // 超过最大重试次数，拒绝且不入队（requeue=false），消息被丢弃或进入死信队列
                channel.basicNack(deliveryTag, false, false);
                log.error("通知消息超过最大重试次数，丢弃消息: retryCount={}", retryCount);
            }
        } catch (Exception ackEx) {
            log.error("确认消息失败时出现异常: {}", ackEx.getMessage(), ackEx);
        }
    }

    /**
     * 从消息头中读取重试次数
     * 
     * <p>首次消费时消息头中没有"x-retry-count"，返回0表示第0次重试。</p>
     *
     * @param message RabbitMQ消息对象
     * @return 重试次数，解析异常时返回0
     */
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
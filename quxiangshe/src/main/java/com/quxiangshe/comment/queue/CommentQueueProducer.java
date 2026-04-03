package com.quxiangshe.comment.queue;

import com.quxiangshe.comment.dto.CommentMessageDTO;
import com.quxiangshe.comment.entity.CommentEntity;
import com.quxiangshe.common.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 评论队列生产者
 * 使用RabbitMQ异步处理评论发布
 * 实现可靠性：消息发送失败会抛出异常进行重试
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentQueueProducer {

    /**
     * RabbitMQ模板
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送评论消息到队列
     * 评论发布成功后立即返回，异步写入数据库
     *
     * @param comment 评论实体
     * @param status 评论状态
     */
    public void sendCommentMessage(CommentEntity comment, Integer status) {
        try {
            // 构建消息DTO
            CommentMessageDTO message = CommentMessageDTO.builder()
                    .commentId(comment.getId())
                    .articleId(comment.getArticleId())
                    .userId(comment.getUserId())
                    .targetId(comment.getTargetId())
                    .targetUserId(comment.getTargetUserId())
                    .content(comment.getContent())
                    .status(status)
                    .createTime(comment.getCreateTime().toString())
                    .build();

            // 发送到RabbitMQ
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.COMMENT_EXCHANGE,  // 交换机
                    RabbitMQConfig.COMMENT_ROUTING_KEY, // 路由键
                    message
            );

            log.info("评论消息已发送到队列: commentId={}, status={}", comment.getId(), status);
        } catch (Exception e) {
            log.error("发送评论消息失败: commentId={}", comment.getId(), e);
            throw e;
        }
    }
}

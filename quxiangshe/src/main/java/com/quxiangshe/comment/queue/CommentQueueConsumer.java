package com.quxiangshe.comment.queue;

import com.quxiangshe.comment.dto.CommentMessageDTO;
import com.quxiangshe.comment.entity.CommentEntity;
import com.quxiangshe.comment.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 评论消息消费者
 * 监听评论队列，异步处理评论写入
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentQueueConsumer {

    /**
     * 评论Mapper
     */
    private final CommentMapper commentMapper;

    /**
     * 监听评论队列，处理评论消息
     * 使用手动确认模式，确保消息可靠处理
     *
     * @param message 评论消息
     */
    @RabbitListener(queues = "comment.queue")
    public void handleCommentMessage(CommentMessageDTO message) {
        log.info("收到评论消息: commentId={}, articleId={}", message.getCommentId(), message.getArticleId());

        try {
            // 查询评论是否存在
            CommentEntity comment = commentMapper.selectById(message.getCommentId());
            if (comment == null) {
                log.warn("评论不存在，跳过处理: commentId={}", message.getCommentId());
                return;
            }

            // 更新评论状态
            comment.setStatus(message.getStatus());
            commentMapper.updateById(comment);

            log.info("评论消息处理成功: commentId={}", message.getCommentId());
        } catch (Exception e) {
            log.error("评论消息处理失败: commentId={}", message.getCommentId(), e);
            // 抛出异常让RabbitMQ进行重试
            throw e;
        }
    }
}

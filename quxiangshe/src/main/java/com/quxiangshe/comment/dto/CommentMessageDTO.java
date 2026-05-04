package com.quxiangshe.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 评论消息DTO
 * 用于RabbitMQ异步处理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    private Long commentId;

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 评论用户ID
     */
    private Long userId;

    /**
     * 目标评论ID
     */
    private Long targetId;

    /**
     * 被回复的用户ID
     */
    private Long targetUserId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private String createTime;
}

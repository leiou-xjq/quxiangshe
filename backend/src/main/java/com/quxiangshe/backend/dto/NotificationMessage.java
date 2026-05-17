package com.quxiangshe.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知消息DTO，用于通过消息队列（MQ）异步投递系统通知。
 * <p>
 * 支持的通知类型：点赞、评论、关注、审核通过、审核未通过。
 * 每条消息包含通知类型、目标用户、触发者、关联笔记/评论及附加信息，
 * 附带时间戳保证消息时序性。
 * </p>
 *
 * @author 趣享社技术团队
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 点赞通知 */
    public static final String TYPE_LIKE = "LIKE";
    /** 评论通知 */
    public static final String TYPE_COMMENT = "COMMENT";
    /** 关注通知 */
    public static final String TYPE_FOLLOW = "FOLLOW";
    /** 审核通过通知 */
    public static final String TYPE_REVIEW_PASSED = "REVIEW_PASSED";
    /** 审核未通过通知 */
    public static final String TYPE_REVIEW_REJECTED = "REVIEW_REJECTED";

    /** 通知类型 */
    private String type;

    /** 接收通知的用户ID */
    private Long userId;

    /** 触发通知的用户ID（发送者） */
    private Long fromUserId;

    /** 关联的笔记ID（可选） */
    private Long noteId;

    /** 关联的评论ID（可选） */
    private Long commentId;

    /** 附加信息（JSON格式） */
    private String extra;

    /** 消息时间戳 */
    private LocalDateTime timestamp;
}
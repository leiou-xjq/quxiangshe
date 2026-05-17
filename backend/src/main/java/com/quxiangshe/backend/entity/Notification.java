package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知实体类，对应数据库表 notification。
 * <p>
 * 管理系统中的各类通知，包括社交互动通知（点赞、评论、关注）、
 * 系统通知以及审核结果通知（通过/未通过）。
 * 每条通知关联发送者（from_user_id）和可选的目标笔记/评论。
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Data
@TableName("notification")
public class Notification {
    
    /** 通知ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 接收通知的用户ID */
    private Long userId;
    
    /** 通知类型：1-点赞 2-评论 3-关注 4-系统 5-审核通过 6-审核未通过 */
    private Integer type;
    
    /** 触发通知的用户ID（发送者） */
    private Long fromUserId;
    
    /** 关联的笔记ID（可选） */
    private Long noteId;
    
    /** 关联的评论ID（可选） */
    private Long commentId;
    
    /** 通知文本内容 */
    private String content;
    
    /** 是否已读：0-未读 1-已读 */
    private Integer isRead;
    
    /** 通知创建时间 */
    private LocalDateTime createdAt;
    
    /** 点赞通知 */
    public static final int TYPE_LIKE = 1;
    /** 评论通知 */
    public static final int TYPE_COMMENT = 2;
    /** 关注通知 */
    public static final int TYPE_FOLLOW = 3;
    /** 系统通知 */
    public static final int TYPE_SYSTEM = 4;
    /** 审核通过通知 */
    public static final int TYPE_REVIEW_PASSED = 5;
    /** 审核未通过通知 */
    public static final int TYPE_REVIEW_REJECTED = 6;
}

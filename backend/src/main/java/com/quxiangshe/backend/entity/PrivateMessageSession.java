package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信会话实体类，对应数据库表 private_message_session。
 * <p>
 * 每个会话记录用户与另一位用户之间的私信对话状态，
 * 包含最后一条消息的摘要信息（ID、内容、时间）和未读计数。
 * 会话按最后消息时间倒序排列，用于聊天列表页展示。
 * </p>
 *
 * @author 趣享社技术团队
 */
@Data
@TableName("private_message_session")
public class PrivateMessageSession {
    
    /** 会话ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 当前用户ID */
    private Long userId;
    
    /** 对方用户ID */
    private Long targetUserId;
    
    /** 最后一条消息ID */
    private Long lastMessageId;
    
    /** 最后一条消息内容摘要 */
    private String lastMessageContent;
    
    /** 最后一条消息时间 */
    private LocalDateTime lastMessageTime;
    
    /** 未读消息数 */
    private Integer unreadCount;
    
    /** 会话创建时间 */
    private LocalDateTime createdAt;
    
    /** 会话更新时间 */
    private LocalDateTime updatedAt;
}
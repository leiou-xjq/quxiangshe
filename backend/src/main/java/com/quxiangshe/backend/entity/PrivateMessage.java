package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信消息实体类，对应数据库表 private_message。
 * <p>
 * 记录用户之间的一对一私信消息，支持文本和图片两种消息类型。
 * 每条消息归属于一个会话（session_id），支持发送后2分钟内撤回、
 * 发送者及接收者单边删除（双边删除后消息不再对任何人可见）。
 * </p>
 *
 * @author 趣享社技术团队
 */
@Data
@TableName("private_message")
public class PrivateMessage {
    
    /** 消息ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 所属会话ID */
    private Long sessionId;
    
    /** 发送者用户ID */
    private Long senderId;
    
    /** 接收者用户ID */
    private Long receiverId;
    
    /** 消息类型：1-文本 2-图片 */
    private Integer messageType;
    
    /** 消息文本内容（文本消息时使用） */
    private String content;
    
    /** 图片URL（图片消息时使用） */
    private String imageUrl;
    
    /** 是否已撤回：0-未撤回 1-已撤回 */
    private Integer isRecalled;
    
    /** 撤回时间 */
    private LocalDateTime recallTime;
    
    /** 发送者是否已删除：0-未删除 1-已删除 */
    private Integer isDeletedSender;
    
    /** 接收者是否已删除：0-未删除 1-已删除 */
    private Integer isDeletedReceiver;
    
    /** 消息创建时间 */
    private LocalDateTime createdAt;
}
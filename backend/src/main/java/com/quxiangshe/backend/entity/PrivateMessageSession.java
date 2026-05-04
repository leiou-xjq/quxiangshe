package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("private_message_session")
public class PrivateMessageSession {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long targetUserId;
    
    private Long lastMessageId;
    
    private String lastMessageContent;
    
    private LocalDateTime lastMessageTime;
    
    private Integer unreadCount;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
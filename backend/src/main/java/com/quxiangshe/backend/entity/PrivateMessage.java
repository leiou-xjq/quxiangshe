package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("private_message")
public class PrivateMessage {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long sessionId;
    
    private Long senderId;
    
    private Long receiverId;
    
    private Integer messageType;
    
    private String content;
    
    private String imageUrl;
    
    private Integer isRecalled;
    
    private LocalDateTime recallTime;
    
    private Integer isDeletedSender;
    
    private Integer isDeletedReceiver;
    
    private LocalDateTime createdAt;
}
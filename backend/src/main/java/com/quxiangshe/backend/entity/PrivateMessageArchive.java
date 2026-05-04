package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("private_message_archive")
public class PrivateMessageArchive {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long sessionId;
    
    private Long senderId;
    
    private Long receiverId;
    
    private Integer messageType;
    
    private String content;
    
    private String imageUrl;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime archivedAt;
}
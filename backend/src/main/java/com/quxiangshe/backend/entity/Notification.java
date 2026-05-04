package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知实体类
 * 
 * @author 趣享社技术团队
 */
@Data
@TableName("notification")
public class Notification {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Integer type;
    
    private Long fromUserId;
    
    private Long noteId;
    
    private Long commentId;
    
    private String content;
    
    private Integer isRead;
    
    private LocalDateTime createdAt;
    
    public static final int TYPE_LIKE = 1;
    public static final int TYPE_COMMENT = 2;
    public static final int TYPE_FOLLOW = 3;
    public static final int TYPE_SYSTEM = 4;
    // 审核通知类型
    public static final int TYPE_REVIEW_PASSED = 5;     // 审核通过
    public static final int TYPE_REVIEW_REJECTED = 6;   // 审核未通过
}

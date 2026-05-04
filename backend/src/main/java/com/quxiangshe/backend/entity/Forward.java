package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 转发记录实体类
 * 对应数据库表: note_forward
 * 
 * @author 趣享社技术团队
 */
@Data
@TableName("note_forward")
public class Forward {
    
    /**
     * 转发ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 原笔记ID
     */
    private Long originalNoteId;
    
    /**
     * 转发者用户ID
     */
    private Long userId;
    
    /**
     * 转发时的评论内容
     */
    private String content;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

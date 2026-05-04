package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记点赞关联实体类
 * 对应数据库表: note_like
 * 
 * @author 趣享社技术团队
 */
@Data
@TableName("note_like")
public class NoteLike {
    
    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 笔记ID
     */
    private Long noteId;
    
    /**
     * 点赞用户ID
     */
    private Long userId;
    
    /**
     * 点赞时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

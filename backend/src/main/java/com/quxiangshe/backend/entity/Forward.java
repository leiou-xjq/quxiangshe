package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 转发记录实体类，对应数据库表 note_forward。
 * <p>
 * 记录用户对笔记的转发操作，包括转发者和原笔记的关联关系
 * 以及转发时附加的评论文本。支持按原笔记查询所有转发记录
 * 和按用户查询转发历史。
 * </p>
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

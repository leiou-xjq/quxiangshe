package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记点赞关联实体类，对应数据库表 note_like。
 * <p>
 * 记录用户对笔记的点赞关系，每条记录表示一次点赞操作。
 * 用于去重检查（防止重复点赞）和批量查询用户点赞状态，
 * 以支持前端列表中的点赞图标高亮展示。
 * </p>
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

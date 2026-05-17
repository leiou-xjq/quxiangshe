package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记收藏关联实体类，对应数据库表 note_favorite。
 * <p>
 * 记录用户对笔记的收藏关系，每条记录表示一次收藏操作。
 * 支持按用户分页查询收藏列表、批量检查收藏状态，
 * 前端据此展示收藏图标状态及用户收藏夹内容。
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Data
@TableName("note_favorite")
public class NoteFavorite {
    
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
     * 收藏用户ID
     */
    private Long userId;
    
    /**
     * 收藏时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

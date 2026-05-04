package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 黑名单实体类
 * 对应数据库表: blacklist
 * 
 * @author 趣享社技术团队
 */
@Data
@TableName("blacklist")
public class Blacklist {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID（拉黑者）
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 被拉黑的用户ID
     */
    @TableField("blocked_id")
    private Long blockedId;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
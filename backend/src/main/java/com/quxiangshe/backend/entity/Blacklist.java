package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 黑名单实体类，对应数据库表 blacklist。
 * <p>
 * 记录用户之间的拉黑关系。user_id 为拉黑者，
 * blocked_id 为被拉黑者。被拉黑后，拉黑者将不再看到被拉黑者的内容
 * 且被拉黑者无法向拉黑者发送私信或互动。
 * </p>
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
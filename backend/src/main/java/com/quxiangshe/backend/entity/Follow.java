package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注关系实体类
 * 对应数据库表: follow
 * 
 * @author 趣享社技术团队
 */
@Data
@TableName("follow")
public class Follow {
    
    /**
     * 关注ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 关注者用户ID
     */
    @TableField("follower_id")
    private Long followerId;
    
    /**
     * 被关注者用户ID
     */
    @TableField("following_id")
    private Long followingId;
    
    /**
     * 关注时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
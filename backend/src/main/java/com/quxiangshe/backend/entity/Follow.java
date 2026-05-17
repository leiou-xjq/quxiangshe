package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注关系实体类，对应数据库表 follow。
 * <p>
 * 记录用户之间的单向关注关系。follower_id 为关注者（粉丝），
 * following_id 为被关注者（博主）。支持游标分页查询粉丝列表和关注列表。
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Data
@TableName("follow")
public class Follow {
    
    /** 关注记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 关注者用户ID（粉丝） */
    @TableField("follower_id")
    private Long followerId;
    
    /** 被关注者用户ID（博主） */
    @TableField("following_id")
    private Long followingId;
    
    /** 关注时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
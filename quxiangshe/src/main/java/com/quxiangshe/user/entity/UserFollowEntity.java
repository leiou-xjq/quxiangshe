package com.quxiangshe.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户关注关系实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_follow")
public class UserFollowEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关注者用户ID
     */
    private Long userId;

    /**
     * 被关注者用户ID
     */
    private Long followUserId;

    /**
     * 关注时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

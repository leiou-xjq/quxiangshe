package com.quxiangshe.feed.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Feed流实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("feed")
public class FeedEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（Feed所有者）
     */
    private Long userId;

    /**
     * 动态ID
     */
    private Long postId;

    /**
     * 动态创建者ID
     */
    private Long creatorId;

    /**
     * 来源类型：0-关注 1-收藏 2-推荐
     */
    private Integer sourceType;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 来源类型常量
     */
    public static final int SOURCE_TYPE_FOLLOW = 0;
    public static final int SOURCE_TYPE_FAVORITE = 1;
    public static final int SOURCE_TYPE_RECOMMEND = 2;
}

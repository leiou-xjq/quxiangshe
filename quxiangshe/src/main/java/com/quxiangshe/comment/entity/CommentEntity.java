package com.quxiangshe.comment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论实体
 * 对应数据库表 t_comment
 * 支持两层扁平化结构：一级评论和二级回复
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_comment")
public class CommentEntity {

    /**
     * 评论ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 回复的目标评论ID（0=一级评论）
     */
    private Long targetId;

    /**
     * 被回复的用户ID
     */
    private Long targetUserId;

    /**
     * 文章/内容ID
     */
    private Long articleId;

    /**
     * 评论用户ID
     */
    private Long userId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 回复数（一级评论的回复数）
     */
    private Integer replyCount;

    /**
     * 状态：0=待审核，1=正常，2=敏感词替换
     */
    private Integer status;

    /**
     * 逻辑删除：0=正常，1=已删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== 常量定义 ====================

    /**
     * 一级评论标识（target_id=0表示一级评论）
     */
    public static final long TARGET_ID_ROOT = 0L;

    /**
     * 状态：待审核
     */
    public static final int STATUS_PENDING = 0;

    /**
     * 状态：正常
     */
    public static final int STATUS_NORMAL = 1;

    /**
     * 状态：敏感词替换
     */
    public static final int STATUS_SENSITIVE_REPLACED = 2;

    /**
     * 逻辑删除：是
     */
    public static final int DELETED_YES = 1;

    /**
     * 逻辑删除：否
     */
    public static final int DELETED_NO = 0;
}

package com.quxiangshe.comment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论点赞实体
 * 对应数据库表 t_comment_like
 * 记录用户对评论的点赞信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_comment_like")
public class CommentLikeEntity {

    /**
     * 点赞记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 评论ID
     */
    private Long commentId;

    /**
     * 点赞用户ID
     */
    private Long userId;

    /**
     * 点赞时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

package com.quxiangshe.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评论响应DTO
 * 用于返回给前端的评论信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDTO {

    /**
     * 评论ID
     */
    private Long commentId;

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 评论用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像URL
     */
    private String avatarUrl;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 目标评论ID（0=一级评论）
     */
    private Long targetId;

    /**
     * 被回复的用户ID
     */
    private Long targetUserId;

    /**
     * 被回复的用户名
     */
    private String targetUsername;

    /**
     * 被回复的用户昵称
     */
    private String targetNickname;

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
     * 当前用户是否点赞
     */
    private Boolean isLiked;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 直接子评论列表（用于两层扁平展示）
     */
    private List<CommentResponseDTO> replies;
}

package com.quxiangshe.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建评论请求DTO
 */
@Data
public class CommentCreateRequestDTO {

    /**
     * 评论内容（1-1000字符）
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(min = 1, max = 1000, message = "评论内容长度1-1000字符")
    private String content;

    /**
     * 回复的目标评论ID（0表示一级评论，不传或传0表示创建一级评论）
     */
    private Long targetId = 0L;

    /**
     * 被回复的用户ID（用于标记回复目标）
     */
    private Long targetUserId;
}

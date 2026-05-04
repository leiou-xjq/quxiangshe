package com.quxiangshe.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 添加评论请求DTO
 * 
 * @author 趣享社技术团队
 */
@Data
@Schema(description = "添加评论请求")
public class CreateCommentRequest {
    
    @Schema(description = "笔记ID", required = true)
    private Long noteId;
    
    @Schema(description = "父评论ID(用于回复)")
    private Long parentId;
    
    @Schema(description = "评论内容", required = true)
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论不能超过500字")
    private String content;
}

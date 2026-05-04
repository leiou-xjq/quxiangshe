package com.quxiangshe.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 发布笔记请求DTO
 * 
 * @author 趣享社技术团队
 */
@Data
@Schema(description = "发布笔记请求")
public class CreateNoteRequest {
    
    @Schema(description = "笔记标题", required = true)
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过200字")
    private String title;
    
    @Schema(description = "笔记内容", required = true)
    @NotBlank(message = "内容不能为空")
    private String content;
    
    @Schema(description = "图片URL列表")
    private List<String> images;
    
    @Schema(description = "视频URL")
    private String video;
    
    @Schema(description = "视频封面URL")
    private String videoCover;
    
    @Schema(description = "标签列表")
    private List<String> tags;
    
    @Schema(description = "地理位置")
    private String location;
}

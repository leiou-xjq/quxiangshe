package com.quxiangshe.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 发布笔记请求DTO，用于接收前端发布笔记的HTTP请求参数。
 * <p>
 * 包含笔记的标题、内容、图片URL列表、视频URL、视频封面、
 * 标签列表及地理位置等完整发布信息。使用Jakarta Validation
 * 进行参数校验（标题必填且不超过200字，内容必填）。
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Data
@Schema(description = "发布笔记请求")
public class CreateNoteRequest {
    
    /** 笔记标题，必填，不超过200字 */
    @Schema(description = "笔记标题", required = true)
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过200字")
    private String title;
    
    /** 笔记正文内容，必填 */
    @Schema(description = "笔记内容", required = true)
    @NotBlank(message = "内容不能为空")
    private String content;
    
    /** 图片URL列表（可选） */
    @Schema(description = "图片URL列表")
    private List<String> images;
    
    /** 视频URL（可选） */
    @Schema(description = "视频URL")
    private String video;
    
    /** 视频封面URL（可选） */
    @Schema(description = "视频封面URL")
    private String videoCover;
    
    /** 标签列表（可选） */
    @Schema(description = "标签列表")
    private List<String> tags;
    
    /** 地理位置信息（可选） */
    @Schema(description = "地理位置")
    private String location;
}

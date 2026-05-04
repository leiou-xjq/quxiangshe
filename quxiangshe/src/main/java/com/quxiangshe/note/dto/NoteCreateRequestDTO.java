package com.quxiangshe.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建笔记请求DTO
 */
@Data
public class NoteCreateRequestDTO {

    /**
     * 标题（1-200字符）
     */
    @NotBlank(message = "标题不能为空")
    @Size(min = 1, max = 200, message = "标题长度1-200字符")
    private String title;

    /**
     * 正文内容（1-10000字符）
     */
    @NotBlank(message = "正文内容不能为空")
    @Size(min = 1, max = 10000, message = "正文内容长度1-10000字符")
    private String content;

    /**
     * 封面图片URL
     */
    private String coverImage;

    /**
     * 分类
     */
    private String category = "默认";

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 图片URL列表
     */
    private List<String> images;
}

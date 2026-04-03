package com.quxiangshe.note.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 笔记响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteResponseDTO {

    /**
     * 笔记ID
     */
    private Long noteId;

    /**
     * 发布者用户ID
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
     * 用户头像
     */
    private String avatarUrl;

    /**
     * 标题
     */
    private String title;

    /**
     * 正文内容
     */
    private String content;

    /**
     * 封面图片URL
     */
    private String coverImage;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 图片列表
     */
    private List<String> images;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 收藏数
     */
    private Integer collectCount;

    /**
     * 浏览数
     */
    private Integer viewCount;

    /**
     * 当前用户是否点赞
     */
    private Boolean isLiked;

    /**
     * 当前用户是否收藏
     */
    private Boolean isCollected;

    /**
     * 审核状态：0=待审核，1=通过，2=拒绝
     */
    private Integer auditStatus;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 创建时间
     */
    private String createTime;
}

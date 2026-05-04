package com.quxiangshe.feed.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Feed项DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedItemDTO {

    /**
     * 动态ID
     */
    private Long postId;

    /**
     * 发布者用户ID
     */
    private Long userId;

    /**
     * 发布者用户名
     */
    private String username;

    /**
     * 发布者昵称
     */
    private String nickname;

    /**
     * 发布者头像
     */
    private String avatarUrl;

    /**
     * 动态内容
     */
    private String content;

    /**
     * 媒体URL列表
     */
    private List<String> mediaUrls;

    /**
     * 媒体类型列表
     */
    private List<String> mediaTypes;

    /**
     * AI摘要
     */
    private String aiSummary;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 转发数
     */
    private Integer shareCount;

    /**
     * 当前用户是否点赞
     */
    private Boolean isLiked;

    /**
     * 创建时间戳（秒）
     */
    private Long createdAt;

    /**
     * 创建时间
     */
    private String createdTime;
}

package com.quxiangshe.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一搜索响应DTO
 * 支持笔记和用户两种搜索结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponseDTO {

    /**
     * 结果类型：note-笔记，user-用户
     */
    private String type;

    /**
     * 笔记结果列表
     */
    private List<SearchNoteDTO> notes;

    /**
     * 用户结果列表
     */
    private List<SearchUserDTO> users;

    /**
     * 总记录数
     */
    private Long totalCount;

    /**
     * 当前页码
     */
    private Integer page;

    /**
     * 每页数量
     */
    private Integer size;

    /**
     * 是否有更多
     */
    private Boolean hasMore;

    /**
     * 搜索耗时（毫秒）
     */
    private Long costTime;

    /**
     * 笔记搜索结果DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchNoteDTO {
        private Long noteId;
        private Long userId;
        private String username;
        private String nickname;
        private String avatarUrl;
        private String title;
        private String content;
        private String coverImage;
        private String category;
        private List<String> tags;
        private Integer likeCount;
        private Integer commentCount;
        private Integer collectCount;
        private Integer viewCount;
        private String createTime;
        private String highlightTitle;
        private String highlightContent;
    }

    /**
     * 用户搜索结果DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchUserDTO {
        private Long userId;
        private String username;
        private String nickname;
        private String avatarUrl;
        private String bio;
        private String createTime;
        private String highlightUsername;
        private String highlightNickname;
    }
}

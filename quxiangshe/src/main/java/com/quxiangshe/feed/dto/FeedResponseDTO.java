package com.quxiangshe.feed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Feed流响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedResponseDTO {

    /**
     * 动态列表
     */
    private List<FeedItemDTO> items;

    /**
     * 最后一条动态ID（用于游标）
     */
    private Long lastPostId;

    /**
     * 最后一条动态时间戳
     */
    private Long lastPostTime;

    /**
     * 是否还有更多数据
     */
    private Boolean hasMore;
}

package com.quxiangshe.feed.service;

import com.quxiangshe.feed.dto.FeedResponseDTO;

/**
 * Feed流服务接口
 */
public interface FeedService {

    /**
     * 获取用户Feed流
     * 优先从Redis收件箱拉取，如果为空则从MySQL兜底拉取
     *
     * @param userId      用户ID
     * @param lastPostId  上次返回的最后动态ID
     * @param lastPostTime 上次返回的最后动态时间（秒）
     * @param size        每页数量
     * @return Feed流响应
     */
    FeedResponseDTO getFeed(Long userId, Long lastPostId, Long lastPostTime, Integer size);

    /**
     * 推模式：发布动态时写入粉丝收件箱
     *
     * @param postId    动态ID
     * @param creatorId 创建者ID
     */
    void pushToInbox(Long postId, Long creatorId);

    /**
     * 拉模式：从MySQL兜底拉取
     *
     * @param userId      用户ID
     * @param lastPostId  上次返回的最后动态ID
     * @param lastPostTime 上次返回的最后动态时间
     * @param size        每页数量
     * @return Feed流响应
     */
    FeedResponseDTO pullFromDB(Long userId, Long lastPostId, Long lastPostTime, Integer size);
}

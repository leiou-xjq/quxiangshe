package com.quxiangshe.comment.service;

import com.quxiangshe.comment.dto.CommentResponseDTO;

/**
 * 评论点赞服务接口
 */
public interface CommentLikeService {

    /**
     * 点赞评论
     *
     * @param userId 用户ID
     * @param commentId 评论ID
     */
    void likeComment(Long userId, Long commentId);

    /**
     * 取消点赞
     *
     * @param userId 用户ID
     * @param commentId 评论ID
     */
    void unlikeComment(Long userId, Long commentId);

    /**
     * 检查用户是否已点赞
     *
     * @param userId 用户ID
     * @param commentId 评论ID
     * @return true表示已点赞
     */
    boolean checkUserLiked(Long userId, Long commentId);

    /**
     * 获取评论的点赞数
     *
     * @param commentId 评论ID
     * @return 点赞数
     */
    Long getLikeCount(Long commentId);
}

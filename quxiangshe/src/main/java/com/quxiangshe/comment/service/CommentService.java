package com.quxiangshe.comment.service;

import com.quxiangshe.comment.dto.CommentCreateRequestDTO;
import com.quxiangshe.comment.dto.CommentResponseDTO;

import java.util.List;

/**
 * 评论服务接口
 * 提供评论相关的业务操作
 */
public interface CommentService {

    /**
     * 创建评论
     * 1. 敏感词检测
     * 2. 创建评论实体
     * 3. 异步发送到RabbitMQ
     * 4. 同步更新评论数
     *
     * @param userId  用户ID
     * @param articleId 文章ID
     * @param request 评论请求
     * @return 评论响应（包含评论ID和创建时间）
     */
    CommentResponseDTO createComment(Long userId, Long articleId, CommentCreateRequestDTO request);

    /**
     * 获取文章的两层扁平评论列表
     * 一级评论 + 直接回复，按时间倒序
     *
     * @param articleId    文章ID
     * @param lastCommentId 游标ID（上一页最后一条评论的ID）
     * @param size         每页数量
     * @return 评论列表响应
     */
    CommentListResponse getComments(Long articleId, Long lastCommentId, Integer size);

    /**
     * 获取某个一级评论下的回复列表
     *
     * @param targetId      目标评论ID（一级评论ID）
     * @param articleId    文章ID
     * @param lastCommentId 游标ID
     * @param size         每页数量
     * @return 回复列表响应
     */
    CommentListResponse getReplies(Long targetId, Long articleId, Long lastCommentId, Integer size);

    /**
     * 获取某个一级评论的回复数量
     *
     * @param articleId 文章ID
     * @param targetId 目标评论ID
     * @return 回复数量
     */
    Long getReplyCount(Long articleId, Long targetId);

    /**
     * 点赞评论
     *
     * @param userId    用户ID
     * @param commentId 评论ID
     */
    void likeComment(Long userId, Long commentId);

    /**
     * 取消点赞
     *
     * @param userId    用户ID
     * @param commentId 评论ID
     */
    void unlikeComment(Long userId, Long commentId);

    /**
     * 删除评论（逻辑删除）
     * 只有评论者本人或文章作者可以删除
     *
     * @param userId    用户ID
     * @param commentId 评论ID
     */
    void deleteComment(Long userId, Long commentId);

    /**
     * 恢复评论（管理员操作）
     *
     * @param commentId 评论ID
     */
    void restoreComment(Long commentId);

    /**
     * 审核评论（管理员操作）
     *
     * @param commentId 评论ID
     * @param approved 是否通过
     */
    void reviewComment(Long commentId, boolean approved);

    /**
     * 获取文章的一级评论数量
     *
     * @param articleId 文章ID
     * @return 一级评论数量
     */
    Long getCommentCount(Long articleId);

    /**
     * 评论列表响应内部类
     */
    class CommentListResponse {
        private List<CommentResponseDTO> items;
        private Long lastCommentId;
        private Boolean hasMore;

        public CommentListResponse(List<CommentResponseDTO> items, Long lastCommentId, Boolean hasMore) {
            this.items = items;
            this.lastCommentId = lastCommentId;
            this.hasMore = hasMore;
        }

        public List<CommentResponseDTO> getItems() { return items; }
        public Long getLastCommentId() { return lastCommentId; }
        public Boolean getHasMore() { return hasMore; }
    }
}

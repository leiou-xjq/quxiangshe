package com.quxiangshe.comment.controller;

import com.quxiangshe.comment.dto.CommentCreateRequestDTO;
import com.quxiangshe.comment.dto.CommentResponseDTO;
import com.quxiangshe.comment.service.CommentService;
import com.quxiangshe.common.dto.Response;
import com.quxiangshe.common.annotation.RateLimit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 评论控制器
 * 提供评论相关的REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    /**
     * 评论服务
     */
    private final CommentService commentService;

    /**
     * 创建评论
     * 支持一级评论和回复
     * 敏感词自动检测和替换
     *
     * @param articleId 文章ID
     * @param request 评论请求
     * @param authentication 用户认证信息
     * @return 评论响应
     */
    @PostMapping({"/articles/{articleId}/comments", "/posts/{articleId}/comments"})
    @RateLimit(keyPrefix = "comment:create", limit = 10, windowMs = 60000)
    public Response<CommentResponseDTO> createComment(
            @PathVariable Long articleId,
            @Validated @RequestBody CommentCreateRequestDTO request,
            Authentication authentication) {
        
        Long userId = Long.parseLong(authentication.getName());
        log.info("创建评论: userId={}, articleId={}", userId, articleId);
        
        return Response.success(commentService.createComment(userId, articleId, request));
    }

    /**
     * 获取文章的两层扁平评论列表
     * 一级评论 + 直接回复，按时间倒序
     *
     * @param articleId 文章ID
     * @param lastCommentId 游标ID
     * @param size 每页数量
     * @return 评论列表响应
     */
    @GetMapping({"/articles/{articleId}/comments", "/posts/{articleId}/comments"})
    public Response<CommentService.CommentListResponse> getComments(
            @PathVariable Long articleId,
            @RequestParam(required = false) Long lastCommentId,
            @RequestParam(defaultValue = "20") Integer size) {
        
        log.info("获取评论列表: articleId={}, lastCommentId={}, size={}", articleId, lastCommentId, size);
        
        return Response.success(commentService.getComments(articleId, lastCommentId, size));
    }

    /**
     * 获取某个评论的回复列表
     *
     * @param commentId 评论ID（一级评论ID）
     * @param articleId 文章ID
     * @param lastCommentId 游标ID
     * @param size 每页数量
     * @return 回复列表响应
     */
    @GetMapping("/comments/{commentId}/replies")
    public Response<CommentService.CommentListResponse> getReplies(
            @PathVariable Long commentId,
            @RequestParam Long articleId,
            @RequestParam(required = false) Long lastCommentId,
            @RequestParam(defaultValue = "10") Integer size) {
        
        log.info("获取回复列表: commentId={}, articleId={}, lastCommentId={}, size={}", 
                commentId, articleId, lastCommentId, size);
        
        return Response.success(commentService.getReplies(commentId, articleId, lastCommentId, size));
    }

    /**
     * 获取回复数量
     *
     * @param commentId 评论ID
     * @param articleId 文章ID
     * @return 回复数量
     */
    @GetMapping("/comments/{commentId}/replies/count")
    public Response<Long> getReplyCount(
            @PathVariable Long commentId,
            @RequestParam Long articleId) {
        
        return Response.success(commentService.getReplyCount(articleId, commentId));
    }

    /**
     * 点赞评论
     *
     * @param commentId 评论ID
     * @param authentication 用户认证信息
     * @return 响应
     */
    @PostMapping("/comments/{commentId}/like")
    @RateLimit(keyPrefix = "comment:like", limit = 20, windowMs = 60000)
    public Response<Void> likeComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        
        Long userId = Long.parseLong(authentication.getName());
        log.info("点赞评论: userId={}, commentId={}", userId, commentId);
        
        commentService.likeComment(userId, commentId);
        return Response.success();
    }

    /**
     * 取消点赞
     *
     * @param commentId 评论ID
     * @param authentication 用户认证信息
     * @return 响应
     */
    @DeleteMapping("/comments/{commentId}/like")
    public Response<Void> unlikeComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        
        Long userId = Long.parseLong(authentication.getName());
        log.info("取消点赞: userId={}, commentId={}", userId, commentId);
        
        commentService.unlikeComment(userId, commentId);
        return Response.success();
    }

    /**
     * 删除评论（逻辑删除）
     * 评论者本人或文章作者可以删除
     *
     * @param commentId 评论ID
     * @param authentication 用户认证信息
     * @return 响应
     */
    @DeleteMapping("/comments/{commentId}")
    public Response<Void> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        
        Long userId = Long.parseLong(authentication.getName());
        log.info("删除评论: userId={}, commentId={}", userId, commentId);
        
        commentService.deleteComment(userId, commentId);
        return Response.success();
    }

    /**
     * 获取文章的一级评论数量
     *
     * @param articleId 文章ID
     * @return 评论数量
     */
    @GetMapping({"/articles/{articleId}/comments/count", "/posts/{articleId}/comments/count"})
    public Response<Long> getCommentCount(
            @PathVariable Long articleId) {
        
        return Response.success(commentService.getCommentCount(articleId));
    }

    /**
     * 恢复评论（管理员操作）
     *
     * @param commentId 评论ID
     * @return 响应
     */
    @PostMapping("/admin/comments/{commentId}/restore")
    public Response<Void> restoreComment(
            @PathVariable Long commentId) {
        
        commentService.restoreComment(commentId);
        return Response.success();
    }

    /**
     * 审核评论（管理员操作）
     *
     * @param commentId 评论ID
     * @param approved 是否通过
     * @return 响应
     */
    @PostMapping("/admin/comments/{commentId}/review")
    public Response<Void> reviewComment(
            @PathVariable Long commentId,
            @RequestParam boolean approved) {
        
        commentService.reviewComment(commentId, approved);
        return Response.success();
    }
}

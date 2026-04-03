package com.quxiangshe.comment.service.impl;

import com.quxiangshe.comment.entity.CommentEntity;
import com.quxiangshe.comment.entity.CommentLikeEntity;
import com.quxiangshe.comment.mapper.CommentLikeMapper;
import com.quxiangshe.comment.mapper.CommentMapper;
import com.quxiangshe.comment.service.CommentLikeService;
import com.quxiangshe.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 评论点赞服务实现类
 * 实现点赞去重、记录点赞信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentLikeServiceImpl implements CommentLikeService {

    /**
     * 点赞Mapper
     */
    private final CommentLikeMapper commentLikeMapper;

    /**
     * 评论Mapper
     */
    private final CommentMapper commentMapper;

    /**
     * 点赞评论
     * 1. 检查用户是否已点赞（去重）
     * 2. 插入点赞记录
     * 3. 更新评论点赞数
     *
     * @param userId 用户ID
     * @param commentId 评论ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long userId, Long commentId) {
        // 1. 检查评论是否存在
        CommentEntity comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getDeleted() == 1) {
            throw new BusinessException("评论不存在");
        }

        // 2. 检查用户是否已点赞
        if (commentLikeMapper.checkUserLiked(commentId, userId)) {
            throw new BusinessException("您已经点赞过该评论");
        }

        // 3. 插入点赞记录
        CommentLikeEntity likeEntity = CommentLikeEntity.builder()
                .commentId(commentId)
                .userId(userId)
                .createTime(LocalDateTime.now())
                .build();
        commentLikeMapper.insert(likeEntity);

        // 4. 更新评论点赞数
        commentMapper.incrementLikeCount(commentId);

        log.info("用户 {} 点赞评论 {}", userId, commentId);
    }

    /**
     * 取消点赞
     * 1. 检查用户是否已点赞
     * 2. 删除点赞记录
     * 3. 更新评论点赞数
     *
     * @param userId 用户ID
     * @param commentId 评论ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long userId, Long commentId) {
        // 1. 检查评论是否存在
        CommentEntity comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getDeleted() == 1) {
            throw new BusinessException("评论不存在");
        }

        // 2. 检查用户是否已点赞
        if (!commentLikeMapper.checkUserLiked(commentId, userId)) {
            throw new BusinessException("您还没有点赞该评论");
        }

        // 3. 删除点赞记录（使用自定义删除逻辑）
        commentLikeMapper.deleteByMap(java.util.Map.of(
                "comment_id", commentId,
                "user_id", userId
        ));

        // 4. 更新评论点赞数
        commentMapper.decrementLikeCount(commentId);

        log.info("用户 {} 取消点赞评论 {}", userId, commentId);
    }

    /**
     * 检查用户是否已点赞
     *
     * @param userId 用户ID
     * @param commentId 评论ID
     * @return true表示已点赞
     */
    @Override
    public boolean checkUserLiked(Long userId, Long commentId) {
        return commentLikeMapper.checkUserLiked(commentId, userId);
    }

    /**
     * 获取评论的点赞数
     *
     * @param commentId 评论ID
     * @return 点赞数
     */
    @Override
    public Long getLikeCount(Long commentId) {
        Long count = commentLikeMapper.countByCommentId(commentId);
        return count != null ? count : 0L;
    }
}

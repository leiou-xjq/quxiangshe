package com.quxiangshe.comment.service.impl;

import com.quxiangshe.comment.dto.CommentCreateRequestDTO;
import com.quxiangshe.comment.dto.CommentMessageDTO;
import com.quxiangshe.comment.dto.CommentResponseDTO;
import com.quxiangshe.comment.entity.CommentEntity;
import com.quxiangshe.comment.mapper.CommentMapper;
import com.quxiangshe.comment.queue.CommentQueueProducer;
import com.quxiangshe.comment.service.CommentLikeService;
import com.quxiangshe.comment.service.CommentService;
import com.quxiangshe.comment.service.SensitiveWordService;
import com.quxiangshe.comment.util.CommentCacheUtil;
import com.quxiangshe.comment.util.CommentDistributedLock;
import com.quxiangshe.common.exception.BusinessException;
import com.quxiangshe.note.entity.NoteEntity;
import com.quxiangshe.note.mapper.NoteMapper;
import com.quxiangshe.auth.entity.UserEntity;
import com.quxiangshe.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final NoteMapper noteMapper;
    private final SensitiveWordService sensitiveWordService;
    private final CommentLikeService commentLikeService;
    private final CommentQueueProducer queueProducer;
    private final CommentCacheUtil cacheUtil;
    private final CommentDistributedLock lockUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentResponseDTO createComment(Long userId, Long articleId, CommentCreateRequestDTO request) {
        NoteEntity note = noteMapper.selectById(articleId);
        if (note == null || note.getDeleted() == 1) {
            throw new BusinessException("笔记不存在");
        }

        // 2. 敏感词检测
        String originalContent = request.getContent();
        int status = CommentEntity.STATUS_NORMAL;
        
        // 检测是否包含敏感词
        boolean hasSensitiveWord = sensitiveWordService.containsSensitiveWord(originalContent);
        if (hasSensitiveWord) {
            // 替换敏感词
            request.setContent(sensitiveWordService.replaceSensitiveWord(originalContent));
            status = CommentEntity.STATUS_SENSITIVE_REPLACED;
        }

        // 3. 确定targetId和targetUserId
        Long targetId = request.getTargetId();
        Long targetUserId = request.getTargetUserId();
        
        // 如果是回复评论，校验目标评论是否存在
        if (targetId != null && targetId > 0) {
            CommentEntity targetComment = commentMapper.selectById(targetId);
            if (targetComment == null || targetComment.getDeleted() == 1) {
                throw new BusinessException("目标评论不存在");
            }
            // 如果没有指定targetUserId，从目标评论获取
            if (targetUserId == null) {
                targetUserId = targetComment.getUserId();
            }
        } else {
            // 一级评论
            targetId = 0L;
            targetUserId = null;
        }

        // 4. 创建评论实体
        CommentEntity comment = CommentEntity.builder()
                .articleId(articleId)
                .userId(userId)
                .targetId(targetId)
                .targetUserId(targetUserId)
                .content(request.getContent())
                .likeCount(0)
                .replyCount(0)
                .status(status)
                .deleted(CommentEntity.DELETED_NO)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        // 5. 写入数据库
        commentMapper.insert(comment);

        // 6. 如果是回复评论，更新目标评论的回复数
        if (targetId != null && targetId > 0) {
            commentMapper.incrementReplyCount(targetId);
        }

        // 7. 发送RabbitMQ消息（异步处理）
        try {
            queueProducer.sendCommentMessage(comment, status);
        } catch (Exception e) {
            log.error("发送评论消息失败: commentId={}", comment.getId(), e);
            // 消息发送失败不影响主流程
        }

        // 8. 更新评论数缓存（使用分布式锁保证一致性）
        updateCommentCountCache(articleId);

        log.info("评论创建成功: commentId={}, articleId={}, userId={}, status={}", 
                comment.getId(), articleId, userId, status);

        // 9. 构建返回结果
        CommentResponseDTO result = new CommentResponseDTO();
        result.setCommentId(comment.getId());
        result.setCreateTime(comment.getCreateTime().toString());
        
        return result;
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
    @Override
    public CommentListResponse getComments(Long articleId, Long lastCommentId, Integer size) {
        if (size == null || size <= 0) {
            size = 20;
        }

        Long cursor = (lastCommentId != null && lastCommentId > 0) ? lastCommentId : null;

        // 查询评论（包含一级评论和直接回复）
        List<CommentEntity> comments = commentMapper.selectCommentsWithReplies(articleId, cursor, size + 1);

        // 判断是否有更多
        boolean hasMore = comments.size() > size;
        if (hasMore) {
            comments = comments.subList(0, size);
        }

        if (comments.isEmpty()) {
            return new CommentListResponse(Collections.emptyList(), null, false);
        }

        // 收集需要查询的用户ID
        Set<Long> userIds = new HashSet<>();
        for (CommentEntity comment : comments) {
            userIds.add(comment.getUserId());
            // 添加被回复用户ID
            if (comment.getTargetUserId() != null) {
                userIds.add(comment.getTargetUserId());
            }
        }

        // 批量查询用户信息
        Map<Long, UserEntity> userMap = getUserMap(userIds);

        // 构建两层扁平结构
        Map<Long, List<CommentResponseDTO>> repliesMap = new HashMap<>();
        List<CommentResponseDTO> topComments = new ArrayList<>();

        for (CommentEntity comment : comments) {
            if (comment.getTargetId() == null || comment.getTargetId() == 0L) {
                // 一级评论
                CommentResponseDTO dto = buildCommentDTO(comment, userMap);
                dto.setReplies(new ArrayList<>());
                topComments.add(dto);
                repliesMap.put(comment.getId(), dto.getReplies());
            } else {
                // 回复评论
                Long targetId = comment.getTargetId();
                List<CommentResponseDTO> replies = repliesMap.get(targetId);
                if (replies != null) {
                    replies.add(buildReplyDTO(comment, userMap));
                }
            }
        }

        // 按时间倒序排列
        topComments.sort((a, b) -> {
            if (a.getCreateTime() == null) return 1;
            if (b.getCreateTime() == null) return -1;
            return b.getCreateTime().compareTo(a.getCreateTime());
        });

        // 获取新的游标
        Long newLastCommentId = null;
        if (!comments.isEmpty()) {
            newLastCommentId = comments.get(comments.size() - 1).getId();
        }

        return new CommentListResponse(topComments, newLastCommentId, hasMore);
    }

    /**
     * 获取某个一级评论下的回复列表
     *
     * @param targetId 目标评论ID
     * @param articleId 文章ID
     * @param lastCommentId 游标ID
     * @param size 每页数量
     * @return 回复列表响应
     */
    @Override
    public CommentListResponse getReplies(Long targetId, Long articleId, Long lastCommentId, Integer size) {
        if (size == null || size <= 0) {
            size = 10;
        }

        Long cursor = (lastCommentId != null && lastCommentId > 0) ? lastCommentId : null;

        List<CommentEntity> replies = commentMapper.selectReplies(articleId, targetId, cursor, size + 1);

        boolean hasMore = replies.size() > size;
        if (hasMore) {
            replies = replies.subList(0, size);
        }

        if (replies.isEmpty()) {
            return new CommentListResponse(Collections.emptyList(), null, false);
        }

        // 收集用户ID
        Set<Long> userIds = new HashSet<>();
        for (CommentEntity reply : replies) {
            userIds.add(reply.getUserId());
            if (reply.getTargetUserId() != null) {
                userIds.add(reply.getTargetUserId());
            }
        }

        Map<Long, UserEntity> userMap = getUserMap(userIds);

        List<CommentResponseDTO> items = replies.stream()
                .map(r -> buildReplyDTO(r, userMap))
                .collect(Collectors.toList());

        Long newLastCommentId = null;
        if (!replies.isEmpty()) {
            newLastCommentId = replies.get(replies.size() - 1).getId();
        }

        return new CommentListResponse(items, newLastCommentId, hasMore);
    }

    /**
     * 获取某个一级评论的回复数量
     *
     * @param articleId 文章ID
     * @param targetId 目标评论ID
     * @return 回复数量
     */
    @Override
    public Long getReplyCount(Long articleId, Long targetId) {
        return commentMapper.countReplies(articleId, targetId);
    }

    /**
     * 点赞评论
     *
     * @param userId 用户ID
     * @param commentId 评论ID
     */
    @Override
    public void likeComment(Long userId, Long commentId) {
        // 使用分布式锁保证并发安全
        String lockKey = "like:" + commentId + ":" + userId;
        boolean locked = lockUtil.tryLock(lockKey);
        
        if (!locked) {
            throw new BusinessException("操作过于频繁，请稍后重试");
        }
        
        try {
            // 调用点赞服务
            commentLikeService.likeComment(userId, commentId);
            
            // 更新缓存
            cacheUtil.incrementLikeCount(commentId);
            cacheUtil.cacheUserLikeStatus(commentId, userId);
        } finally {
            lockUtil.unlock(lockKey);
        }
    }

    /**
     * 取消点赞
     *
     * @param userId 用户ID
     * @param commentId 评论ID
     */
    @Override
    public void unlikeComment(Long userId, Long commentId) {
        // 使用分布式锁保证并发安全
        String lockKey = "unlike:" + commentId + ":" + userId;
        boolean locked = lockUtil.tryLock(lockKey);
        
        if (!locked) {
            throw new BusinessException("操作过于频繁，请稍后重试");
        }
        
        try {
            // 调用点赞服务
            commentLikeService.unlikeComment(userId, commentId);
            
            // 更新缓存
            cacheUtil.decrementLikeCount(commentId);
            cacheUtil.removeUserLikeStatus(commentId, userId);
        } finally {
            lockUtil.unlock(lockKey);
        }
    }

    /**
     * 删除评论（逻辑删除）
     *
     * @param userId 用户ID
     * @param commentId 评论ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long userId, Long commentId) {
        // 1. 查询评论
        CommentEntity comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }

        // 2. 校验权限（评论者本人或文章作者）
        NoteEntity note = noteMapper.selectById(comment.getArticleId());
        boolean isOwner = comment.getUserId().equals(userId);
        boolean isPostOwner = note != null && note.getUserId().equals(userId);
        
        if (!isOwner && !isPostOwner) {
            throw new BusinessException("无权限操作");
        }

        // 3. 逻辑删除
        comment.setDeleted(CommentEntity.DELETED_YES);
        commentMapper.updateById(comment);

        // 4. 如果是回复评论，减少目标评论的回复数
        if (comment.getTargetId() != null && comment.getTargetId() > 0) {
            commentMapper.decrementReplyCount(comment.getTargetId());
        }

        // 5. 更新缓存
        updateCommentCountCache(comment.getArticleId());

        log.info("评论删除成功: commentId={}, userId={}", commentId, userId);
    }

    /**
     * 恢复评论（管理员操作）
     *
     * @param commentId 评论ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreComment(Long commentId) {
        CommentEntity comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }

        comment.setDeleted(CommentEntity.DELETED_NO);
        commentMapper.updateById(comment);

        // 如果是回复评论，恢复目标评论的回复数
        if (comment.getTargetId() != null && comment.getTargetId() > 0) {
            commentMapper.incrementReplyCount(comment.getTargetId());
        }

        updateCommentCountCache(comment.getArticleId());
        
        log.info("评论恢复成功: commentId={}", commentId);
    }

    /**
     * 审核评论（管理员操作）
     *
     * @param commentId 评论ID
     * @param approved 是否通过
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewComment(Long commentId, boolean approved) {
        CommentEntity comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }

        if (approved) {
            comment.setStatus(CommentEntity.STATUS_NORMAL);
        } else {
            comment.setDeleted(CommentEntity.DELETED_YES);
        }
        commentMapper.updateById(comment);

        log.info("评论审核完成: commentId={}, approved={}", commentId, approved);
    }

    /**
     * 获取文章的一级评论数量
     *
     * @param articleId 文章ID
     * @return 一级评论数量
     */
    @Override
    public Long getCommentCount(Long articleId) {
        // 先从缓存获取
        Long cachedCount = cacheUtil.getCommentCount(articleId);
        if (cachedCount != null) {
            return cachedCount;
        }

        // 从数据库查询
        Long count = commentMapper.countTopComments(articleId);
        
        // 缓存结果
        cacheUtil.cacheCommentCount(articleId, count);
        
        return count;
    }

    // ==================== 私有方法 ====================

    /**
     * 更新评论数缓存
     *
     * @param articleId 文章ID
     */
    private void updateCommentCountCache(Long articleId) {
        String lockKey = "count:" + articleId;
        boolean locked = lockUtil.tryLock(lockKey);
        
        if (!locked) {
            return;
        }
        
        try {
            Long count = commentMapper.countTopComments(articleId);
            cacheUtil.cacheCommentCount(articleId, count);
        } finally {
            lockUtil.unlock(lockKey);
        }
    }

    /**
     * 批量查询用户信息
     *
     * @param userIds 用户ID集合
     * @return 用户ID到用户实体的映射
     */
    private Map<Long, UserEntity> getUserMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        List<UserEntity> users = userMapper.selectBatchIds(userIds);
        return users.stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u));
    }

    /**
     * 构建评论DTO（一级评论）
     *
     * @param comment 评论实体
     * @param userMap 用户映射
     * @return 评论DTO
     */
    private CommentResponseDTO buildCommentDTO(CommentEntity comment, Map<Long, UserEntity> userMap) {
        UserEntity user = userMap.get(comment.getUserId());
        UserEntity targetUser = comment.getTargetUserId() != null ? userMap.get(comment.getTargetUserId()) : null;
        
        return CommentResponseDTO.builder()
                .commentId(comment.getId())
                .articleId(comment.getArticleId())
                .userId(comment.getUserId())
                .username(user != null ? user.getUsername() : "")
                .nickname(user != null ? user.getNickname() : "")
                .avatarUrl(user != null ? user.getAvatarUrl() : "")
                .content(comment.getContent())
                .targetId(comment.getTargetId())
                .targetUserId(comment.getTargetUserId())
                .targetUsername(targetUser != null ? targetUser.getUsername() : "")
                .targetNickname(targetUser != null ? targetUser.getNickname() : "")
                .likeCount(comment.getLikeCount())
                .replyCount(comment.getReplyCount())
                .status(comment.getStatus())
                .isLiked(false)
                .createTime(comment.getCreateTime() != null ? comment.getCreateTime().toString() : "")
                .build();
    }

    /**
     * 构建回复DTO（二级评论）
     *
     * @param comment 评论实体
     * @param userMap 用户映射
     * @return 评论DTO
     */
    private CommentResponseDTO buildReplyDTO(CommentEntity comment, Map<Long, UserEntity> userMap) {
        return buildCommentDTO(comment, userMap);
    }
}

package com.quxiangshe.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.quxiangshe.backend.config.RabbitMQConfig;
import com.quxiangshe.backend.dto.NotificationMessage;
import com.quxiangshe.backend.entity.Note;
import com.quxiangshe.backend.entity.NoteComment;
import com.quxiangshe.backend.entity.User;
import com.quxiangshe.backend.mapper.NoteCommentMapper;
import com.quxiangshe.backend.mapper.NoteMapper;
import com.quxiangshe.backend.mapper.UserMapper;
import com.quxiangshe.backend.service.IActivityService;
import com.quxiangshe.backend.service.ICommentService;
import com.quxiangshe.backend.service.INotificationService;
import com.quxiangshe.backend.service.INoteService;
import com.quxiangshe.backend.exception.BusinessException;
import com.quxiangshe.backend.dto.CreateCommentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

import static com.quxiangshe.backend.service.impl.ActivityServiceImpl.ACTION_COMMENT;

/**
 * 评论服务实现类 - 抖音风格
 * 
 * 评论结构规范：
 * - parentId = 0: 根评论（一级评论）
 * - parentId > 0: 子评论（回复）
 * - rootId: 所属根评论ID（用于前端渲染）
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements ICommentService {
    
    private final NoteCommentMapper commentMapper;
    private final NoteMapper noteMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private IActivityService activityService;
    private INoteService noteService;
    @Autowired(required = false)
    private INotificationService notificationService;
    @Autowired(required = false)
    private RedissonClient redissonClient;
    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;
    
    // ========== 评论数相关常量 ==========
    private static final String NOTE_COMMENT_COUNT_KEY = "note:comment:count:";
    private static final String COMMENT_INCREMENT_SCRIPT = 
            "local current = redis.call('INCR', KEYS[1]) " +
            "return current";
    private static final String COMMENT_DECREMENT_SCRIPT = 
            "local current = redis.call('DECR', KEYS[1]) " +
            "if current < 0 then " +
            "  redis.call('SET', KEYS[1], 0) " +
            "  return 0 " +
            "end " +
            "return current";
    
    @Override
    public List<NoteComment> getComments(Long noteId, Long rootId) {
        return commentMapper.selectByNoteId(noteId);
    }
    
    /**
     * 使用 Redis + 分布式锁 + Lua 脚本原子递增评论数
     */
    private void incrementCommentCountWithRedis(Long noteId) {
        RLock lock = redissonClient != null ? redissonClient.getLock("comment:note:" + noteId) : null;
        if (lock != null) {
            lock.lock();
        }
        try {
            String commentCountKey = NOTE_COMMENT_COUNT_KEY + noteId;
            redisTemplate.execute(
                RedisScript.of(COMMENT_INCREMENT_SCRIPT, Long.class),
                Collections.singletonList(commentCountKey)
            );
            // 异步更新数据库
            asyncSaveCommentCountToDb(noteId);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }
    
    /**
     * 异步保存评论数到数据库
     */
    private void asyncSaveCommentCountToDb(Long noteId) {
        try {
            String commentCountStr = redisTemplate.opsForValue().get(NOTE_COMMENT_COUNT_KEY + noteId);
            if (commentCountStr != null) {
                Note note = new Note();
                note.setId(noteId);
                note.setCommentCount(Integer.parseInt(commentCountStr));
                noteMapper.updateById(note);
            }
        } catch (Exception e) {
            log.error("异步保存评论数失败: noteId={}", noteId, e);
        }
    }
    
    /**
     * 使用 Redis + 分布式锁 + Lua 脚本原子递减评论数
     */
    private void decrementCommentCountWithRedis(Long noteId, int decrement) {
        RLock lock = redissonClient != null ? redissonClient.getLock("comment:note:" + noteId) : null;
        if (lock != null) {
            lock.lock();
        }
        try {
            String commentCountKey = NOTE_COMMENT_COUNT_KEY + noteId;
            for (int i = 0; i < decrement; i++) {
                redisTemplate.execute(
                    RedisScript.of(COMMENT_DECREMENT_SCRIPT, Long.class),
                    Collections.singletonList(commentCountKey)
                );
            }
            // 异步更新数据库
            asyncSaveCommentCountToDb(noteId);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }
    
    @Lazy
    @Autowired
    public void setActivityService(IActivityService activityService) {
        this.activityService = activityService;
    }
    
    @Lazy
    @Autowired
    public void setNoteService(INoteService noteService) {
        this.noteService = noteService;
    }
    
    /**
     * 发布评论/回复
     * 
     * @param userId 当前用户ID
     * @param request 评论请求
     * @return 创建的评论
     */
    @Override
    @Transactional
    public NoteComment addComment(Long userId, CreateCommentRequest request) {
        // 1. 检查笔记是否存在
        Note note = noteMapper.selectById(request.getNoteId());
        if (note == null || note.getStatus() != 1) {
            throw new BusinessException(404, "笔记不存在");
        }
        
        // 3. 确定parentId和rootId
        Long parentId = 0L;
        Long rootId = 0L;
        
        if (request.getParentId() != null && request.getParentId() > 0) {
            // 是回复评论，需要查询父评论确定rootId
            NoteComment parentComment = commentMapper.selectById(request.getParentId());
            if (parentComment == null) {
                throw new BusinessException(404, "父评论不存在");
            }
            parentId = request.getParentId();
            // rootId = 父评论的rootId（如果父评论是根评论则用父评论ID，否则用父评论的rootId）
            rootId = parentComment.getRootId() == 0 ? parentComment.getId() : parentComment.getRootId();
        }
        
        // 4. 构建评论实体
        NoteComment comment = new NoteComment();
        comment.setNoteId(request.getNoteId());
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setRootId(rootId);
        comment.setContent(request.getContent());
        comment.setLikeCount(0);
        comment.setStatus(1);
        
        // 5. 插入评论
        commentMapper.insert(comment);
        
        // 6. 使用 Redis + 分布式锁原子递增评论数
        incrementCommentCountWithRedis(request.getNoteId());
        
        // 7. 记录用户活跃度
        activityService.recordInteraction(userId);
        activityService.incrementActivityScore(userId, ACTION_COMMENT);
        noteService.incrementHotScore(request.getNoteId(), 2);
        
        // 8. 设置用户信息
        User user = userMapper.selectById(userId);
        if (user != null) {
            comment.setNickname(user.getNickname());
            comment.setAvatar(user.getAvatar());
        }
        
        // 9. 设置被回复者昵称（如果是回复）
        if (parentId > 0) {
            NoteComment parentComment = commentMapper.selectById(parentId);
            if (parentComment != null) {
                User replyToUser = userMapper.selectById(parentComment.getUserId());
                if (replyToUser != null) {
                    comment.setReplyToNickname(replyToUser.getNickname());
                }
            }
        }
        
        // 10. 发送评论通知 (异步MQ)
        if (rabbitTemplate != null && note != null && !note.getUserId().equals(userId)) {
            NotificationMessage msg = NotificationMessage.builder()
                    .type(NotificationMessage.TYPE_COMMENT)
                    .userId(note.getUserId())
                    .fromUserId(userId)
                    .noteId(request.getNoteId())
                    .commentId(comment.getId())
                    .timestamp(LocalDateTime.now())
                    .build();
            rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.NOTIFICATION_ROUTING_KEY, msg);
        }
        
        return comment;
    }
    
/**
 * 获取评论列表
 * 返回扁平列表，支持前端多级嵌套处理
 *
 * @param noteId 笔记ID
 * @param page 页码
 * @param size 每页数量
 * @return 评论列表（扁平）
 */
@Override
public List<NoteComment> getCommentList(Long noteId, int page, int size) {
    List<NoteComment> allComments = commentMapper.selectByNoteId(noteId);
    
    for (NoteComment c : allComments) {
        fillUserInfo(c);
        
        if (c.getParentId() != null && c.getParentId() > 0) {
            NoteComment parent = commentMapper.selectById(c.getParentId());
            if (parent != null) {
                User replyToUser = userMapper.selectById(parent.getUserId());
                if (replyToUser != null) {
                    c.setReplyToNickname(replyToUser.getNickname());
                }
            }
        }
    }
    
    int offset = (page - 1) * size;
    int end = Math.min(offset + size, allComments.size());
    if (offset >= allComments.size()) {
        return new ArrayList<>();
    }
    
    return allComments.subList(offset, end);
}
    
    /**
     * 删除评论（级联删除）
     * - 删除根评论时会删除所有子评论
     * - 删除子评论只删除该评论
     *
     * @param commentId 评论ID
     * @param userId 当前用户ID
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteComment(Long commentId, Long userId, boolean isNoteOwner) {
        log.info("deleteComment called: commentId={}, userId={}, isNoteOwner={}", commentId, userId, isNoteOwner);
        
        // 1. 查询评论
        NoteComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }
        
        log.info("Comment found: userId={}, commentUserId={}", userId, comment.getUserId());
        
        // 2. 验证权限（自己的评论 或 笔记发布者）
        if (!comment.getUserId().equals(userId) && !isNoteOwner) {
            throw new BusinessException(403, "只能删除自己的评论");
        }
        
        // 3. 执行删除
        if (comment.getParentId() == null || comment.getParentId() == 0) {
            // 是根评论，级联删除所有子评论
            deleteCommentRecursive(commentId, comment.getNoteId());
            // 删除根评论
            comment.setStatus(2);
            comment.setDeletedAt(java.time.LocalDateTime.now());
            commentMapper.updateById(comment);
        } else {
            // 是子评论，递归删除该评论及其所有后代评论
            deleteCommentRecursive(commentId, comment.getNoteId());
        }
        
        // 4. 使用 Redis + 分布式锁原子递减评论数
        // 计算实际删除的评论总数（包括所有后代评论）
        int decrease = countCommentAndDescendants(commentId, comment.getNoteId());
        decrementCommentCountWithRedis(comment.getNoteId(), decrease);
        
        // 扣减热度值（评论增加时 +2，删除时 -2）
        noteService.incrementHotScore(comment.getNoteId(), -2 * decrease);
        
        return true;
    }

    @Override
    public NoteComment getCommentById(Long commentId) {
        return commentMapper.selectById(commentId);
    }

    // ==================== 私有方法 ====================
    
    /**
     * 填充用户信息
     */
    private void fillUserInfo(NoteComment comment) {
        if (comment.getUserId() != null) {
            User user = userMapper.selectById(comment.getUserId());
            if (user != null) {
                comment.setNickname(user.getNickname());
                comment.setAvatar(user.getAvatar());
            }
        }
    }
    
    /**
     * 递归逻辑删除评论及其所有后代评论
     */
    private void deleteCommentRecursive(Long commentId, Long noteId) {
        List<NoteComment> children = commentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NoteComment>()
                        .eq(NoteComment::getNoteId, noteId)
                        .eq(NoteComment::getStatus, 1)
                        .eq(NoteComment::getParentId, commentId)
        );
        for (NoteComment child : children) {
            // 递归删除子评论的后代
            deleteCommentRecursive(child.getId(), noteId);
            // 逻辑删除子评论
            child.setStatus(2);
            child.setDeletedAt(java.time.LocalDateTime.now());
            commentMapper.updateById(child);
        }
        
        // 逻辑删除传入的评论本身
        NoteComment comment = commentMapper.selectById(commentId);
        if (comment != null && comment.getStatus() == 1) {
            comment.setStatus(2);
            comment.setDeletedAt(java.time.LocalDateTime.now());
            commentMapper.updateById(comment);
        }
    }
    
    /**
     * 计算评论及其所有后代评论的数量
     */
    private int countCommentAndDescendants(Long commentId, Long noteId) {
        int count = 1;
        List<NoteComment> children = commentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NoteComment>()
                        .eq(NoteComment::getNoteId, noteId)
                        .eq(NoteComment::getStatus, 1)
                        .eq(NoteComment::getParentId, commentId)
        );
        for (NoteComment child : children) {
            count += countCommentAndDescendants(child.getId(), noteId);
        }
        return count;
    }
}
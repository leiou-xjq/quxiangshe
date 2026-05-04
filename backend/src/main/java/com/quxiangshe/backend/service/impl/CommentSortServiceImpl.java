package com.quxiangshe.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quxiangshe.backend.config.CommentSortConfig;
import com.quxiangshe.backend.entity.CommentLike;
import com.quxiangshe.backend.entity.CommentSortData;
import com.quxiangshe.backend.entity.Note;
import com.quxiangshe.backend.entity.NoteComment;
import com.quxiangshe.backend.entity.User;
import com.quxiangshe.backend.mapper.CommentLikeMapper;
import com.quxiangshe.backend.mapper.NoteCommentMapper;
import com.quxiangshe.backend.mapper.NoteMapper;
import com.quxiangshe.backend.mapper.UserMapper;
import com.quxiangshe.backend.service.ICommentSortService;
import com.quxiangshe.backend.service.sort.FullSortStrategy;
import com.quxiangshe.backend.service.sort.ScoreCalculator;
import com.quxiangshe.backend.service.sort.SortStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentSortServiceImpl implements ICommentSortService {
    
    private final NoteCommentMapper noteCommentMapper;
    private final NoteMapper noteMapper;
    private final UserMapper userMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final FullSortStrategy fullSortStrategy;
    private final CommentSortConfig config;
    private final StringRedisTemplate redisTemplate;
    
    @Override
    public List<CommentSortData> getRootComments(Long postId, String sort, String cursor, int size) {
        if (postId == null) {
            return new ArrayList<>();
        }
        SortStrategy strategy = getStrategy(postId, 0L);
        return strategy.getRootComments(postId, sort, cursor, size);
    }
    
    @Override
    public List<CommentSortData> getAllComments(Long postId, String sort, String cursor, int size) {
        if (postId == null) {
            return new ArrayList<>();
        }
        // 强制使用FullSortStrategy来获取所有评论
        return fullSortStrategy.getAllComments(postId, sort, cursor, size);
    }
    
    @Override
    public List<CommentSortData> getChildComments(Long postId, Long rootId, String sort, String cursor, int size) {
        if (postId == null || rootId == null) {
            return new ArrayList<>();
        }
        SortStrategy strategy = getStrategy(postId, rootId);
        return strategy.getChildComments(postId, rootId, sort, cursor, size);
    }
    
@Override
    public long getCommentCount(Long postId, Long rootId) {
        if (postId == null) {
            return 0;
        }
        return fullSortStrategy.getCommentCount(postId, rootId);
    }
    
    @Override
    @Transactional
    public CommentSortData addComment(Long userId, Long postId, Long parentId, String content) {
        if (userId == null || postId == null || content == null || content.isEmpty()) {
            return null;
        }
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        
        NoteComment noteComment = new NoteComment();
        noteComment.setNoteId(postId);
        noteComment.setUserId(userId);
        noteComment.setContent(content);
        noteComment.setLikeCount(0);
        noteComment.setStatus(1);
        
        Long rootId;
        if (parentId == null || parentId == 0) {
            rootId = 0L;
            noteComment.setParentId(0L);
        } else {
            NoteComment parentComment = noteCommentMapper.selectById(parentId);
            if (parentComment == null) {
                return null;
            }
            noteComment.setParentId(parentId);
            // rootId应该是父评论的id（如果父评论是根评论），或者父评论的rootId（如果父评论不是根评论）
            Long parentRootId = parentComment.getRootId();
            if (parentRootId == null || parentRootId == 0) {
                // 父评论是根评论，新评论的rootId是父评论的id
                rootId = parentId;
            } else {
                // 父评论不是根评论，新评论的rootId是父评论的rootId
                rootId = parentRootId;
            }
            
            if (parentComment.getUserId() != null) {
                User replyToUser = userMapper.selectById(parentComment.getUserId());
                if (replyToUser != null) {
                    noteComment.setReplyToNickname(replyToUser.getNickname());
                }
            }
        }
        noteComment.setRootId(rootId);
        
        noteCommentMapper.insert(noteComment);
        
        CommentSortData commentData = CommentSortData.builder()
                .commentId(noteComment.getId())
                .postId(postId)
                .rootId(rootId)
                .parentId(parentId)
                .userId(userId)
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .content(content)
                .likeCount(0)
                .replyCount(0)
                .createdAt(System.currentTimeMillis())
                .status(1)
                .replyToNickname(noteComment.getReplyToNickname())
                .build();
        
        SortStrategy strategy = getStrategy(postId, rootId);
        strategy.addComment(commentData);
        
        fullSortStrategy.refreshCache(postId);
        
        if (rootId != null && rootId != 0) {
            NoteComment rootComment = noteCommentMapper.selectById(rootId);
            if (rootComment != null) {
                rootComment.setReplyCount(rootComment.getReplyCount() != null ? rootComment.getReplyCount() + 1 : 1);
                noteCommentMapper.updateById(rootComment);
            }
        }
        
        return commentData;
    }
    
    @Override
    @Transactional
    public void likeComment(Long commentId, Long userId) {
        if (commentId == null || userId == null) {
            return;
        }
        
        // 检查是否已经点赞
        if (checkUserLikedComment(commentId, userId)) {
            return;
        }
        
        NoteComment comment = noteCommentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            return;
        }
        
        // 添加点赞记录
        CommentLike commentLike = new CommentLike();
        commentLike.setCommentId(commentId);
        commentLike.setUserId(userId);
        commentLike.setCreatedAt(java.time.LocalDateTime.now());
        commentLikeMapper.insert(commentLike);
        
        // 更新评论点赞数
        int newCount = (comment.getLikeCount() != null ? comment.getLikeCount() : 0) + 1;
        comment.setLikeCount(newCount);
        noteCommentMapper.updateById(comment);
        
        // 更新Redis排序索引
        SortStrategy strategy = getStrategy(comment.getNoteId(), comment.getRootId());
        CommentSortData commentData = CommentSortData.builder()
                .commentId(commentId)
                .likeCount(newCount)
                .replyCount(comment.getReplyCount())
                .createdAt(comment.getCreatedAt() != null ? 
                    comment.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                .build();
        double score = ScoreCalculator.calculateHotScore(commentData);
        strategy.updateScore(commentId, score);
        
        fullSortStrategy.refreshCache(comment.getNoteId());
    }
    
    @Override
    @Transactional
    public void unlikeComment(Long commentId, Long userId) {
        if (commentId == null || userId == null) {
            return;
        }
        
        // 检查是否已经点赞
        if (!checkUserLikedComment(commentId, userId)) {
            return;
        }
        
        NoteComment comment = noteCommentMapper.selectById(commentId);
        if (comment == null) {
            return;
        }
        
        // 删除点赞记录
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CommentLike> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getCommentId, commentId)
               .eq(CommentLike::getUserId, userId);
        commentLikeMapper.delete(wrapper);
        
        // 更新评论点赞数
        int newCount = Math.max(0, (comment.getLikeCount() != null ? comment.getLikeCount() : 0) - 1);
        comment.setLikeCount(newCount);
        noteCommentMapper.updateById(comment);
        
// 更新Redis排序索引
        SortStrategy strategy = getStrategy(comment.getNoteId(), comment.getRootId());
        CommentSortData commentData = CommentSortData.builder()
                .commentId(commentId)
                .likeCount(newCount)
                .replyCount(comment.getReplyCount())
                .createdAt(comment.getCreatedAt() != null ? 
                    comment.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                .build();
        double score = ScoreCalculator.calculateHotScore(commentData);
        strategy.updateScore(commentId, score);
        
        fullSortStrategy.refreshCache(comment.getNoteId());
    }
    
    private boolean checkUserLikedComment(Long commentId, Long userId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CommentLike> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getCommentId, commentId)
               .eq(CommentLike::getUserId, userId);
        return commentLikeMapper.selectCount(wrapper) > 0;
    }
    
    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        if (commentId == null || userId == null) {
            return;
        }
        NoteComment comment = noteCommentMapper.selectById(commentId);
        if (comment == null) {
            return;
        }
        if (!userId.equals(comment.getUserId())) {
            return;
        }
        
        Long noteId = comment.getNoteId();
        
        // 递归物理删除所有子评论
        deleteCommentPhysicalRecursive(commentId, noteId);
        
        // 物理删除当前评论
        noteCommentMapper.deleteById(commentId);
        
        // 更新笔记评论数
        updateNoteCommentCount(noteId);
        
        // 刷新Redis评论树缓存
        fullSortStrategy.refreshCache(noteId);
    }
    
    private void deleteCommentPhysicalRecursive(Long parentId, Long noteId) {
        List<NoteComment> children = noteCommentMapper.selectList(
                new LambdaQueryWrapper<NoteComment>()
                        .eq(NoteComment::getNoteId, noteId)
                        .eq(NoteComment::getStatus, 1)
                        .eq(NoteComment::getParentId, parentId)
        );
        for (NoteComment child : children) {
            deleteCommentPhysicalRecursive(child.getId(), noteId);
            noteCommentMapper.deleteById(child.getId());
        }
    }
    
    @Override
    public NoteComment getCommentById(Long commentId) {
        if (commentId == null) {
            return null;
        }
        return noteCommentMapper.selectById(commentId);
    }
    
    private void updateNoteCommentCount(Long noteId) {
        long count = noteCommentMapper.selectCount(
                new LambdaQueryWrapper<NoteComment>()
                        .eq(NoteComment::getNoteId, noteId)
                        .eq(NoteComment::getStatus, 1)
        );
        Note note = noteMapper.selectById(noteId);
        if (note != null) {
            note.setCommentCount((int) count);
            noteMapper.updateById(note);
        }
    }
    
    @Override
    public void initCommentSort(Long postId) {
        if (postId == null) {
            return;
        }
        
        int pageSize = 500;
        long total = noteCommentMapper.selectCount(
            new LambdaQueryWrapper<NoteComment>()
                .eq(NoteComment::getNoteId, postId)
                .eq(NoteComment::getStatus, 1)
        );
        
        if (total == 0) {
            return;
        }
        
        int pageNum = 0;
        int processed = 0;
        while (processed < total) {
            List<NoteComment> comments = noteCommentMapper.selectList(
                new LambdaQueryWrapper<NoteComment>()
                    .eq(NoteComment::getNoteId, postId)
                    .eq(NoteComment::getStatus, 1)
                    .orderByAsc(NoteComment::getCreatedAt)
                    .last("LIMIT " + pageSize + " OFFSET " + processed)
            );
            
            if (comments == null || comments.isEmpty()) {
                break;
            }
            
            List<Long> userIds = comments.stream()
                    .map(NoteComment::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, User> userMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                List<User> users = userMapper.selectByIds(userIds);
                for (User u : users) {
                    userMap.put(u.getId(), u);
                }
            }
            
            List<Long> parentIds = comments.stream()
                    .map(NoteComment::getParentId)
                    .filter(id -> id != null && id != 0)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, User> replyToUserMap = new HashMap<>();
            if (!parentIds.isEmpty()) {
                Map<Long, NoteComment> parentMap = new HashMap<>();
                for (NoteComment c : comments) {
                    if (c.getParentId() != null && c.getParentId() != 0) {
                        parentMap.put(c.getParentId(), c);
                    }
                }
                
                Set<Long> replyToUserIds = parentMap.values().stream()
                        .map(NoteComment::getUserId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                
                if (!replyToUserIds.isEmpty()) {
                    List<User> replyToUsers = userMapper.selectByIds(new ArrayList<>(replyToUserIds));
                    for (User u : replyToUsers) {
                        replyToUserMap.put(u.getId(), u);
                    }
                }
            }
            
            for (NoteComment noteComment : comments) {
                User user = userMap.get(noteComment.getUserId());
                
                String replyToNickname = null;
                Long parentId = noteComment.getParentId();
                if (parentId != null && parentId != 0) {
                    NoteComment parentComment = noteCommentMapper.selectById(parentId);
                    if (parentComment != null && parentComment.getUserId() != null) {
                        User replyToUser = replyToUserMap.get(parentComment.getUserId());
                        if (replyToUser != null) {
                            replyToNickname = replyToUser.getNickname();
                        }
                    }
                }
                
                Long rootId = noteComment.getRootId();
                
                CommentSortData commentData = CommentSortData.builder()
                        .commentId(noteComment.getId())
                        .postId(noteComment.getNoteId())
                        .rootId(rootId)
                        .parentId(noteComment.getParentId())
                        .userId(noteComment.getUserId())
                        .nickname(user != null ? user.getNickname() : null)
                        .avatar(user != null ? user.getAvatar() : null)
                        .content(noteComment.getContent())
                        .likeCount(noteComment.getLikeCount())
                        .replyCount(noteComment.getReplyCount())
                        .createdAt(noteComment.getCreatedAt() != null ? noteComment.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                        .status(noteComment.getStatus())
                        .replyToNickname(replyToNickname)
                        .build();
                
                SortStrategy strategy = getStrategy(postId, 0L);
                strategy.addComment(commentData);
            }
            
            processed += comments.size();
            pageNum++;
        }
        
        log.info("Initialized comment sort for post: {}, total comments: {}", postId, total);
    }
    
    private SortStrategy getStrategy(Long postId, Long rootId) {
        return getStrategyInternal(postId, rootId);
    }
    
    private SortStrategy getStrategy(Long postId, long rootId) {
        // 只使用FullSortStrategy（comment_tree缓存）
        return fullSortStrategy;
    }
    
    private SortStrategy getStrategyInternal(Long postId, Long rootId) {
        // 只使用FullSortStrategy（comment_tree缓存）
        return fullSortStrategy;
    }
    
    @Override
    public void verifyAllCommentSort() {
        log.info("开始全量校验所有笔记的评论排序数据...");
        
        long totalNotes = noteMapper.selectCount(
            new LambdaQueryWrapper<Note>()
                .eq(Note::getStatus, 1)
        );
        
        log.info("共有 {} 个笔记需要校验", totalNotes);
        
        int pageSize = 100;
        int processed = 0;
        
        while (processed < totalNotes) {
            List<Note> notes = noteMapper.selectList(
                new LambdaQueryWrapper<Note>()
                    .eq(Note::getStatus, 1)
                    .last("LIMIT " + processed + ", " + pageSize)
            );
            
            if (notes.isEmpty()) {
                break;
            }
            
            for (Note note : notes) {
                try {
                    initCommentSort(note.getId());
                } catch (Exception e) {
                    log.warn("校验笔记 {} 的评论数据失败: {}", note.getId(), e.getMessage());
                }
            }
            
            processed += notes.size();
            log.info("已校验 {}/{} 个笔记", processed, totalNotes);
        }
        
        log.info("全量校验完成");
    }
}
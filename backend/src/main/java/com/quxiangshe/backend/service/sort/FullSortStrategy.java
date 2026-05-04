package com.quxiangshe.backend.service.sort;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quxiangshe.backend.config.CommentSortConfig;
import com.quxiangshe.backend.entity.*;
import com.quxiangshe.backend.mapper.NoteCommentMapper;
import com.quxiangshe.backend.mapper.UserMapper;
import com.quxiangshe.backend.service.sort.ScoreCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FullSortStrategy implements SortStrategy {
    
    private final StringRedisTemplate redisTemplate;
    private final CommentSortConfig config;
    private final NoteCommentMapper noteCommentMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String ROOT_KEY_PATTERN = "post:%d:root_comments";
    private static final String CHILDREN_KEY_PATTERN = "post:%d:comment:%d:children";
    private static final String HOT_ROOT_KEY_PATTERN = "post:%d:hot:root_comments";
    private static final String HOT_CHILDREN_KEY_PATTERN = "post:%d:comment:%d:hot:children";
    private static final String COMMENT_DATA_KEY_PATTERN = "post:%d:comment:%d:data";
    private static final String COMMENT_META_KEY_PATTERN = "post:%d:comment:%d:meta";
    private static final String COMMENT_TREE_KEY_PATTERN = "post:%d:comment_tree";
    private static final String COMMENT_COUNT_KEY_PATTERN = "post:%d:comment_count";
    
    private String getRootKey(Long postId) {
        return String.format(ROOT_KEY_PATTERN, postId);
    }
    
    private String getChildrenKey(Long postId, Long rootId) {
        return String.format(CHILDREN_KEY_PATTERN, postId, rootId);
    }
    
    private String getHotRootKey(Long postId) {
        return String.format(HOT_ROOT_KEY_PATTERN, postId);
    }
    
    private String getHotChildrenKey(Long postId, Long rootId) {
        return String.format(HOT_CHILDREN_KEY_PATTERN, postId, rootId);
    }
    
    private String getCommentDataKey(Long postId, Long commentId) {
        return String.format(COMMENT_DATA_KEY_PATTERN, postId, commentId);
    }
    
    private String getCommentMetaKey(Long postId, Long commentId) {
        return String.format(COMMENT_META_KEY_PATTERN, postId, commentId);
    }
    
    private String getCommentCountKey(Long postId) {
        return String.format(COMMENT_COUNT_KEY_PATTERN, postId);
    }
    
    public void clearCommentTree(Long postId) {
        if (postId == null) return;
        String treeKey = getCommentTreeKey(postId);
        redisTemplate.delete(treeKey);
    }
    
    public void incrementCommentCount(Long postId, int delta) {
        if (postId == null) return;
        String key = getCommentCountKey(postId);
        redisTemplate.opsForValue().increment(key, delta);
    }
    
    public void setCommentCount(Long postId, long count) {
        if (postId == null) return;
        String key = getCommentCountKey(postId);
        redisTemplate.opsForValue().set(key, String.valueOf(count));
    }
    
    public long getCommentCount(Long postId) {
        if (postId == null) return 0;
        String key = getCommentCountKey(postId);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0;
    }
    
    @Override
    public List<CommentSortData> getRootComments(Long postId, String sort, String cursor, int size) {
        String key = getRootKey(postId);
        return getCommentsByKey(postId, key, sort, cursor, size);
    }
    
    @Override
    public List<CommentSortData> getAllComments(Long postId, String sort, String cursor, int size) {
        String rootKey = getRootKey(postId);
        String childrenKeyPattern = "post:" + postId + ":comment:*:children";
        
        Set<String> allKeys = new HashSet<>();
        allKeys.add(rootKey);
        
        try {
            ScanOptions scanOptions = ScanOptions.scanOptions().match(childrenKeyPattern).count(100).build();
            try (Cursor<String> cursor2 = redisTemplate.scan(scanOptions)) {
                while (cursor2.hasNext()) {
                    allKeys.add(cursor2.next());
                }
            }
        } catch (Exception e) {
            log.warn("扫描子评论key失败: {}", e.getMessage());
        }
        
        List<CommentSortData> result = new ArrayList<>();
        
        for (String key : allKeys) {
            Set<ZSetOperations.TypedTuple<String>> tuples = getTuplesBySort(key, sort, 0, size - 1);
            if (tuples != null && !tuples.isEmpty()) {
                List<Long> ids = tuples.stream()
                        .map(t -> Long.parseLong(Objects.requireNonNull(t.getValue())))
                        .collect(Collectors.toList());
                List<CommentSortData> comments = loadComments(postId, ids);
                result.addAll(comments);
            }
        }
        
        result.sort((a, b) -> {
            if ("hot".equals(sort) || "hottest".equals(sort)) {
                double scoreA = ScoreCalculator.calculateHotScore(a);
                double scoreB = ScoreCalculator.calculateHotScore(b);
                return Double.compare(scoreB, scoreA); // 高的在前
            } else if ("time_desc".equals(sort)) {
                return Long.compare(
                    b.getCreatedAt() != null ? b.getCreatedAt() : 0,
                    a.getCreatedAt() != null ? a.getCreatedAt() : 0
                );
            } else {
                return Long.compare(
                    a.getCreatedAt() != null ? a.getCreatedAt() : 0,
                    b.getCreatedAt() != null ? b.getCreatedAt() : 0
                );
            }
        });
        
        int fromIndex = 0;
        if (cursor != null && !cursor.isEmpty()) {
            fromIndex = Integer.parseInt(cursor);
        }
        int toIndex = Math.min(fromIndex + size, result.size());
        
        return result.subList(fromIndex, toIndex);
    }
    
    private List<CommentSortData> getCommentsByKey(Long postId, String key, String sort, String cursor, int size) {
        List<CommentSortData> result = new ArrayList<>();
        
        Set<ZSetOperations.TypedTuple<String>> tuples;
        if (cursor == null || cursor.isEmpty()) {
            tuples = getTuplesBySort(key, sort, 0, size - 1);
        } else {
            Double score = Double.parseDouble(cursor);
            tuples = getTuplesBySortWithCursor(key, sort, score, size);
        }
        
        if (tuples != null && !tuples.isEmpty()) {
            List<Long> ids = tuples.stream()
                    .map(t -> Long.parseLong(Objects.requireNonNull(t.getValue())))
                    .collect(Collectors.toList());
            result = loadComments(postId, ids);
        }
        return result;
    }
    
    private Set<ZSetOperations.TypedTuple<String>> getTuplesBySort(String key, String sort, long start, long end) {
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        
        if ("hot".equals(sort) || "hottest".equals(sort)) {
            return zSetOps.reverseRangeWithScores(key, start, end);
        } else if ("time_desc".equals(sort)) {
            return zSetOps.reverseRangeWithScores(key, start, end);
        } else {
            return zSetOps.rangeWithScores(key, start, end);
        }
    }
    
    private Set<ZSetOperations.TypedTuple<String>> getTuplesBySortWithCursor(String key, String sort, Double cursorScore, int size) {
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        
        if ("hot".equals(sort) || cursorScore == null || cursorScore == Double.MIN_VALUE) {
            return zSetOps.reverseRangeWithScores(key, 0, size);
        }
        
        Set<String> members;
        if ("time_asc".equals(sort)) {
            members = zSetOps.rangeByScore(key, cursorScore, Double.POSITIVE_INFINITY, 0, size);
        } else {
            members = zSetOps.rangeByScore(key, Double.NEGATIVE_INFINITY, cursorScore, 0, size);
        }
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }
        Set<ZSetOperations.TypedTuple<String>> result = new LinkedHashSet<>();
        for (String member : members) {
            Double score = zSetOps.score(key, member);
            if (score != null) {
                result.add(ZSetOperations.TypedTuple.of(member, score));
            }
        }
        return result;
    }
    
    @Override
    public List<CommentSortData> getChildComments(Long postId, Long rootId, String sort, String cursor, int size) {
        String key = getChildrenKey(postId, rootId);
        return getCommentsByKey(postId, key, sort, cursor, size);
    }
    
    @Override
    public void addComment(CommentSortData comment) {
        if (comment == null || comment.getCommentId() == null) return;
        
        Long postId = comment.getPostId();
        Long rootId = comment.getRootId();
        String commentKey = rootId != null && rootId != 0 ? getChildrenKey(postId, rootId) : getRootKey(postId);
        
        double score = ScoreCalculator.calculateHotScore(comment);
        
        try {
            redisTemplate.opsForValue().set(getCommentDataKey(postId, comment.getCommentId()), objectMapper.writeValueAsString(comment));
            redisTemplate.opsForValue().set(getCommentMetaKey(postId, comment.getCommentId()), postId + ":" + rootId);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败: {}", e.getMessage());
            return;
        }
        
        redisTemplate.opsForZSet().add(commentKey, comment.getCommentId().toString(), score);
        
        if (rootId == null || rootId == 0) {
            if (ScoreCalculator.isHotComment(comment, config.getHotThreshold())) {
                redisTemplate.opsForZSet().add(getHotRootKey(postId), comment.getCommentId().toString(), score);
            }
        }
    }
    
    @Override
    public void updateScore(Long commentId, double score) {
        redisTemplate.opsForZSet().add(getRootKey(0L), commentId.toString(), score);
        redisTemplate.opsForZSet().add(getHotRootKey(0L), commentId.toString(), score);
        
        String metaValue = redisTemplate.opsForValue().get(getCommentMetaKey(0L, commentId));
        if (metaValue != null && !metaValue.isEmpty()) {
            String[] parts = metaValue.split(":");
            if (parts.length >= 2) {
                try {
                    Long postId = Long.parseLong(parts[0]);
                    Long rootId = parts[1].isEmpty() ? 0L : Long.parseLong(parts[1]);
                    
                    String mainKey = rootId != 0 ? getChildrenKey(postId, rootId) : getRootKey(postId);
                    redisTemplate.opsForZSet().add(mainKey, commentId.toString(), score);
                    
                    if (rootId == 0) {
                        redisTemplate.opsForZSet().add(getHotRootKey(postId), commentId.toString(), score);
                    } else {
                        redisTemplate.opsForZSet().add(getHotChildrenKey(postId, rootId), commentId.toString(), score);
                    }
                } catch (Exception e) {
                    log.warn("更新评论分数解析meta失败: commentId={}, meta={}", commentId, metaValue);
                }
            }
        }
    }
    
    @Override
    public void removeComment(Long commentId) {
        String metaValue = redisTemplate.opsForValue().get(getCommentMetaKey(0L, commentId));
        
        if (metaValue != null && !metaValue.isEmpty()) {
            String[] parts = metaValue.split(":");
            if (parts.length >= 2) {
                try {
                    Long postId = Long.parseLong(parts[0]);
                    Long rootId = parts[1].isEmpty() ? 0L : Long.parseLong(parts[1]);
                    
                    String mainKey = rootId != 0 ? getChildrenKey(postId, rootId) : getRootKey(postId);
                    String hotKey = rootId != 0 ? getHotChildrenKey(postId, rootId) : getHotRootKey(postId);
                    
                    redisTemplate.opsForZSet().remove(mainKey, commentId.toString());
                    redisTemplate.opsForZSet().remove(hotKey, commentId.toString());
                    redisTemplate.opsForZSet().remove(getRootKey(postId), commentId.toString());
                    redisTemplate.opsForZSet().remove(getHotRootKey(postId), commentId.toString());
                    
                    redisTemplate.delete(getCommentDataKey(postId, commentId));
                    redisTemplate.delete(getCommentMetaKey(postId, commentId));
                } catch (Exception e) {
                    log.warn("删除评论解析meta失败: commentId={}, meta={}", commentId, metaValue);
                }
            }
        }
    }
    
    @Override
    public long getCommentCount(Long postId, Long rootId) {
        String key = rootId != null && rootId != 0 ? getChildrenKey(postId, rootId) : getRootKey(postId);
        Long size = redisTemplate.opsForZSet().size(key);
        return size != null ? size : 0;
    }
    
    /**
     * 从数据库分页获取评论树 (优化版)
     */
    private CommentTreeResponse getCommentTreeFromDb(Long postId, String sort, String cursor, int size) {
        try {
            int offset = 0;
            if (cursor != null && !cursor.isEmpty()) {
                try {
                    offset = Integer.parseInt(cursor);
                } catch (NumberFormatException e) {
                    offset = 0;
                }
            }
            
            // 1. 从 Redis 获取根评论ID列表（按热度排序）
            String rootKey = getRootKey(postId);
            Long totalCount = redisTemplate.opsForZSet().zCard(rootKey);
            
            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(rootKey, offset, offset + size - 1);
            
            if (tuples == null || tuples.isEmpty()) {
                return CommentTreeResponse.builder()
                        .roots(Collections.emptyList())
                        .totalRoots(totalCount != null ? totalCount.intValue() : 0)
                        .cursor(null)
                        .build();
            }
            
            // 2. 提取评论ID并批量查询
            List<Long> commentIds = tuples.stream()
                    .map(t -> Long.parseLong(Objects.requireNonNull(t.getValue())))
                    .collect(Collectors.toList());
            
            List<NoteComment> rootComments = noteCommentMapper.selectByIds(commentIds);
            
            if (rootComments == null || rootComments.isEmpty()) {
                return CommentTreeResponse.builder()
                        .roots(Collections.emptyList())
                        .totalRoots(0)
                        .cursor(null)
                        .build();
            }
            
            // 2. 收集用户ID并查询用户信息
            Set<Long> userIds = rootComments.stream()
                    .map(NoteComment::getUserId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            
            Map<Long, User> userMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                List<User> users = userMapper.selectByIds(new ArrayList<>(userIds));
                for (User u : users) {
                    userMap.put(u.getId(), u);
                }
            }
            
            // 3. 构建根评论VO
            List<CommentTreeVO> roots = new ArrayList<>();
            for (NoteComment c : rootComments) {
                User user = userMap.get(c.getUserId());
                CommentTreeVO vo = CommentTreeVO.builder()
                        .commentId(c.getId())
                        .postId(c.getNoteId())
                        .parentId(c.getParentId())
                        .rootId(c.getRootId())
                        .userId(c.getUserId())
                        .nickname(user != null ? user.getNickname() : null)
                        .avatar(user != null ? user.getAvatar() : null)
                        .content(c.getContent())
                        .likeCount(c.getLikeCount())
                        .replyCount(c.getReplyCount())
                        .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                        .status(c.getStatus())
                        .hotScore(calculateHotScore(c))
                        .build();
                roots.add(vo);
            }
            
            // 3.5 为每个根评论加载子评论（最多10条）
            for (CommentTreeVO root : roots) {
                loadChildrenComments(root, userMap);
            }
            
            // 4. 计算总根评论数
            Long totalRoots = noteCommentMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NoteComment>()
                            .eq(NoteComment::getNoteId, postId)
                            .eq(NoteComment::getStatus, 1)
                            .isNull(NoteComment::getParentId)
            );
            
            // 5. 返回分页结果
            String newCursor = null;
            if (roots.size() == size) {
                newCursor = String.valueOf(offset + size);
            }
            
            return CommentTreeResponse.builder()
                    .roots(roots)
                    .totalRoots(totalRoots != null ? totalRoots.intValue() : 0)
                    .cursor(newCursor)
                    .build();
                    
        } catch (Exception e) {
            log.error("从数据库获取评论树失败: postId={}", postId, e);
            return CommentTreeResponse.builder()
                    .roots(Collections.emptyList())
                    .totalRoots(0)
                    .cursor(null)
                    .build();
        }
    }
    
    /**
     * 计算评论热度分数 (从NoteComment)
     */
    private double calculateHotScore(NoteComment comment) {
        if (comment == null) return 0;
        double LIKE_WEIGHT = 2.0;
        double REPLY_WEIGHT = 3.0;
        double TIME_DECAY = 0.1;
        
        double baseScore = (comment.getLikeCount() != null ? comment.getLikeCount() : 0) * LIKE_WEIGHT + 
                          (comment.getReplyCount() != null ? comment.getReplyCount() : 0) * REPLY_WEIGHT;
        if (comment.getCreatedAt() != null) {
            long hoursSince = (System.currentTimeMillis() - comment.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()) / 3600000;
            baseScore -= hoursSince * TIME_DECAY;
        }
        return baseScore;
    }
    
    private List<CommentSortData> loadComments(Long postId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> keys = ids.stream()
                .map(id -> getCommentDataKey(postId, id))
                .collect(Collectors.toList());
        
        List<String> jsonList = redisTemplate.opsForValue().multiGet(keys);
        
        List<CommentSortData> comments = new ArrayList<>();
        if (jsonList != null) {
            for (String json : jsonList) {
                if (json != null) {
                    try {
                        comments.add(objectMapper.readValue(json, CommentSortData.class));
                    } catch (JsonProcessingException e) {
                        log.error("JSON反序列化失败: {}", e.getMessage());
                    }
                }
            }
        }
        return comments;
    }
    
    private void collectCommentIds(CommentTreeVO comment, Set<Long> ids) {
        if (comment == null) return;
        if (comment.getChildren() != null) {
            for (CommentTreeVO child : comment.getChildren()) {
                ids.add(child.getCommentId());
                collectCommentIds(child, ids);
            }
        }
    }
    
    private String getCommentTreeKey(Long postId) {
        return String.format(COMMENT_TREE_KEY_PATTERN, postId);
    }
    
    public CommentTreeResponse getCommentTree(Long postId, String sort, String cursor, int size) {
        try {
            // 1. 从 Redis 有序集合获取根评论ID列表（按热度排序）
            String rootKey = getRootKey(postId);
            Long totalCount = redisTemplate.opsForZSet().zCard(rootKey);
            
            if (totalCount == null || totalCount == 0) {
                // Redis 没有数据，从数据库加载
                return getCommentTreeFromDb(postId, sort, cursor, size);
            }
            
            // 2. 计算偏移量
            long offset = 0;
            if (cursor != null && !cursor.isEmpty()) {
                try {
                    offset = Long.parseLong(cursor);
                } catch (NumberFormatException e) {
                    offset = 0;
                }
            }
            
            // 3. 分页获取评论ID（按热度从高到低）
            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(rootKey, offset, offset + size - 1);
            
            if (tuples == null || tuples.isEmpty()) {
                return CommentTreeResponse.builder()
                        .roots(Collections.emptyList())
                        .totalRoots(totalCount.intValue())
                        .cursor(null)
                        .build();
            }
            
            // 4. 提取评论ID
            List<Long> commentIds = tuples.stream()
                    .map(t -> Long.parseLong(Objects.requireNonNull(t.getValue())))
                    .collect(Collectors.toList());
            
            // 5. 从数据库批量查询评论详情
            List<NoteComment> comments = noteCommentMapper.selectByIds(commentIds);
            
            // 6. 收集用户ID并查询用户信息
            Set<Long> userIds = comments.stream()
                    .map(NoteComment::getUserId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            
            Map<Long, User> userMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                List<User> users = userMapper.selectByIds(new ArrayList<>(userIds));
                for (User u : users) {
                    userMap.put(u.getId(), u);
                }
            }
            
            // 7. 构建评论VO
            List<CommentTreeVO> roots = new ArrayList<>();
            for (NoteComment c : comments) {
                User user = userMap.get(c.getUserId());
                CommentTreeVO vo = CommentTreeVO.builder()
                        .commentId(c.getId())
                        .postId(c.getNoteId())
                        .parentId(c.getParentId())
                        .rootId(c.getRootId())
                        .userId(c.getUserId())
                        .nickname(user != null ? user.getNickname() : null)
                        .avatar(user != null ? user.getAvatar() : null)
                        .content(c.getContent())
                        .likeCount(c.getLikeCount())
                        .replyCount(c.getReplyCount())
                        .createdAt(c.getCreatedAt() != null ? 
                            c.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                        .status(c.getStatus())
                        .build();
                roots.add(vo);
            }
            
            // 7.5 为每个根评论加载子评论
            for (CommentTreeVO root : roots) {
                loadChildrenComments(root, userMap);
            }
            
            // 8. 返回分页结果
            String newCursor = null;
            if (offset + size < totalCount) {
                newCursor = String.valueOf(offset + size);
            }
            
            return CommentTreeResponse.builder()
                    .roots(roots)
                    .totalRoots(totalCount.intValue())
                    .cursor(newCursor)
                    .build();
                    
        } catch (Exception e) {
            log.error("从Redis获取评论树失败: postId={}", postId, e);
            // 降级到数据库查询
            return getCommentTreeFromDb(postId, sort, cursor, size);
        }
    }
    
    public void buildAndCacheCommentTree(Long postId) {
        List<NoteComment> comments = noteCommentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NoteComment>()
                        .eq(NoteComment::getNoteId, postId)
                        .eq(NoteComment::getStatus, 1)
                        .orderByAsc(NoteComment::getCreatedAt)
        );
        
        if (comments == null || comments.isEmpty()) {
            redisTemplate.opsForValue().set(getCommentTreeKey(postId), "{\"roots\":[],\"totalRoots\":0}");
            return;
        }
        
        Set<Long> userIds = comments.stream()
                .map(NoteComment::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectByIds(new ArrayList<>(userIds));
            for (User u : users) {
                userMap.put(u.getId(), u);
            }
        }
        
        Map<Long, NoteComment> commentMap = new HashMap<>();
        for (NoteComment c : comments) {
            commentMap.put(c.getId(), c);
        }
        
        Map<Long, List<CommentTreeVO>> childrenMap = new HashMap<>();
        for (NoteComment c : comments) {
            Long parentId = c.getParentId();
            if (parentId == null || parentId == 0) {
                parentId = 0L;
            }
            childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>());
        }
        
        List<CommentTreeVO> roots = new ArrayList<>();
        Map<Long, CommentTreeVO> commentVoMap = new HashMap<>();
        
        for (NoteComment c : comments) {
            User user = userMap.get(c.getUserId());
            String replyToNickname = null;
            
            if (c.getParentId() != null && c.getParentId() != 0) {
                NoteComment parent = commentMap.get(c.getParentId());
                if (parent != null && parent.getUserId() != null) {
                    User replyToUser = userMap.get(parent.getUserId());
                    if (replyToUser != null) {
                        replyToNickname = replyToUser.getNickname();
                    }
                }
            }
            
            CommentTreeVO vo = CommentTreeVO.builder()
                    .commentId(c.getId())
                    .postId(c.getNoteId())
                    .parentId(c.getParentId())
                    .rootId(c.getRootId())
                    .userId(c.getUserId())
                    .nickname(user != null ? user.getNickname() : null)
                    .avatar(user != null ? user.getAvatar() : null)
                    .content(c.getContent())
                    .likeCount(c.getLikeCount())
                    .replyCount(c.getReplyCount())
                    .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                    .status(c.getStatus())
                    .replyToNickname(replyToNickname)
                    .build();
            
            commentVoMap.put(c.getId(), vo);
            
            if (c.getParentId() == null || c.getParentId() == 0) {
                roots.add(vo);
            } else {
                Long parentId = c.getParentId();
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(vo);
            }
        }
        
        for (NoteComment c : comments) {
            Long parentId = c.getParentId();
            if (parentId == null || parentId == 0) {
                continue;
            }
            CommentTreeVO child = commentVoMap.get(c.getId());
            CommentTreeVO parent = commentVoMap.get(parentId);
            if (parent != null) {
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(child);
            }
        }
        
        Collections.sort(roots, (a, b) -> {
            CommentTreeVO aVo = commentVoMap.get(a.getCommentId());
            CommentTreeVO bVo = commentVoMap.get(b.getCommentId());
            if (aVo == null || bVo == null) return 0;
            CommentSortData aData = CommentSortData.builder()
                    .likeCount(aVo.getLikeCount())
                    .replyCount(aVo.getReplyCount())
                    .createdAt(aVo.getCreatedAt())
                    .build();
            CommentSortData bData = CommentSortData.builder()
                    .likeCount(bVo.getLikeCount())
                    .replyCount(bVo.getReplyCount())
                    .createdAt(bVo.getCreatedAt())
                    .build();
            double scoreA = ScoreCalculator.calculateHotScore(aData);
            double scoreB = ScoreCalculator.calculateHotScore(bData);
            return Double.compare(scoreB, scoreA);
        });
        
        for (CommentTreeVO root : roots) {
            sortChildrenByHot(root);
        }
        
        for (CommentTreeVO vo : commentVoMap.values()) {
            if (vo.getHotScore() == null) {
                CommentSortData data = CommentSortData.builder()
                        .likeCount(vo.getLikeCount())
                        .replyCount(vo.getReplyCount())
                        .createdAt(vo.getCreatedAt())
                        .build();
                vo.setHotScore(ScoreCalculator.calculateHotScore(data));
            }
        }
        
        CommentTreeResponse response = CommentTreeResponse.builder()
                .roots(roots)
                .totalRoots(roots.size())
                .build();
        
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(getCommentTreeKey(postId), json);
            log.info("构建评论树缓存完成: postId={}, totalRoots={}", postId, roots.size());
        } catch (JsonProcessingException e) {
            log.error("序列化评论树失败: {}", e.getMessage());
        }
    }
    
    private void sortChildrenByHot(CommentTreeVO parent) {
        if (parent.getChildren() == null || parent.getChildren().isEmpty()) {
            return;
        }
        
        List<CommentTreeVO> children = parent.getChildren();
        Collections.sort(children, (a, b) -> {
            CommentSortData aData = CommentSortData.builder()
                    .likeCount(a.getLikeCount())
                    .replyCount(a.getReplyCount())
                    .createdAt(a.getCreatedAt())
                    .build();
            CommentSortData bData = CommentSortData.builder()
                    .likeCount(b.getLikeCount())
                    .replyCount(b.getReplyCount())
                    .createdAt(b.getCreatedAt())
                    .build();
            double scoreA = ScoreCalculator.calculateHotScore(aData);
            double scoreB = ScoreCalculator.calculateHotScore(bData);
            return Double.compare(scoreB, scoreA);
        });
        
        for (CommentTreeVO child : children) {
            sortChildrenByHot(child);
        }
    }
    
    public void refreshCache(Long postId) {
        if (postId == null) {
            return;
        }
        
        try {
            log.info("开始刷新评论树缓存: postId={}", postId);
            buildAndCacheCommentTree(postId);
            log.info("刷新评论树缓存完成: postId={}", postId);
        } catch (Exception e) {
            log.error("刷新评论树缓存失败: postId={}, error={}", postId, e.getMessage());
        }
    }
    
    private static final double TOP_SCORE = Double.MAX_VALUE;
    
    public boolean addCommentToTree(Long postId, NoteComment noteComment, boolean isRoot) {
        if (postId == null || noteComment == null || noteComment.getId() == null) {
            return false;
        }
        
        try {
            String treeKey = getCommentTreeKey(postId);
            String json = redisTemplate.opsForValue().get(treeKey);
            
            boolean isFirstLoad = (json == null || json.isEmpty());
            if (isFirstLoad) {
                buildAndCacheCommentTree(postId);
                json = redisTemplate.opsForValue().get(treeKey);
            }
            
            if (json == null || json.isEmpty()) {
                return false;
            }
            
            CommentTreeResponse response = objectMapper.readValue(json, CommentTreeResponse.class);
            List<CommentTreeVO> roots = response.getRoots();
            if (roots == null) {
                roots = new ArrayList<>();
            }
            
            User user = userMapper.selectById(noteComment.getUserId());
            String replyToNickname = null;
            if (!isRoot && noteComment.getParentId() != null) {
                NoteComment parent = noteCommentMapper.selectById(noteComment.getParentId());
                if (parent != null && parent.getUserId() != null) {
                    User replyUser = userMapper.selectById(parent.getUserId());
                    if (replyUser != null) {
                        replyToNickname = replyUser.getNickname();
                    }
                }
            }
            
            CommentTreeVO newCommentVO = CommentTreeVO.builder()
                    .commentId(noteComment.getId())
                    .postId(noteComment.getNoteId())
                    .parentId(noteComment.getParentId())
                    .rootId(noteComment.getRootId())
                    .userId(noteComment.getUserId())
                    .nickname(user != null ? user.getNickname() : null)
                    .avatar(user != null ? user.getAvatar() : null)
                    .content(noteComment.getContent())
                    .likeCount(noteComment.getLikeCount())
                    .replyCount(noteComment.getReplyCount())
                    .createdAt(noteComment.getCreatedAt() != null ? 
                        noteComment.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                    .status(noteComment.getStatus())
                    .replyToNickname(replyToNickname)
                    .hotScore(TOP_SCORE)
                    .build();
            newCommentVO.setChildren(new ArrayList<>());
            
            if (isRoot) {
                roots.add(0, newCommentVO);
            } else {
                Long parentId = noteComment.getParentId();
                boolean found = false;
                for (CommentTreeVO root : roots) {
                    if (root.getCommentId().equals(parentId)) {
                        if (root.getChildren() == null) {
                            root.setChildren(new ArrayList<>());
                        }
                        root.getChildren().add(0, newCommentVO);
                        root.setReplyCount((root.getReplyCount() != null ? root.getReplyCount() : 0) + 1);
                        found = true;
                        break;
                    }
                    if (root.getChildren() != null) {
                        found = addChildToParent(root.getChildren(), parentId, newCommentVO);
                        if (found) break;
                    }
                }
                if (!found) {
                    log.warn("未找到parent评论,跳过增量更新: parentId={}", parentId);
                }
            }
            
            response.setRoots(roots);
            response.setTotalRoots(roots.size());
            
            String newJson = objectMapper.writeValueAsString(response);
            
            redisTemplate.opsForValue().set(treeKey, newJson);
            
            incrementCommentCount(postId, 1);
            
            log.info("增量添加评论到缓存树: postId={}, commentId={}, isRoot={}", postId, noteComment.getId(), isRoot);
            return true;
        } catch (Exception e) {
            log.error("增量添加评论到缓存树失败: postId={}, error={}", postId, e.getMessage());
            return false;
        }
    }
    
    private boolean addChildToParent(List<CommentTreeVO> children, Long parentId, CommentTreeVO newChild) {
        for (CommentTreeVO child : children) {
            if (child.getCommentId().equals(parentId)) {
                if (child.getChildren() == null) {
                    child.setChildren(new ArrayList<>());
                }
                child.getChildren().add(0, newChild);
                child.setReplyCount((child.getReplyCount() != null ? child.getReplyCount() : 0) + 1);
                return true;
            }
            if (child.getChildren() != null && !child.getChildren().isEmpty()) {
                if (addChildToParent(child.getChildren(), parentId, newChild)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean removeCommentFromTree(Long postId, Long commentId) {
        if (postId == null || commentId == null) {
            return false;
        }
        
        try {
            String treeKey = getCommentTreeKey(postId);
            String json = redisTemplate.opsForValue().get(treeKey);
            
            if (json == null || json.isEmpty()) {
                return true;
            }
            
            CommentTreeResponse response = objectMapper.readValue(json, CommentTreeResponse.class);
            List<CommentTreeVO> roots = response.getRoots();
            if (roots == null) {
                return true;
            }
            
            boolean removed = removeCommentFromList(roots, commentId);
            
            if (removed) {
                response.setRoots(roots);
                response.setTotalRoots(roots.size());
                
                String newJson = objectMapper.writeValueAsString(response);
                redisTemplate.opsForValue().set(treeKey, newJson);
                
                log.info("从缓存树删除评论: postId={}, commentId={}", postId, commentId);
            }
            return removed;
        } catch (Exception e) {
            log.error("从缓存树删除评论失败: postId={}, error={}", postId, e.getMessage());
            return false;
        }
    }
    
    private boolean removeCommentFromList(List<CommentTreeVO> list, Long commentId) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getCommentId().equals(commentId)) {
                list.remove(i);
                return true;
            }
            if (list.get(i).getChildren() != null && !list.get(i).getChildren().isEmpty()) {
                if (removeCommentFromList(list.get(i).getChildren(), commentId)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean removeCommentAndChildrenFromTree(Long postId, Long commentId, Long parentId) {
        if (postId == null || commentId == null) {
            return false;
        }
        
        try {
            String treeKey = getCommentTreeKey(postId);
            String json = redisTemplate.opsForValue().get(treeKey);
            
            if (json == null || json.isEmpty()) {
                return true;
            }
            
            CommentTreeResponse response = objectMapper.readValue(json, CommentTreeResponse.class);
            List<CommentTreeVO> roots = response.getRoots();
            if (roots == null) {
                return true;
            }
            
            int removedCount = 0;
            
            // 递归删除评论及所有子评论（包含统计）
            // 注意：removeCommentRecursive会递归删除并统计该评论及其所有子孙
            removedCount += removeCommentRecursive(roots, commentId);
            
            // 如果是根评论，同时也从roots列表中移除
            // 这一步不重复统计，只确保清理干净
            if (parentId == null || parentId == 0) {
                roots.removeIf(r -> r.getCommentId().equals(commentId));
            }
            
            response.setRoots(roots);
            response.setTotalRoots(roots.size());
            
            String newJson = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(treeKey, newJson);
            
            // 更新评论数
            if (removedCount > 0) {
                incrementCommentCount(postId, -removedCount);
            }
            
            log.info("从缓存树删除评论(含子评论): postId={}, commentId={}, removedCount={}", postId, commentId, removedCount);
            return true;
        } catch (Exception e) {
            log.error("从缓存树删除评论失败: postId={}, error={}", postId, e.getMessage());
            return false;
        }
    }
    
    private int removeCommentRecursive(List<CommentTreeVO> comments, Long commentId) {
        int count = 0;
        Iterator<CommentTreeVO> iterator = comments.iterator();
        while (iterator.hasNext()) {
            CommentTreeVO comment = iterator.next();
            if (comment.getCommentId().equals(commentId)) {
                iterator.remove();
                count += 1;
                // 递归删除子评论
                if (comment.getChildren() != null && !comment.getChildren().isEmpty()) {
                    count += countAllChildren(comment.getChildren());
                    comment.getChildren().clear();
                }
                return count;
            }
            if (comment.getChildren() != null && !comment.getChildren().isEmpty()) {
                count += removeCommentRecursive(comment.getChildren(), commentId);
            }
        }
        return count;
    }
    
    private int countAllChildren(List<CommentTreeVO> comments) {
        int count = 0;
        for (CommentTreeVO comment : comments) {
            count += 1;
            if (comment.getChildren() != null && !comment.getChildren().isEmpty()) {
                count += countAllChildren(comment.getChildren());
            }
        }
        return count;
    }
    
    private static final int CHILDREN_PAGE_SIZE = 10;
    
    private void loadChildrenComments(CommentTreeVO root, Map<Long, User> userMap) {
        Long rootId = root.getCommentId();
        
        Long totalChildren = noteCommentMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NoteComment>()
                        .eq(NoteComment::getRootId, rootId)
                        .eq(NoteComment::getStatus, 1)
                        .ne(NoteComment::getParentId, 0)
        );
        
        root.setTotalChildren(totalChildren != null ? totalChildren.intValue() : 0);
        
        List<NoteComment> childrenComments = noteCommentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NoteComment>()
                        .eq(NoteComment::getRootId, rootId)
                        .eq(NoteComment::getStatus, 1)
                        .ne(NoteComment::getParentId, 0)
                        .orderByAsc(NoteComment::getCreatedAt)
                        .last("LIMIT " + (CHILDREN_PAGE_SIZE + 1))
        );
        
        if (childrenComments != null && !childrenComments.isEmpty()) {
            boolean hasMore = childrenComments.size() > CHILDREN_PAGE_SIZE;
            if (hasMore) {
                childrenComments = childrenComments.subList(0, CHILDREN_PAGE_SIZE);
            }
            
            List<CommentTreeVO> children = new ArrayList<>();
            Set<Long> childUserIds = new HashSet<>();
            for (NoteComment c : childrenComments) {
                if (c.getUserId() != null) {
                    childUserIds.add(c.getUserId());
                }
            }
            
            Map<Long, User> childUserMap = new HashMap<>();
            if (!childUserIds.isEmpty()) {
                List<User> childUsers = userMapper.selectByIds(new ArrayList<>(childUserIds));
                for (User u : childUsers) {
                    childUserMap.put(u.getId(), u);
                }
            }
            
            for (NoteComment c : childrenComments) {
                User user = childUserMap.get(c.getUserId());
                String replyToNickname = null;
                
                if (c.getParentId() != null && c.getParentId() != 0) {
                    NoteComment parent = noteCommentMapper.selectById(c.getParentId());
                    if (parent != null && parent.getUserId() != null) {
                        User replyToUser = userMapper.selectById(parent.getUserId());
                        if (replyToUser != null) {
                            replyToNickname = replyToUser.getNickname();
                        }
                    }
                }
                
                CommentTreeVO childVo = CommentTreeVO.builder()
                        .commentId(c.getId())
                        .postId(c.getNoteId())
                        .parentId(c.getParentId())
                        .rootId(c.getRootId())
                        .userId(c.getUserId())
                        .nickname(user != null ? user.getNickname() : null)
                        .avatar(user != null ? user.getAvatar() : null)
                        .content(c.getContent())
                        .likeCount(c.getLikeCount())
                        .replyCount(c.getReplyCount())
                        .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                        .status(c.getStatus())
                        .replyToNickname(replyToNickname)
                        .hotScore(calculateHotScore(c))
                        .build();
                
                Long childTotalChildren = noteCommentMapper.selectCount(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NoteComment>()
                                .eq(NoteComment::getRootId, rootId)
                                .eq(NoteComment::getParentId, c.getId())
                                .eq(NoteComment::getStatus, 1)
                );
                childVo.setTotalChildren(childTotalChildren != null ? childTotalChildren.intValue() : 0);
                childVo.setHasMoreChildren(childTotalChildren != null && childTotalChildren > 0);
                
                children.add(childVo);
            }
            
            root.setChildren(children);
            root.setHasMoreChildren(hasMore);
        } else {
            root.setChildren(new ArrayList<>());
            root.setHasMoreChildren(false);
        }
    }
}
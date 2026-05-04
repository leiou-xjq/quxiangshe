package com.quxiangshe.backend.service.sort;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quxiangshe.backend.config.CommentSortConfig;
import com.quxiangshe.backend.entity.CommentSortData;
import com.quxiangshe.backend.service.impl.CommentSortServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BucketSortStrategy implements SortStrategy {
    
    private final StringRedisTemplate redisTemplate;
    private final CommentSortConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String ROOT_KEY_PATTERN = "post:%d:root_comments";
    private static final String CHILDREN_KEY_PATTERN = "post:%d:comment:%d:children";
    private static final String HOT_ROOT_KEY_PATTERN = "post:%d:hot:root_comments";
    private static final String HOT_CHILDREN_KEY_PATTERN = "post:%d:comment:%d:hot:children";
    private static final String COMMENT_DATA_KEY_PATTERN = "post:%d:comment:%d:data";
    private static final String COMMENT_META_KEY_PATTERN = "post:%d:comment:%d:meta";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long BUCKET_TTL = 30 * 24 * 3600;  // 30天
    
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
    
    private String getRootKey(Long postId, String date) {
        return String.format("post:%d:bucket:%s:root_comments", postId, date);
    }
    
    private String getChildrenKey(Long postId, Long rootId, String date) {
        return String.format("post:%d:comment:%d:bucket:%s:children", postId, rootId, date);
    }
    
    private String getCommentDataKey(Long postId, Long commentId) {
        return String.format(COMMENT_DATA_KEY_PATTERN, postId, commentId);
    }
    
    private String getCommentMetaKey(Long postId, Long commentId) {
        return String.format(COMMENT_META_KEY_PATTERN, postId, commentId);
    }
    
    @Override
    public List<CommentSortData> getRootComments(Long postId, String sort, String cursor, int size) {
        List<CommentSortData> result = new ArrayList<>();
        Set<Long> allIds = new LinkedHashSet<>();
        
        if ("hot".equals(sort)) {
            Set<String> hotIds;
            Long cursorId = parseCursor(cursor);
            
            if (cursorId != null) {
                hotIds = redisTemplate.opsForZSet().reverseRange(getHotRootKey(postId), 0, -1);
                if (hotIds != null) {
                    boolean passedCursor = false;
                    for (String id : hotIds) {
                        long commentId = Long.parseLong(id);
                        if (commentId == cursorId) {
                            passedCursor = true;
                            continue;
                        }
                        if (passedCursor) {
                            allIds.add(commentId);
                            if (allIds.size() >= size) break;
                        }
                    }
                }
            } else {
                hotIds = redisTemplate.opsForZSet().reverseRange(getHotRootKey(postId), 0, size - 1);
                if (hotIds != null) {
                    allIds.addAll(hotIds.stream().map(Long::parseLong).collect(Collectors.toSet()));
                }
            }
            
            int remaining = size - allIds.size();
            if (remaining > 0) {
                gatherFromBuckets(postId, null, "time_desc", null, remaining, allIds);
            }
        } else {
            gatherFromBuckets(postId, null, sort, cursor, size, allIds);
        }
        
        if (!allIds.isEmpty()) {
            result = loadComments(postId, new ArrayList<>(allIds));
        }
        return result;
    }
    
    private Long parseCursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private void gatherFromBuckets(Long postId, Long rootId, String sort, String cursor, int limitSize, Set<Long> allIds) {
        int days = 30;
        
        for (int i = 0; i < days; i++) {
            String date = LocalDate.now().minusDays(i).format(DATE_FORMAT);
            String bucketKey = rootId != null && rootId != 0 ? 
                getChildrenKey(postId, rootId, date) : getRootKey(postId, date);
            
            Long bucketSize = redisTemplate.opsForZSet().size(bucketKey);
            if (bucketSize == null || bucketSize == 0) {
                continue;
            }
            
            Set<String> ids;
            if ("time_desc".equals(sort)) {
                ids = redisTemplate.opsForZSet().reverseRange(bucketKey, 0, bucketSize);
            } else if ("time_asc".equals(sort)) {
                ids = redisTemplate.opsForZSet().range(bucketKey, 0, bucketSize);
            } else {
                ids = redisTemplate.opsForZSet().reverseRange(bucketKey, 0, bucketSize);
            }
            
            if (ids != null) {
                for (String id : ids) {
                    allIds.add(Long.parseLong(id));
                    if (allIds.size() >= limitSize) break;
                }
            }
            
            if (allIds.size() >= limitSize) break;
        }
    }
    
    @Override
    public List<CommentSortData> getChildComments(Long postId, Long rootId, String sort, String cursor, int size) {
        List<CommentSortData> result = new ArrayList<>();
        Set<Long> allIds = new LinkedHashSet<>();
        
        if ("hot".equals(sort)) {
            Set<String> hotIds;
            Long cursorId = parseCursor(cursor);
            
            if (cursorId != null) {
                hotIds = redisTemplate.opsForZSet().reverseRange(getHotChildrenKey(postId, rootId), 0, -1);
                if (hotIds != null) {
                    boolean passedCursor = false;
                    for (String id : hotIds) {
                        long commentId = Long.parseLong(id);
                        if (commentId == cursorId) {
                            passedCursor = true;
                            continue;
                        }
                        if (passedCursor) {
                            allIds.add(commentId);
                            if (allIds.size() >= size) break;
                        }
                    }
                }
            } else {
                hotIds = redisTemplate.opsForZSet().reverseRange(getHotChildrenKey(postId, rootId), 0, size - 1);
                if (hotIds != null) {
                    allIds.addAll(hotIds.stream().map(Long::parseLong).collect(Collectors.toSet()));
                }
            }
            
            int remaining = size - allIds.size();
            if (remaining > 0) {
                gatherFromBuckets(postId, rootId, "time_desc", null, remaining, allIds);
            }
        } else {
            gatherFromBuckets(postId, rootId, sort, cursor, size, allIds);
        }
        
        if (!allIds.isEmpty()) {
            result = loadComments(postId, new ArrayList<>(allIds));
        }
        return result;
    }
    
    @Override
    public void addComment(CommentSortData comment) {
        if (comment == null || comment.getCommentId() == null) return;
        
        Long postId = comment.getPostId();
        Long rootId = comment.getRootId();
        String bucketDate = LocalDate.now().format(DATE_FORMAT);
        
        String bucketKey = rootId != null && rootId != 0 ? 
            getChildrenKey(postId, rootId, bucketDate) : getRootKey(postId, bucketDate);
        
        double score = "hot".equals("hottest") ? 
            ScoreCalculator.calculateHotScore(comment) : 
            (comment.getCreatedAt() != null ? comment.getCreatedAt() : System.currentTimeMillis());
        
        try {
            redisTemplate.opsForValue().set(getCommentDataKey(postId, comment.getCommentId()), objectMapper.writeValueAsString(comment));
            redisTemplate.opsForValue().set(getCommentMetaKey(postId, comment.getCommentId()), postId + ":" + rootId);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败: {}", e.getMessage());
            return;
        }
        
        redisTemplate.opsForZSet().add(bucketKey, comment.getCommentId().toString(), score);
        redisTemplate.expire(bucketKey, BUCKET_TTL, java.util.concurrent.TimeUnit.SECONDS);
        
        if (rootId == null || rootId == 0) {
            if (ScoreCalculator.isHotComment(comment, config.getHotThreshold())) {
                redisTemplate.opsForZSet().add(getHotRootKey(postId), comment.getCommentId().toString(), score);
                redisTemplate.expire(getHotRootKey(postId), BUCKET_TTL, java.util.concurrent.TimeUnit.SECONDS);
            }
        } else {
            redisTemplate.opsForZSet().add(getHotChildrenKey(postId, rootId), comment.getCommentId().toString(), score);
            redisTemplate.expire(getHotChildrenKey(postId, rootId), BUCKET_TTL, java.util.concurrent.TimeUnit.SECONDS);
        }
    }
    
    @Override
    public void updateScore(Long commentId, double score) {
        String metaValue = redisTemplate.opsForValue().get(getCommentMetaKey(0L, commentId));
        
        if (metaValue != null && !metaValue.isEmpty()) {
            String[] parts = metaValue.split(":");
            if (parts.length >= 2) {
                try {
                    Long postId = Long.parseLong(parts[0]);
                    Long rootId = parts[1].isEmpty() ? 0L : Long.parseLong(parts[1]);
                    
                    String hotKey = rootId != null && rootId != 0 ? getHotChildrenKey(postId, rootId) : getHotRootKey(postId);
                    redisTemplate.opsForZSet().add(hotKey, commentId.toString(), score);
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
                    
                    for (int i = 0; i < 30; i++) {
                        String date = LocalDate.now().minusDays(i).format(DATE_FORMAT);
                        String bucketKey = rootId != 0 ? 
                            getChildrenKey(postId, rootId, date) : getRootKey(postId, date);
                        redisTemplate.opsForZSet().remove(bucketKey, commentId.toString());
                    }
                    
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
        Set<String> keys = new HashSet<>();
        
        String pattern = rootId != null && rootId != 0 ? 
            "post:" + postId + ":comment:" + rootId + ":bucket:*:children" : 
            "post:" + postId + ":bucket:*:root_comments";
        
        ScanOptions scanOptions = ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        
        long total = 0;
        for (String key : keys) {
            Long size = redisTemplate.opsForZSet().size(key);
            if (size != null) {
                total += size;
            }
        }
        
        Long hotSize = redisTemplate.opsForZSet().size(
            rootId != null && rootId != 0 ? getHotChildrenKey(postId, rootId) : getHotRootKey(postId)
        );
        if (hotSize != null) {
            total += hotSize;
        }
        
        return total;
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
    
    @Override
    public List<CommentSortData> getAllComments(Long postId, String sort, String cursor, int size) {
        return getRootComments(postId, sort, cursor, size);
    }
}
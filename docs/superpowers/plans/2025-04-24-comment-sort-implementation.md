# 评论排序系统实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现基于 Redis ZSet 的高性能评论排序系统，支持全量排序和时间分桶+聚合两种策略

**Architecture:** 自适应策略 - 评论数量≤1000时使用全量ZSet排序，>1000时使用时间分桶+热点聚合。每次从桶中取100条数据，热点判定分数提高到50

**Tech Stack:** Spring Boot + Redis + MyBatis

---

## 文件结构

```
comment-sort/
├── dto/
│   └── CommentSortRequest.java         # 评论排序请求DTO
├── entity/
│   └── CommentSortData.java            # 排序数据实体（非数据库实体）
├── config/
│   └── CommentSortConfig.java            # 排序配置类
├── service/
│   ├── ICommentSortService.java        # 排序服务接口
│   └── impl/
│       └── CommentSortServiceImpl.java # 排序服务实现
├── strategy/
│   ├── SortStrategy.java              # 排序策略接口
│   ├── FullSortStrategy.java          # 全量排序策略
│   ├── BucketSortStrategy.java        # 分桶排序策略
│   └── ScoreCalculator.java          # 热度计算器
└── controller/
    └── CommentSortController.java     # 排序API控制器
```

---

## Task 1: 创建配置类和实体

**Files:**
- Create: `backend/src/main/java/com/quxiangshe/backend/config/CommentSortConfig.java`
- Create: `backend/src/main/java/com/quxiangshe/backend/entity/CommentSortData.java`

- [ ] **Step 1: 创建 CommentSortConfig 配置类**

```java
package com.quxiangshe.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 评论排序配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "comment.sort")
public class CommentSortConfig {
    
    /** 切换分桶策略的数量阈值 */
    private int threshold = 1000;
    
    /** 每个桶最多取出的数据条数 */
    private int bucketSize = 100;
    
    /** 热点判定分数阈值 */
    private int hotThreshold = 50;
    
    /** 热点桶最少评论数 */
    private int hotMinComments = 10;
}
```

- [ ] **Step 2: 创建 CommentSortData 实体类**

```java
package com.quxiangshe.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 评论排序数据（Redis存储用，非数据库实体）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentSortData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 评论ID */
    private Long commentId;
    
    /** 笔记ID */
    private Long postId;
    
    /** 根评论ID（0表示根评论） */
    private Long rootId;
    
    /** 用户ID */
    private Long userId;
    
    /** 昵称 */
    private String nickname;
    
    /** 头像 */
    private String avatar;
    
    /** 评论内容 */
    private String content;
    
    /** 点赞数 */
    private Integer likeCount;
    
    /** 回复数 */
    private Integer replyCount;
    
    /** 创建时间戳（毫秒） */
    private Long createdAt;
    
    /** 状态：1-正常 */
    private Integer status;
    
    /** 被回复者昵称 */
    private String replyToNickname;
}
```

---

## Task 2: 创建排序策略接口和实现

**Files:**
- Create: `backend/src/main/java/com/quxiangshe/backend/service/sort/SortStrategy.java`
- Create: `backend/src/main/java/com/quxiangshe/backend/service/sort/FullSortStrategy.java`
- Create: `backend/src/main/java/com/quxiangshe/backend/service/sort/BucketSortStrategy.java`
- Create: `backend/src/main/java/com/quxiangshe/backend/service/sort/ScoreCalculator.java`

- [ ] **Step 1: 创建 SortStrategy 排序策略接口**

```java
package com.quxiangshe.backend.service.sort;

import com.quxiangshe.backend.entity.CommentSortData;

import java.util.List;

/**
 * 评论排序策略接口
 */
public interface SortStrategy {
    
    /**
     * 获取根评论排序列表
     */
    List<CommentSortData> getRootComments(Long postId, String sort, String cursor, int size);
    
    /**
     * 获取子评论排序列表
     */
    List<CommentSortData> getChildComments(Long postId, Long rootId, String sort, String cursor, int size);
    
    /**
     * 添加评论到排序索引
     */
    void addComment(CommentSortData comment);
    
    /**
     * 更新评论排序分数
     */
    void updateScore(Long commentId, double score);
    
    /**
     * 从排序索引中移除评论
     */
    void removeComment(Long commentId);
    
    /**
     * 获取评论总数
     */
    long getCommentCount(Long postId, Long rootId);
}
```

- [ ] **Step 2: 创建 ScoreCalculator 热度计算器**

```java
package com.quxiangshe.backend.service.sort;

import com.quxiangshe.backend.entity.CommentSortData;

/**
 * 热度分数计算器
 */
public class ScoreCalculator {
    
    private static final double LIKE_WEIGHT = 2.0;
    private static final double REPLY_WEIGHT = 3.0;
    private static final double TIME_DECAY = 0.1;
    
    /**
     * 计算综合热度分数
     * 热度 = 点赞数 * 2 + 回复数 * 3 - 时间衰减因子
     */
    public static double calculateHotScore(CommentSortData comment) {
        if (comment == null) {
            return 0;
        }
        
        double baseScore = comment.getLikeCount() * LIKE_WEIGHT + 
                          comment.getReplyCount() * REPLY_WEIGHT;
        
        // 时间衰减
        if (comment.getCreatedAt() != null) {
            long hoursSince = (System.currentTimeMillis() - comment.getCreatedAt()) / 3600000;
            baseScore -= hoursSince * TIME_DECAY;
        }
        
        return baseScore;
    }
    
    /**
     * 计算时间排序分数（时间戳，越新分数越高）
     */
    public static double calculateTimeScore(CommentSortData comment) {
        return comment != null && comment.getCreatedAt() != null ? 
               comment.getCreatedAt() : 0;
    }
    
    /**
     * 计算时间倒序分数（取负值实现倒序）
     */
    public static double calculateTimeDescScore(CommentSortData comment) {
        return comment != null && comment.getCreatedAt() != null ? 
               -comment.getCreatedAt() : 0;
    }
    
    /**
     * 判断是否为热点评论
     * 综合热度 >= 50
     */
    public static boolean isHotComment(CommentSortData comment, int hotThreshold) {
        if (comment == null) {
            return false;
        }
        
        double score = comment.getLikeCount() * LIKE_WEIGHT + 
                      comment.getReplyCount() * REPLY_WEIGHT;
        
        return score >= hotThreshold;
    }
}
```

- [ ] **Step 3: 创建 FullSortStrategy 全量排序策略**

```java
package com.quxiangshe.backend.service.sort;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.quxiangshe.backend.config.CommentSortConfig;
import com.quxiangshe.backend.entity.CommentSortData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 全量排序策略 - 评论数量少时使用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FullSortStrategy implements SortStrategy {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final CommentSortConfig config;
    
    private static final String ROOT_COMMENTS_KEY = "post:%d:root_comments";
    private static final String CHILDREN_KEY = "post:%d:comment:%d:children";
    private static final String HOT_ROOT_KEY = "post:%d:hot:root_comments";
    private static final String HOT_CHILDREN_KEY = "post:%d:comment:%d:hot:children";
    
    @Override
    public List<CommentSortData> getRootComments(Long postId, String sort, String cursor, int size) {
        String key = String.format(ROOT_COMMENTS_KEY, postId);
        return getComments(key, sort, cursor, size);
    }
    
    @Override
    public List<CommentSortData> getChildComments(Long postId, Long rootId, String sort, String cursor, int size) {
        String key = String.format(CHILDREN_KEY, postId, rootId);
        return getComments(key, sort, cursor, size);
    }
    
    private List<CommentSortData> getComments(String key, String sort, String cursor, int size) {
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        
        // 确定排序方式
        boolean reverse = !"time_asc".equals(sort);
        
        // 游标处理（cursor为score值）
        double startScore = cursor != null && !cursor.isEmpty() ? 
                        Double.parseDouble(cursor) : Double.MIN_VALUE;
        
        // 使用 ZRANGEBYSCORE 进行分页查询
        Set<Object> results = zSetOps.rangeByScore(key, startScore, Double.MAX_VALUE, 0, size);
        
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取下一个游标
        double nextScore = startScore;
        List<CommentSortData> comments = new ArrayList<>();
        for (Object obj : results) {
            CommentSortData comment = deserialize(obj);
            if (comment != null) {
                comments.add(comment);
                nextScore = Math.max(nextScore, getScore(comment, sort));
            }
        }
        
        // 反转结果（如果需要）
        if (reverse && "time_desc".equals(sort)) {
            Collections.reverse(comments);
        }
        
        return comments;
    }
    
    private double getScore(CommentSortData comment, String sort) {
        return switch (sort) {
            case "hottest" -> ScoreCalculator.calculateHotScore(comment);
            case "time_asc" -> ScoreCalculator.calculateTimeScore(comment);
            case "time_desc" -> ScoreCalculator.calculateTimeDescScore(comment);
            default -> ScoreCalculator.calculateHotScore(comment);
        };
    }
    
    @Override
    public void addComment(CommentSortData comment) {
        if (comment == null || comment.getCommentId() == null) {
            return;
        }
        
        double score = ScoreCalculator.calculateHotScore(comment);
        
        if (comment.getRootId() == null || comment.getRootId() == 0) {
            // 根评论
            String key = String.format(ROOT_COMMENTS_KEY, comment.getPostId());
            redisTemplate.opsForZSet().add(key, serialize(comment), score);
            
            // 同时加入热点桶
            if (ScoreCalculator.isHotComment(comment, config.getHotThreshold())) {
                String hotKey = String.format(HOT_ROOT_KEY, comment.getPostId());
                redisTemplate.opsForZSet().add(hotKey, serialize(comment), score);
            }
        } else {
            // 子评论
            String key = String.format(CHILDREN_KEY, comment.getPostId(), comment.getRootId());
            redisTemplate.opsForZSet().add(key, serialize(comment), score);
            
            String hotKey = String.format(HOT_CHILDREN_KEY, comment.getPostId(), comment.getRootId());
            redisTemplate.opsForZSet().add(hotKey, serialize(comment), score);
        }
    }
    
    @Override
    public void updateScore(Long commentId, double score) {
        // 更新所有相关ZSet中的分数
        // 需要从MySQL查询评论信息确定postId和rootId
    }
    
    @Override
    public void removeComment(Long commentId) {
        // 从所有相关ZSet中移除
        // 需要从MySQL查询评论信息确定key
    }
    
    @Override
    public long getCommentCount(Long postId, Long rootId) {
        String key = rootId == null || rootId == 0 ? 
                   String.format(ROOT_COMMENTS_KEY, postId) : 
                   String.format(CHILDREN_KEY, postId, rootId);
        
        Long count = redisTemplate.opsForZSet().zCard(key);
        return count != null ? count : 0;
    }
    
    private String serialize(CommentSortData comment) {
        try {
            return new ObjectMapper().writeValueAsString(comment);
        } catch (JsonProcessingException e) {
            log.error("序列化评论失败", e);
            return "{}";
        }
    }
    
    private CommentSortData deserialize(Object obj) {
        try {
            String json = obj.toString();
            return new ObjectMapper().readValue(json, CommentSortData.class);
        } catch (JsonProcessingException e) {
            log.error("反序列化评论失败", e);
            return null;
        }
    }
}
```

- [ ] **Step 4: 创建 BucketSortStrategy 分桶排序策略**

```java
package com.quxiangshe.backend.service.sort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 分桶排序策略 - 评论数量多时使用
 * 每次从桶中取100条数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BucketSortStrategy implements SortStrategy {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final CommentSortConfig config;
    
    private static final String BUCKET_KEY = "post:%d:bucket:%s:root_comments";
    private static final String HOT_KEY = "post:%d:hot:root_comments";
    private static final String CHILD_BUCKET_KEY = "post:%d:comment:%d:bucket:%s:children";
    private static final String CHILD_HOT_KEY = "post:%d:comment:%d:hot:children";
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    @Override
    public List<CommentSortData> getRootComments(Long postId, String sort, String cursor, int size) {
        List<CommentSortData> result = new ArrayList<>();
        int bucketSize = config.getBucketSize();
        
        // 1. 先查热点桶（优先，最多bucketSize条）
        String hotKey = String.format(HOT_KEY, postId);
        Set<Object> hotResults = redisTemplate.opsForZSet()
                .reverseRange(hotKey, 0, bucketSize - 1);
        
        if (hotResults != null) {
            for (Object obj : hotResults) {
                CommentSortData c = deserialize(obj);
                if (c != null && c.getStatus() == 1) {
                    result.add(c);
                }
            }
        }
        
        // 2. 如果热点桶数据不足，从时间桶补充
        if (result.size() < size) {
            int remaining = size - result.size();
            Set<Long> addedIds = result.stream()
                    .map(CommentSortData::getCommentId)
                    .collect(Collectors.toSet());
            
            // 从当天时间桶获取
            String today = LocalDate.now().format(DATE_FORMATTER);
            String bucketKey = String.format(BUCKET_KEY, postId, today);
            
            for (int i = 0; i < 7 && result.size() < size; i++) {
                String date = LocalDate.now().minusDays(i).format(DATE_FORMATTER);
                String key = String.format(BUCKET_KEY, postId, date);
                
                Set<Object> bucketResults = redisTemplate.opsForZSet()
                        .reverseRange(key, 0, remaining - 1);
                
                if (bucketResults != null) {
                    for (Object obj : bucketResults) {
                        CommentSortData c = deserialize(obj);
                        if (c != null && c.getStatus() == 1 && !addedIds.contains(c.getCommentId())) {
                            result.add(c);
                            addedIds.add(c.getCommentId());
                            
                            if (result.size() >= size) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    @Override
    public List<CommentSortData> getChildComments(Long postId, Long rootId, String sort, String cursor, int size) {
        List<CommentSortData> result = new ArrayList<>();
        int bucketSize = config.getBucketSize();
        
        // 1. 先查热点桶
        String hotKey = String.format(CHILD_HOT_KEY, postId, rootId);
        Set<Object> hotResults = redisTemplate.opsForZSet()
                .reverseRange(hotKey, 0, bucketSize - 1);
        
        if (hotResults != null) {
            for (Object obj : hotResults) {
                CommentSortData c = deserialize(obj);
                if (c != null && c.getStatus() == 1) {
                    result.add(c);
                }
            }
        }
        
        // 2. 从时间桶补充
        if (result.size() < size) {
            int remaining = size - result.size();
            Set<Long> addedIds = result.stream()
                    .map(CommentSortData::getCommentId)
                    .collect(Collectors.toSet());
            
            for (int i = 0; i < 7 && result.size() < size; i++) {
                String date = LocalDate.now().minusDays(i).format(DATE_FORMATTER);
                String key = String.format(CHILD_BUCKET_KEY, postId, rootId, date);
                
                Set<Object> bucketResults = redisTemplate.opsForZSet()
                        .reverseRange(key, 0, remaining - 1);
                
                if (bucketResults != null) {
                    for (Object obj : bucketResults) {
                        CommentSortData c = deserialize(obj);
                        if (c != null && c.getStatus() == 1 && !addedIds.contains(c.getCommentId())) {
                            result.add(c);
                            addedIds.add(c.getCommentId());
                            
                            if (result.size() >= size) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    @Override
    public void addComment(CommentSortData comment) {
        if (comment == null || comment.getCommentId() == null) {
            return;
        }
        
        double score = ScoreCalculator.calculateHotScore(comment);
        String date = LocalDate.now().format(DATE_FORMATTER);
        
        if (comment.getRootId() == null || comment.getRootId() == 0) {
            // 根��论 - 加入时间桶
            String bucketKey = String.format(BUCKET_KEY, comment.getPostId(), date);
            redisTemplate.opsForZSet().add(bucketKey, serialize(comment), score);
            
            // 同时加入热点桶
            if (ScoreCalculator.isHotComment(comment, config.getHotThreshold())) {
                String hotKey = String.format(HOT_KEY, comment.getPostId());
                redisTemplate.opsForZSet().add(hotKey, serialize(comment), score);
            }
        } else {
            // 子评论
            String bucketKey = String.format(CHILD_BUCKET_KEY, comment.getPostId(), comment.getRootId(), date);
            redisTemplate.opsForZSet().add(bucketKey, serialize(comment), score);
            
            if (ScoreCalculator.isHotComment(comment, config.getHotThreshold())) {
                String hotKey = String.format(CHILD_HOT_KEY, comment.getPostId(), comment.getRootId());
                redisTemplate.opsForZSet().add(hotKey, serialize(comment), score);
            }
        }
    }
    
    @Override
    public void updateScore(Long commentId, double score) {
        // 从MySQL查询评论信息，然后更新所有相关桶
    }
    
    @Override
    public void removeComment(Long commentId) {
        // 从所有相关桶中移除
    }
    
    @Override
    public long getCommentCount(Long postId, Long rootId) {
        // 统计所有桶的总数
        long total = 0;
        
        String date = LocalDate.now().format(DATE_FORMATTER);
        for (int i = 0; i < 30; i++) {
            String d = LocalDate.now().minusDays(i).format(DATE_FORMATTER);
            String key = rootId == null || rootId == 0 ?
                       String.format(BUCKET_KEY, postId, d) :
                       String.format(CHILD_BUCKET_KEY, postId, rootKey, d);
            
            Long count = redisTemplate.opsForZSet().zCard(key);
            total += count != null ? count : 0;
        }
        
        return total;
    }
    
    private String serialize(CommentSortData comment) {
        try {
            return new ObjectMapper().writeValueAsString(comment);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
    
    private CommentSortData deserialize(Object obj) {
        try {
            String json = obj.toString();
            return new ObjectMapper().readValue(json, CommentSortData.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
```

---

## Task 3: 创建评论排序服务接口和实现

**Files:**
- Create: `backend/src/main/java/com/quxiangshe/backend/service/ICommentSortService.java`
- Create: `backend/src/main/java/com/quxiangshe/backend/service/impl/CommentSortServiceImpl.java`

- [ ] **Step 1: 创建 ICommentSortService 接口**

```java
package com.quxiangshe.backend.service;

import com.quxiangshe.backend.entity.CommentSortData;

import java.util.List;

/**
 * 评论排序服务接口
 */
public interface ICommentSortService {
    
    /**
     * 获取根评论排序列表
     */
    List<CommentSortData> getRootComments(Long postId, String sort, String cursor, int size);
    
    /**
     * 获取子评论排序列表
     */
    List<CommentSortData> getChildComments(Long postId, Long rootId, String sort, String cursor, int size);
    
    /**
     * 发表评论
     */
    CommentSortData addComment(Long userId, Long postId, Long parentId, String content);
    
    /**
     * 点赞评论
     */
    void likeComment(Long commentId);
    
    /**
     * 取消点赞
     */
    void unlikeComment(Long commentId);
    
    /**
     * 删除评论（级联删除子评论）
     */
    void deleteComment(Long commentId, Long userId);
    
    /**
     * 初始化评论排序（从数据库迁移）
     */
    void initCommentSort(Long postId);
}
```

- [ ] **Step 2: 创建 CommentSortServiceImpl 实现**

```java
package com.quxiangshe.backend.service.impl;

import com.quxiangshe.backend.config.CommentSortConfig;
import com.quxiangshe.backend.entity.CommentSortData;
import com.quxiangshe.backend.entity.NoteComment;
import com.quxiangshe.backend.entity.User;
import com.quxiangshe.backend.mapper.NoteCommentMapper;
import com.quxiangshe.backend.mapper.UserMapper;
import com.quxiangshe.backend.service.ICommentSortService;
import com.quxiangshe.backend.service.sort.SortStrategy;
import com.quxiangshe.backend.service.sort.FullSortStrategy;
import com.quxiangshe.backend.service.sort.BucketSortStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 评论排序服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentSortServiceImpl implements ICommentSortService {
    
    private final NoteCommentMapper commentMapper;
    private final UserMapper userMapper;
    private final CommentSortConfig config;
    private final FullSortStrategy fullSortStrategy;
    private final BucketSortStrategy bucketSortStrategy;
    
    @Override
    public List<CommentSortData> getRootComments(Long postId, String sort, String cursor, int size) {
        SortStrategy strategy = getStrategy(postId, 0L);
        return strategy.getRootComments(postId, sort, cursor, size);
    }
    
    @Override
    public List<CommentSortData> getChildComments(Long postId, Long rootId, String sort, String cursor, int size) {
        SortStrategy strategy = getStrategy(postId, rootId);
        return strategy.getChildComments(postId, rootId, sort, cursor, size);
    }
    
    @Override
    public CommentSortData addComment(Long userId, Long postId, Long parentId, String content) {
        // 1. 查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 2. 确定rootId
        Long rootId = 0L;
        Long parentIdVal = parentId != null ? parentId : 0L;
        
        if (parentIdVal > 0) {
            NoteComment parentComment = commentMapper.selectById(parentIdVal);
            if (parentComment != null) {
                rootId = parentComment.getRootId() == 0 ? parentComment.getId() : parentComment.getRootId();
            }
        }
        
        // 3. 构建评论数据
        NoteComment comment = new NoteComment();
        comment.setNoteId(postId);
        comment.setUserId(userId);
        comment.setParentId(parentIdVal);
        comment.setRootId(rootId);
        comment.setContent(content);
        comment.setLikeCount(0);
        comment.setStatus(1);
        
        commentMapper.insert(comment);
        
        // 4. 构建排序数据
        CommentSortData sortData = CommentSortData.builder()
                .commentId(comment.getId())
                .postId(postId)
                .rootId(rootId)
                .userId(userId)
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .content(content)
                .likeCount(0)
                .replyCount(0)
                .createdAt(System.currentTimeMillis())
                .status(1)
                .build();
        
        // 5. 添加到排序索引
        SortStrategy strategy = getStrategy(postId, rootId);
        strategy.addComment(sortData);
        
        return sortData;
    }
    
    @Override
    public void likeComment(Long commentId) {
        NoteComment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            return;
        }
        
        // 更新数据库
        comment.setLikeCount(comment.getLikeCount() + 1);
        commentMapper.updateById(comment);
        
        // 更新排序索引
        updateSortIndex(comment);
    }
    
    @Override
    public void unlikeComment(Long commentId) {
        NoteComment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            return;
        }
        
        comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        commentMapper.updateById(comment);
        
        updateSortIndex(comment);
    }
    
    @Override
    public void deleteComment(Long commentId, Long userId) {
        NoteComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return;
        }
        
        // 验证权限
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("只能删除自己的评论");
        }
        
        // 软删除
        comment.setStatus(2);
        commentMapper.updateById(comment);
        
        // 如果是根评论，级联删除子评论
        if (comment.getParentId() == null || comment.getParentId() == 0) {
            // 查找所有子评论并软删除
        }
        
        // 从排序索引中移除
        SortStrategy strategy = getStrategy(comment.getNoteId(), comment.getRootId());
        strategy.removeComment(commentId);
    }
    
    @Override
    public void initCommentSort(Long postId) {
        // 从MySQL查询所有评论，初始化排序索引
        List<NoteComment> comments = commentMapper.selectByNoteId(postId);
        
        for (NoteComment comment : comments) {
            if (comment.getStatus() != 1) {
                continue;
            }
            
            User user = userMapper.selectById(comment.getUserId());
            
            CommentSortData sortData = CommentSortData.builder()
                    .commentId(comment.getId())
                    .postId(comment.getNoteId())
                    .rootId(comment.getRootId())
                    .userId(comment.getUserId())
                    .nickname(user != null ? user.getNickname() : null)
                    .avatar(user != null ? user.getAvatar() : null)
                    .content(comment.getContent())
                    .likeCount(comment.getLikeCount())
                    .replyCount(0)
                    .createdAt(comment.getCreatedAt() != null ? 
                           comment.getCreatedAt().atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 : 0)
                    .status(comment.getStatus())
                    .build();
            
            SortStrategy strategy = getStrategy(postId, comment.getRootId());
            strategy.addComment(sortData);
        }
        
        log.info("评论排序初始化完成: postId={}, count={}", postId, comments.size());
    }
    
    /**
     * 选择排序策略
     */
    private SortStrategy getStrategy(Long postId, Long rootId) {
        long count = fullSortStrategy.getCommentCount(postId, rootId);
        
        if (count > config.getThreshold()) {
            // 评论数量多，使用分桶策略
            return bucketSortStrategy;
        } else {
            // 评论数量少，使用全量策略
            return fullSortStrategy;
        }
    }
    
    private void updateSortIndex(NoteComment comment) {
        // 重新计算并更新排序索引中的分数
    }
}
```

---

## Task 4: 创建控制器

**Files:**
- Create: `backend/src/main/java/com/quxiangshe/backend/controller/CommentSortController.java`

- [ ] **Step 1: 创建 CommentSortController**

```java
package com.quxiangshe.backend.controller;

import com.quxiangshe.backend.R;
import com.quxiangshe.backend.entity.CommentSortData;
import com.quxiangshe.backend.service.ICommentSortService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评论排序API控制器
 */
@RestController
@RequestMapping("/api/comment/sorted")
@RequiredArgsConstructor
public class CommentSortController {
    
    private final ICommentSortService commentSortService;
    
    /**
     * 获取根评论排序列表
     */
    @GetMapping("/{postId}/roots")
    public R<Map<String, Object>> getRootComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "hottest") String sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        
        List<CommentSortData> comments = commentSortService.getRootComments(postId, sort, cursor, size);
        
        Map<String, Object> result = new HashMap<>();
        result.put("data", comments);
        result.put("hasMore", comments.size() >= size);
        result.put("nextCursor", comments.isEmpty() ? null : 
                  String.valueOf(comments.get(comments.size() - 1).getCreatedAt()));
        
        return R.ok(result);
    }
    
    /**
     * 获取子评论排序列表
     */
    @GetMapping("/{postId}/children/{rootId}")
    public R<Map<String, Object>> getChildComments(
            @PathVariable Long postId,
            @PathVariable Long rootId,
            @RequestParam(defaultValue = "hottest") String sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        
        List<CommentSortData> comments = commentSortService.getChildComments(postId, rootId, sort, cursor, size);
        
        Map<String, Object> result = new HashMap<>();
        result.put("data", comments);
        result.put("hasMore", comments.size() >= size);
        result.put("nextCursor", comments.isEmpty() ? null : 
                  String.valueOf(comments.get(comments.size() - 1).getCreatedAt()));
        
        return R.ok(result);
    }
    
    /**
     * 点赞评论
     */
    @PostMapping("/{commentId}/like")
    public R<Void> likeComment(@PathVariable Long commentId) {
        commentSortService.likeComment(commentId);
        return R.ok(null);
    }
    
    /**
     * 取消点赞
     */
    @DeleteMapping("/{commentId}/like")
    public R<Void> unlikeComment(@PathVariable Long commentId) {
        commentSortService.unlikeComment(commentId);
        return R.ok(null);
    }
}
```

---

## Task 5: 添加配置和注入

**Files:**
- Modify: `backend/src/main/java/com/quxiangshe/backend/BackendApplication.java`

- [ ] **Step 1: 确认服务能被扫描到**

确保 CommentSortConfig、CommentSortServiceImpl 都在 component scan 路径下。

---

## Task 6: 测试验证

**Files:**
- Test: 使用 Postman 或 curl 测试API

- [ ] **Step 1: 测试获取根评论**

```bash
curl -X GET "http://localhost:8080/api/comment/sorted/400858/roots?sort=hottest&size=20"
```

- [ ] **Step 2: 测试点赞**

```bash
curl -X POST "http://localhost:8080/api/comment/sorted/1/like"
```

- [ ] **Step 3: 测试初始化排序**

调用 initCommentSort 方法从数据库迁移评论数据到 Redis。

---
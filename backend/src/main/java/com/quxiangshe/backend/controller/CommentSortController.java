package com.quxiangshe.backend.controller;

import com.quxiangshe.backend.common.R;
import com.quxiangshe.backend.entity.CommentSortData;
import com.quxiangshe.backend.service.ICommentSortService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "评论排序", description = "评论排序相关接口")
@RestController
@RequestMapping("/comment/sorted")
@RequiredArgsConstructor
public class CommentSortController {
    
    private final ICommentSortService commentSortService;
    private final StringRedisTemplate redisTemplate;
    
    private Long getCurrentUserId(HttpServletRequest request) {
        String userIdStr = request.getAttribute("userId") != null ? 
            request.getAttribute("userId").toString() : null;
        return userIdStr != null ? Long.parseLong(userIdStr) : null;
    }
    
    @Operation(summary = "获取根评论列表")
    @GetMapping("/{postId}/roots")
    public R<Map<String, Object>> getRootComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "hottest") String sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 50);
        List<CommentSortData> list = commentSortService.getRootComments(postId, sort, cursor, size);
        
        Map<String, Object> result = new HashMap<>();
        result.put("data", list);
        result.put("hasMore", list.size() == size);
        
        if (!list.isEmpty() && list.size() == size) {
            CommentSortData last = list.get(list.size() - 1);
            String nextCursor = last.getCreatedAt() + "_" + last.getCommentId();
            result.put("nextCursor", nextCursor);
        }
        
        return R.ok(result);
    }
    
    @Operation(summary = "获取子评论列表")
    @GetMapping("/{postId}/children/{rootId}")
    public R<Map<String, Object>> getChildComments(
            @PathVariable Long postId,
            @PathVariable Long rootId,
            @RequestParam(defaultValue = "hottest") String sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 50);
        List<CommentSortData> list = commentSortService.getChildComments(postId, rootId, sort, cursor, size);
        
        Map<String, Object> result = new HashMap<>();
        result.put("data", list);
        result.put("hasMore", list.size() == size);
        
        if (!list.isEmpty() && list.size() == size) {
            CommentSortData last = list.get(list.size() - 1);
            String nextCursor = last.getCreatedAt() + "_" + last.getCommentId();
            result.put("nextCursor", nextCursor);
        }
        
        return R.ok(result);
    }
    
    @Operation(summary = "点赞评论")
    @PostMapping("/{commentId}/like")
    public R<?> likeComment(
            @PathVariable Long commentId,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return R.fail(401, "请先登录");
        }

        try {
            int likeCount = commentSortService.likeComment(commentId, userId);
            return R.ok(Map.of("liked", true, "likeCount", likeCount));
        } catch (Exception e) {
            return R.fail(400, e.getMessage());
        }
    }

    @Operation(summary = "取消点赞")
    @DeleteMapping("/{commentId}/like")
    public R<?> unlikeComment(
            @PathVariable Long commentId,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return R.fail(401, "请先登录");
        }

        try {
            int likeCount = commentSortService.unlikeComment(commentId, userId);
            return R.ok(Map.of("liked", false, "likeCount", likeCount));
        } catch (Exception e) {
            return R.fail(400, e.getMessage());
        }
    }
    
    @Operation(summary = "初始化评论排序数据")
    @PostMapping("/init/{postId}")
    public R<?> initCommentSort(@PathVariable Long postId) {
        try {
            commentSortService.initCommentSort(postId);
            return R.ok("初始化成功");
        } catch (Exception e) {
            return R.fail(400, e.getMessage());
        }
    }
    
    @Operation(summary = "查询评论状态")
    @GetMapping("/status/{postId}")
    public R<Map<String, Object>> getStatus(@PathVariable Long postId) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取Redis中的评论数
        String countStr = redisTemplate.opsForValue().get("post:" + postId + ":comment_count");
        long commentCount = countStr != null ? Long.parseLong(countStr) : 0;
        result.put("commentCount", commentCount);
        
        // 检查comment_tree是否存在
        Boolean treeExists = redisTemplate.hasKey("post:" + postId + ":comment_tree");
        result.put("commentTreeExists", treeExists != null && treeExists);
        
        return R.ok(result);
    }
}
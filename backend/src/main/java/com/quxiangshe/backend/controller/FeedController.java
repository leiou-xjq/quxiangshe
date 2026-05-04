package com.quxiangshe.backend.controller;

import com.quxiangshe.backend.common.R;
import com.quxiangshe.backend.service.IActivityService;
import com.quxiangshe.backend.service.IFeedService;
import com.quxiangshe.backend.vo.NoteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Feed流控制器
 * 提供个性化信息流推荐接口，使用游标分页
 * 
 * @author 趣享社技术团队
 */
@Tag(name = "Feed流", description = "个性化信息流推荐接口")
@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
public class FeedController {
    
    private final IFeedService feedService;
    private final IActivityService activityService;
    
    /**
     * 获取用户Feed流
     * 使用游标分页，避免Offset性能问题
     */
    @Operation(summary = "获取Feed流")
    @GetMapping
    public R<Map<String, Object>> getFeed(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return R.fail(401, "请先登录");
        }
        
        // 限制每页数量
        size = Math.min(size, 50);
        
        List<NoteVO> notes = feedService.getFeed(userId, cursor, size);
        
        // 构建响应
        Map<String, Object> result = new HashMap<>();
        result.put("data", notes);
        result.put("hasMore", notes.size() == size);
        
        // 生成下一页游标
        if (!notes.isEmpty() && notes.size() == size) {
            NoteVO lastNote = notes.get(notes.size() - 1);
            if (lastNote.getCreatedAt() != null) {
                String nextCursor = lastNote.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli() + "_" + lastNote.getId();
                result.put("nextCursor", nextCursor);
            }
        }
        
        return R.ok(result);
    }
    
    /**
     * 初始化指定作者的Redis粉丝活跃度排名数据
     */
    @Operation(summary = "初始化Redis粉丝活跃度排名")
    @PostMapping("/init-fans-activity/{authorId}")
    public R<String> initFansActivity(@PathVariable Long authorId) {
        activityService.updateFansActivityRank(authorId);
        return R.ok("Redis粉丝活跃度排名初始化完成");
    }
    
    /**
     * 查询关注Tab是否有更新（红点提示）
     */
    @Operation(summary = "查询关注Tab是否有更新")
    @GetMapping("/follow-updated")
    public R<Map<String, Object>> getFollowUpdated(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return R.fail(401, "请先登录");
        }
        
        boolean hasUpdate = feedService.hasFollowUpdate(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("hasUpdate", hasUpdate);
        
        return R.ok(result);
    }
    
    /**
     * 清除关注Tab更新标记（用户点击关注Tab时调用）
     */
    @Operation(summary = "清除关注Tab更新标记")
    @PostMapping("/follow-updated/clear")
    public R<String> clearFollowUpdated(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return R.fail(401, "请先登录");
        }
        
        feedService.clearFollowUpdate(userId);
        
        return R.ok("已清除");
    }
    
    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        return userId != null ? (Long) userId : null;
    }
}
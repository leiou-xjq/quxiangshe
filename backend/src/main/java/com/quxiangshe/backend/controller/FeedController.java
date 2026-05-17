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
 * <p>提供个性化信息流推荐接口，使用游标分页代替传统Offset分页，
 * 避免深度翻页的性能退化问题。Feed源包括：
 * <ul>
 *   <li>关注博主的笔记（按发布时间倒序）</li>
 *   <li>系统推荐的笔记（基于用户兴趣标签）</li>
 * </ul>
 * 支持关注Tab红点提示（基于Redis标记位）。</p>
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
     * 获取用户个性化Feed流
     * <p>使用游标分页，游标格式为时间戳_笔记ID，保证数据一致性和分页连续性。</p>
     * 
     * @param cursor  游标（上一页最后一条笔记的createdAtMillis_id）
     * @param size    每页数量（最大50）
     * @param request HTTP请求
     * @return Feed流数据及分页信息
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
        
        // 限制每页最大数量，防止恶意拉取
        size = Math.min(size, 50);
        
        List<NoteVO> notes = feedService.getFeed(userId, cursor, size);
        
        // 构建响应
        Map<String, Object> result = new HashMap<>();
        result.put("data", notes);
        result.put("hasMore", notes.size() == size);
        
        // 游标格式：时间戳_笔记ID，确保分页去重和时序一致性
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
     * 初始化Redis粉丝活跃度排名
     * 计算指定作者的所有粉丝的互动活跃度分数，写入Redis ZSET用于智能分发排序。
     * 
     * @param authorId 作者用户ID
     * @return 执行结果
     */
    @Operation(summary = "初始化Redis粉丝活跃度排名")
    @PostMapping("/init-fans-activity/{authorId}")
    public R<String> initFansActivity(@PathVariable Long authorId) {
        activityService.updateFansActivityRank(authorId);
        return R.ok("Redis粉丝活跃度排名初始化完成");
    }
    
    /**
     * 查询关注Tab是否有新内容更新
     * 根据Redis标记位判断，用于前端展示红点提示。
     * 
     * @param request HTTP请求
     * @return hasUpdate字段标识是否有新内容
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
     * 清除关注Tab更新标记
     * 用户点击关注Tab时调用，清除Redis中的更新标志位。
     * 
     * @param request HTTP请求
     * @return 清除结果
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
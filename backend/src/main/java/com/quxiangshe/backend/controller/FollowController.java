package com.quxiangshe.backend.controller;

import com.quxiangshe.backend.common.R;
import com.quxiangshe.backend.service.IFollowService;
import com.quxiangshe.backend.vo.PageVO;
import com.quxiangshe.backend.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 关注控制器
 * 提供关注/取消关注、关注列表、粉丝列表等接口
 * 
 * @author 趣享社技术团队
 */
@Tag(name = "关注管理", description = "用户关注相关接口")
@Slf4j
@RestController
@RequestMapping("/follow")
@RequiredArgsConstructor
public class FollowController {
    
    private final IFollowService followService;
    
    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        return userId != null ? (Long) userId : null;
    }
    
    /**
     * 关注用户
     */
    @Operation(summary = "关注用户")
    @PostMapping("/{userId}")
    public R<Void> follow(
            @PathVariable Long userId,
            HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return R.fail(401, "请先登录");
        }
        
        followService.follow(currentUserId, userId);
        return R.ok("关注成功", null);
    }
    
    /**
     * 取消关注
     */
    @Operation(summary = "取消关注")
    @DeleteMapping("/{userId}")
    public R<Void> unfollow(
            @PathVariable Long userId,
            HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return R.fail(401, "请先登录");
        }
        
        followService.unfollow(currentUserId, userId);
        return R.ok("取消关注成功", null);
    }
    
    /**
     * 获取关注列表
     */
    @Operation(summary = "获取关注列表")
    @GetMapping("/following")
    public R<PageVO<UserVO>> getFollowingList(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long targetUserId = userId;
        if (targetUserId == null) {
            targetUserId = getCurrentUserId(request);
        }
        
        if (targetUserId == null) {
            return R.fail(401, "请先登录");
        }
        
        Long currentUserId = getCurrentUserId(request);
        
        PageVO<UserVO> list = followService.getFollowingList(targetUserId, cursor, size, currentUserId);
        return R.ok(list);
    }
    
    /**
     * 获取粉丝列表
     */
    @Operation(summary = "获取粉丝列表")
    @GetMapping("/followers")
    public R<PageVO<UserVO>> getFollowersList(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long targetUserId = userId;
        if (targetUserId == null) {
            targetUserId = getCurrentUserId(request);
        }
        
        if (targetUserId == null) {
            return R.fail(401, "请先登录");
        }
        
        Long currentUserId = getCurrentUserId(request);
        log.info("getFollowersList - targetUserId: {}, currentUserId: {}", targetUserId, currentUserId);
        
        PageVO<UserVO> list = followService.getFollowersList(targetUserId, cursor, size, currentUserId);
        return R.ok(list);
    }
    
    /**
     * 获取关注状态
     */
    @Operation(summary = "获取关注状态")
    @GetMapping("/status/{userId}")
    public R<Boolean> getFollowStatus(
            @PathVariable Long userId,
            HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return R.ok(false);
        }
        
        boolean isFollowing = followService.isFollowing(currentUserId, userId);
        return R.ok(isFollowing);
    }
    
    /**
     * 获取用户关注数
     */
    @Operation(summary = "获取关注数")
    @GetMapping("/count/following/{userId}")
    public R<Long> getFollowingCount(@PathVariable Long userId) {
        long count = followService.getFollowingCount(userId);
        return R.ok(count);
    }
    
    /**
     * 获取用户粉丝数
     */
    @Operation(summary = "获取粉丝数")
    @GetMapping("/count/followers/{userId}")
    public R<Long> getFollowersCount(@PathVariable Long userId) {
        long count = followService.getFollowersCount(userId);
        return R.ok(count);
    }
}
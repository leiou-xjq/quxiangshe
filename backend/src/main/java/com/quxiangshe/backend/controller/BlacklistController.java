package com.quxiangshe.backend.controller;

import com.quxiangshe.backend.common.R;
import com.quxiangshe.backend.service.IBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 黑名单控制器
 * 
 * @author 趣享社技术团队
 */
@Tag(name = "黑名单管理", description = "用户拉黑相关接口")
@RestController
@RequestMapping("/blacklist")
@RequiredArgsConstructor
public class BlacklistController {
    
    private final IBlacklistService blacklistService;
    
    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        return userId != null ? (Long) userId : null;
    }
    
    /**
     * 拉黑用户
     */
    @Operation(summary = "拉黑用户")
    @PostMapping("/{userId}")
    public R<Void> blockUser(
            @PathVariable Long userId,
            HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return R.fail(401, "请先登录");
        }
        
        if (currentUserId.equals(userId)) {
            return R.fail(400, "不能拉黑自己");
        }
        
        boolean success = blacklistService.blockUser(currentUserId, userId);
        return success ? R.ok("拉黑成功", null) : R.fail("拉黑失败");
    }
    
    /**
     * 取消拉黑
     */
    @Operation(summary = "取消拉黑")
    @DeleteMapping("/{userId}")
    public R<Void> unblockUser(
            @PathVariable Long userId,
            HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return R.fail(401, "请先登录");
        }
        
        boolean success = blacklistService.unblockUser(currentUserId, userId);
        return success ? R.ok("取消拉黑成功", null) : R.fail("取消拉黑失败");
    }
    
    /**
     * 检查是否已拉黑
     */
    @Operation(summary = "检查是否已拉黑")
    @GetMapping("/check/{userId}")
    public R<Boolean> checkBlocked(
            @PathVariable Long userId,
            HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return R.ok(false);
        }
        
        boolean blocked = blacklistService.isBlocked(currentUserId, userId);
        return R.ok(blocked);
    }
}
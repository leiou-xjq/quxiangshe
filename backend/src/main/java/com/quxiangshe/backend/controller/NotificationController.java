package com.quxiangshe.backend.controller;

import com.quxiangshe.backend.common.R;
import com.quxiangshe.backend.entity.Notification;
import com.quxiangshe.backend.service.INotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通知控制器
 * <p>提供通知列表（offset分页）、未读计数、标记已读（单个/全部）、删除通知等接口。
 * 通知类型包括：点赞、评论、关注、系统消息等。</p>
 * 
 * @author 趣享社技术团队
 */
@Tag(name = "通知管理", description = "通知相关接口")
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {
    
    private final INotificationService notificationService;
    
    /**
     * 获取当前用户的通知列表（offset分页）
     * 
     * @param size    每页数量（默认20）
     * @param offset  偏移量（默认0）
     * @param request HTTP请求
     * @return 通知列表
     */
    @Operation(summary = "获取通知列表")
    @GetMapping("/list")
    public R<List<Notification>> getNotifications(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int offset,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<Notification> notifications = notificationService.getNotifications(userId, size, offset);
        return R.ok(notifications);
    }
    
    /**
     * 获取当前用户未读通知数量
     * 
     * @param request HTTP请求
     * @return 未读通知数
     */
    @Operation(summary = "获取未读数量")
    @GetMapping("/unread-count")
    public R<Integer> getUnreadCount(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Integer count = notificationService.getUnreadCount(userId);
        return R.ok(count);
    }
    
    /**
     * 标记单条通知为已读
     * 
     * @param id      通知ID
     * @param request HTTP请求
     * @return 操作结果
     */
    @Operation(summary = "标记已读")
    @PutMapping("/read/{id}")
    public R<String> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        notificationService.markAsRead(userId, id);
        return R.ok("标记成功", null);
    }
    
    /**
     * 将当前用户所有通知标记为已读
     * 
     * @param request HTTP请求
     * @return 操作结果
     */
    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public R<String> markAllAsRead(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        notificationService.markAllAsRead(userId);
        return R.ok("标记成功", null);
    }
    
    /**
     * 删除指定通知（仅允许删除自己收到的通知）
     * 
     * @param id      通知ID
     * @param request HTTP请求
     * @return 操作结果
     */
    @Operation(summary = "删除通知")
    @DeleteMapping("/{id}")
    public R<String> deleteNotification(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        notificationService.deleteNotification(id, userId);
        return R.ok("删除成功", null);
    }
    
    private Long getCurrentUserId(HttpServletRequest request) {
        String userIdStr = request.getAttribute("userId") != null ? 
            request.getAttribute("userId").toString() : null;
        return userIdStr != null ? Long.parseLong(userIdStr) : null;
    }
}

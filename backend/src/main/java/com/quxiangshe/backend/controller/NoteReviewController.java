package com.quxiangshe.backend.controller;

import com.quxiangshe.backend.common.R;
import com.quxiangshe.backend.entity.NoteReview;
import com.quxiangshe.backend.entity.User;
import com.quxiangshe.backend.service.impl.NoteReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 笔记审核管理接口
 * 提供审核统计、待审核列表、人工审核等功能
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@RestController
@RequestMapping("/review")
@Tag(name = "笔记审核管理", description = "内容审核相关接口")
public class NoteReviewController {
    
    private final NoteReviewService noteReviewService;
    
    public NoteReviewController(NoteReviewService noteReviewService) {
        this.noteReviewService = noteReviewService;
    }
    
    private boolean hasReviewPermission(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        return User.ROLE_ADMIN.equals(role) || User.ROLE_MODERATOR.equals(role);
    }
    
    @Operation(summary = "获取审核统计")
    @GetMapping("/statistics")
    public R<Map<String, Object>> getStatistics(HttpServletRequest request) {
        if (!hasReviewPermission(request)) {
            return R.fail("无权限访问");
        }
        try {
            Map<String, Object> stats = noteReviewService.getStatistics();
            return R.ok(stats);
        } catch (Exception e) {
            log.error("获取审核统计失败: {}", e.getMessage());
            return R.fail("获取统计失败");
        }
    }
    
    @Operation(summary = "获取待审核列表")
    @GetMapping("/pending")
    public R<List<NoteReview>> getPendingReviews(
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        if (!hasReviewPermission(request)) {
            return R.fail("无权限访问");
        }
        try {
            List<NoteReview> reviews = noteReviewService.getPendingReviews(limit);
            return R.ok(reviews);
        } catch (Exception e) {
            log.error("获取待审核列表失败: {}", e.getMessage());
            return R.fail("获取列表失败");
        }
    }
    
    @Operation(summary = "人工审核通过")
    @PostMapping("/approve/{reviewId}")
    public R<String> approveReview(@PathVariable Long reviewId, HttpServletRequest request) {
        if (!hasReviewPermission(request)) {
            return R.fail("无权限访问");
        }
        try {
            noteReviewService.approveReview(reviewId);
            return R.ok("审核通过");
        } catch (Exception e) {
            log.error("审核通过失败: {}", e.getMessage());
            return R.fail("操作失败");
        }
    }
    
    @Operation(summary = "人工审核拒绝")
    @PostMapping("/reject/{reviewId}")
    public R<String> rejectReview(
            @PathVariable Long reviewId,
            @RequestParam String reason,
            @RequestParam(required = false) String tags,
            HttpServletRequest request) {
        if (!hasReviewPermission(request)) {
            return R.fail("无权限访问");
        }
        try {
            List<String> tagList = null;
            if (tags != null && !tags.isEmpty()) {
                tagList = Arrays.asList(tags.split(","));
            }
            noteReviewService.rejectReview(reviewId, reason, tagList);
            return R.ok("已拒绝");
        } catch (Exception e) {
            log.error("审核拒绝失败: {}", e.getMessage());
            return R.fail("操作失败");
        }
    }
}
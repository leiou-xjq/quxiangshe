package com.quxiangshe.backend.controller;

import com.quxiangshe.backend.common.R;
import com.quxiangshe.backend.service.IReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 举报控制器
 * 
 * @author 趣享社技术团队
 */
@Tag(name = "举报管理", description = "内容举报相关接口")
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {
    
    private final IReportService reportService;
    
    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        return userId != null ? (Long) userId : null;
    }
    
    /**
     * 提交举报
     */
    @Operation(summary = "提交举报")
    @PostMapping
    public R<Void> submitReport(
            @RequestParam Integer targetType,
            @RequestParam Long targetId,
            @RequestParam Integer reason,
            @RequestParam(required = false) String description,
            HttpServletRequest request) {
        
        Long reporterId = getCurrentUserId(request);
        if (reporterId == null) {
            return R.fail(401, "请先登录");
        }
        
        // 参数校验
        if (targetType < 1 || targetType > 3) {
            return R.fail(400, "无效的目标类型");
        }
        if (reason < 1 || reason > 4) {
            return R.fail(400, "无效的举报原因");
        }
        
        boolean success = reportService.submitReport(reporterId, targetType, targetId, reason, description);
        return success ? R.ok("举报成功", null) : R.fail("举报失败");
    }
}
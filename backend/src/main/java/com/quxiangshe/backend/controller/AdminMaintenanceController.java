package com.quxiangshe.backend.controller;

import com.quxiangshe.backend.common.R;
import com.quxiangshe.backend.scheduler.ReconciliationScheduler;
import com.quxiangshe.backend.scheduler.RedisKeyCleanupScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台 - 系统维护接口
 * 用于手动触发清理、对账等维护任务
 */
@Tag(name = "系统维护", description = "手动触发清理、对账等维护任务")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminMaintenanceController {

    private final ReconciliationScheduler reconciliationScheduler;
    private final RedisKeyCleanupScheduler redisKeyCleanupScheduler;

    @Operation(summary = "手动触发计数对账")
    @PostMapping("/reconcile/counts")
    public R<String> triggerReconciliation() {
        reconciliationScheduler.reconcileNoteCounts();
        return R.ok("对账任务已触发");
    }

    @Operation(summary = "清理粉丝数缓存旧数据")
    @PostMapping("/cleanup/follower-cache")
    public R<String> cleanupFollowerCache() {
        int count = redisKeyCleanupScheduler.cleanupFollowerCache();
        return R.ok("已清理 " + count + " 个旧缓存");
    }

    @Operation(summary = "重建粉丝数缓存")
    @PostMapping("/rebuild/follower-cache")
    public R<String> rebuildFollowerCache() {
        redisKeyCleanupScheduler.rebuildFollowerCache();
        return R.ok("缓存重建完成");
    }

    @Operation(summary = "执行全部清理任务")
    @PostMapping("/cleanup/all")
    public R<String> runAllCleanup() {
        redisKeyCleanupScheduler.cleanupOldKeys();
        return R.ok("清理任务执行完成");
    }
}
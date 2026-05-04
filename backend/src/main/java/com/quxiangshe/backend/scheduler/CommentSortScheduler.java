package com.quxiangshe.backend.scheduler;

import com.quxiangshe.backend.service.ICommentSortService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 评论排序定时任务
 * 每日凌晨全量校验Redis数据与MySQL一致
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentSortScheduler {
    
    private final ICommentSortService commentSortService;
    
    /**
     * 每日凌晨2点全量校验
     * 重新从MySQL同步所有笔记的评论数据到Redis
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void verifyCommentSortData() {
        log.info("开始全量校验评论排序数据...");
        long startTime = System.currentTimeMillis();
        
        try {
            commentSortService.verifyAllCommentSort();
            long costTime = System.currentTimeMillis() - startTime;
            log.info("全量校验评论排序数据完成，耗时: {}ms", costTime);
        } catch (Exception e) {
            log.error("全量校验评论排序数据失败: {}", e.getMessage(), e);
        }
    }
}
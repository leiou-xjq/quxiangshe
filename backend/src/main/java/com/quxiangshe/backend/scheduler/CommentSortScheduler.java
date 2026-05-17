package com.quxiangshe.backend.scheduler;

import com.quxiangshe.backend.service.ICommentSortService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 评论排序定时任务
 * <p>每日凌晨执行全量校验，确保Redis中的评论排序数据与MySQL数据库保持一致。
 * 当Redis缓存因异常丢失或过期时，此任务将重建全部排序数据。
 *
 * @author 趣享社技术团队
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentSortScheduler {
    
    private final ICommentSortService commentSortService;
    
    /**
     * 每日凌晨2点全量校验
     * <p>重新从MySQL同步所有笔记的评论排序数据到Redis，
     * 保障缓存数据的完整性和准确性
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
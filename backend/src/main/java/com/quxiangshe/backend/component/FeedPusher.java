package com.quxiangshe.backend.component;

import com.quxiangshe.backend.service.IFeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Feed推送组件
 * 专门用于触发Feed推送，解决循环依赖问题
 * 支持异步推送，分批推送给活跃粉丝
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedPusher {
    
    private static final int MAX_PUSH_BATCHES = 5;           // 分5批推送
    private static final long BATCH_INTERVAL_MS = 1000;    // 每批间隔1秒
    private static final int MAX_RETRY_TIMES = 3;           // 重试3次
    
    private final ApplicationContext applicationContext;
    
    /**
     * 推送笔记到粉丝的Feed
     * 自动检测粉丝数，选择同步或异步分批推送
     * @param noteId 笔记ID
     * @param authorId 作者ID
     */
    public void pushNote(Long noteId, Long authorId) {
        pushNoteAsync(noteId, authorId);
    }
    
    /**
     * 异步推送笔记到粉丝的Feed
     * @param noteId 笔记ID
     * @param authorId 作者ID
     */
    @Async("pushExecutor")
    public CompletableFuture<Boolean> pushNoteAsync(Long noteId, Long authorId) {
        log.info("开始异步推送笔记: noteId={}, authorId={}", noteId, authorId);
        long startTime = System.currentTimeMillis();
        
        try {
            IFeedService feedService = applicationContext.getBean(IFeedService.class);
            
            // 获取博主粉丝数
            long followerCount = feedService.getFollowerCount(authorId);
            
            // 根据粉丝数决定推送策略
            if (followerCount > 100000) {
                // 推拉结合模式: 一次性处理，Redis获取粉丝，Pipeline批量写入
                feedService.pushNoteToFeed(noteId, authorId);
            } else {
                // 推模式/拉模式: 直接同步执行
                feedService.pushNoteToFeed(noteId, authorId);
            }
            
            long costTime = System.currentTimeMillis() - startTime;
            log.info("异步推送笔记完成: noteId={}, authorId={}, cost={}ms", 
                noteId, authorId, costTime);
            
            // 清除作者相关缓存（粉丝数、分类数据）
            feedService.evictAllCachesByAuthor(authorId);
            
            return CompletableFuture.completedFuture(true);
            
        } catch (Exception e) {
            log.error("异步推送笔记失败: noteId={}, authorId={}, error={}", 
                noteId, authorId, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 分批推送（推拉结合模式）
     * @param noteId 笔记ID
     * @param authorId 作者ID
     * @param feedService Feed服务
     */
    private void pushInBatches(Long noteId, Long authorId, IFeedService feedService) {
        log.info("开始分批推送: noteId={}, authorId={}, 总批数={}", 
            noteId, authorId, MAX_PUSH_BATCHES);
        
        for (int batch = 0; batch < MAX_PUSH_BATCHES; batch++) {
            log.info("推拉结合-第{}/{}批开始: noteId={}", batch + 1, MAX_PUSH_BATCHES, noteId);
            long batchStartTime = System.currentTimeMillis();
            
            boolean batchSuccess = false;
            
            // 重试机制
            for (int retry = 0; retry < MAX_RETRY_TIMES; retry++) {
                try {
                    feedService.pushNoteInBatch(noteId, authorId, batch, MAX_PUSH_BATCHES);
                    batchSuccess = true;
                    break;
                } catch (Exception e) {
                    log.warn("推拉结合-第{}/{}批-重试{}/{}失败: noteId={}, error={}", 
                        batch + 1, MAX_PUSH_BATCHES, retry + 1, MAX_RETRY_TIMES, noteId, e.getMessage());
                    
                    if (retry < MAX_RETRY_TIMES - 1) {
                        try {
                            Thread.sleep(500); // 重试间隔500ms
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
            
            long batchCostTime = System.currentTimeMillis() - batchStartTime;
            
            if (batchSuccess) {
                log.info("推拉结合-第{}/{}批完成: noteId={}, cost={}ms", 
                    batch + 1, MAX_PUSH_BATCHES, noteId, batchCostTime);
            } else {
                log.error("推拉结合-第{}/{}批最终失败: noteId={}, cost={}ms", 
                    batch + 1, MAX_PUSH_BATCHES, noteId, batchCostTime);
            }
            
            // 每批之间间隔1秒（最后一批不需要间隔）
            if (batch < MAX_PUSH_BATCHES - 1) {
                try {
                    Thread.sleep(BATCH_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.info("分批推送全部完成: noteId={}, authorId={}", noteId, authorId);
    }
}
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
 * 
 * <p>专门用于触发Feed（信息流）推送，解决Spring Bean循环依赖问题（从Service层抽离）。
 * 支持异步推送，根据粉丝数量自动选择推送策略：
 * 大V用户（粉丝>10万）采用推拉结合模式分批推送，
 * 普通用户直接全量推送。</p>
 * 
 * @author 趣享社技术团队
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedPusher {
    
    /** 分批推送的总批次数 */
    private static final int MAX_PUSH_BATCHES = 5;
    /** 每批之间的间隔时间（毫秒），避免瞬时流量过大 */
    private static final long BATCH_INTERVAL_MS = 1000;
    /** 单批推送的最大重试次数 */
    private static final int MAX_RETRY_TIMES = 3;
    
    /** 通过ApplicationContext延迟获取Bean，解决循环依赖 */
    private final ApplicationContext applicationContext;
    
    /**
     * 推送笔记到粉丝的Feed
     * 
     * <p>业务入口，默认走异步推送路径。粉丝数由FeedService内部判断，
     * 大V自动切换为分批推送模式。</p>
     *
     * @param noteId   笔记ID
     * @param authorId 作者（博主）ID
     */
    public void pushNote(Long noteId, Long authorId) {
        pushNoteAsync(noteId, authorId);
    }
    
    /**
     * 异步推送笔记到粉丝的Feed
     * 
     * <p>在线程池"pushExecutor"中执行，不阻塞主线程。
     * 推送完成后清除作者相关缓存（粉丝数等），确保下次查询拿到最新数据。</p>
     *
     * @param noteId   笔记ID
     * @param authorId 作者（博主）ID
     * @return true-推送成功，false-推送失败
     */
    @Async("pushExecutor")
    public CompletableFuture<Boolean> pushNoteAsync(Long noteId, Long authorId) {
        log.info("开始异步推送笔记: noteId={}, authorId={}", noteId, authorId);
        long startTime = System.currentTimeMillis();
        
        try {
            // 通过ApplicationContext获取Bean，避免构造函数循环依赖
            IFeedService feedService = applicationContext.getBean(IFeedService.class);
            
            // 获取博主粉丝数，用于选择推送策略
            long followerCount = feedService.getFollowerCount(authorId);
            
            // 根据粉丝数决定推送策略
            if (followerCount > 100000) {
                // 大V用户：推拉结合模式，Redis获取粉丝，Pipeline批量写入
                feedService.pushNoteToFeed(noteId, authorId);
            } else {
                // 普通用户：直接全量推送
                feedService.pushNoteToFeed(noteId, authorId);
            }
            
            long costTime = System.currentTimeMillis() - startTime;
            log.info("异步推送笔记完成: noteId={}, authorId={}, cost={}ms", 
                noteId, authorId, costTime);
            
            // 清除作者相关缓存（粉丝数、分类数据），确保下次查询数据一致
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
     * 
     * <p>将粉丝总数分成N批依次推送，每批间隔1秒，单批失败自动重试3次。
     * 适用于百万级粉丝的大V场景，避免一次性加载过多粉丝数据导致OOM。</p>
     *
     * @param noteId      笔记ID
     * @param authorId    作者ID
     * @param feedService  Feed服务接口
     */
    private void pushInBatches(Long noteId, Long authorId, IFeedService feedService) {
        log.info("开始分批推送: noteId={}, authorId={}, 总批数={}", 
            noteId, authorId, MAX_PUSH_BATCHES);
        
        for (int batch = 0; batch < MAX_PUSH_BATCHES; batch++) {
            log.info("推拉结合-第{}/{}批开始: noteId={}", batch + 1, MAX_PUSH_BATCHES, noteId);
            long batchStartTime = System.currentTimeMillis();
            
            boolean batchSuccess = false;
            
            // 单批失败重试机制，最多重试MAX_RETRY_TIMES次
            for (int retry = 0; retry < MAX_RETRY_TIMES; retry++) {
                try {
                    feedService.pushNoteInBatch(noteId, authorId, batch, MAX_PUSH_BATCHES);
                    batchSuccess = true;
                    break; // 成功后跳出重试循环
                } catch (Exception e) {
                    log.warn("推拉结合-第{}/{}批-重试{}/{}失败: noteId={}, error={}", 
                        batch + 1, MAX_PUSH_BATCHES, retry + 1, MAX_RETRY_TIMES, noteId, e.getMessage());
                    
                    if (retry < MAX_RETRY_TIMES - 1) {
                        try {
                            // 重试前等待500ms，给下游服务恢复时间
                            Thread.sleep(500);
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
            
            // 每批之间间隔指定时间，平滑流量峰值（最后一批不需要间隔）
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
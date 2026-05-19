package com.quxiangshe.backend.scheduler;

import com.quxiangshe.backend.service.IViolationCaseLibraryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 违规案例库同步定时任务
 *
 * 核心职责：定期将DB中的待同步案例同步到Milvus向量数据库
 * 业务模块：审核模块（RAG Layer 2）
 *
 * 同步策略：
 *   - 每天凌晨3点执行全量同步
 *   - 每次最多同步100条记录
 *   - 失败不影响其他记录
 *
 * 触发场景：
 *   - 新增案例时向量插入失败（通过本任务重试）
 *   - Milvus服务重启后的数据恢复
 *
 * @author 趣享社技术团队
 */
@Slf4j
@Component
public class CaseSyncScheduler {

    @Autowired
    private IViolationCaseLibraryService caseLibraryService;

    @Value("${rag.sync.batch-size:100}")
    private int syncBatchSize;

    /**
     * 每天凌晨3点执行案例同步
     *
     * 流程：
     *   1. 查询待同步案例（embedding_id为null的记录）
     *   2. 逐个生成向量并插入Milvus
     *   3. 更新DB的embedding_id
     *   4. 记录同步结果
     *
     * 设计要点：
     *   - 失败跳过继续同步下一条，避免一条失败阻塞全部
     *   - 使用独立线程池执行（避免阻塞主线程）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void syncCasesToMilvus() {
        log.info("开始执行违规案例同步任务");

        try {
            long startTime = System.currentTimeMillis();
            long countBefore = caseLibraryService.countEnabled();

            int syncedCount = caseLibraryService.syncPendingCases(syncBatchSize);

            long duration = System.currentTimeMillis() - startTime;
            long countAfter = caseLibraryService.countEnabled();

            log.info("违规案例同步完成: 同步数量={}, 同步前={}, 同步后={}, 耗时={}ms",
                syncedCount, countBefore, countAfter, duration);

        } catch (Exception e) {
            log.error("违规案例同步任务异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 每小时执行一次增量同步（补漏）
     *
     * 用于处理高峰期新增案例中向量插入失败的情况
     */
    @Scheduled(cron = "0 30 * * * ?")
    public void incrementalSync() {
        try {
            int syncedCount = caseLibraryService.syncPendingCases(10);
            if (syncedCount > 0) {
                log.info("增量同步完成: 同步数量={}", syncedCount);
            }
        } catch (Exception e) {
            log.error("增量同步任务异常: {}", e.getMessage(), e);
        }
    }
}
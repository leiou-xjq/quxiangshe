package com.quxiangshe.backend.service.impl;

import com.quxiangshe.backend.entity.Follow;
import com.quxiangshe.backend.mapper.FollowMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 智能Feed分发服务
 * 根据博主粉丝数量和粉丝活跃度智能选择推送策略
 * 
 * 策略规则：
 * - 粉丝<1000：推模式
 * - 1000≤粉丝≤10万：拉模式
 * - 粉丝>10万：推拉结合（活跃粉丝推、超1000分批/普通粉丝拉/僵尸粉丝不推送）
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Service
public class SmartFeedDistributionService {

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    @Qualifier("feedDistributeExecutor")
    private Executor executor;

    @Value("${feed.blogger.small:1000}")
    private long smallBloggerThreshold;

    @Value("${feed.blogger.medium:100000}")
    private long mediumBloggerThreshold;

    @Value("${feed.active-threshold:120}")
    private double activeThreshold;

    @Value("${feed.normal-threshold:20}")
    private double normalThreshold;

    @Value("${feed.active-push-batch-size:1000}")
    private int activePushBatchSize;

    // Redis Key前缀
    private static final String FANS_ACTIVE_RANK = "fans:active:";
    private static final String FANS_NORMAL_RANK = "fans:normal:";
    private static final String FANS_INACTIVE_RANK = "fans:inactive:";
    private static final String PUSH_INBOX_PREFIX = "feed:inbox:push:";
    private static final String PULL_OUTBOX_PREFIX = "feed:outbox:pull:";

    /**
     * 推送模式枚举
     */
    public enum PushMode {
        PUSH,           // 推模式 (<1000粉丝)
        PULL,           // 拉模式 (1000-10万粉丝)
        HYBRID          // 推拉结合 (>10万粉丝)
    }

    /**
     * 粉丝分类
     */
    @Data
    public static class FanSegment {
        private List<Long> activeFans;   // 活跃粉丝（>=120分）
        private List<Long> normalFans;    // 普通粉丝（20-120分）
        private List<Long> inactiveFans;  // 僵尸粉丝（<20分）
        private long totalCount;
    }

    /**
     * 智能分发笔记
     * 
     * @param noteId 笔记ID
     * @param authorId 作者ID
     * @return 分发结果
     */
    public DistributionResult distribute(Long noteId, Long authorId) {
        log.info("开始智能Feed分发: noteId={}, authorId={}", noteId, authorId);
        long startTime = System.currentTimeMillis();

        // 1. 获取粉丝数量
        long followerCount = getFollowerCount(authorId);
        log.info("作者粉丝数: authorId={}, count={}", authorId, followerCount);

        // 2. 确定推送模式
        PushMode mode = determinePushMode(followerCount);
        log.info("确定推送模式: mode={}, followerCount={}", mode, followerCount);

        // 3. 根据模式执行分发
        DistributionResult result;
        switch (mode) {
            case PUSH:
                result = pushMode(noteId, authorId, followerCount);
                break;
            case PULL:
                result = pullMode(noteId, authorId);
                break;
            case HYBRID:
                result = hybridMode(noteId, authorId);
                break;
            default:
                result = new DistributionResult();
                result.setSuccess(false);
                result.setMessage("未知的推送模式");
        }

        result.setTotalCostMs(System.currentTimeMillis() - startTime);
        log.info("智能Feed分发完成: noteId={}, mode={}, success={}, cost={}ms", 
            noteId, mode, result.isSuccess(), result.getTotalCostMs());

        return result;
    }

    /**
     * 确定推送模式
     */
    private PushMode determinePushMode(long fansCount) {
        if (fansCount < smallBloggerThreshold) {
            return PushMode.PUSH;
        } else if (fansCount <= mediumBloggerThreshold) {
            return PushMode.PULL;
        } else {
            return PushMode.HYBRID;
        }
    }

    /**
     * 推模式 (<1000粉丝)
     * 直接推送到所有粉丝收件箱
     */
    private DistributionResult pushMode(Long noteId, Long authorId, long followerCount) {
        log.info("执行推模式: noteId={}, authorId={}, followerCount={}", 
            noteId, authorId, followerCount);

        DistributionResult result = new DistributionResult();
        result.setMode(PushMode.PUSH);

        try {
            // 获取所有粉丝
            List<Long> fans = getAllFollowers(authorId);
            if (fans.isEmpty()) {
                result.setSuccess(true);
                result.setMessage("无粉丝，跳过推送");
                return result;
            }

            // 计算score
            double score = System.currentTimeMillis();

            // 分批推送
            int batchSize = 500;
            int totalBatches = (int) Math.ceil((double) fans.size() / batchSize);
            int successCount = 0;

            for (int i = 0; i < fans.size(); i += batchSize) {
                int end = Math.min(i + batchSize, fans.size());
                List<Long> batch = fans.subList(i, end);

                // 异步执行每批推送
                int batchNum = i / batchSize + 1;
                final int finalBatchNum = batchNum;
                CompletableFuture.runAsync(() -> {
                    pushToInboxBatch(noteId, authorId, batch, score);
                    log.debug("推模式第{}/{}批完成: noteId={}, count={}", 
                        finalBatchNum, totalBatches, noteId, batch.size());
                }, executor);

                successCount += batch.size();
            }

            result.setSuccess(true);
            result.setMessage(String.format("推模式完成，共推送%d个粉丝", successCount));
            result.setPushCount(successCount);

        } catch (Exception e) {
            log.error("推模式执行失败: noteId={}, error={}", noteId, e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("推送失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 拉模式 (1000-10万粉丝)
     * 只写入作者发件箱，粉丝拉取
     */
    private DistributionResult pullMode(Long noteId, Long authorId) {
        log.info("执行拉模式: noteId={}, authorId={}", noteId, authorId);

        DistributionResult result = new DistributionResult();
        result.setMode(PushMode.PULL);

        try {
            // 写入作者发件箱
            double score = System.currentTimeMillis();
            String outboxKey = PULL_OUTBOX_PREFIX + authorId;
            redisTemplate.opsForZSet().add(outboxKey, noteId.toString(), score);
            redisTemplate.expire(outboxKey, 24, java.util.concurrent.TimeUnit.HOURS);

            result.setSuccess(true);
            result.setMessage("已写入作者发件箱");
            result.setPushCount(0); // 拉模式不直接推送

        } catch (Exception e) {
            log.error("拉模式执行失败: noteId={}, error={}", noteId, e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("写入发件箱失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 推拉结合模式 (>10万粉丝)
     * 活跃粉丝推，普通粉丝拉，僵尸粉丝不推
     */
    private DistributionResult hybridMode(Long noteId, Long authorId) {
        log.info("执行推拉结合模式: noteId={}, authorId={}", noteId, authorId);

        DistributionResult result = new DistributionResult();
        result.setMode(PushMode.HYBRID);

        try {
            // 1. 获取粉丝分类
            FanSegment segment = getFanSegment(authorId);
            log.info("粉丝分类: active={}, normal={}, inactive={}", 
                segment.getActiveFans().size(), 
                segment.getNormalFans().size(),
                segment.getInactiveFans().size());

            result.setTotalFans(segment.getTotalCount());
            result.setActiveFans(segment.getActiveFans().size());
            result.setNormalFans(segment.getNormalFans().size());
            result.setInactiveFans(segment.getInactiveFans().size());

            // 2. 活跃粉丝 -> 收件箱（分批推送，超过1000则分批）
            if (!segment.getActiveFans().isEmpty()) {
                CompletableFuture.runAsync(() -> {
                    pushActiveFansByBatches(noteId, authorId, segment.getActiveFans());
                }, executor);
            }

            // 3. 普通粉丝 -> 发件箱（拉模式）
            if (!segment.getNormalFans().isEmpty()) {
                pushToOutbox(noteId, authorId);
            }

            // 4. 僵尸粉丝 -> 不推送
            log.info("僵尸粉丝不推送: count={}", segment.getInactiveFans().size());

            result.setSuccess(true);
            result.setMessage(String.format("推拉结合完成: 活跃推送%d, 普通拉取%d, 僵尸跳过%d", 
                segment.getActiveFans().size(), 
                segment.getNormalFans().size(),
                segment.getInactiveFans().size()));
            result.setPushCount(segment.getActiveFans().size());

        } catch (Exception e) {
            log.error("推拉结合模式执行失败: noteId={}, error={}", noteId, e.getMessage(), e);
            // 降级到拉模式
            return pullMode(noteId, authorId);
        }

        return result;
    }

    /**
     * 分批推送活跃粉丝（超过1000则分批排序推送）
     */
    private void pushActiveFansByBatches(Long noteId, Long authorId, List<Long> activeFans) {
        // 按粉丝ID排序，保证顺序一致
        Collections.sort(activeFans);

        int totalBatches = (int) Math.ceil((double) activeFans.size() / activePushBatchSize);
        log.info("活跃粉丝分批推送: total={}, batchSize={}, totalBatches={}", 
            activeFans.size(), activePushBatchSize, totalBatches);

        for (int batch = 0; batch < totalBatches; batch++) {
            int start = batch * activePushBatchSize;
            int end = Math.min(start + activePushBatchSize, activeFans.size());
            List<Long> batchFans = activeFans.subList(start, end);

            log.info("推送第{}/{}批: noteId={}, count={}", 
                batch + 1, totalBatches, noteId, batchFans.size());

            double score = System.currentTimeMillis() + batch; // 每批略微增加score保证顺序
            pushToInboxBatch(noteId, authorId, batchFans, score);

            // 每批间隔500ms，避免瞬间压力过大
            if (batch < totalBatches - 1) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.info("活跃粉丝全部推送完成: noteId={}, count={}", noteId, activeFans.size());
    }

    /**
     * 获取粉丝分类（从Redis获取）
     */
    private FanSegment getFanSegment(Long authorId) {
        FanSegment segment = new FanSegment();
        segment.setActiveFans(new ArrayList<>());
        segment.setNormalFans(new ArrayList<>());
        segment.setInactiveFans(new ArrayList<>());

        try {
            // 获取活跃粉丝
            Set<String> activeMembers = redisTemplate.opsForSet()
                .members(FANS_ACTIVE_RANK + authorId);
            if (activeMembers != null) {
                segment.setActiveFans(activeMembers.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList()));
            }

            // 获取普通粉丝
            Set<String> normalMembers = redisTemplate.opsForSet()
                .members(FANS_NORMAL_RANK + authorId);
            if (normalMembers != null) {
                segment.setNormalFans(normalMembers.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList()));
            }

            // 获取僵尸粉丝
            Set<String> inactiveMembers = redisTemplate.opsForSet()
                .members(FANS_INACTIVE_RANK + authorId);
            if (inactiveMembers != null) {
                segment.setInactiveFans(inactiveMembers.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList()));
            }

            segment.setTotalCount(
                segment.getActiveFans().size() + 
                segment.getNormalFans().size() + 
                segment.getInactiveFans().size()
            );

        } catch (Exception e) {
            log.error("获取粉丝分类失败: authorId={}, error={}", authorId, e.getMessage());
            // 降级：获取所有粉丝作为普通粉丝
            segment.setNormalFans(getAllFollowers(authorId));
            segment.setTotalCount(segment.getNormalFans().size());
        }

        return segment;
    }

    /**
     * 批量写入收件箱
     */
    private void pushToInboxBatch(Long noteId, Long authorId, List<Long> fans, double score) {
        try {
            // 使用Pipeline批量写入
            for (Long fanId : fans) {
                String inboxKey = PUSH_INBOX_PREFIX + fanId;
                redisTemplate.opsForZSet().add(inboxKey, noteId.toString(), score);
                redisTemplate.expire(inboxKey, 24, java.util.concurrent.TimeUnit.HOURS);
            }
        } catch (Exception e) {
            log.error("批量写入收件箱失败: noteId={}, error={}", noteId, e.getMessage());
        }
    }

    /**
     * 写入发件箱
     */
    private void pushToOutbox(Long noteId, Long authorId) {
        try {
            double score = System.currentTimeMillis();
            String outboxKey = PULL_OUTBOX_PREFIX + authorId;
            redisTemplate.opsForZSet().add(outboxKey, noteId.toString(), score);
            redisTemplate.expire(outboxKey, 24, java.util.concurrent.TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("写入发件箱失败: noteId={}, error={}", noteId, e.getMessage());
        }
    }

    /**
     * 获取所有粉丝
     */
    private List<Long> getAllFollowers(Long authorId) {
        try {
            List<Follow> follows = followMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Follow>()
                    .eq(Follow::getFollowingId, authorId)
            );
            return follows.stream()
                .map(Follow::getFollowerId)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取粉丝列表失败: authorId={}, error={}", authorId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取粉丝数量
     */
    private long getFollowerCount(Long authorId) {
        try {
            Long count = followMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Follow>()
                    .eq(Follow::getFollowingId, authorId)
            );
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("获取粉丝数失败: authorId={}, error={}", authorId, e.getMessage());
            return 0;
        }
    }

    /**
     * 分发结果
     */
    @Data
    public static class DistributionResult {
        private boolean success;
        private String message;
        private PushMode mode;
        private long pushCount;
        private long totalFans;
        private long activeFans;
        private long normalFans;
        private long inactiveFans;
        private long totalCostMs;
    }
}
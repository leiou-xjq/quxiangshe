package com.quxiangshe.backend.task;

import com.quxiangshe.backend.config.RabbitMQConfig;
import com.quxiangshe.backend.dto.NotificationMessage;
import com.quxiangshe.backend.dto.VideoTranscodeMessage;
import com.quxiangshe.backend.entity.Note;
import com.quxiangshe.backend.entity.NoteReview;
import com.quxiangshe.backend.entity.ViolationCaseLibrary;
import com.quxiangshe.backend.mapper.NoteMapper;
import com.quxiangshe.backend.mapper.NoteReviewMapper;
import com.quxiangshe.backend.service.INotificationService;
import com.quxiangshe.backend.service.IReputationService;
import com.quxiangshe.backend.service.IViolationCaseLibraryService;
import com.quxiangshe.backend.service.impl.ValueReviewService;
import com.quxiangshe.backend.component.FeedPusher;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 异步审核任务
 * 
 * 核心职责：将耗时的内容审核逻辑异步化，确保发布接口快速响应
 * 业务模块：审核模块（异步层）
 * 
 * 审核流程：
 *   1. 发布接口保存笔记后通过 MQ 投递审核消息
 *   2. MQ消费者调用本类的 asyncReview() 方法
 *   3. 全程由 LLM（豆包/价值观审核服务）进行内容审核
 *   4. 审核通过：status=1，触发Feed分发、视频转码
 *   5. 审核违规：status=2，发送审核未通过通知
 * 
 * 设计要点：
 *   - @Async("reviewExecutor") 确保审核在独立线程池中执行
 *   - CompletableFuture 支持异步回调，不阻塞MQ消费者
 *   - 异常时返回违规结果，保证笔记不会处于待审核挂起状态
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Component
public class ReviewAsyncTask {

    @Autowired
    private ValueReviewService valueReviewService;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private NoteReviewMapper noteReviewMapper;

    @Autowired
    private FeedPusher feedPusher;

    @Autowired
    private INotificationService notificationService;

    @Autowired
    private IViolationCaseLibraryService caseLibraryService;

    @Autowired
    private IReputationService reputationService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${review.async-enabled:true}")
    private boolean asyncEnabled;

    @Value("${review.enabled:true}")
    private boolean reviewEnabled;

    @Value("${reputation.increase.review-passed:2}")
    private int reputationBonus;

    @Value("${reputation.decrease.review-rejected:5}")
    private int reputationPenalty;

/**
     * 异步执行内容审核（不含图片）
     * 
     * @param noteId 笔记ID
     * @param userId 发布者ID
     * @param title 笔记标题
     * @param content 笔记内容
     * @return CompletableFuture包装的审核结果
     */
    @Async("reviewExecutor")
    public CompletableFuture<ReviewResult> asyncReview(Long noteId, Long userId,
                                                         String title, String content) {
        return asyncReview(noteId, userId, title, content, null);
    }

    /**
     * 异步执行内容审核（支持图片多模态审核）
     * 
     * 流程：
     *   1. 创建审核记录（status=待审核）
     *   2. 调用LLM价值观审核服务（含图片）
     *   3. 综合判定结果
     *   4. 根据判定更新笔记状态并触发后续动作
     * 
     * @param noteId 笔记ID
     * @param userId 发布者ID
     * @param title 标题
     * @param content 内容
     * @param imageUrls 图片URL列表（可选，用于多模态审核）
     * @return CompletableFuture包装的审核结果
     */
    @Async("reviewExecutor")
    public CompletableFuture<ReviewResult> asyncReview(Long noteId, Long userId,
                                                         String title, String content, List<String> imageUrls) {
        log.info("开始异步审核: noteId={}, userId={}, title={}, images={}", noteId, userId, title,
            imageUrls != null ? imageUrls.size() : 0);
        long startTime = System.currentTimeMillis();

        try {
            // 1. 创建审核记录（status=待审核）
            NoteReview review = createReviewRecord(noteId, userId, title, content);

            // 2. 调用LLM价值观审核服务进行内容判定
            ValueReviewService.ValueReviewResult valueResult = valueReviewService.review(title, content, null, imageUrls);

            // 3. 综合LLM判定结果生成最终审核结论
            ReviewResult finalResult = combineResult(review, valueResult);

            // 4. 根据审核结果处理笔记状态（通过/违规）
            handleReviewResult(noteId, finalResult);

            log.info("异步审核完成: noteId={}, passed={}, status={}, reason={}, cost={}ms",
                noteId, finalResult.isPassed(), finalResult.getStatus(), finalResult.getReason(), System.currentTimeMillis() - startTime);

            return CompletableFuture.completedFuture(finalResult);

        } catch (Exception e) {
            // 审核系统异常时默认标记为违规，保证安全
            log.error("异步审核异常: noteId={}, error={}", noteId, e.getMessage(), e);
            return CompletableFuture.completedFuture(
                ReviewResult.violation("审核系统异常: " + e.getMessage(), Collections.emptyList())
            );
        }
    }

    /**
     * 综合判定结果（仅使用LLM判定）
     * 
     * 判定逻辑：
     *   - VIOLATION（违规）：直接判定为违规
     *   - SUSPICIOUS（疑似违规）：也判定为违规（保守策略）
     *   - NORMAL（正常）：判定通过
     * 
     * @param review 审核记录
     * @param valueResult LLM审核结果
     * @return 最终审核结论
     */
    private ReviewResult combineResult(NoteReview review, ValueReviewService.ValueReviewResult valueResult) {
        // 记录LLM判定到审核记录中
        review.setLayer3LlmVerdict(valueResult.getStatus());

        String llmStatus = valueResult.getStatus();
        
        // 违规或疑似违规均判定为违规（保守策略，宁可误杀不可放过）
        if (llmStatus != null && ("VIOLATION".equals(llmStatus) || "SUSPICIOUS".equals(llmStatus))) {
            if ("SUSPICIOUS".equals(llmStatus)) {
                // 疑似违规通常会升级为违规
                log.warn("内容疑似违规，判定为违规: {}", valueResult.getReason());
            }
            return ReviewResult.violation(valueResult.getReason(), valueResult.getTags());
        }
        
        return ReviewResult.pass();
    }

    /**
     * 处理审核结果，更新笔记状态并触发后续动作
     * 
     * 审核通过：
     *   1. 笔记状态更新为正常(status=1)
     *   2. 有视频时投递视频转码任务到MQ
     *   3. 触发Feed分发（推送到粉丝收件箱/发件箱）
     * 
     * 审核违规：
     *   1. 笔记状态更新为下架(status=2)
     *   2. 发送审核拒绝通知给作者
     * 
     * @param noteId 笔记ID
     * @param result 审核结果
     */
    private void handleReviewResult(Long noteId, ReviewResult result) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            log.error("笔记不存在: noteId={}", noteId);
            return;
        }

        log.info("handleReviewResult: noteId={}, result.status={}, result.passed={}, result.reason={}", 
            noteId, result.getStatus(), result.isPassed(), result.getReason());

        if ("VIOLATION".equals(result.getStatus())) {
            // 违规处理：标记为下架
            note.setStatus(2);
            noteMapper.updateById(note);
            log.info("笔记标记为违规: noteId={}, status={}, reason={}", noteId, result.getStatus(), result.getReason());

            // 发送审核未通过通知（MQ异步投递）
            if (result.getReason() != null && rabbitTemplate != null) {
                NotificationMessage msg = NotificationMessage.builder()
                        .type(NotificationMessage.TYPE_REVIEW_REJECTED)
                        .userId(note.getUserId())
                        .noteId(noteId)
                        .extra(result.getReason())
                        .timestamp(LocalDateTime.now())
                        .build();
                rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_EXCHANGE,
                        RabbitMQConfig.NOTIFICATION_ROUTING_KEY, msg);
                log.info("审核未通过通知已投递MQ: noteId={}, userId={}", noteId, note.getUserId());
            }

            // ===== 自动入库到违规案例库 =====
            try {
                ViolationCaseLibrary caseInfo = ViolationCaseLibrary.builder()
                    .caseType(extractCaseType(result.getTags()))
                    .title(note.getTitle())
                    .content(note.getContent())
                    .violationReason(result.getReason())
                    .violationTags(result.getTags() != null ? String.join(",", result.getTags()) : null)
                    .sourceReviewId(null)
                    .build();
                Long caseId = caseLibraryService.addCase(caseInfo);
                if (caseId != null) {
                    log.info("违规案例已自动入库: caseId={}, caseType={}", caseId, caseInfo.getCaseType());
                }
            } catch (Exception e) {
                log.error("违规案例自动入库失败: noteId={}, error={}", noteId, e.getMessage());
            }
            // ===== 自动入库结束 =====

            // ===== 更新用户信誉分（违规扣分） =====
            try {
                reputationService.decreaseReputation(note.getUserId(), reputationPenalty, "审核违规");
                log.info("用户信誉分已扣除: userId={}, penalty={}", note.getUserId(), reputationPenalty);
            } catch (Exception e) {
                log.error("更新用户信誉分失败: userId={}, error={}", note.getUserId(), e.getMessage());
            }
            // ===== 信誉分更新结束 =====

        } else {
            // 审核通过处理
            note.setStatus(1);
            noteMapper.updateById(note);
            log.info("笔记审核通过: noteId={}, 触发Feed推送", noteId);

            // 有视频则投递视频转码任务到MQ
            if (note.getVideo() != null && !note.getVideo().isEmpty() && rabbitTemplate != null) {
                VideoTranscodeMessage transcodeMsg = VideoTranscodeMessage.builder()
                        .noteId(noteId)
                        .originalUrl(note.getVideo())
                        .targetFormat("mp4")
                        .targetWidth(1280)
                        .targetHeight(720)
                        .timestamp(LocalDateTime.now())
                        .build();
                rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_EXCHANGE,
                        RabbitMQConfig.VIDEO_ROUTING_KEY, transcodeMsg);
                log.info("视频转码任务已投递MQ: noteId={}", noteId);
            }

            // 触发Feed分发（推送到粉丝收件箱）
            feedPusher.pushNote(noteId, note.getUserId());

            // ===== 更新用户信誉分（审核通过加分） =====
            try {
                reputationService.increaseReputation(note.getUserId(), reputationBonus);
                log.info("用户信誉分已增加: userId={}, bonus={}", note.getUserId(), reputationBonus);
            } catch (Exception e) {
                log.error("更新用户信誉分失败: userId={}, error={}", note.getUserId(), e.getMessage());
            }
            // ===== 信誉分更新结束 =====
        }
    }

    /**
     * 创建审核记录
     * 初始状态为"待审核"
     * 
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @param title 标题
     * @param content 内容
     * @return 审核记录实体
     */
    private NoteReview createReviewRecord(Long noteId, Long userId, String title, String content) {
        NoteReview review = new NoteReview();
        review.setNoteId(noteId);
        review.setUserId(userId);
        review.setTitle(title);
        review.setContent(content);
        review.setReviewStatus(NoteReview.STATUS_PENDING);
        review.setCreatedAt(LocalDateTime.now());
        
        noteReviewMapper.insert(review);
        return review;
    }

    /**
     * 审核结果（内部静态类）
     * 
     * status取值：
     *   - NORMAL：审核通过
     *   - VIOLATION：违规
     *   - SUSPICIOUS：疑似违规（审核不通过）
     */
    @Data
    public static class ReviewResult {
        private boolean passed;          // 是否通过审核
        private String status;           // 审核状态码
        private String reason;           // 违规原因描述
        private List<String> matchedWords; // 命中的敏感词（预留，未使用）
        private List<String> tags;        // 违规标签列表

        public static ReviewResult pass() {
            ReviewResult result = new ReviewResult();
            result.setPassed(true);
            result.setStatus("NORMAL");
            return result;
        }

        public static ReviewResult violation(String reason, List<String> tags) {
            ReviewResult result = new ReviewResult();
            result.setPassed(false);
            result.setStatus("VIOLATION");
            result.setReason(reason);
            result.setTags(tags);
            return result;
        }

/**
          * 创建疑似违规结果（当前业务上按违规处理）
          */
        public static ReviewResult suspicious(String reason, List<String> tags) {
            ReviewResult result = new ReviewResult();
            result.setPassed(false);
            result.setStatus("SUSPICIOUS");
            result.setReason(reason);
            result.setTags(tags);
            return result;
        }
    }

    /**
     * 从违规标签提取案例类型
     *
     * 映射规则：
     *   - 毒鸡汤 → toxic_soup
     *   - 性别对立 → gender_discrimination
     *   - 错误价值观 → incorrect_values
     *   - 制造焦虑 → anxiety
     *   - 消极厌世 → negative
     *   - 极端观点 → extreme
     *   - 伪逻辑错误 → false_logic
     *   - 默认 → incorrect_values
     *
     * @param tags 违规标签列表
     * @return 案例类型
     */
    private String extractCaseType(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return ViolationCaseLibrary.CASE_TYPE_INCORRECT_VALUES;
        }

        String tagStr = String.join("", tags).toLowerCase();

        if (tagStr.contains("毒鸡汤") || tagStr.contains("鸡汤")) {
            return ViolationCaseLibrary.CASE_TYPE_TOXIC_SOUP;
        } else if (tagStr.contains("性别") || tagStr.contains("对立")) {
            return ViolationCaseLibrary.CASE_TYPE_GENDER_DISCRIMINATION;
        } else if (tagStr.contains("焦虑")) {
            return ViolationCaseLibrary.CASE_TYPE_ANXIETY;
        } else if (tagStr.contains("消极") || tagStr.contains("厌世")) {
            return ViolationCaseLibrary.CASE_TYPE_NEGATIVE;
        } else if (tagStr.contains("极端")) {
            return ViolationCaseLibrary.CASE_TYPE_EXTREME;
        } else if (tagStr.contains("逻辑") || tagStr.contains("因果")) {
            return ViolationCaseLibrary.CASE_TYPE_FALSE_LOGIC;
        } else if (tagStr.contains("价值观") || tagStr.contains("三观")) {
            return ViolationCaseLibrary.CASE_TYPE_INCORRECT_VALUES;
        }

        return ViolationCaseLibrary.CASE_TYPE_INCORRECT_VALUES;
    }
}
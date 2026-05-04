package com.quxiangshe.backend.task;

import com.quxiangshe.backend.config.RabbitMQConfig;
import com.quxiangshe.backend.dto.NotificationMessage;
import com.quxiangshe.backend.dto.VideoTranscodeMessage;
import com.quxiangshe.backend.entity.Note;
import com.quxiangshe.backend.entity.NoteReview;
import com.quxiangshe.backend.mapper.NoteMapper;
import com.quxiangshe.backend.mapper.NoteReviewMapper;
import com.quxiangshe.backend.service.INotificationService;
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
 * 将耗时的审核逻辑异步化，确保发布接口快速响应
 * 
 * 审核流程：全程由LLM进行价值观和敏感词审核
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
    private RabbitTemplate rabbitTemplate;

    @Value("${review.async-enabled:true}")
    private boolean asyncEnabled;

    @Value("${review.enabled:true}")
    private boolean reviewEnabled;

/**
     * 异步执行内容审核
     * 不阻塞主发布流程
     *
     * @param noteId 笔记ID
     * @param userId 发布者ID
     * @param title 标题
     * @param content 内容
     */
    @Async("reviewExecutor")
    public CompletableFuture<ReviewResult> asyncReview(Long noteId, Long userId,
                                                        String title, String content) {
        return asyncReview(noteId, userId, title, content, null);
    }

    /**
     * 异步执行内容审核（支持图片审核）
     */
    @Async("reviewExecutor")
    public CompletableFuture<ReviewResult> asyncReview(Long noteId, Long userId,
                                                        String title, String content, List<String> imageUrls) {
        log.info("开始异步审核: noteId={}, userId={}, title={}, images={}", noteId, userId, title,
            imageUrls != null ? imageUrls.size() : 0);
        long startTime = System.currentTimeMillis();

        try {
            NoteReview review = createReviewRecord(noteId, userId, title, content);

            ValueReviewService.ValueReviewResult valueResult = valueReviewService.review(title, content, null, imageUrls);

            ReviewResult finalResult = combineResult(review, valueResult);

            handleReviewResult(noteId, finalResult);

            log.info("异步审核完成: noteId={}, passed={}, status={}, reason={}, cost={}ms",
                noteId, finalResult.isPassed(), finalResult.getStatus(), finalResult.getReason(), System.currentTimeMillis() - startTime);

            return CompletableFuture.completedFuture(finalResult);

        } catch (Exception e) {
            log.error("异步审核异常: noteId={}, error={}", noteId, e.getMessage(), e);
            return CompletableFuture.completedFuture(
                ReviewResult.violation("审核系统异常: " + e.getMessage(), Collections.emptyList())
            );
        }
    }

    /**
     * 综合判定结果（只使用LLM判定）
     */
    private ReviewResult combineResult(NoteReview review, ValueReviewService.ValueReviewResult valueResult) {
        review.setLayer3LlmVerdict(valueResult.getStatus());

        String llmStatus = valueResult.getStatus();
        
        if (llmStatus != null && ("VIOLATION".equals(llmStatus) || "SUSPICIOUS".equals(llmStatus))) {
            if ("SUSPICIOUS".equals(llmStatus)) {
                log.warn("内容疑似违规，判定为违规: {}", valueResult.getReason());
            }
            return ReviewResult.violation(valueResult.getReason(), valueResult.getTags());
        }
        
        return ReviewResult.pass();
    }

    /**
     * 处理审核结果
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
            note.setStatus(2);
            noteMapper.updateById(note);
            log.info("笔记标记为违规: noteId={}, status={}, reason={}", noteId, result.getStatus(), result.getReason());

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

        } else {
            note.setStatus(1);
            noteMapper.updateById(note);
            log.info("笔记审核通过: noteId={}, 触发Feed推送", noteId);

            // 审核通过后触发视频转码
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

            feedPusher.pushNote(noteId, note.getUserId());
        }
    }

    /**
     * 创建审核记录
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
     * 审核结果
     */
    @Data
    public static class ReviewResult {
        private boolean passed;
        private String status;
        private String reason;
        private List<String> matchedWords;
        private List<String> tags;

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

        public static ReviewResult suspicious(String reason, List<String> tags) {
            ReviewResult result = new ReviewResult();
            result.setPassed(false);
            result.setStatus("SUSPICIOUS");
            result.setReason(reason);
            result.setTags(tags);
            return result;
        }
    }
}
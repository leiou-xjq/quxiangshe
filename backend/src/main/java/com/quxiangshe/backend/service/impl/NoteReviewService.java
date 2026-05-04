package com.quxiangshe.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quxiangshe.backend.dto.CreateNoteRequest;
import com.quxiangshe.backend.entity.NoteReview;
import com.quxiangshe.backend.mapper.NoteReviewMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 笔记内容审核服务
 * 审核流程：全程由LLM进行价值观和敏感词审核
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Service
public class NoteReviewService {
    
    private final NoteReviewMapper noteReviewMapper;
    private final DoubaoLlmService doubaoLlmService;
    private final ObjectMapper objectMapper;
    
    @Value("${review.enabled:true}")
    private boolean reviewEnabled;
    
    public NoteReviewService(
            NoteReviewMapper noteReviewMapper,
            DoubaoLlmService doubaoLlmService,
            ObjectMapper objectMapper) {
        this.noteReviewMapper = noteReviewMapper;
        this.doubaoLlmService = doubaoLlmService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 审核结果
     */
    @Data
    public static class ReviewResponse {
        private boolean passed;
        private int status;
        private String message;
        private String reason;
        private List<String> tags;
        private Long reviewId;
        
        public static ReviewResponse pass(Long reviewId) {
            ReviewResponse response = new ReviewResponse();
            response.setPassed(true);
            response.setStatus(NoteReview.STATUS_NORMAL);
            response.setMessage("审核通过");
            response.setReviewId(reviewId);
            return response;
        }
        
        public static ReviewResponse suspicious(String message, Long reviewId) {
            ReviewResponse response = new ReviewResponse();
            response.setPassed(false);
            response.setStatus(NoteReview.STATUS_SUSPICIOUS);
            response.setMessage(message);
            response.setReviewId(reviewId);
            return response;
        }
        
        public static ReviewResponse violation(String reason, List<String> tags, Long reviewId) {
            ReviewResponse response = new ReviewResponse();
            response.setPassed(false);
            response.setStatus(NoteReview.STATUS_VIOLATION);
            response.setMessage("内容违规，禁止发布");
            response.setReason(reason);
            response.setTags(tags);
            response.setReviewId(reviewId);
            return response;
        }
    }
    
    /**
     * 审核笔记
     * 流程：LLM价值观审核（包含敏感词检测）
     */
    public ReviewResponse reviewNote(Long noteId, Long userId, CreateNoteRequest request) {
        log.info("开始审核笔记, noteId: {}, userId: {}", noteId, userId);
        
        if (!reviewEnabled) {
            log.info("审核功能未启用，直接通过");
            return ReviewResponse.pass(null);
        }
        
        NoteReview review = createReviewRecord(noteId, userId, request);
        
        try {
            DoubaoLlmService.ReviewResult llmResult = doubaoLlmService.review(
                request.getTitle(),
                request.getContent(),
                null
            );
            
            review.setLayer3LlmVerdict(llmResult.getStatus());
            review.setLlmResponse(llmResult.getRawResponse());
            
            if ("VIOLATION".equals(llmResult.getStatus())) {
                log.warn("大模型判定违规: {}", llmResult.getReason());
                return handleViolation(review, llmResult.getReason(), llmResult.getTags());
            } else if ("SUSPICIOUS".equals(llmResult.getStatus())) {
                log.warn("大模型判定疑似: {}，统一视为违规", llmResult.getReason());
                return handleViolation(review, "内容疑似违规: " + llmResult.getReason(), llmResult.getTags());
            }
            
            log.info("笔记审核通过, noteId: {}", noteId);
            return handleNormal(review);
            
        } catch (Exception e) {
            log.error("审核过程异常: {}", e.getMessage(), e);
            return handleViolation(review, "审核系统异常: " + e.getMessage(), Arrays.asList("系统异常"));
        }
    }
    
    private ReviewResponse handleNormal(NoteReview review) {
        review.setReviewStatus(NoteReview.STATUS_NORMAL);
        review.setReviewResult("审核通过");
        review.setReviewTime(LocalDateTime.now());
        noteReviewMapper.updateById(review);
        
        return ReviewResponse.pass(review.getId());
    }
    
    private ReviewResponse handleViolation(NoteReview review, String reason, List<String> tags) {
        review.setReviewStatus(NoteReview.STATUS_VIOLATION);
        review.setReviewResult("内容违规，禁止发布");
        review.setViolationReason(reason);
        review.setViolationTags(toJson(tags));
        review.setReviewTime(LocalDateTime.now());
        noteReviewMapper.updateById(review);
        
        return ReviewResponse.violation(reason, tags, review.getId());
    }
    
    private NoteReview createReviewRecord(Long noteId, Long userId, CreateNoteRequest request) {
        NoteReview review = new NoteReview();
        review.setNoteId(noteId);
        review.setUserId(userId);
        review.setTitle(request.getTitle());
        review.setContent(request.getContent());
        review.setTags(toJson(request.getTags()));
        review.setImages(toJson(request.getImages()));
        review.setVideo(request.getVideo());
        review.setLocation(request.getLocation());
        review.setReviewStatus(NoteReview.STATUS_PENDING);
        
        noteReviewMapper.insert(review);
        
        return review;
    }
    
    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("JSON序列化失败: {}", e.getMessage());
            return obj.toString();
        }
    }
    
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalPending", noteReviewMapper.countPending());
        stats.put("totalViolation", noteReviewMapper.countViolations());
        stats.put("todayStats", noteReviewMapper.getTodayStatistics());
        
        return stats;
    }
    
    public List<NoteReview> getPendingReviews(int limit) {
        return noteReviewMapper.findPendingReviews(limit);
    }
    
    public void approveReview(Long reviewId) {
        NoteReview review = noteReviewMapper.selectById(reviewId);
        if (review != null) {
            review.setReviewStatus(NoteReview.STATUS_NORMAL);
            review.setReviewResult("人工审核通过");
            review.setReviewTime(LocalDateTime.now());
            noteReviewMapper.updateById(review);
        }
    }
    
    public void rejectReview(Long reviewId, String reason, List<String> tags) {
        NoteReview review = noteReviewMapper.selectById(reviewId);
        if (review != null) {
            review.setReviewStatus(NoteReview.STATUS_VIOLATION);
            review.setReviewResult("人工审核拒绝");
            review.setViolationReason(reason);
            review.setViolationTags(toJson(tags));
            review.setReviewTime(LocalDateTime.now());
            noteReviewMapper.updateById(review);
        }
    }
}
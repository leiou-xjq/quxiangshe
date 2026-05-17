package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记审核记录实体类，对应数据库表 note_review。
 * <p>
 * 记录每次笔记发布的三层审核结果：第一层敏感词检测（layer_1_passed）、
 * 第二层RAG相似案例检索（layer_2_rag_score）、第三层大模型判定（layer_3_llm_verdict）。
 * 审核完成后根据审核状态（通过/疑似/违规）决定笔记上架或下架。
 * 违规记录可被导入案例库（case_imported）作为后续审核的参考。
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Data
@TableName("note_review")
public class NoteReview {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 笔记ID
     */
    private Long noteId;
    
    /**
     * 发布者用户ID
     */
    private Long userId;
    
    /**
     * 笔记标题
     */
    private String title;
    
    /**
     * 笔记内容
     */
    private String content;
    
    /**
     * 标签(JSON数组)
     */
    private String tags;
    
    /**
     * 图片(JSON数组)
     */
    private String images;
    
    /**
     * 视频URL
     */
    private String video;
    
    /**
     * 地理位置
     */
    private String location;
    
    /**
     * 审核状态: 0-待审核 1-正常 2-疑似 3-违规
     */
    private Integer reviewStatus;
    
    /**
     * 审核结果详情
     */
    private String reviewResult;
    
    /**
     * 违规原因
     */
    private String violationReason;
    
    /**
     * 违规标签(JSON数组)
     */
    private String violationTags;
    
/**
     * 敏感词检测结果(JSON)
     */
    private String sensitiveWordsFound;
    
    /**
     * RAG检索相似案例(JSON)
     */
    private String similarityCases;
    
    /**
     * 大模型原始响应(JSON)
     */
    private String llmResponse;
    
/**
 * 第一层敏感词检测是否通过
 */
    @TableField("layer_1_passed")
    private Boolean layer1Passed;
    
    /**
     * 第二层RAG相似度得分
     */
    @TableField("layer_2_rag_score")
    private Double layer2RagScore;
    
    /**
     * 第三层大模型判定结果
     */
    @TableField("layer_3_llm_verdict")
    private String layer3LlmVerdict;
    
    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;
    
    /**
     * 是否已导入案例库
     */
    private Integer caseImported;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 审核状态常量
     */
    public static final int STATUS_PENDING = 0;    // 待审核
    public static final int STATUS_NORMAL = 1;    // 正常
    public static final int STATUS_SUSPICIOUS = 2; // 疑似
    public static final int STATUS_VIOLATION = 3;  // 违规
}
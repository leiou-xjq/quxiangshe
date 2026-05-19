package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 违规案例库实体类，对应数据库表 violation_case_library。
 *
 * 核心职责：存储违规案例，用于RAG相似案例检索
 * 业务模块：审核模块（RAG Layer 2）
 *
 * 案例来源：
 *   - 审核违规时自动入库（ReviewAsyncTask触发）
 *   - 人工导入（管理员操作）
 *
 * 向量同步：
 *   - 新增时生成向量并存储到Milvus
 *   - embedding_id字段关联Milvus向量ID
 *
 * @author 趣享社技术团队
 */
@Data
@Builder
@TableName("violation_case_library")
public class ViolationCaseLibrary {

    /**
     * 违规类型：毒鸡汤
     */
    public static final String CASE_TYPE_TOXIC_SOUP = "toxic_soup";

    /**
     * 违规类型：性别对立
     */
    public static final String CASE_TYPE_GENDER_DISCRIMINATION = "gender_discrimination";

    /**
     * 违规类型：错误价值观
     */
    public static final String CASE_TYPE_INCORRECT_VALUES = "incorrect_values";

    /**
     * 违规类型：制造焦虑
     */
    public static final String CASE_TYPE_ANXIETY = "anxiety";

    /**
     * 违规类型：消极厌世
     */
    public static final String CASE_TYPE_NEGATIVE = "negative";

    /**
     * 违规类型：极端观点
     */
    public static final String CASE_TYPE_EXTREME = "extreme";

    /**
     * 违规类型：伪逻辑错误
     */
    public static final String CASE_TYPE_FALSE_LOGIC = "false_logic";

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 违规类型: toxic_soup/gender_discrimination/incorrect_values/anxiety/negative/extreme/false_logic
     */
    private String caseType;

    /**
     * 案例标题
     */
    private String title;

    /**
     * 违规内容原文
     */
    private String content;

    /**
     * 违规原因描述
     */
    private String violationReason;

    /**
     * 违规标签(JSON数组)
     */
    private String violationTags;

    /**
     * Milvus向量ID
     */
    private Long embeddingId;

    /**
     * 来源审核记录ID
     */
    private Long sourceReviewId;

    /**
     * 状态: 0-禁用, 1-启用
     */
    private Integer status;

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
     * 状态常量
     */
    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;
}
-- 违规案例库表 - 用于RAG相似案例检索
-- 执行顺序: V9

CREATE TABLE IF NOT EXISTS violation_case_library (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    case_type VARCHAR(50) NOT NULL COMMENT '违规类型: toxic_soup-毒鸡汤, gender_discrimination-性别对立, incorrect_values-错误价值观, anxiety-制造焦虑, negative-消极厌世, extreme-极端观点, false_logic-伪逻辑错误, etc',
    title VARCHAR(255) NOT NULL COMMENT '案例标题',
    content TEXT NOT NULL COMMENT '违规内容原文',
    violation_reason VARCHAR(500) COMMENT '违规原因描述',
    violation_tags JSON COMMENT '违规标签列表: ["毒鸡汤","消极"]',
    embedding_id BIGINT COMMENT 'Milvus向量ID',
    source_review_id BIGINT COMMENT '来源审核记录ID',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_case_type (case_type) COMMENT '违规类型索引',
    INDEX idx_embedding_id (embedding_id) COMMENT '向量ID索引',
    INDEX idx_status (status) COMMENT '状态索引',
    INDEX idx_source_review (source_review_id) COMMENT '来源审核记录索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='违规案例库-用于RAG检索';
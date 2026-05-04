-- 笔记审核记录表
-- 用于记录每次笔记发布的审核结果，支持三层审核体系

CREATE TABLE IF NOT EXISTS note_review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    note_id BIGINT COMMENT '笔记ID',
    user_id BIGINT COMMENT '发布者用户ID',
    title VARCHAR(200) COMMENT '笔记标题',
    content TEXT COMMENT '笔记内容',
    tags VARCHAR(500) COMMENT '标签(JSON数组)',
    images VARCHAR(2000) COMMENT '图片(JSON数组)',
    video VARCHAR(500) COMMENT '视频URL',
    location VARCHAR(200) COMMENT '地理位置',
    
    -- 审核结果
    review_status TINYINT DEFAULT 0 COMMENT '审核状态: 0-待审核 1-正常 2-疑似 3-违规',
    review_result VARCHAR(500) COMMENT '审核结果详情',
    violation_reason VARCHAR(500) COMMENT '违规原因',
    violation_tags VARCHAR(500) COMMENT '违规标签(JSON数组)',
    
    -- 审核过程数据
    sensitive_words_found VARCHAR(1000) COMMENT '敏感词检测结果(JSON)',
    similarity_cases TEXT COMMENT 'RAG检索相似案例(JSON)',
    llm_response TEXT COMMENT '大模型原始响应(JSON)',
    
    -- 审核层级
    layer_1_passed BOOLEAN DEFAULT TRUE COMMENT '第一层敏感词检测是否通过',
    layer_2_rag_score FLOAT COMMENT '第二层RAG相似度得分',
    layer_3_llm_verdict VARCHAR(50) COMMENT '第三层大模型判定结果',
    
    -- 时间
    review_time DATETIME COMMENT '审核时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引
    INDEX idx_note_id (note_id),
    INDEX idx_user_id (user_id),
    INDEX idx_review_status (review_status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记审核记录表';

-- 违规案例库（用于RAG检索）
CREATE TABLE IF NOT EXISTS violation_case_library (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    case_type VARCHAR(50) COMMENT '案例类型: political/pornographic/violence/advertising/discrimination',
    title VARCHAR(200) COMMENT '案例标题',
    content TEXT COMMENT '违规内容',
    violation_reason VARCHAR(500) COMMENT '违规原因',
    tags VARCHAR(500) COMMENT '违规标签(JSON)',
    embedding_id VARCHAR(100) COMMENT '向量ID(Milvus)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_case_type (case_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='违规案例库';

-- 预置审核规则表
CREATE TABLE IF NOT EXISTS review_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_type VARCHAR(50) COMMENT '规则类型: sensitive/rag/llm',
    rule_name VARCHAR(100) COMMENT '规则名称',
    rule_content TEXT COMMENT '规则内容',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    priority INT DEFAULT 0 COMMENT '优先级',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rule_type (rule_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核规则表';

-- 插入预置审核规则
INSERT INTO review_rule (rule_type, rule_name, rule_content, priority) VALUES
('llm', '社会主义核心价值观审核', '严格审核内容是否符合富强、民主、文明、和谐、自由、平等、公正、法治、爱国、敬业、诚信、友善', 100),
('llm', '政治敏感审核', '拒绝任何损害国家形象、歪曲历史、抹黑英雄的内容', 99),
('llm', '低俗色情审核', '拒绝任何色情低俗、暴力血腥内容', 98),
('llm', '诈骗广告审核', '拒绝任何诈骗钱财、虚假宣传、诱导消费内容', 97),
('llm', '网络暴力审核', '拒绝任何侮辱攻击、网暴他人、种族歧视内容', 96);
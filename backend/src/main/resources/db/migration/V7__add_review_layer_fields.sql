-- 笔记审核三层检测字段
-- 执行顺序: V7

-- 1. 第一层敏感词检测是否通过
ALTER TABLE note_review 
ADD COLUMN layer1_passed TINYINT DEFAULT 1 COMMENT '第一层敏感词检测是否通过: 0-未通过, 1-通过';

-- 2. 第二层RAG相似度得分
ALTER TABLE note_review 
ADD COLUMN layer2_rag_score DOUBLE COMMENT '第二层RAG相似度得分';

-- 3. 第三层大模型判定结果
ALTER TABLE note_review 
ADD COLUMN layer3_llm_verdict VARCHAR(50) COMMENT '第三层大模型判定结果: normal-正常, suspicious-疑似, violation-违规';
-- 用户信誉分字段 - 用于动态审核模式
-- 执行顺序: V10

ALTER TABLE user
ADD COLUMN reputation_score INT DEFAULT 50 COMMENT '信誉分: 0-100, >=阈值走同步审核, <阈值走异步审核';
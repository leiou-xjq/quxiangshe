package com.quxiangshe.backend.service;

/**
 * 用户信誉分服务接口
 *
 * 核心职责：管理用户信誉分，支持Redis缓存加速查询
 * 业务模块：用户模块
 *
 * 信誉分规则：
 *   - 初始值：50分（满分100）
 *   - 审核通过：+2分（单日上限+10）
 *   - 审核违规：-5分
 *   - 被举报核实：-10分
 *   - 恶意刷量：-20分
 *   - 低于0分：锁定账号
 *
 * 缓存策略：
 *   - Redis缓存TTL：1小时
 *   - DB为真相源，读取时优先查Redis
 *   - 更新时先写DB，异步更新Redis
 *
 * @author 趣享社技术团队
 */
public interface IReputationService {

    /**
     * 获取用户信誉分（优先查Redis缓存）
     *
     * @param userId 用户ID
     * @return 信誉分（0-100）
     */
    int getReputationScore(Long userId);

    /**
     * 增加信誉分（发布审核通过时调用）
     *
     * @param userId 用户ID
     * @param delta 增加分数（正数）
     */
    void increaseReputation(Long userId, int delta);

    /**
     * 减少信誉分（审核违规/被举报时调用）
     *
     * @param userId 用户ID
     * @param delta 减少分数（正数）
     * @param reason 原因
     */
    void decreaseReputation(Long userId, int delta, String reason);

    /**
     * 强制设置信誉分（管理员操作）
     *
     * @param userId 用户ID
     * @param score 目标分数
     */
    void setReputationScore(Long userId, int score);

    /**
     * 检查用户是否可走同步审核通道
     *
     * @param userId 用户ID
     * @return true=可走同步审核
     */
    boolean canSyncReview(Long userId);
}
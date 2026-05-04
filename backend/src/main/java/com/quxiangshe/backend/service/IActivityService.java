package com.quxiangshe.backend.service;

import com.quxiangshe.backend.entity.UserActivity;

/**
 * 用户活跃度服务接口
 * 
 * @author 趣享社技术团队
 */
public interface IActivityService {
    
    /**
     * 记录用户登录
     * 1. 判断是否新的一天登录
     * 2. 更新登录天数
     * 3. 计算活跃分数
     * 4. 同步到Redis缓存
     * 
     * @param userId 用户ID
     */
    void recordLogin(Long userId);
    
    /**
     * 记录用户互动（点赞、收藏、评论、关注）
     * 1. 检查今日互动次数限制
     * 2. 更新互动次数
     * 3. 计算活跃分数
     * 4. 同步到Redis缓存
     * 
     * @param userId 用户ID
     */
    void recordInteraction(Long userId);
    
    /**
     * 获取用户活跃分数
     * 优先从Redis获取，未命中从数据库获取
     * 
     * @param userId 用户ID
     * @return 活跃分数
     */
    double getActivityScore(Long userId);
    
    /**
     * 批量获取用户活跃分数
     * 
     * @param userIds 用户ID列表
     * @return 用户ID -> 活跃分数的映射
     */
    java.util.Map<Long, Double> getActivityScores(java.util.List<Long> userIds);
    
    /**
     * 同步Redis数据到数据库
     */
    void syncToDatabase();
    
    /**
     * 重置今日互动计数
     * 每天凌晨执行
     */
    void resetDailyInteractionCount();
    
    /**
     * 同步所有用户活跃度数据到Redis Sorted Set
     * 每小时执行一次
     */
    void syncActivityToRedis();
    
    /**
     * 更新指定作者的粉丝活跃度排名
     * @param authorId 作者ID
     */
    void updateFansActivityRank(Long authorId);
    
    /**
     * 增量更新用户活跃度分数（用户互动时触发）
     * 白天增量实时更新，互动触发 ZINCRBY
     * @param userId 用户ID
     * @param actionType 互动类型：1-点赞,2-收藏,3-评论,4-关注
     */
    void incrementActivityScore(Long userId, int actionType);
    
    /**
     * 每天0点执行：活跃度衰减80%，并重新计算粉丝分类
     */
    void decayActivityAndRecalculate();
    
    /**
     * 每小时执行：增量更新当日活跃用户的粉丝分类
     */
    void hourlySyncFansClassification();
}
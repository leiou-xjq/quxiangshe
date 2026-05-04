package com.quxiangshe.backend.service;

import com.quxiangshe.backend.vo.NoteVO;

import java.util.List;

/**
 * Feed流服务接口
 * 提供个性化信息流推荐功能，支持推模式、拉模式、推拉结合
 * 
 * @author 趣享社技术团队
 */
public interface IFeedService {
    
    /**
     * 获取用户Feed流
     * @param userId 用户ID
     * @param cursor 游标 (格式: timestamp_noteId)
     * @param size 每页数量
     * @return Feed列表
     */
    List<NoteVO> getFeed(Long userId, String cursor, int size);
    
    /**
     * 推送笔记到粉丝收件箱
     * @param noteId 笔记ID
     * @param authorId 作者ID
     */
    void pushNoteToFeed(Long noteId, Long authorId);
    
    /**
     * 获取博主粉丝数
     * @param userId 博主ID
     * @return 粉丝数
     */
    long getFollowerCount(Long userId);
    
    /**
     * 分批推送笔记到活跃粉丝收件箱（推拉结合模式）
     * @param noteId 笔记ID
     * @param authorId 作者ID
     * @param batchNum 当前批次号 (0开始)
     * @param totalBatches 总批次数
     */
    void pushNoteInBatch(Long noteId, Long authorId, int batchNum, int totalBatches);
    
    /**
     * 计算用户活跃度评分
     * @param userId 用户ID
     * @return 活跃度评分
     */
    double calculateActivityScore(Long userId);
    
    /**
     * 清除用户关注列表缓存
     * @param userId 用户ID
     */
    void evictFollowingCache(Long userId);
    
    /**
     * 清除用户粉丝数缓存
     * @param userId 用户ID
     */
    void evictFollowerCache(Long userId);
    
    /**
     * 清除用户Feed缓存
     * @param userId 用户ID
     */
    void evictUserFeedCache(Long userId);
    
    /**
     * 清除作者的所有粉丝缓存（发布新笔记时调用）
     * @param authorId 作者ID
     */
    void evictAllCachesByAuthor(Long authorId);
    
    /**
     * 查询关注Tab是否有更新（红点提示）
     * @param userId 用户ID
     * @return true=有更新, false=无更新
     */
    boolean hasFollowUpdate(Long userId);
    
    /**
     * 清除关注Tab更新标记（用户点击关注Tab时调用）
     * 使用分布式锁保证原子性
     * @param userId 用户ID
     */
    void clearFollowUpdate(Long userId);
    
    /**
     * 设置所有粉丝的关注Tab更新标记
     * 笔记发布时触发，异步批量设置
     * @param authorId 作者ID
     * @param noteId 笔记ID
     */
    void setFollowUpdateForFans(Long authorId, Long noteId);
}
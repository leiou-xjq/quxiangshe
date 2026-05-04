package com.quxiangshe.backend.service;

import com.quxiangshe.backend.entity.Blacklist;

import java.util.List;

/**
 * 黑名单服务接口
 * 
 * @author 趣享社技术团队
 */
public interface IBlacklistService {
    
    /**
     * 拉黑用户
     * @param userId 拉黑者ID
     * @param blockedId 被拉黑用户ID
     * @return 成功返回true
     */
    boolean blockUser(Long userId, Long blockedId);
    
    /**
     * 取消拉黑
     * @param userId 用户ID
     * @param blockedId 被拉黑用户ID
     * @return 成功返回true
     */
    boolean unblockUser(Long userId, Long blockedId);
    
    /**
     * 检查是否已拉黑
     * @param userId 用户ID
     * @param blockedId 被检查用户ID
     * @return 是否已拉黑
     */
    boolean isBlocked(Long userId, Long blockedId);
    
    /**
     * 获取用户的黑名单列表
     * @param userId 用户ID
     * @return 黑名单用户ID列表
     */
    List<Long> getBlockedUserIds(Long userId);
}
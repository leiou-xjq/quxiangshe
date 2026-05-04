package com.quxiangshe.backend.service;

import com.quxiangshe.backend.vo.PageVO;
import com.quxiangshe.backend.vo.UserVO;

import java.util.List;

/**
 * 关注服务接口
 * 
 * @author 趣享社技术团队
 */
public interface IFollowService {
    
    /**
     * 关注用户
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     * @return 成功返回true
     */
    boolean follow(Long followerId, Long followingId);
    
    /**
     * 取消关注
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     * @return 成功返回true
     */
    boolean unfollow(Long followerId, Long followingId);
    
    /**
     * 获取关注列表
     * @param userId 用户ID
     * @param cursor 游标（首页传null）
     * @param size 每页数量
     * @return 关注列表
     */
    PageVO<UserVO> getFollowingList(Long userId, String cursor, int size);
    
    /**
     * 获取关注列表（带当前用户关注状态）
     * @param userId 用户ID
     * @param cursor 游标
     * @param size 每页数量
     * @param currentUserId 当前登录用户ID
     * @return 关注列表
     */
    PageVO<UserVO> getFollowingList(Long userId, String cursor, int size, Long currentUserId);
    
    /**
     * 获取粉丝列表
     * @param userId 用户ID
     * @param cursor 游标（首页传null）
     * @param size 每页数量
     * @return 粉丝列表
     */
    PageVO<UserVO> getFollowersList(Long userId, String cursor, int size);
    
    /**
     * 获取粉丝列表（带当前用户关注状态）
     * @param userId 用户ID
     * @param cursor 游标（首页传null）
     * @param size 每页数量
     * @param currentUserId 当前登录用户ID（用于检查关注状态）
     * @return 粉丝列表
     */
    PageVO<UserVO> getFollowersList(Long userId, String cursor, int size, Long currentUserId);
    
    /**
     * 获取关注数
     * @param userId 用户ID
     * @return 关注数
     */
    long getFollowingCount(Long userId);
    
    /**
     * 获取粉丝数
     * @param userId 用户ID
     * @return 粉丝数
     */
    long getFollowersCount(Long userId);
    
    /**
     * 检查是否已关注
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     * @return 是否已关注
     */
    boolean isFollowing(Long followerId, Long followingId);
    
    /**
     * 获取所有粉丝ID列表（用于Feed推送）
     * @param userId 用户ID
     * @return 粉丝ID列表
     */
    List<Long> getFollowerIds(Long userId);
}
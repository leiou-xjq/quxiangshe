package com.quxiangshe.backend.service;

import com.quxiangshe.backend.dto.ChangePasswordRequest;
import com.quxiangshe.backend.dto.UpdateUserRequest;
import com.quxiangshe.backend.entity.User;
import com.quxiangshe.backend.vo.UserVO;

/**
 * 用户服务接口
 * 
 * @author 趣享社技术团队
 */
public interface IUserService {
    
    /**
     * 根据ID获取用户
     * @param id 用户ID
     * @return 用户信息
     */
    User getById(Long id);
    
    /**
     * 获取当前用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    UserVO getCurrentUser(Long userId);
    
    /**
     * 获取指定用户公开信息
     * @param id 用户ID
     * @return 用户公开信息
     */
    UserVO getUserInfo(Long id);
    
    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param request 更新请求
     * @return 更新后的用户信息
     */
    UserVO updateUser(Long userId, UpdateUserRequest request);
    
    /**
     * 修改密码
     * @param userId 用户ID
     * @param request 修改密码请求
     */
    void changePassword(Long userId, ChangePasswordRequest request);
    
    /**
     * 转换为VO
     * @param user 用户实体
     * @return 用户VO
     */
    UserVO convertToVO(User user);
}
package com.quxiangshe.backend.service.impl;

import com.quxiangshe.backend.dto.ChangePasswordRequest;
import com.quxiangshe.backend.dto.UpdateUserRequest;
import com.quxiangshe.backend.entity.User;
import com.quxiangshe.backend.exception.BusinessException;
import com.quxiangshe.backend.mapper.UserMapper;
import com.quxiangshe.backend.service.IUserService;
import com.quxiangshe.backend.service.ISearchService;
import com.quxiangshe.backend.util.PasswordUtil;
import com.quxiangshe.backend.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/**
 * 用户服务实现类
 *
 * <p>核心职责：
 * <ul>
 *   <li>用户信息查询（当前用户 / 公开信息，含敏感数据脱敏）</li>
 *   <li>用户资料更新（昵称、头像、性别、简介等），同步至搜索引擎</li>
 *   <li>密码修改（需验证原密码）</li>
 *   <li>VO 转换（完整用户信息 / 脱敏公开信息）</li>
 * </ul>
 *
 * <p>所属业务模块：用户信息管理
 *
 * @author 趣享社技术团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {
    
    private final UserMapper userMapper;
    private final ISearchService searchService;
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 根据ID查询用户实体
     *
     * @param id 用户ID
     * @return 用户实体，不存在时返回 null
     */
    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }
    
    /**
     * 获取当前登录用户的完整信息（含手机号、邮箱等敏感字段）
     *
     * @param userId 当前用户ID
     * @return 用户信息VO
     * @throws BusinessException 当用户不存在时
     */
    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return convertToVO(user);
    }
    
    /**
     * 获取用户公开信息（脱敏处理：手机号中间4位、邮箱前缀部分替换为星号）
     *
     * @param id 目标用户ID
     * @return 脱敏后的用户信息VO
     * @throws BusinessException 当用户不存在时
     */
    @Override
    public UserVO getUserInfo(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 返回公开信息（脱敏处理）
        return convertToPublicVO(user);
    }
    
    /**
     * 更新用户资料
     *
     * <p>仅更新请求中非 null 的字段。更新成功后同步到搜索引擎。
     *
     * @param userId  当前用户ID
     * @param request 用户更新请求（含需要修改的字段）
     * @return 更新后的用户信息VO
     * @throws BusinessException 当用户不存在时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUser(Long userId, UpdateUserRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        
        // 逐字段判断是否非null，仅更新客户端实际传入的字段
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getBirthday() != null) {
            user.setBirthday(request.getBirthday());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        
        userMapper.updateById(user);
        log.info("用户信息更新成功: userId={}", userId);
        
        // 同步到ES
        searchService.syncUser(userId);
        
        return convertToVO(user);
    }
    
    /**
     * 修改密码
     *
     * <p>需要验证原密码正确性，且新密码与确认密码一致。
     *
     * @param userId  当前用户ID
     * @param request 修改密码请求（含原密码、新密码、确认密码）
     * @throws BusinessException 当用户不存在、原密码错误或两次密码不一致时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        
        // 验证原密码
        if (!PasswordUtil.verify(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(1004001, "原密码错误");
        }
        
        // 验证新密码和确认密码
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(1004003, "两次密码不一致");
        }
        
        // 更新密码
        user.setPassword(PasswordUtil.encode(request.getNewPassword()));
        userMapper.updateById(user);
        
        log.info("密码修改成功: userId={}", userId);
    }
    
    /**
     * 将 User 实体转换为包含完整信息的 UserVO
     *
     * @param user 用户实体
     * @return UserVO，实体为 null 时返回 null
     */
    @Override
    public UserVO convertToVO(User user) {
        if (user == null) {
            return null;
        }
        
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setNickname(user.getNickname());
        vo.setGender(user.getGender());
        vo.setBirthday(user.getBirthday() != null ? user.getBirthday().toString() : null);
        vo.setBio(user.getBio());
        vo.setStatus(user.getStatus());
        vo.setLastLoginIp(user.getLastLoginIp());
        vo.setLastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().format(DATE_TIME_FORMATTER) : null);
        vo.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().format(DATE_TIME_FORMATTER) : null);
        vo.setUpdatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().format(DATE_TIME_FORMATTER) : null);
        
        return vo;
    }
    
    /**
     * 转换为公开用户信息VO（数据脱敏）
     *
     * <p>脱敏规则：
     * <ul>
     *   <li>手机号：11位手机号中间4位替换为星号（138****1234）</li>
     *   <li>邮箱：前缀超过2位时只保留前2位，其余替换为星号（ab***@domain.com）</li>
     * </ul>
     *
     * @param user 用户实体
     * @return 脱敏后的 UserVO
     */
    private UserVO convertToPublicVO(User user) {
        if (user == null) {
            return null;
        }
        
        UserVO vo = convertToVO(user);
        
        // 脱敏处理
        if (vo.getPhone() != null && vo.getPhone().length() == 11) {
            vo.setPhone(vo.getPhone().substring(0, 3) + "****" + vo.getPhone().substring(7));
        }
        if (vo.getEmail() != null && vo.getEmail().contains("@")) {
            String[] parts = vo.getEmail().split("@");
            if (parts[0].length() > 2) {
                vo.setEmail(parts[0].substring(0, 2) + "***@" + parts[1]);
            }
        }
        
        return vo;
    }
}
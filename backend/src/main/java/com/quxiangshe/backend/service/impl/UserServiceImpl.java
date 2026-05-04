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
 * @author 趣享社技术团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {
    
    private final UserMapper userMapper;
    private final ISearchService searchService;
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }
    
    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return convertToVO(user);
    }
    
    @Override
    public UserVO getUserInfo(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 返回公开信息（脱敏处理）
        return convertToPublicVO(user);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUser(Long userId, UpdateUserRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        
        // 更新字段
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
     * 转换为公开VO（脱敏处理）
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
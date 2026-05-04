package com.quxiangshe.user.service;

import com.quxiangshe.user.vo.UserProfileVO;

public interface UserService {

    UserProfileVO getCurrentUser(Long userId);

    UserProfileVO getUserProfile(Long userId, Long currentUserId);

    void follow(Long userId, Long targetUserId);

    void unfollow(Long userId, Long targetUserId);
}
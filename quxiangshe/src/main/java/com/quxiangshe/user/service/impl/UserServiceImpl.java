package com.quxiangshe.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quxiangshe.auth.entity.UserEntity;
import com.quxiangshe.common.exception.BusinessException;
import com.quxiangshe.common.util.JwtUtil;
import com.quxiangshe.user.mapper.UserMapper;
import com.quxiangshe.user.mapper.UserFollowMapper;
import com.quxiangshe.user.entity.UserFollowEntity;
import com.quxiangshe.user.service.UserService;
import com.quxiangshe.user.vo.UserProfileVO;
import com.quxiangshe.note.mapper.NoteMapper;
import com.quxiangshe.note.entity.NoteEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserFollowMapper userFollowMapper;
    private final NoteMapper noteMapper;

    @Override
    public UserProfileVO getCurrentUser(Long userId) {
        return getUserProfile(userId, userId);
    }

    @Override
    public UserProfileVO getUserProfile(Long userId, Long currentUserId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(1002, "用户不存在");
        }

        UserProfileVO vo = new UserProfileVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setBio(user.getBio());

        Long followCount = userFollowMapper.selectCount(
            new LambdaQueryWrapper<UserFollowEntity>()
                .eq(UserFollowEntity::getUserId, userId)
        );
        vo.setFollowCount(followCount.intValue());

        Long followerCount = userFollowMapper.selectCount(
            new LambdaQueryWrapper<UserFollowEntity>()
                .eq(UserFollowEntity::getFollowUserId, userId)
        );
        vo.setFollowerCount(followerCount.intValue());

        Long postCount = noteMapper.selectCount(
            new LambdaQueryWrapper<NoteEntity>()
                .eq(NoteEntity::getUserId, userId)
                .eq(NoteEntity::getStatus, NoteEntity.STATUS_NORMAL)
        );
        vo.setPostCount(postCount.intValue());

        if (currentUserId != null && !currentUserId.equals(userId)) {
            Long following = userFollowMapper.selectCount(
                new LambdaQueryWrapper<UserFollowEntity>()
                    .eq(UserFollowEntity::getUserId, currentUserId)
                    .eq(UserFollowEntity::getFollowUserId, userId)
            );
            vo.setIsFollowing(following > 0);

            Long followed = userFollowMapper.selectCount(
                new LambdaQueryWrapper<UserFollowEntity>()
                    .eq(UserFollowEntity::getUserId, userId)
                    .eq(UserFollowEntity::getFollowUserId, currentUserId)
            );
            vo.setIsFollowed(followed > 0);
        }

        return vo;
    }

    @Override
    public void follow(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BusinessException(2002, "不能关注自己");
        }

        UserEntity target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new BusinessException(1002, "用户不存在");
        }

        Long exists = userFollowMapper.selectCount(
            new LambdaQueryWrapper<UserFollowEntity>()
                .eq(UserFollowEntity::getUserId, userId)
                .eq(UserFollowEntity::getFollowUserId, targetUserId)
        );

        if (exists > 0) {
            return;
        }

        UserFollowEntity follow = new UserFollowEntity();
        follow.setUserId(userId);
        follow.setFollowUserId(targetUserId);
        userFollowMapper.insert(follow);
    }

    @Override
    public void unfollow(Long userId, Long targetUserId) {
        userFollowMapper.delete(
            new LambdaQueryWrapper<UserFollowEntity>()
                .eq(UserFollowEntity::getUserId, userId)
                .eq(UserFollowEntity::getFollowUserId, targetUserId)
        );
    }
}
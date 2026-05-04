package com.quxiangshe.user.vo;

import lombok.Data;

@Data
public class UserProfileVO {
    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private Integer followCount;
    private Integer followerCount;
    private Integer postCount;
    private Boolean isFollowing;
    private Boolean isFollowed;
}
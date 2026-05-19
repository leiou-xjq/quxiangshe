package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体类，对应数据库表 user。
 * <p>
 * 用户是系统的核心主体，支持多种登录方式（用户名/手机号/邮箱/微信OpenID）。
 * 采用BCrypt加密存储密码，支持三种角色（普通用户/审核员/管理员），
 * 包含完整的个人信息（头像/昵称/性别/生日/简介）及软删除机制。
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Data
@TableName("user")
public class User {
    
    /** 普通用户角色 */
    public static final String ROLE_USER = "USER";
    /** 审核员角色 */
    public static final String ROLE_MODERATOR = "MODERATOR";
    /** 管理员角色 */
    public static final String ROLE_ADMIN = "ADMIN";
    
    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户名，用于登录
     */
    private String username;
    
    /**
     * 手机号，用于登录
     */
    private String phone;
    
    /**
     * 邮箱，用于登录
     */
    private String email;
    
    /**
     * 密码，BCrypt加密存储
     */
    private String password;
    
    /**
     * 微信OpenID
     */
    private String wechatOpenId;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 性别：0-未知，1-男，2-女
     */
    private Integer gender;
    
    /**
     * 生日
     */
    private LocalDate birthday;
    
    /**
     * 个人简介
     */
    private String bio;
    
    /**
     * 状态：0-禁用，1-正常，2-待审核
     */
    private Integer status;

    /**
     * 信誉分：0-100，用于决定审核模式
     * >= syncReviewThreshold: 同步审核（快速通道）
     * < syncReviewThreshold: 异步审核（普通）
     */
    @TableField("reputation_score")
    private Integer reputationScore;

    /**
     * 角色：USER-普通用户, MODERATOR-审核员, ADMIN-管理员
     */
    private String role;
    
    /**
     * 最后登录IP地址
     */
    @TableField("last_login_ip")
    private String lastLoginIp;
    
    /**
     * 最后登录时间
     */
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    /**
     * 删除时间，用于软删除
     */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
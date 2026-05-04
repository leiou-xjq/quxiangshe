package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户会话实体类
 * 对应数据库表: user_session
 * 
 * @author 趣享社技术团队
 */
@Data
@TableName("user_session")
public class UserSession {
    
    /**
     * 会话ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 会话类型：access-访问令牌，refresh-刷新令牌
     */
    @TableField("session_type")
    private String sessionType;
    
    /**
     * Token值
     */
    private String token;
    
    /**
     * 设备信息
     */
    @TableField("device_info")
    private String deviceInfo;
    
    /**
     * IP地址
     */
    @TableField("ip_address")
    private String ipAddress;
    
    /**
     * 用户代理
     */
    @TableField("user_agent")
    private String userAgent;
    
    /**
     * 状态：0-无效，1-有效
     */
    private Integer status;
    
    /**
     * 过期时间
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;
    
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
}
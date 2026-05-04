package com.quxiangshe.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应DTO
 * 
 * 字段说明:
 * - accessToken: 访问令牌，用于API认证
 * - refreshToken: 刷新令牌，用于续期AccessToken
 * - expiresIn: AccessToken过期时间（秒）
 * - userId: 用户ID
 * - username: 用户名
 * - nickname: 昵称（可选）
 * - avatarUrl: 头像URL（可选）
 * 
 * 使用说明:
 * - accessToken: 有效期30分钟，放在请求头Authorization中
 * - refreshToken: 有效期7天，用于刷新AccessToken
 * - expiresIn: 1800秒（30分钟）
 * 
 * @author quxiangshe
 * @since 2024
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    /**
     * AccessToken
     * 用于访问需要认证的API
     * 格式: Bearer {token}
     */
    private String accessToken;

    /**
     * RefreshToken
     * 用于刷新AccessToken
     * 有效期7天
     */
    private String refreshToken;

    /**
     * AccessToken过期时间（秒）
     * 30分钟 = 1800秒
     */
    private Long expiresIn;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatarUrl;
}

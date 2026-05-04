package com.quxiangshe.auth.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 令牌刷新请求DTO
 * 
 * 字段说明:
 * - refreshToken: 刷新令牌
 * 
 * 验证规则:
 * - refreshToken: 不能为空
 * 
 * 使用说明:
 * 1. 登录成功后获得RefreshToken
 * 2. AccessToken过期前，使用RefreshToken换取新的AccessToken
 * 3. RefreshToken有效期7天
 * 4. 每次刷新都会验证Redis中的Token是否有效
 * 
 * @author quxiangshe
 * @since 2024
 */
@Data
public class RefreshTokenRequestDTO {

    /**
     * 刷新令牌
     * 7天有效期，存储在Redis中
     */
    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}

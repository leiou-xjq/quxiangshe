package com.quxiangshe.auth.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录请求DTO
 * 
 * 字段说明:
 * - username: 支持用户名、手机号、邮箱三种登录方式
 * - password: 用户密码
 * 
 * 验证规则:
 * - username: 不能为空
 * - password: 不能为空
 * 
 * @author quxiangshe
 * @since 2024
 */
@Data
public class LoginRequestDTO {

    /**
     * 用户名（支持用户名/手机号/邮箱）
     * 前端可传入任一登录标识，后端自动识别
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     * 明文传输，后端使用BCrypt验证
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}

package com.quxiangshe.auth.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求DTO
 * 
 * 字段说明:
 * - username: 登录账号，4-20字符
 * - password: 登录密码，6-20字符
 * - phone: 手机号（必填），11位数字
 * - email: 邮箱（必填）
 * - nickname: 昵称（可选），最长50字符
 * 
 * 验证规则:
 * - username: 4-20字符，不允许重复
 * - password: 6-20字符
 * - phone: 11位手机号（以1开头的2-9开头）
 * - email: 邮箱格式
 * - nickname: 可选，最长50字符
 * 
 * @author quxiangshe
 * @since 2024
 */
@Data
public class RegisterRequestDTO {

    /**
     * 用户名（登录账号）
     * 4-20字符，用于登录
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度4-20字符")
    private String username;

    /**
     * 密码
     * 6-20字符，BCrypt加密存储
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20字符")
    private String password;

    /**
     * 手机号
     * 11位数字，以1开头
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱
     * 邮箱格式
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 昵称
     * 可选，最长50字符
     * 为空时使用username作为昵称
     */
    @Size(max = 50, message = "昵称最长50字符")
    private String nickname;
}

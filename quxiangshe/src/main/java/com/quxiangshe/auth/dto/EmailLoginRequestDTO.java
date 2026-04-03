package com.quxiangshe.auth.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 邮箱登录请求DTO
 * 
 * 字段说明:
 * - email: 邮箱地址
 * - code: 6位数字验证码
 * 
 * 验证规则:
 * - email: 不能为空，邮箱格式
 * - code: 不能为空，6位数字
 * 
 * 使用流程:
 * 1. 先调用 /api/v1/auth/send-email-code 获取验证码
 * 2. 验证码有效期5分钟
 * 3. 使用邮箱+验证码登录
 * 
 * @author quxiangshe
 * @since 2024
 */
@Data
public class EmailLoginRequestDTO {

    /**
     * 邮箱
     * 邮箱格式
     */
    @NotBlank(message = "邮箱不能为空")
    private String email;

    /**
     * 验证码
     * 6位数字，从Redis获取
     */
    @NotBlank(message = "验证码不能为空")
    private String code;
}
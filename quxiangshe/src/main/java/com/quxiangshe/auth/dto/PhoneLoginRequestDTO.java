package com.quxiangshe.auth.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 手机号登录请求DTO
 * 
 * 字段说明:
 * - phone: 11位手机号
 * - code: 6位数字验证码
 * 
 * 验证规则:
 * - phone: 不能为空，11位手机号格式
 * - code: 不能为空，6位数字
 * 
 * 使用流程:
 * 1. 先调用 /api/v1/auth/send-code 获取验证码
 * 2. 验证码有效期5分钟
 * 3. 使用手机号+验证码登录
 * 
 * @author quxiangshe
 * @since 2024
 */
@Data
public class PhoneLoginRequestDTO {

    /**
     * 手机号
     * 11位数字，以1开头
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /**
     * 验证码
     * 6位数字，从Redis获取
     */
    @NotBlank(message = "验证码不能为空")
    private String code;
}
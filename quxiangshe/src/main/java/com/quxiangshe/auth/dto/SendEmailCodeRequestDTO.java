package com.quxiangshe.auth.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 发送邮箱验证码请求DTO
 * 
 * 字段说明:
 * - email: 邮箱地址
 * 
 * 验证规则:
 * - email: 不能为空，邮箱格式
 * 
 * 使用说明:
 * 1. 调用此接口获取验证码
 * 2. 验证码发送到邮箱（当前仅打印到日志）
 * 3. 验证码有效期5分钟
 * 4. 使用EmailLoginRequestDTO + 验证码登录
 * 
 * @author quxiangshe
 * @since 2024
 */
@Data
public class SendEmailCodeRequestDTO {

    /**
     * 邮箱
     * 邮箱格式
     */
    @NotBlank(message = "邮箱不能为空")
    private String email;
}
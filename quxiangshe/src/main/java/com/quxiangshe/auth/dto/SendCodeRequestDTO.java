package com.quxiangshe.auth.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 发送手机验证码请求DTO
 * 
 * 字段说明:
 * - phone: 11位手机号
 * 
 * 验证规则:
 * - phone: 不能为空，11位手机号格式
 * 
 * 使用说明:
 * 1. 调用此接口获取验证码
 * 2. 验证码发送到手机（当前仅打印到日志）
 * 3. 验证码有效期5分钟
 * 4. 使用PhoneLoginRequestDTO + 验证码登录
 * 
 * @author quxiangshe
 * @since 2024
 */
@Data
public class SendCodeRequestDTO {

    /**
     * 手机号
     * 11位数字，以1开头
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;
}
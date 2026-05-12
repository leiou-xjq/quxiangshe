package com.quxiangshe.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求DTO（统一登录接口）
 * 
 * loginType 两种模式：
 * - password: 密码登录，需要 username + password
 * - emailCode: 验证码登录，需要 email + emailCode
 * 
 * @author 趣享社技术团队
 */
@Data
@Schema(description = "用户登录请求")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginRequest {
    
    @Schema(description = "登录类型: password=密码登录, emailCode=验证码登录", example = "password")
    @NotBlank(message = "登录类型不能为空")
    private String loginType;
    
    @Schema(description = "用户名（密码登录时必填）", example = "zhangsan")
    private String username;
    
    @Schema(description = "邮箱（验证码登录时必填）", example = "test@example.com")
    private String email;
    
    @Schema(description = "密码（密码登录时必填）", example = "Aa123456")
    private String password;
    
    @Schema(description = "邮箱验证码（验证码登录时必填）", example = "123456")
    private String emailCode;
    
    @Schema(description = "图形验证码")
    private String captcha;
    
    @Schema(description = "图形验证码Key")
    private String captchaKey;
}
package com.quxiangshe.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求DTO
 * 
 * @author 趣享社技术团队
 */
@Data
@Schema(description = "用户登录请求")
public class LoginRequest {
    
    @Schema(description = "登录类型: username/phone/email", example = "username")
    @NotBlank(message = "登录类型不能为空")
    private String loginType;
    
    @Schema(description = "登录值(用户名/手机号/邮箱)", example = "zhangsan")
    @NotBlank(message = "登录值不能为空")
    private String loginValue;
    
    @Schema(description = "密码", example = "Aa123456")
    @NotBlank(message = "密码不能为空")
    private String password;
    
    @Schema(description = "图形验证码")
    private String captcha;
    
    @Schema(description = "图形验证码Key")
    private String captchaKey;
}
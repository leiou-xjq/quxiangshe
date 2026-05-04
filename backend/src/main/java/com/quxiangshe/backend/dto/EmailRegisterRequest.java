package com.quxiangshe.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 邮箱验证码注册请求DTO
 *
 * @author 趣享社技术团队
 */
@Data
@Schema(description = "邮箱验证码注册请求")
public class EmailRegisterRequest {

    @Schema(description = "邮箱", example = "test@example.com")
    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$", message = "邮箱格式不正确")
    private String email;

    @Schema(description = "验证码", example = "123456")
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须是6位数字")
    private String code;

    @Schema(description = "密码", example = "Aa123456")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "昵称", example = "趣享用户")
    @NotBlank(message = "昵称不能为空")
    private String nickname;
}
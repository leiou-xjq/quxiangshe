package com.quxiangshe.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 发送验证码请求DTO
 *
 * @author 趣享社技术团队
 */
@Data
@Schema(description = "发送验证码请求")
public class SendCodeRequest {

    @Schema(description = "邮箱", example = "test@example.com")
    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$", message = "邮箱格式不正确")
    private String email;
}
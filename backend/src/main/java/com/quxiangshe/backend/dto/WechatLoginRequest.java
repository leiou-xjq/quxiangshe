package com.quxiangshe.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信登录请求DTO
 * 
 * @author 趣享社技术团队
 */
@Data
@Schema(description = "微信登录请求")
public class WechatLoginRequest {
    
    @Schema(description = "微信授权code", example = "微信授权返回的code")
    @NotBlank(message = "授权code不能为空")
    private String code;
    
    @Schema(description = "昵称（首次登录时需要）", example = "微信用户")
    private String nickname;
    
    @Schema(description = "头像URL", example = "https://xxx.jpg")
    private String avatar;
}

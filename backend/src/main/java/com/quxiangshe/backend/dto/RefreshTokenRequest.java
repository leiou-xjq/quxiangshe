package com.quxiangshe.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Token刷新请求DTO
 * 
 * @author 趣享社技术团队
 */
@Data
@Schema(description = "Token刷新请求")
public class RefreshTokenRequest {
    
    @Schema(description = "刷新Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @NotBlank(message = "刷新Token不能为空")
    private String refreshToken;
}
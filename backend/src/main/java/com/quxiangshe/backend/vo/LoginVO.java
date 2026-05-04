package com.quxiangshe.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录响应VO
 * 
 * @author 趣享社技术团队
 */
@Data
@Schema(description = "登录响应")
public class LoginVO {
    
    @Schema(description = "访问Token")
    private String accessToken;
    
    @Schema(description = "刷新Token")
    private String refreshToken;
    
    @Schema(description = "Token类型")
    private String tokenType;
    
    @Schema(description = "Token过期时间(秒)")
    private Integer expiresIn;
    
    @Schema(description = "用户信息")
    private UserVO user;
}
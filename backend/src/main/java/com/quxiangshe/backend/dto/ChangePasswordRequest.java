package com.quxiangshe.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求DTO
 * 
 * @author 趣享社技术团队
 */
@Data
@Schema(description = "修改密码请求")
public class ChangePasswordRequest {
    
    @Schema(description = "原密码", example = "Aa123456")
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;
    
    @Schema(description = "新密码", example = "Bb123456")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度必须为6-20位")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{6,20}$", 
             message = "新密码必须包含大小写字母和数字")
    private String newPassword;
    
    @Schema(description = "确认新密码", example = "Bb123456")
    @NotBlank(message = "确认新密码不能为空")
    private String confirmPassword;
}
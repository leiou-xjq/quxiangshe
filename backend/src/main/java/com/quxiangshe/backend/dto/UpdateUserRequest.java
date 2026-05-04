package com.quxiangshe.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 更新用户信息请求DTO
 * 
 * @author 趣享社技术团队
 */
@Data
@Schema(description = "更新用户信息请求")
public class UpdateUserRequest {
    
    @Schema(description = "昵称", example = "张三")
    @Size(min = 2, max = 20, message = "昵称长度必须为2-20位")
    private String nickname;
    
    @Schema(description = "头像URL")
    private String avatar;
    
    @Schema(description = "性别: 0-未知, 1-男, 2-女", example = "1")
    private Integer gender;
    
    @Schema(description = "生日", example = "1995-06-15")
    private LocalDate birthday;
    
    @Schema(description = "个人简介")
    @Size(max = 255, message = "个人简介不能超过255个字符")
    private String bio;
    
    @Schema(description = "邮箱")
    private String email;
    
    @Schema(description = "电话")
    private String phone;
}
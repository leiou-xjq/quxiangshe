package com.quxiangshe.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户信息VO
 * 
 * @author 趣享社技术团队
 */
@Data
@Schema(description = "用户信息")
public class UserVO {
    
    @Schema(description = "用户ID")
    private Long id;
    
    @Schema(description = "用户名")
    private String username;
    
    @Schema(description = "手机号")
    private String phone;
    
    @Schema(description = "邮箱")
    private String email;
    
    @Schema(description = "头像URL")
    private String avatar;
    
    @Schema(description = "昵称")
    private String nickname;
    
    @Schema(description = "性别: 0-未知, 1-男, 2-女")
    private Integer gender;
    
    @Schema(description = "生日")
    private String birthday;
    
    @Schema(description = "个人简介")
    private String bio;
    
    @Schema(description = "状态: 0-禁用, 1-正常, 2-待审核")
    private Integer status;
    
    @Schema(description = "最后登录IP")
    private String lastLoginIp;
    
    @Schema(description = "最后登录时间")
    private String lastLoginAt;
    
    @Schema(description = "创建时间")
    private String createdAt;
    
    @Schema(description = "更新时间")
    private String updatedAt;
    
    @Schema(description = "是否已关注当前用户")
    private Boolean isFollowing;
    
    @Schema(description = "是否有更多数据")
    private Boolean hasMore;
    
    @Schema(description = "下一页游标")
    private String nextCursor;
}
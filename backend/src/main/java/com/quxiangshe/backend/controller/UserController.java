package com.quxiangshe.backend.controller;

import com.quxiangshe.backend.common.R;
import com.quxiangshe.backend.dto.ChangePasswordRequest;
import com.quxiangshe.backend.dto.UpdateUserRequest;
import com.quxiangshe.backend.service.IUserService;
import com.quxiangshe.backend.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * <p>提供用户信息查询、修改个人信息、修改密码等接口。
 * 所有接口需要JWT认证，userId从请求Attribute中提取（由JwtAuthenticationFilter注入）。</p>
 * 
 * @author 趣享社技术团队
 */
@Tag(name = "用户管理", description = "用户信息管理接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    
    private final IUserService userService;
    
    /**
     * 获取当前登录用户的个人信息
     * 
     * @param request HTTP请求（携带JWT认证后的userId属性）
     * @return 当前用户信息VO
     */
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public R<UserVO> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserVO user = userService.getCurrentUser(userId);
        return R.ok(user);
    }
    
    /**
     * 更新当前登录用户的个人信息
     * 
     * @param request    更新请求体（含昵称、头像、简介等）
     * @param httpRequest HTTP请求
     * @return 更新后的用户信息VO
     */
    @Operation(summary = "更新当前用户信息")
    @PutMapping("/me")
    public R<UserVO> updateCurrentUser(@Valid @RequestBody UpdateUserRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        UserVO user = userService.updateUser(userId, request);
        return R.ok("更新成功", user);
    }
    
    /**
     * 修改当前登录用户的密码
     * 需提供原密码进行校验。
     * 
     * @param request    修改密码请求（含原密码和新密码）
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @Operation(summary = "修改密码")
    @PutMapping("/me/password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        userService.changePassword(userId, request);
        return R.ok("密码修改成功", null);
    }
    
    /**
     * 获取指定用户的公开信息
     * 无需登录即可查看。
     * 
     * @param id      目标用户ID
     * @param request HTTP请求
     * @return 目标用户信息VO
     */
    @Operation(summary = "获取指定用户信息")
    @GetMapping("/{id}")
    public R<UserVO> getUserInfo(
            @Parameter(description = "用户ID") @PathVariable Long id,
            HttpServletRequest request) {
        UserVO user = userService.getUserInfo(id);
        return R.ok(user);
    }
}
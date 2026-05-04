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
 * 
 * @author 趣享社技术团队
 */
@Tag(name = "用户管理", description = "用户信息管理接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    
    private final IUserService userService;
    
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public R<UserVO> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserVO user = userService.getCurrentUser(userId);
        return R.ok(user);
    }
    
    @Operation(summary = "更新当前用户信息")
    @PutMapping("/me")
    public R<UserVO> updateCurrentUser(@Valid @RequestBody UpdateUserRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        UserVO user = userService.updateUser(userId, request);
        return R.ok("更新成功", user);
    }
    
    @Operation(summary = "修改密码")
    @PutMapping("/me/password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        userService.changePassword(userId, request);
        return R.ok("密码修改成功", null);
    }
    
    @Operation(summary = "获取指定用户信息")
    @GetMapping("/{id}")
    public R<UserVO> getUserInfo(
            @Parameter(description = "用户ID") @PathVariable Long id,
            HttpServletRequest request) {
        UserVO user = userService.getUserInfo(id);
        return R.ok(user);
    }
}
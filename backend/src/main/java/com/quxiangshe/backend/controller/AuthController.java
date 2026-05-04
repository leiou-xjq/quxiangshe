package com.quxiangshe.backend.controller;

import com.quxiangshe.backend.annotation.RateLimit;
import com.quxiangshe.backend.common.R;
import com.quxiangshe.backend.dto.*;
import com.quxiangshe.backend.service.IAuthService;
import com.quxiangshe.backend.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 集成Redis限流，使用@RateLimit注解统一处理
 * 
 * @author 趣享社技术团队
 */
@Tag(name = "认证管理", description = "用户注册、登录、登出等认证接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final IAuthService authService;
    
    /**
     * 用户注册 - 滑动窗口限流
     * 60秒内最多允许3次请求
     */
    @Operation(summary = "用户注册")
    @RateLimit(
        key = "register",
        maxRequests = 3,
        windowSeconds = 60,
        type = RateLimit.LimitType.SLIDING_WINDOW,
        message = "注册请求过于频繁，请稍后再试"
    )
    @PostMapping("/register")
    public R<LoginVO> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        LoginVO result = authService.register(request, ipAddress);
        return R.ok("注册成功", result);
    }
    
    /**
     * 用户登录 - 固定窗口限流
     * 1分钟内最多允许5次登录失败
     */
    @Operation(summary = "用户登录")
    @RateLimit(
        key = "login",
        maxRequests = 5,
        windowSeconds = 60,
        type = RateLimit.LimitType.FIXED_WINDOW,
        message = "登录尝试过于频繁，请稍后再试"
    )
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        LoginVO result = authService.login(request, ipAddress);
        return R.ok("登录成功", result);
    }
    
    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId != null) {
            authService.logout(userId);
        }
        return R.ok("登出成功", null);
    }
    
    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public R<LoginVO> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        LoginVO result = authService.refreshToken(request.getRefreshToken(), ipAddress);
        return R.ok("刷新成功", result);
    }
    
    /**
     * 发送验证码 - 固定窗口限流
     * 60秒内最多允许1次请求
     */
    @Operation(summary = "发送验证码")
    @RateLimit(
        key = "send-code",
        maxRequests = 1,
        windowSeconds = 60,
        type = RateLimit.LimitType.FIXED_WINDOW,
        message = "发送验证码过于频繁，请稍后再试"
    )
    @PostMapping("/send-code")
    public R<Void> sendVerifyCode(@Valid @RequestBody SendCodeRequest request) {
        authService.sendVerifyCode(request.getEmail());
        return R.ok("验证码发送成功", null);
    }
    
    /**
     * 邮箱验证码登录
     */
    @Operation(summary = "邮箱验证码登录")
    @RateLimit(
        key = "email-login",
        maxRequests = 10,
        windowSeconds = 60,
        type = RateLimit.LimitType.FIXED_WINDOW,
        message = "登录尝试过于频繁，请稍后再试"
    )
    @PostMapping("/email-login")
    public R<LoginVO> emailLogin(@Valid @RequestBody EmailLoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        LoginVO result = authService.emailLogin(request.getEmail(), request.getCode(), ipAddress);
        return R.ok("登录成功", result);
    }
    
    /**
     * 邮箱验证码注册
     */
    @Operation(summary = "邮箱验证码注册")
    @RateLimit(
        key = "email-register",
        maxRequests = 3,
        windowSeconds = 60,
        type = RateLimit.LimitType.FIXED_WINDOW,
        message = "注册请求过于频繁，请稍后再试"
    )
    @PostMapping("/email-register")
    public R<LoginVO> emailRegister(@Valid @RequestBody EmailRegisterRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        LoginVO result = authService.emailRegister(request, ipAddress);
        return R.ok("注册成功", result);
    }
    
    /**
     * 发送重置密码验证码
     */
    @Operation(summary = "发送重置密码验证码")
    @RateLimit(key = "reset-code", maxRequests = 3, windowSeconds = 60)
    @PostMapping("/reset-code")
    public R<String> sendResetCode(@Valid @RequestBody SendCodeRequest request) {
        authService.sendResetCode(request.getEmail());
        return R.ok("验证码发送成功", null);
    }
    
    /**
     * 重置密码
     */
    @Operation(summary = "重置密码")
    @PostMapping("/reset-password")
    public R<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return R.ok("密码重置成功", null);
    }
    
    /**
     * 微信登录
     */
    @Operation(summary = "微信登录")
    @PostMapping("/wechat-login")
    public R<LoginVO> wechatLogin(@Valid @RequestBody WechatLoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        LoginVO result = authService.wechatLogin(request, ipAddress);
        return R.ok("登录成功", result);
    }
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        try {
            Object userId = request.getAttribute("userId");
            return userId != null ? (Long) userId : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

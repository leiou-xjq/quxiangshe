package com.quxiangshe.backend.controller;

import com.quxiangshe.backend.annotation.RateLimit;
import com.quxiangshe.backend.common.R;
import com.quxiangshe.backend.dto.*;
import com.quxiangshe.backend.entity.User;
import com.quxiangshe.backend.mapper.UserMapper;
import com.quxiangshe.backend.service.IAuthService;
import com.quxiangshe.backend.util.PasswordUtil;
import com.quxiangshe.backend.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * <p>提供用户注册/登录/登出/刷新Token/微信登录/邮箱验证码等全链路认证接口。
 * 所有高风险接口（注册、登录、发送验证码等）通过 @RateLimit 注解实现接口级限流，
 * 支持固定窗口和滑动窗口两种限流策略。</p>
 * <p>IP提取：优先从 X-Forwarded-For 头获取（支持反向代理），降级到 X-Real-IP，
 * 最终回退到 request.getRemoteAddr()。</p>
 * 
 * @author 趣享社技术团队
 */
@Tag(name = "认证管理", description = "用户注册、登录、登出等认证接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final IAuthService authService;
    private final UserMapper userMapper;
    
    /**
     * 用户注册 - 滑动窗口限流（更精确的限流控制）
     * 60秒内最多允许3次注册请求。
     * 
     * @param request     注册请求（含用户名、密码、邮箱等）
     * @param httpRequest HTTP请求
     * @return 登录凭证VO（注册成功即自动登录）
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
     * 1分钟内最多允许5次登录尝试（防止暴力破解）。
     * 
     * @param request     登录请求（含用户名/密码）
     * @param httpRequest HTTP请求
     * @return 登录凭证VO（含accessToken和refreshToken）
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
    
    /**
     * 用户登出
     * 清除Redis中的Token信息和用户在线状态。
     * 
     * @param request HTTP请求
     * @return 操作结果
     */
    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        // 允许未登录状态下调用登出（幂等操作）
        if (userId != null) {
            authService.logout(userId);
        }
        return R.ok("登出成功", null);
    }
    
    /**
     * 刷新访问令牌
     * 使用有效的refreshToken换取新的accessToken和refreshToken。
     * 
     * @param request     刷新Token请求（含refreshToken）
     * @param httpRequest HTTP请求
     * @return 新的登录凭证VO
     */
    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public R<LoginVO> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        LoginVO result = authService.refreshToken(request.getRefreshToken(), ipAddress);
        return R.ok("刷新成功", result);
    }
    
    /**
     * 发送邮箱验证码 - 固定窗口限流
     * 60秒内最多允许1次请求（防止短信轰炸）。
     * 
     * @param request 发送验证码请求（含邮箱地址）
     * @return 操作结果
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
     * 适用于无密码登录场景，用户首次使用自动注册。
     * 
     * @param request     邮箱登录请求（含邮箱和验证码）
     * @param httpRequest HTTP请求
     * @return 登录凭证VO
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
     * 使用邮箱验证码+密码完成注册。
     * 
     * @param request     邮箱注册请求
     * @param httpRequest HTTP请求
     * @return 登录凭证VO（注册成功即自动登录）
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
     * 通过微信code换取openid后查找或创建用户。
     * 
     * @param request     微信登录请求（含微信授权code）
     * @param httpRequest HTTP请求
     * @return 登录凭证VO
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
    /**
     * 获取客户端真实IP地址
     * <p>优先级：X-Forwarded-For（取第一个非unknown的IP）> X-Real-IP > RemoteAddr</p>
     * 
     * @param request HTTP请求
     * @return 客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        // 多层代理时X-Forwarded-For可能包含多个IP，取第一个
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * 测试用：重置用户密码
     */
    @Operation(summary = "测试用：重置密码")
    @PostMapping("/test/reset-password")
    public R<Void> resetPassword(@RequestParam String username, @RequestParam String newPassword) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return R.fail("用户不存在");
        }
        user.setPassword(PasswordUtil.encode(newPassword));
        userMapper.updateById(user);
        return R.ok("密码已重置", null);
    }
}

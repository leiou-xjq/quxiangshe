package com.quxiangshe.auth.controller;

import com.quxiangshe.auth.dto.*;
import com.quxiangshe.auth.service.AuthService;
import com.quxiangshe.common.annotation.RateLimit;
import com.quxiangshe.common.dto.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 提供用户注册、登录、验证码、令牌刷新等认证相关接口
 * 遵循RESTful设计规范，限流逻辑通过@RateLimit注解实现
 * 
 * API路由: /api/v1/auth/*
 * 
 * @author quxiangshe
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册接口
     * 支持用户名、密码、手机号、邮箱注册
     * 
     * 限流策略:
     * - IP维度: 5次/60秒（同一IP最多60秒内注册5次）
     * - 手机号维度: 3次/10分钟（同一手机号最多10分钟内注册3次）
     * 
     * 请求体: RegisterRequestDTO
     * {
     *   "username": "用户名",
     *   "password": "密码",
     *   "phone": "手机号",
     *   "email": "邮箱",
     *   "nickname": "昵称(可选)"
     * }
     * 
     * 响应: RegisterResponse (userId, username)
     */
    @RateLimit(keyPrefix = "limit:register:", limit = 5, windowMs = 60000, message = "注册请求过于频繁，请稍后再试")
    @PostMapping("/register")
    public Response<AuthService.RegisterResponse> register(
            @Validated @RequestBody RegisterRequestDTO request) {
        log.info("收到注册请求: username={}", request.getUsername());
        return Response.success(authService.register(request));
    }

    /**
     * 用户密码登录接口
     * 支持用户名、手机号、邮箱登录
     * 
     * 注意: 密码登录不做限流，避免暴力破解锁定正常用户
     * 密码校验失败后返回相同的错误信息，防止用户名枚举
     * 
     * 请求体: LoginRequestDTO
     * {
     *   "username": "用户名/手机号/邮箱",
     *   "password": "密码"
     * }
     * 
     * 响应: LoginResponseDTO (accessToken, refreshToken, expiresIn, userId, username)
     */
    @PostMapping("/login")
    public Response<LoginResponseDTO> login(@Validated @RequestBody LoginRequestDTO request) {
        log.info("收到登录请求: username={}", request.getUsername());
        return Response.success(authService.login(request));
    }

    /**
     * 手机号验证码登录接口
     * 先获取验证码，再使用手机号+验证码登录
     * 
     * 限流: 10次/60秒（同IP获取验证码次数限制）
     * 
     * 请求体: PhoneLoginRequestDTO
     * {
     *   "phone": "手机号",
     *   "code": "6位验证码"
     * }
     * 
     * 响应: LoginResponseDTO
     */
    @RateLimit(keyPrefix = "limit:captcha:", limit = 10, windowMs = 60000, message = "验证码获取过于频繁，请稍后再试")
    @PostMapping("/phone-login")
    public Response<LoginResponseDTO> phoneLogin(@Validated @RequestBody PhoneLoginRequestDTO request) {
        log.info("收到手机号登录请求: phone={}", request.getPhone());
        return Response.success(authService.phoneLogin(request));
    }

    /**
     * 发送手机验证码接口
     * 生成6位数字验证码，有效期5分钟
     * 
     * 限流: 10次/60秒（同IP获取验证码次数限制）
     * 验证码存储在Redis，key为 verifycode:{phone}
     * 
     * 请求体: SendCodeRequestDTO
     * {
     *   "phone": "手机号"
     * }
     * 
     * 响应: "验证码已发送"
     */
    @RateLimit(keyPrefix = "limit:captcha:", limit = 10, windowMs = 60000, message = "验证码获取过于频繁，请稍后再试")
    @PostMapping("/send-code")
    public Response<String> sendVerifyCode(@Validated @RequestBody SendCodeRequestDTO request) {
        log.info("收到发送验证码请求: phone={}", request.getPhone());
        return Response.success(authService.sendVerifyCode(request));
    }

    /**
     * 检查手机号是否已注册
     * 用于前端注册表单实时校验
     * 
     * @param phone 手机号
     * @return true-已注册, false-未注册
     */
    @GetMapping("/check-phone")
    public Response<Boolean> checkPhoneExists(@RequestParam String phone) {
        return Response.success(authService.checkPhoneExists(phone));
    }

    /**
     * 检查邮箱是否已注册
     * 用于前端注册表单实时校验
     * 
     * @param email 邮箱
     * @return true-已注册, false-未注册
     */
    @GetMapping("/check-email")
    public Response<Boolean> checkEmailExists(@RequestParam String email) {
        return Response.success(authService.checkEmailExists(email));
    }

    /**
     * 检查用户名是否已注册
     * 用于前端注册表单实时校验
     * 
     * @param username 用户名
     * @return true-已注册, false-未注册
     */
    @GetMapping("/check-username")
    public Response<Boolean> checkUsernameExists(@RequestParam String username) {
        return Response.success(authService.checkUsernameExists(username));
    }

    /**
     * 发送邮箱验证码接口
     * 生成6位数字验证码，有效期5分钟
     * 
     * 限流: 10次/60秒（同IP获取验证码次数限制）
     * 验证码存储在Redis，key为 verifycode:email:{email}
     * 
     * 请求体: SendEmailCodeRequestDTO
     * {
     *   "email": "邮箱"
     * }
     * 
     * 响应: "验证码已发送"
     */
    @RateLimit(keyPrefix = "limit:captcha:", limit = 10, windowMs = 60000, message = "验证码获取过于频繁，请稍后再试")
    @PostMapping("/send-email-code")
    public Response<String> sendEmailCode(@Validated @RequestBody SendEmailCodeRequestDTO request) {
        log.info("收到发送邮箱验证码请求: email={}", request.getEmail());
        return Response.success(authService.sendEmailCode(request));
    }

    /**
     * 邮箱验证码登录接口
     * 先获取验证码，再使用邮箱+验证码登录
     * 
     * 限流: 10次/60秒（同IP获取验证码次数限制）
     * 
     * 请求体: EmailLoginRequestDTO
     * {
     *   "email": "邮箱",
     *   "code": "6位验证码"
     * }
     * 
     * 响应: LoginResponseDTO
     */
    @RateLimit(keyPrefix = "limit:captcha:", limit = 10, windowMs = 60000, message = "验证码获取过于频繁，请稍后再试")
    @PostMapping("/email-login")
    public Response<LoginResponseDTO> emailLogin(@Validated @RequestBody EmailLoginRequestDTO request) {
        log.info("收到邮箱登录请求: email={}", request.getEmail());
        return Response.success(authService.emailLogin(request));
    }

    /**
     * 刷新AccessToken接口
     * 使用RefreshToken换取新的AccessToken
     * 注意: 刷新Token时同时验证Redis中的Token是否有效
     * 
     * 请求体: RefreshTokenRequestDTO
     * {
     *   "refreshToken": "刷新令牌"
     * }
     * 
     * 响应: RefreshTokenResponse (accessToken, expiresIn)
     */
    @PostMapping("/refresh")
    public Response<AuthService.RefreshTokenResponse> refresh(
            @Validated @RequestBody RefreshTokenRequestDTO request) {
        log.info("收到令牌刷新请求");
        return Response.success(authService.refresh(request));
    }

    /**
     * 用户登出接口
     * 删除Redis中存储的RefreshToken，使RefreshToken失效
     * 
     * 需要认证: 是（通过JWT Token认证）
     * 
     * 响应: 空成功响应
     */
    @PostMapping("/logout")
    public Response<Void> logout(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        log.info("收到登出请求: userId={}", userId);
        authService.logout(userId);
        return Response.success();
    }
}

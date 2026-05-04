package com.quxiangshe.backend.service;

import com.quxiangshe.backend.dto.*;
import com.quxiangshe.backend.vo.LoginVO;

/**
 * 认证服务接口
 *
 * @author 趣享社技术团队
 */
public interface IAuthService {

    /**
     * 用户注册
     * @param request 注册请求
     * @param ipAddress IP地址
     * @return 登录响应
     */
    LoginVO register(RegisterRequest request, String ipAddress);

    /**
     * 用户登录
     * @param request 登录请求
     * @param ipAddress IP地址
     * @return 登录响应
     */
    LoginVO login(LoginRequest request, String ipAddress);

    /**
     * 用户退出登录
     * @param userId 用户ID
     */
    void logout(Long userId);

    /**
     * 刷新Token
     * @param refreshToken 刷新Token
     * @param ipAddress IP地址
     * @return 新的登录响应
     */
    LoginVO refreshToken(String refreshToken, String ipAddress);

    /**
     * 发送验证码到邮箱
     * @param email 邮箱
     */
    void sendVerifyCode(String email);

    /**
     * 邮箱验证码登录
     * @param email 邮箱
     * @param code 验证码
     * @param ipAddress IP地址
     * @return 登录响应
     */
    LoginVO emailLogin(String email, String code, String ipAddress);

    /**
     * 邮箱验证码注册
     * @param request 注册请求
     * @param ipAddress IP地址
     * @return 登录响应
     */
    LoginVO emailRegister(EmailRegisterRequest request, String ipAddress);

    /**
     * 发送重置密码验证码到邮箱
     * @param email 邮箱
     */
    void sendResetCode(String email);

    /**
     * 重置密码
     * @param request 重置请求
     */
    void resetPassword(ResetPasswordRequest request);

    /**
     * 微信登录
     * @param request 微信登录请求
     * @param ipAddress IP地址
     * @return 登录响应
     */
    LoginVO wechatLogin(WechatLoginRequest request, String ipAddress);
}
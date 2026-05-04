package com.quxiangshe.auth.service;

import com.quxiangshe.auth.dto.EmailLoginRequestDTO;
import com.quxiangshe.auth.dto.LoginRequestDTO;
import com.quxiangshe.auth.dto.LoginResponseDTO;
import com.quxiangshe.auth.dto.PhoneLoginRequestDTO;
import com.quxiangshe.auth.dto.RefreshTokenRequestDTO;
import com.quxiangshe.auth.dto.RegisterRequestDTO;
import com.quxiangshe.auth.dto.SendCodeRequestDTO;
import com.quxiangshe.auth.dto.SendEmailCodeRequestDTO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 用户ID和用户名
     */
    RegisterResponse register(RegisterRequestDTO request);

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应（包含Token）
     */
    LoginResponseDTO login(LoginRequestDTO request);

    /**
     * 手机号登录
     *
     * @param request 手机号登录请求
     * @return 登录响应（包含Token）
     */
    LoginResponseDTO phoneLogin(PhoneLoginRequestDTO request);

    /**
     * 邮箱登录
     *
     * @param request 邮箱登录请求
     * @return 登录响应（包含Token）
     */
    LoginResponseDTO emailLogin(EmailLoginRequestDTO request);

    /**
     * 发送手机验证码
     *
     * @param request 手机号请求
     * @return 发送结果
     */
    String sendVerifyCode(SendCodeRequestDTO request);

    /**
     * 发送邮箱验证码
     *
     * @param request 邮箱请求
     * @return 发送结果
     */
    String sendEmailCode(SendEmailCodeRequestDTO request);

    /**
     * 检查手机号是否已注册
     *
     * @param phone 手机号
     * @return true表示已注册
     */
    boolean checkPhoneExists(String phone);

    /**
     * 检查邮箱是否已注册
     *
     * @param email 邮箱
     * @return true表示已注册
     */
    boolean checkEmailExists(String email);

    /**
     * 检查用户名是否已注册
     *
     * @param username 用户名
     * @return true表示已注册
     */
    boolean checkUsernameExists(String username);

    /**
     * 令牌刷新
     *
     * @param request 刷新请求
     * @return 新的AccessToken
     */
    RefreshTokenResponse refresh(RefreshTokenRequestDTO request);

    /**
     * 登出
     *
     * @param userId 用户ID
     */
    void logout(Long userId);

    /**
     * 注册响应
     */
    class RegisterResponse {
        private Long userId;
        private String username;

        public RegisterResponse(Long userId, String username) {
            this.userId = userId;
            this.username = username;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }
    }

    /**
     * 刷新令牌响应
     */
    class RefreshTokenResponse {
        private String accessToken;
        private Long expiresIn;

        public RefreshTokenResponse(String accessToken, Long expiresIn) {
            this.accessToken = accessToken;
            this.expiresIn = expiresIn;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public Long getExpiresIn() {
            return expiresIn;
        }
    }
}

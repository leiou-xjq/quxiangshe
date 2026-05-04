package com.quxiangshe.backend.service;

/**
 * 邮件服务接口
 *
 * @author 趣享社技术团队
 */
public interface IEmailService {

    /**
     * 发送验证码到邮箱
     *
     * @param email 目标邮箱
     * @return 是否发送成功
     */
    boolean sendVerifyCode(String email);

    /**
     * 验证验证码是否正确
     *
     * @param email 邮箱
     * @param code 验证码
     * @return 是否正确
     */
    boolean verifyCode(String email, String code);
}
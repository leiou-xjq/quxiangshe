package com.quxiangshe.backend.service;

/**
 * 短信服务接口
 * 
 * @author 趣享社技术团队
 */
public interface ISmsService {
    
    /**
     * 发送验证码
     * @param phone 手机号
     * @return 是否发送成功
     */
    boolean sendVerifyCode(String phone);
    
    /**
     * 验证验证码
     * @param phone 手机号
     * @param code 验证码
     * @return 是否验证通过
     */
    boolean verifyCode(String phone, String code);
}
package com.quxiangshe.backend.service.impl;

import com.quxiangshe.backend.service.ISmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 短信服务实现类
 * 腾讯云短信服务 - 验证码存储到Redis
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TencentSmsServiceImpl implements ISmsService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${sms.tencent.app-id:}")
    private String appId;
    
    @Value("${sms.tencent.sign-name:}")
    private String signName;
    
    @Value("${sms.tencent.template-id:}")
    private String templateId;
    
    @Value("${sms.expire-seconds:300}")
    private int expireSeconds;
    
    @Value("${sms.mock-enabled:false}")
    private boolean mockEnabled;
    
    private static final String VERIFY_CODE_KEY_PREFIX = "sms:verify:";
    private static final int CODE_LENGTH = 6;
    
    @Override
    public boolean sendVerifyCode(String phone) {
        try {
            String code = generateCode();
            
            if (mockEnabled) {
                log.info("[Mock] 发送验证码: phone={}, code={}", phone, code);
            } else {
                // TODO: 调用腾讯云短信API发送短信
                // 这里先记录日志，实际使用时需要配置腾讯云参数
                log.info("发送验证码: phone={}, code={}", phone, code);
                log.warn("腾讯云短信服务未配置，请设置sms.tencent相关配置");
            }
            
            storeCode(phone, code);
            return true;
        } catch (Exception e) {
            log.error("发送验证码异常: {}", phone, e);
            return false;
        }
    }
    
    @Override
    public boolean verifyCode(String phone, String code) {
        String key = VERIFY_CODE_KEY_PREFIX + phone;
        String storedCode = (String) redisTemplate.opsForValue().get(key);
        
        if (storedCode == null) {
            log.warn("验证码已过期或不存在: {}", phone);
            return false;
        }
        
        if (storedCode.equals(code)) {
            redisTemplate.delete(key);
            log.info("验证码验证成功: {}", phone);
            return true;
        }
        
        log.warn("验证码错误: {}, 输入: {}, 存储: {}", phone, code, storedCode);
        return false;
    }
    
    private String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
    
    private void storeCode(String phone, String code) {
        String key = VERIFY_CODE_KEY_PREFIX + phone;
        redisTemplate.opsForValue().set(key, code, expireSeconds, TimeUnit.SECONDS);
    }
}
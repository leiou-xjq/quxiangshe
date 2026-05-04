package com.quxiangshe.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云短信配置类
 * 
 * @author 趣享社技术团队
 */
@Configuration
@ConfigurationProperties(prefix = "sms.tencent")
@Data
public class TencentSmsConfig {
    
    private String secretId;
    private String secretKey;
    private String appId;
    private String signName;
    private String templateId;
}
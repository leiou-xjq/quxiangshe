package com.quxiangshe.backend.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云OSS配置类
 * 
 * @author 趣享社技术团队
 */
@Configuration
@ConfigurationProperties(prefix = "oss.aliyun")
@Data
public class OssConfig {
    
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private boolean enabled;
    
    @Bean
    public OSS ossClient() {
        if (!enabled || endpoint == null || endpoint.isEmpty()
            || accessKeyId == null || accessKeyId.isEmpty()
            || accessKeySecret == null || accessKeySecret.isEmpty()) {
            return null;
        }
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}

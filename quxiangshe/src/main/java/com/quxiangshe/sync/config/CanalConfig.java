package com.quxiangshe.sync.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Canal配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "canal")
public class CanalConfig {

    /**
     * 是否启用Canal同步
     */
    private boolean enabled = true;

    /**
     * Canal服务器地址
     */
    private String server = "127.0.0.1:11111";

    /**
     * 目标实例名称
     */
    private String destination = "quxiangshe";

    /**
     * 用户名
     */
    private String username = "canal";

    /**
     * 密码
     */
    private String password = "canal";

    /**
     * 订阅的表（逗号分隔，支持正则）
     * 例如：t_note,user 或 .*\\..*
     */
    private String tables = "t_note,user";

    /**
     * 批量大小
     */
    private int batchSize = 5;

    /**
     * 消费者线程数
     */
    private int threads = 2;

    /**
     * 获取服务器主机
     */
    public String getHost() {
        return server.split(":")[0];
    }

    /**
     * 获取服务器端口
     */
    public int getPort() {
        String[] parts = server.split(":");
        return parts.length > 1 ? Integer.parseInt(parts[1]) : 11111;
    }
}

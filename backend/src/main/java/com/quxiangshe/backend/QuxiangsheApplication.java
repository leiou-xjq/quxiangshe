package com.quxiangshe.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 理享后端服务启动类
 *
 * @author 理享技术团队
 */
@SpringBootApplication
@MapperScan("com.quxiangshe.backend.mapper")
@EnableConfigurationProperties
@EnableScheduling
public class QuxiangsheApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(QuxiangsheApplication.class, args);
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║                                                           ║");
        System.out.println("║       趣享社后端服务启动成功！                             ║");
        System.out.println("║       API文档: http://localhost:8080/api/doc.html         ║");
        System.out.println("║                                                           ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
    }
}
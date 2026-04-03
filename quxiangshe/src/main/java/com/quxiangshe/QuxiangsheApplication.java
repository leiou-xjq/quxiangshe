package com.quxiangshe;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 趣享社社交平台后端启动类
 */
@SpringBootApplication(scanBasePackages = "com.quxiangshe")
@MapperScan({"com.quxiangshe.**.mapper"})
@EnableScheduling
@EnableRetry
public class QuxiangsheApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuxiangsheApplication.class, args);
    }
}

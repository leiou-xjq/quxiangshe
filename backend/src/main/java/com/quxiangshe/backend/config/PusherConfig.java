package com.quxiangshe.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@Profile("!pusher")
public class PusherConfig {
    public PusherConfig() {
        log.info("Pusher配置已禁用 (未配置pusher依赖)");
    }
}
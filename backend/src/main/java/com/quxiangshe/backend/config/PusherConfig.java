package com.quxiangshe.backend.config;

import com.pusher.rest.Pusher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PusherConfig {

    @Value("${pusher.app-id:2149929}")
    private String appId;

    @Value("${pusher.key:5039ccc58e39370fb8e0}")
    private String key;

    @Value("${pusher.secret:8f7005715b9c10d3b6f0}")
    private String secret;

    @Value("${pusher.cluster:ap3}")
    private String cluster;

    @Bean
    public Pusher pusher() {
        return new Pusher(appId, key, secret, cluster);
    }
}
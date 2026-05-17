package com.quxiangshe.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置
 * <p>注册ChatWebSocketHandler到 /ws/chat 端点，允许所有来源的WebSocket连接。
 * WebSocket认证不走Spring Security HTTP Filter链，而是由ChatWebSocketHandler
 * 在连接建立时通过URL参数中的JWT token进行身份验证。</p>
 * 
 * @author 趣享社技术团队
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler) {
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    /**
     * 注册WebSocket处理器
     * 
     * @param registry WebSocket处理器注册表
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 聊天WebSocket端点，允许任意来源连接
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOrigins("*");
    }
}
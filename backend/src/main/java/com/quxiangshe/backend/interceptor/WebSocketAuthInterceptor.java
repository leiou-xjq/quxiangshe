package com.quxiangshe.backend.interceptor;

import com.quxiangshe.backend.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Value("${jwt.token-prefix:Bearer }")
    private String tokenPrefix;

    @Override
    public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request,
                                   org.springframework.http.server.ServerHttpResponse response,
                                   org.springframework.web.socket.WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (StringUtils.isNotBlank(bearerToken) && bearerToken.startsWith(tokenPrefix)) {
            String token = bearerToken.substring(tokenPrefix.length());
            try {
                Claims claims = jwtUtil.getClaimsFromToken(token);
                if (claims != null) {
                    Long userId = Long.parseLong(claims.getSubject());
                    String username = claims.get("username", String.class);

                    attributes.put("ws_userId", userId);
                    attributes.put("ws_username", username);

                    log.debug("WebSocket认证成功: userId={}, username={}", userId, username);
                    return true;
                }
            } catch (Exception e) {
                log.warn("WebSocket JWT验证失败: {}", e.getMessage());
            }
        }

        log.warn("WebSocket认证失败: 无效的Token");
        return false;
    }

    @Override
    public void afterHandshake(org.springframework.http.server.ServerHttpRequest request,
                               org.springframework.http.server.ServerHttpResponse response,
                               org.springframework.web.socket.WebSocketHandler wsHandler,
                               Exception exception) {
    }
}
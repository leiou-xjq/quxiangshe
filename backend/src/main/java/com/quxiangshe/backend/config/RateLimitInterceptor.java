package com.quxiangshe.backend.config;

import com.quxiangshe.backend.util.SlidingWindowRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接口限流拦截器
 * 对敏感接口进行限流保护
 *
 * @author 趣享社技术团队
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final SlidingWindowRateLimiter rateLimiter;

    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    private static final Map<String, RateLimitConfig> LIMIT_CONFIG = new ConcurrentHashMap<>();

    static {
        // 登录接口：每分钟10次
        LIMIT_CONFIG.put("/api/auth/login", new RateLimitConfig(10, 60));
        // 注册接口：每分钟5次
        LIMIT_CONFIG.put("/api/auth/register", new RateLimitConfig(5, 60));
        // 发送验证码：每分钟1次
        LIMIT_CONFIG.put("/api/auth/sendCode", new RateLimitConfig(1, 60));
        // 发布笔记：每分钟10次
        LIMIT_CONFIG.put("/api/note", new RateLimitConfig(10, 60));
        // 点赞：每分钟30次
        LIMIT_CONFIG.put("/api/note/*/like", new RateLimitConfig(30, 60));
        // 收藏：每分钟30次
        LIMIT_CONFIG.put("/api/note/*/favorite", new RateLimitConfig(30, 60));
        // 评论：每分钟20次
        LIMIT_CONFIG.put("/api/note/*/comment", new RateLimitConfig(20, 60));
        // 私信发送：每分钟30次
        LIMIT_CONFIG.put("/api/message/send", new RateLimitConfig(30, 60));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!rateLimitEnabled) {
            return true;
        }

        String uri = request.getRequestURI();
        String userId = getUserId(request);

        // 查找匹配的限流配置
        RateLimitConfig config = findMatchingConfig(uri);
        if (config == null) {
            return true;
        }

        // 构建限流key：接口路径 + 用户ID
        String rateLimitKey = uri + ":" + userId;

        // 尝试获取令牌
        boolean allowed = rateLimiter.tryAcquire(rateLimitKey, config.maxRequests, config.windowSeconds);

        if (!allowed) {
            log.warn("接口限流触发: uri={}, userId={}, maxRequests={}, windowSeconds={}",
                    uri, userId, config.maxRequests, config.windowSeconds);

            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            return false;
        }

        return true;
    }

    private RateLimitConfig findMatchingConfig(String uri) {
        for (Map.Entry<String, RateLimitConfig> entry : LIMIT_CONFIG.entrySet()) {
            String pattern = entry.getKey();
            if (matchUri(pattern, uri)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean matchUri(String pattern, String uri) {
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return uri.startsWith(prefix);
        }
        return pattern.equals(uri);
    }

    private String getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        return userId != null ? userId.toString() : getClientIp(request);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    static class RateLimitConfig {
        int maxRequests;
        int windowSeconds;

        RateLimitConfig(int maxRequests, int windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }
    }
}
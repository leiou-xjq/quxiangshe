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
 * 接口级别限流拦截器
 * 
 * <p>基于Spring MVC拦截器实现，与注解模式互补：{@link com.quxiangshe.backend.aspect.RateLimitAspect}负责
 * 业务方法级限流，本拦截器负责敏感接口（登录、注册、验证码等）的路径级限流。
 * 使用ConcurrentHashMap存储限流配置，支持路径前缀通配符匹配。</p>
 * 
 * <p>限流key为"URI:用户ID"组合，未登录用户使用IP地址代替用户ID，
 * 确保所有请求都能被限流保护。</p>
 * 
 * @author 趣享社技术团队
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final SlidingWindowRateLimiter rateLimiter;

    /** 限流总开关，可通过配置动态关闭 */
    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    /** 接口限流配置表：key=URI路径（支持*通配符），value=限流参数 */
    private static final Map<String, RateLimitConfig> LIMIT_CONFIG = new ConcurrentHashMap<>();

    static {
        // 以下配置均为每个用户/每分钟的限制次数
        LIMIT_CONFIG.put("/api/auth/login", new RateLimitConfig(10, 60));
        LIMIT_CONFIG.put("/api/auth/register", new RateLimitConfig(5, 60));
        // 验证码接口限制严格，防止短信轰炸
        LIMIT_CONFIG.put("/api/auth/sendCode", new RateLimitConfig(1, 60));
        LIMIT_CONFIG.put("/api/note", new RateLimitConfig(10, 60));
        // 交互类接口放宽限制，提升用户体验
        LIMIT_CONFIG.put("/api/note/*/like", new RateLimitConfig(30, 60));
        LIMIT_CONFIG.put("/api/note/*/favorite", new RateLimitConfig(30, 60));
        LIMIT_CONFIG.put("/api/note/*/comment", new RateLimitConfig(20, 60));
        LIMIT_CONFIG.put("/api/message/send", new RateLimitConfig(30, 60));
    }

    /**
     * 请求前置处理：限流检查
     * 
     * <p>处理流程：检查开关 → 查找匹配的限流配置 → 构建限流Key → 
     * 滑动窗口限流判断 → 超限返回429状态码。</p>
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param handler  处理器
     * @return true-放行，false-拦截（返回429）
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 限流总开关关闭时直接放行
        if (!rateLimitEnabled) {
            return true;
        }

        String uri = request.getRequestURI();
        // 获取用户标识：已登录用userId，未登录用IP兜底
        String userId = getUserId(request);

        // 查找匹配当前URI的限流配置
        RateLimitConfig config = findMatchingConfig(uri);
        if (config == null) {
            // 不在限流名单中的接口直接放行
            return true;
        }

        // 构建限流key：格式为"接口路径:用户ID"
        String rateLimitKey = uri + ":" + userId;

        // 滑动窗口限流判断
        boolean allowed = rateLimiter.tryAcquire(rateLimitKey, config.maxRequests, config.windowSeconds);

        if (!allowed) {
            log.warn("接口限流触发: uri={}, userId={}, maxRequests={}, windowSeconds={}",
                    uri, userId, config.maxRequests, config.windowSeconds);

            // 返回JSON格式的429错误响应
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            return false;
        }

        return true;
    }

    /**
     * 查找与当前URI匹配的限流配置
     * 
     * <p>遍历所有限流规则，支持精确匹配和前缀通配符匹配。
     * 通配符规则：以"*"结尾的路径为前缀匹配，
     * 如"/api/note/*"可匹配"/api/note/123/like"。</p>
     *
     * @param uri 当前请求的URI路径
     * @return 匹配的限流配置，无匹配时返回null
     */
    private RateLimitConfig findMatchingConfig(String uri) {
        for (Map.Entry<String, RateLimitConfig> entry : LIMIT_CONFIG.entrySet()) {
            String pattern = entry.getKey();
            if (matchUri(pattern, uri)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * URI路径匹配
     * 
     * @param pattern 限流配置中的路径模板（支持"*"后缀通配）
     * @param uri     实际请求的URI
     * @return true-匹配，false-不匹配
     */
    private boolean matchUri(String pattern, String uri) {
        // 通配符匹配：去掉末尾"*"后进行前缀匹配
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return uri.startsWith(prefix);
        }
        // 精确匹配
        return pattern.equals(uri);
    }

    /**
     * 获取用户标识：优先取已登录用户ID，未登录则取客户端IP
     *
     * @param request HTTP请求
     * @return 用户标识字符串（userId或IP）
     */
    private String getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        // 已登录用户使用userId，未登录用户使用IP作为兜底标识
        return userId != null ? userId.toString() : getClientIp(request);
    }

    /**
     * 获取客户端IP地址，支持反向代理
     *
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        // 优先级：X-Forwarded-For → X-Real-IP → RemoteAddr
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 限流配置内部类：封装每个接口的限流参数
     */
    static class RateLimitConfig {
        /** 窗口内允许的最大请求数 */
        int maxRequests;
        /** 限流窗口大小（秒） */
        int windowSeconds;

        RateLimitConfig(int maxRequests, int windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }
    }
}
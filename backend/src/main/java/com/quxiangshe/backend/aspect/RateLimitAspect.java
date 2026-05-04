package com.quxiangshe.backend.aspect;

import com.quxiangshe.backend.annotation.RateLimit;
import com.quxiangshe.backend.exception.BusinessException;
import com.quxiangshe.backend.util.FixedWindowRateLimiter;
import com.quxiangshe.backend.util.SlidingWindowRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * 限流切面
 * 使用AOP统一处理限流逻辑，不侵入业务代码
 * 支持用户级别限流 + IP级别兜底限流
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {
    
    private final FixedWindowRateLimiter fixedWindowRateLimiter;
    private final SlidingWindowRateLimiter slidingWindowRateLimiter;
    
    /**
     * 环绕通知，处理限流
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 获取请求对象
        HttpServletRequest request = getHttpRequest();
        if (request == null) {
            return joinPoint.proceed();
        }
        
        // 获取客户端IP
        String clientIp = getClientIp(request);
        
        // 构建限流key（替换占位符）
        String rateLimitKey = buildKey(rateLimit.key(), joinPoint, clientIp);
        
        // 1. 用户级别限流检查
        boolean userAllowed = checkRateLimit(rateLimitKey, rateLimit);
        
        // 2. IP级别兜底限流检查
        String ipKey = "rate:ip:" + clientIp;
        boolean ipAllowed = fixedWindowRateLimiter.tryAcquire(ipKey, 30, 60); // 每IP每分钟30次
        
        // 如果用户级别被限流
        if (!userAllowed) {
            log.warn("用户级别限流触发: ip={}, key={}", clientIp, rateLimitKey);
            throw new BusinessException(429, rateLimit.message());
        }
        
        // 如果IP级别被限流
        if (!ipAllowed) {
            log.warn("IP级别限流触发: ip={}, key={}", clientIp, rateLimitKey);
            throw new BusinessException(429, "操作过于频繁，请稍后重试");
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * 执行限流检查
     */
    private boolean checkRateLimit(String key, RateLimit rateLimit) {
        if (rateLimit.type() == RateLimit.LimitType.SLIDING_WINDOW) {
            return slidingWindowRateLimiter.tryAcquire(key, rateLimit.maxRequests(), rateLimit.windowSeconds());
        } else {
            return fixedWindowRateLimiter.tryAcquire(key, rateLimit.maxRequests(), rateLimit.windowSeconds());
        }
    }
    
    /**
     * 获取HttpServletRequest
     */
    private HttpServletRequest getHttpRequest() {
        ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * 构建限流key，替换占位符
     */
    private String buildKey(String prefix, ProceedingJoinPoint joinPoint, String ip) {
        String key = prefix;
        
        // 替换方法参数中的占位符
        if (key.contains("{") && key.contains("}")) {
            Object[] args = joinPoint.getArgs();
            java.lang.reflect.Method method = getMethod(joinPoint);
            if (method != null) {
                java.lang.reflect.Parameter[] parameters = method.getParameters();
                for (int i = 0; i < parameters.length && i < args.length; i++) {
                    String paramName = "{" + parameters[i].getName() + "}";
                    if (key.contains(paramName) && args[i] != null) {
                        key = key.replace(paramName, args[i].toString());
                    }
                }
            }
        }
        
        return key;
    }
    
    /**
     * 获取执行的方法
     */
    private java.lang.reflect.Method getMethod(ProceedingJoinPoint joinPoint) {
        try {
            String methodName = joinPoint.getSignature().getName();
            Class<?>[] parameterTypes = Arrays.stream(joinPoint.getArgs())
                    .map(Object::getClass)
                    .toArray(Class<?>[]::new);
            return joinPoint.getTarget().getClass().getMethod(methodName, parameterTypes);
        } catch (Exception e) {
            return null;
        }
    }
}

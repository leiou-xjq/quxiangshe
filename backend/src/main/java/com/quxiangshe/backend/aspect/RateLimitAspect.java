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
 * 
 * <p>基于Spring AOP的环绕通知实现声明式限流，拦截标注了{@link RateLimit}注解的方法。
 * 采用双层限流策略：外层按用户维度限流（注解配置），内层按IP维度兜底限流（硬编码30次/分钟），
 * 防止未登录用户或恶意IP绕过用户级限流。</p>
 * 
 * <p>设计要点：<br>
 * 1. 非HTTP请求（如定时任务）直接放行，避免阻塞后台任务<br>
 * 2. IP解析支持反向代理（X-Forwarded-For, X-Real-IP）<br>
 * 3. 异常时默认放行，限流组件故障不影响业务</p>
 * 
 * @author 趣享社技术团队
 * @since 1.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {
    
    private final FixedWindowRateLimiter fixedWindowRateLimiter;
    private final SlidingWindowRateLimiter slidingWindowRateLimiter;
    
    /**
     * 环绕通知，在标注了{@link RateLimit}的方法执行前进行限流检查
     * 
     * <p>处理流程：获取HTTP请求 → 解析客户端IP → 构建限流Key → 
     * 用户级限流检查 → IP级限流检查 → 任一不通过则抛429异常。</p>
     *
     * @param joinPoint 连接点，可获取目标方法及参数
     * @param rateLimit 限流注解实例，包含限流配置参数
     * @return 目标方法的返回值
     * @throws Throwable 限流不通过时抛出BusinessException(429)
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 获取请求对象（非HTTP请求直接放行）
        HttpServletRequest request = getHttpRequest();
        if (request == null) {
            return joinPoint.proceed();
        }
        
        // 获取客户端真实IP（支持反向代理穿透）
        String clientIp = getClientIp(request);
        
        // 构建限流key：将注解中的{参数名}占位符替换为实际参数值
        String rateLimitKey = buildKey(rateLimit.key(), joinPoint, clientIp);
        
        // 1. 用户级别限流检查（按注解配置的策略执行）
        boolean userAllowed = checkRateLimit(rateLimitKey, rateLimit);
        
        // 2. IP级别兜底限流检查（硬编码：每IP每分钟30次，作为未登录用户/恶意IP的最后防线）
        String ipKey = "rate:ip:" + clientIp;
        boolean ipAllowed = fixedWindowRateLimiter.tryAcquire(ipKey, 30, 60);
        
        // 用户级限流触发
        if (!userAllowed) {
            log.warn("用户级别限流触发: ip={}, key={}", clientIp, rateLimitKey);
            throw new BusinessException(429, rateLimit.message());
        }
        
        // IP级限流触发
        if (!ipAllowed) {
            log.warn("IP级别限流触发: ip={}, key={}", clientIp, rateLimitKey);
            throw new BusinessException(429, "操作过于频繁，请稍后重试");
        }
        
        // 两级限流均通过，执行目标方法
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
     * 获取客户端真实IP地址，支持反向代理穿透
     * 
     * <p>解析优先级：X-Forwarded-For（代理链） → X-Real-IP（Nginx） → 
     * RemoteAddr（直连）。多级代理时取第一个IP（最接近客户端）。</p>
     *
     * @param request HTTP请求对象
     * @return 客户端真实IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        // 1. 尝试从X-Forwarded-For获取（格式：client, proxy1, proxy2）
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            // 2. 尝试从X-Real-IP获取（Nginx单级代理）
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            // 3. 兜底：直接从TCP连接获取
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个IP（客户端真实IP）
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

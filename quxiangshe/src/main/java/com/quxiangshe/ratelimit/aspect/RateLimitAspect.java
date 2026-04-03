package com.quxiangshe.ratelimit.aspect;

import com.quxiangshe.common.annotation.RateLimit;
import com.quxiangshe.common.constant.RateLimitConstants;
import com.quxiangshe.common.exception.RateLimitException;
import com.quxiangshe.common.util.BlacklistUtil;
import com.quxiangshe.common.util.RedisLuaRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 限流切面类
 * 基于AOP实现注解拦截，完成「黑名单校验→Redis+Lua限流校验」逻辑
 * 支持密码登录不做限流、验证码/注册接口限流
 * 限流触发后自动加入黑名单
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisLuaRateLimiter rateLimiter;
    private final BlacklistUtil blacklistUtil;

    /**
     * 限流切面环绕通知
     * 执行顺序：
     * 1. 先校验IP是否在黑名单 → 是则直接返回429拦截
     * 2. 不在黑名单，才执行原有的滑动窗口限流
     * 3. 如果限流判定超限 → 自动加入黑名单10分钟
     * 4. 否则正常放行
     */
    @Around("@annotation(com.quxiangshe.common.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取客户端IP
        String clientIp = getClientIp();

        // 第一步：校验IP是否在黑名单
        if (blacklistUtil.isBlacklisted(clientIp)) {
            log.warn("IP在黑名单中，直接拦截: ip={}", clientIp);
            throw new RateLimitException(RateLimitConstants.RATE_LIMIT_CODE, 
                "您的请求过于频繁，已被限制10分钟");
        }

        // 获取限流注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        // 检查是否启用限流
        if (!rateLimit.enabled()) {
            return joinPoint.proceed();
        }

        // 第二步：执行原有的滑动窗口限流
        String keyPrefix = rateLimit.keyPrefix();
        int limit = getLimitValue(rateLimit);
        long windowMs = getWindowMsValue(rateLimit);
        long expireMs = windowMs + 1000;

        // 构建限流Key
        String key = buildRateLimitKey(keyPrefix, clientIp);

        // 执行限流校验
        boolean allowed = rateLimiter.tryAcquire(key, limit, windowMs, expireMs);

        if (!allowed) {
            // 第三步：限流触发，自动加入黑名单
            blacklistUtil.addToBlacklist(clientIp);
            
            // 获取自定义消息或使用默认消息
            String message = rateLimit.message();
            if (message == null || message.isEmpty()) {
                message = RateLimitConstants.RATE_LIMIT_MESSAGE;
            }
            log.warn("限流触发并加入黑名单: key={}, ip={}, limit={}, windowMs={}", 
                key, clientIp, limit, windowMs);
            throw new RateLimitException(RateLimitConstants.RATE_LIMIT_CODE, message);
        }

        // 第四步：正常放行
        log.debug("限流校验通过: key={}, ip={}", key, clientIp);
        return joinPoint.proceed();
    }

    /**
     * 构建限流Key
     * 基础粒度：单IP+接口名
     * 
     * @param prefix key前缀
     * @param ip     客户端IP
     * @return 限流Key
     */
    private String buildRateLimitKey(String prefix, String ip) {
        if (prefix == null || prefix.isEmpty()) {
            return "limit:" + ip;
        }
        return prefix + ip;
    }

    /**
     * 获取限流阈值
     * 如果注解值为-1，使用默认值
     * 
     * @param rateLimit 限流注解
     * @return 限流阈值
     */
    private int getLimitValue(RateLimit rateLimit) {
        if (rateLimit.limit() > 0) {
            return rateLimit.limit();
        }
        // 根据keyPrefix判断使用哪个默认值
        String prefix = rateLimit.keyPrefix();
        if (prefix != null) {
            if (prefix.contains("captcha")) {
                return RateLimitConstants.CAPTCHA_DEFAULT_LIMIT;
            } else if (prefix.contains("register:phone")) {
                return RateLimitConstants.REGISTER_PHONE_DEFAULT_LIMIT;
            } else if (prefix.contains("register")) {
                return RateLimitConstants.REGISTER_DEFAULT_LIMIT;
            }
        }
        return RateLimitConstants.CAPTCHA_DEFAULT_LIMIT;
    }

    /**
     * 获取窗口时间
     * 如果注解值为-1，使用默认值
     * 
     * @param rateLimit 限流注解
     * @return 窗口时间（毫秒）
     */
    private long getWindowMsValue(RateLimit rateLimit) {
        if (rateLimit.windowMs() > 0) {
            return rateLimit.windowMs();
        }
        // 根据keyPrefix判断使用哪个默认值
        String prefix = rateLimit.keyPrefix();
        if (prefix != null) {
            if (prefix.contains("register:phone")) {
                return RateLimitConstants.REGISTER_PHONE_WINDOW_MS;
            } else if (prefix.contains("register")) {
                return RateLimitConstants.REGISTER_DEFAULT_WINDOW_MS;
            }
        }
        return RateLimitConstants.CAPTCHA_DEFAULT_WINDOW_MS;
    }

    /**
     * 获取客户端IP地址
     * 
     * @return 客户端IP
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "unknown";
            }
            
            HttpServletRequest request = attributes.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            
            return ip != null ? ip : "unknown";
        } catch (Exception e) {
            log.warn("获取客户端IP失败: {}", e.getMessage());
            return "unknown";
        }
    }
}
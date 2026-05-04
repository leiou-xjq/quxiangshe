package com.quxiangshe.ratelimit.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quxiangshe.auth.mapper.AuthUserMapper;
import com.quxiangshe.common.dto.BusinessError;
import com.quxiangshe.common.exception.BusinessException;
import com.quxiangshe.ratelimit.service.LoginRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.BufferedReader;
import java.util.Map;

/**
 * 登录限流切面
 * 针对登录接口进行限流
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LoginRateLimitAspect {

    private final LoginRateLimitService loginRateLimitService;
    private final AuthUserMapper authUserMapper;
    private final ObjectMapper objectMapper;

    /**
     * 登录限流切面
     */
    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) && " +
            "(execution(* com.quxiangshe.auth.controller.AuthController.login(..)) || " +
            "execution(* com.quxiangshe.auth.controller.AuthController.register(..)))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取当前请求
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        
        // 获取用户标识（优先从请求体JSON获取，其次使用IP）
        String identifier = getIdentifierFromJson(request);
        
        // 使用IP作为备选
        if (identifier == null || identifier.isEmpty()) {
            identifier = getClientIp(request);
        }
        
        // 检查是否允许登录
        if (!loginRateLimitService.tryAcquire(identifier)) {
            log.warn("登录限流触发: identifier={}, uri={}", identifier, request.getRequestURI());
            
            // 判断是否为老用户（手机号已注册）
            boolean isOldUser = checkPhoneExists(identifier);
            
            if (isOldUser) {
                // 老用户：提示可使用手机号登录
                throw new BusinessException(BusinessError.LOGIN_RATE_LIMIT_OLD_USER);
            } else {
                // 新用户：提示稍后重试
                throw new BusinessException(BusinessError.LOGIN_RATE_LIMIT);
            }
        }

        return joinPoint.proceed();
    }

    /**
     * 从请求体JSON中获取用户名或手机号
     */
    private String getIdentifierFromJson(HttpServletRequest request) {
        try {
            String contentType = request.getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                StringBuilder sb = new StringBuilder();
                BufferedReader reader = request.getReader();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                
                Map<String, Object> json = objectMapper.readValue(sb.toString(), Map.class);
                
                // 优先获取 username
                Object username = json.get("username");
                if (username != null && !username.toString().isEmpty()) {
                    return username.toString();
                }
                
                // 其次获取 phone
                Object phone = json.get("phone");
                if (phone != null && !phone.toString().isEmpty()) {
                    return phone.toString();
                }
            }
        } catch (Exception e) {
            log.debug("解析请求体失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 检查手机号是否已注册
     */
    private boolean checkPhoneExists(String phone) {
        if (phone == null || phone.isEmpty() || phone.length() < 11) {
            return false;
        }
        Long count = authUserMapper.countByPhone(phone);
        return count != null && count > 0;
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
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
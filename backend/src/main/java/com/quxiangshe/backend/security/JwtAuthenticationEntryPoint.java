package com.quxiangshe.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quxiangshe.backend.common.R;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT认证入口点
 * <p>当用户未携带有效Token访问受保护资源时，Spring Security会调用此入口点。
 * 返回统一的JSON格式401响应，前端根据此响应码跳转至登录页。
 *
 * @author 趣享社技术团队
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper;
    
    /**
     * 处理未认证请求的入口方法
     * <p>设置HTTP状态码为401，返回统一响应体 {@link R#fail(401, ...)}
     *
     * @param request       HTTP请求
     * @param response      HTTP响应
     * @param authException 认证异常
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, 
                         AuthenticationException authException) throws IOException, ServletException {
        
        log.warn("未认证的访问: {} - {}", request.getRequestURI(), authException.getMessage());
        
        // 设置401状态码及JSON响应头
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        // 返回统一格式的错误响应
        R<Void> result = R.fail(401, "未授权，请先登录");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
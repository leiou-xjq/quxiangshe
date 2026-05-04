package com.quxiangshe.auth.filter;

import com.quxiangshe.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 * 
 * 功能说明:
 * 1. 从HTTP请求头中提取JWT Token
 * 2. 验证Token有效性（签名、过期时间）
 * 3. 提取用户信息（userId、username、roles）
 * 4. 设置Spring Security上下文
 * 
 * 工作流程:
 * 1. 获取请求头中的Authorization字段
 * 2. 验证Bearer Token格式
 * 3. 调用JwtUtil验证Token
 * 4. 构建UserDetails对象
 * 5. 设置SecurityContextHolder中的认证信息
 * 6. 继续执行过滤器链
 * 
 * 注意:
 * - 该过滤器在UsernamePasswordAuthenticationFilter之前执行
 * - 只验证Token，不处理登录表单
 * - 认证失败时不会阻止请求，而是让后续的授权过滤器处理
 * 
 * @author quxiangshe
 * @since 2024
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /**
     * 请求头中的Token前缀
     * 格式: Bearer {token}
     */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 执行JWT认证过滤
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException IO异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 1. 从请求头获取Token
            // 格式: Authorization: Bearer {token}
            String token = extractTokenFromHeader(request);
            
            if (token != null) {
                // 2. 验证Token（签名、过期时间）
                if (jwtUtil.validateToken(token)) {
                    // 3. 提取用户信息
                    String userId = jwtUtil.extractUserId(token);
                    String username = jwtUtil.extractUsername(token);
                    String roles = jwtUtil.extractRoles(token);
                    
                    // 4. 设置SecurityContext（仅当未认证时）
                    if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        // 构建UserDetails对象
                        UserDetails userDetails = buildUserDetails(userId, username, roles);
                        
                        // 创建认证Token并设置到SecurityContext
                        // Principal: 用户信息（UserDetails）
                        // Credentials: 凭证（密码，这里为空）
                        // Authorities: 权限列表（从roles转换）
                        UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, 
                                        null, 
                                        userDetails.getAuthorities()
                                );
                        // 设置请求详情（IP地址、Session ID等）
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        // 设置到SecurityContext（线程本地存储）
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        log.debug("JWT认证成功: userId={}, username={}", userId, username);
                    }
                } else {
                    log.debug("Token验证失败或已过期");
                }
            }
        } catch (Exception e) {
            // 捕获异常避免阻断请求链
            log.error("JWT认证过程发生异常: {}", e.getMessage(), e);
        }
        
        // 5. 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取Token
     * 
     * 提取规则:
     * 1. 获取Authorization请求头
     * 2. 检查是否以Bearer开头
     * 3. 截取Token部分（去掉"Bearer "前缀）
     * 
     * @param request HTTP请求
     * @return Token字符串（不含Bearer前缀），如果不存在则返回null
     */
    private String extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 构建Spring Security的UserDetails对象
     * 
     * 构建规则:
     * - username: userId（使用用户ID作为用户名，便于获取当前用户）
     * - password: 空（Token认证不需要密码）
     * - authorities: 从roles转换（"USER" -> ROLE_USER）
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param roles 角色（逗号分隔，如"USER,ADMIN"）
     * @return UserDetails对象
     */
    private UserDetails buildUserDetails(String userId, String username, String roles) {
        // 使用userId作为Principal
        return User.builder()
                .username(userId)
                .password("")
                // 转换roles为权限列表
                // Spring Security会自动添加"ROLE_"前缀
                // "USER" -> ROLE_USER
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(
                        roles != null ? roles : "ROLE_USER"
                )))
                .build();
    }
}

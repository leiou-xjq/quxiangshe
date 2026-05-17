package com.quxiangshe.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quxiangshe.backend.common.R;
import com.quxiangshe.backend.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT认证过滤器
 * <p>继承OncePerRequestFilter，确保每个请求仅经过一次过滤。
 * 从请求头中提取JWT Token，验证有效后将用户信息写入Spring Security上下文。
 *
 * <p>处理流程：
 * <ol>
 *   <li>检查请求路径，WebSocket连接（/ws/）跳过认证</li>
 *   <li>从请求头提取Bearer Token</li>
 *   <li>解析Token获取Claims，验证是否过期</li>
 *   <li>构建UsernamePasswordAuthenticationToken并注入SecurityContext</li>
 *   <li>无论认证成功与否，均放行请求（匿名访问由后续权限注解控制）</li>
 * </ol>
 *
 * @author 趣享社技术团队
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    
    @Value("${jwt.header-name:Authorization}")
    private String headerName;

    @Value("${jwt.token-prefix:Bearer }")
    private String tokenPrefix;

    /**
     * 判断当前请求是否需要跳过过滤器
     * <p>WebSocket连接（/ws/）使用独立的认证机制，此处直接跳过
     *
     * @param request HTTP请求
     * @return true表示跳过过滤器
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/ws/");
    }

    /**
     * 过滤器核心逻辑
     * <p>从请求头提取Token → 解析并验证 → 注入Security上下文 → 放行
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 从请求头提取JWT Token
        String token = getTokenFromRequest(request);
        
        if (token != null) {
            // 解析Token，获取Claims
            Claims claims = jwtUtil.getClaimsFromToken(token);
            
            // 验证Token有效性（不为null且未过期）
            if (claims != null && !claims.getExpiration().before(new java.util.Date())) {
                // 从Claims中提取用户信息
                Long userId = Long.parseLong(claims.getSubject());
                String username = (String) claims.get("username");
                String role = (String) claims.getOrDefault("role", "USER");
                
                // 构造权限集合（前端角色需加ROLE_前缀以匹配Spring Security约定）
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + role)
                );
                
                // 构建认证令牌，principal为用户ID
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                authorities
                        );
                
                // 将用户信息写入请求属性，方便Controller层直接获取
                request.setAttribute("userId", userId);
                request.setAttribute("username", username);
                request.setAttribute("role", role);
                
                // 将认证信息注入Spring Security上下文，后续权限校验基于此
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("JWT认证成功: userId={}, username={}", userId, username);
            } else {
                log.warn("Token已过期或无效");
            }
        }
        
        // 继续执行过滤器链（即使认证失败也放行，由Controller层注解控制权限）
        filterChain.doFilter(request, response);
    }
    
    /**
     * 从请求头中提取JWT Token
     * <p>格式：Authorization: Bearer &lt;token&gt;
     *
     * @param request HTTP请求
     * @return Token字符串，未找到则返回null
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(headerName);
        
        // 校验请求头格式：以"Bearer "开头
        if (bearerToken != null && bearerToken.startsWith(tokenPrefix)) {
            return bearerToken.substring(tokenPrefix.length());
        }
        
        return null;
    }
}
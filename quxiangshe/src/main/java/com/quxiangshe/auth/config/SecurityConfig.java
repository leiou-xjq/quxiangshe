package com.quxiangshe.auth.config;

import com.quxiangshe.auth.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * Spring Security配置类
 * 
 * 安全策略配置:
 * 1. 密码加密: 使用BCryptPasswordEncoder
 * 2. 认证方式: JWT Token（无状态认证）
 * 3. Session管理: STATELESS（无Session）
 * 4. CORS配置: 允许跨域请求
 * 5. 请求授权: 基于URL模式的权限控制
 * 
 * 认证流程:
 * 1. JWT过滤器(JwtAuthenticationFilter)拦截请求
 * 2. 从请求头提取Token并验证
 * 3. 设置Spring Security上下文
 * 4. 控制器处理请求
 * 
 * @author quxiangshe
 * @since 2024
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 密码编码器
     * 使用BCrypt算法加密密码
     * BCrypt是一种单向哈希函数，每次加密结果不同（自动加盐）
     * 验证时使用matches()方法比对
     * 
     * @return PasswordEncoder实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security过滤器链配置
     * 
     * 配置项:
     * - CSRF: 禁用（JWT无状态，无需CSRF防护）
     * - CORS: 启用（允许跨域）
     * - Session: 禁用（无状态JWT）
     * - 授权规则: 基于URL模式
     * - 过滤器: JWT认证过滤器
     * 
     * @param http HttpSecurity对象
     * @return SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（使用JWT无需CSRF防护）
            // JWT Token在请求头中传递，不依赖Cookie
            .csrf(AbstractHttpConfigurer::disable)
            // 配置CORS（跨域资源共享）
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 禁用Session（使用JWT无状态）
            // STATELESS: 每次请求都重新认证，不使用Session存储用户状态
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // 配置请求授权规则
            .authorizeHttpRequests(auth -> auth
                // 认证相关接口允许匿名访问（注册、登录、验证码等）
                .requestMatchers("/api/v1/auth/**").permitAll()
                // 笔记接口需要认证
                .requestMatchers("/api/v1/notes/**").authenticated()
                // Feed流接口需要认证（获取个人动态流）
                .requestMatchers("/api/v1/feed/**").authenticated()
                // 动态发布/操作接口需要认证（发布、点赞、收藏等）
                .requestMatchers("/api/v1/posts/**").authenticated()
                // 用户动态列表接口需要认证
                .requestMatchers("/api/v1/users/*/posts").authenticated()
                // 评论相关接口需要认证（发表评论、回复等）
                .requestMatchers("/api/v1/comments/**").authenticated()
                // 用户关注接口需要认证
                .requestMatchers("/api/v1/user/follow/**").authenticated()
                // 用户信息接口需要认证
                .requestMatchers("/api/v1/user/**").authenticated()
                // 搜索接口公开（可匿名访问）
                .requestMatchers("/api/v1/search/**").permitAll()
                // 其他接口公开
                .anyRequest().permitAll()
            )
            // 添加JWT认证过滤器
            // 在UsernamePasswordAuthenticationFilter之前执行
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // 禁用默认登录页（使用自己的登录页面）
            .formLogin(AbstractHttpConfigurer::disable)
            // 禁用HTTP Basic认证（使用JWT）
            .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * CORS跨域配置
     * 
     * 配置说明:
     * - AllowedOriginPatterns: 允许所有来源（*）
     * - AllowedMethods: 允许的HTTP方法
     * - AllowedHeaders: 允许的请求头
     * - ExposedHeaders: 暴露给前端的响应头（Authorization用于Token传递）
     * - AllowCredentials: 允许携带凭证（Cookie等）
     * - MaxAge: 预检请求缓存时间（1小时）
     * 
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许所有来源（生产环境应指定具体域名）
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许所有请求头
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        // 暴露给前端的响应头（前端可读取）
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        // 允许携带凭证
        configuration.setAllowCredentials(true);
        // 预检请求缓存时间（秒）
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

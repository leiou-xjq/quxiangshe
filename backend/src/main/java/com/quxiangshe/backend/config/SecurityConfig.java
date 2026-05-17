package com.quxiangshe.backend.config;

import com.quxiangshe.backend.security.JwtAuthenticationFilter;
import com.quxiangshe.backend.security.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security核心配置
 * <p>配置内容：
 * <ul>
 *   <li>JWT无状态认证（STATELESS会话策略）</li>
 *   <li>CORS跨域配置（环境变量 dynamically 配置允许源）</li>
 *   <li>CSRF禁用（前后端分离RESTful API不需要）</li>
 *   <li>安全响应头：XSS防护（ENABLED_MODE_BLOCK）、防点击劫持（frame-options: DENY）</li>
 *   <li>URL访问控制：/auth/**、/ws/**、Swagger文档免认证，其余接口需JWT</li>
 *   <li>WebSocket专用安全过滤链（/ws/** 独立配置，避免主链干扰）</li>
 * </ul></p>
 * 
 * @author 趣享社技术团队
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    /**
     * CORS 允许的源（环境变量配置）
     * 开发环境默认值：localhost
     * 生产环境配置：实际域名，多个用逗号分隔
     */
    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:8080,http://127.0.0.1:5173,http://127.0.0.1:8080}")
    private String corsAllowedOrigins;
    
    /**
     * 密码编码器（BCrypt）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    /**
     * 安全过滤链（主链）
     * <p>处理所有非WebSocket的HTTP请求：
     * <ul>
     *   <li>禁用CSRF、CORS</li>
     *   <li>STATELESS会话（JWT无状态认证）</li>
     *   <li>认证/注册/Swagger/WebSocket端点免认证</li>
     *   <li>JWT认证过滤器在UsernamePasswordAuthenticationFilter之前执行</li>
     *   <li>安全响应头：XSS防护（启用+拦截模式）、防frame嵌套（DENY）</li>
     * </ul></p>
     * 
     * @param http HttpSecurity配置构建器
     * @return 安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint))   // 认证失败时返回401 JSON
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/auth/**").permitAll()                   // 认证相关接口公开
                    .requestMatchers("/ws/**", "/ws/chat").permitAll()        // WebSocket握手免认证
                    .requestMatchers("/doc.html", "/swagger-ui/**", "/v3/api-docs/**",
                            "/swagger-resources/**", "/webjars/**").permitAll() // Swagger文档公开
                    .anyRequest().authenticated())                           // 其余接口需JWT认证
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // 安全响应头配置
            .headers(headers -> headers
                    // 防止 XSS 攻击：浏览器检测到反射型XSS时拦截页面加载
                    .xssProtection(xss -> xss
                            .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                    // 防止点击劫持：禁止页面被iframe嵌入
                    .frameOptions(frame -> frame.deny()));

        return http.build();
    }

    /**
     * WebSocket专用安全过滤链
     * <p>独立于主链配置，仅匹配 /ws/** 路径，关闭CSRF，允许所有请求。
     * 实际认证由ChatWebSocketHandler在WebSocket握手阶段通过Query参数中的token完成。</p>
     * 
     * @param http HttpSecurity配置构建器
     * @return WebSocket安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain webSocketSecurityFilterChain(HttpSecurity http) throws Exception {
        // securityMatcher限定此链仅处理WebSocket请求
        http.securityMatcher("/ws/**")
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
    
    /**
     * CORS跨域配置
     * <p>从环境变量 cors.allowed-origins 读取允许的源（支持逗号分隔多个域名）。
     * 开发环境默认允许localhost系列，生产环境应修改为实际域名。
     * 允许的HTTP方法：GET/POST/PUT/DELETE/OPTIONS/PATCH。
     * 暴露的响应头：Authorization（JWT Token）、Content-Disposition（文件下载）。</p>
     * 
     * @return CORS配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 从环境变量/配置文件加载允许的源（逗号分隔）
        String[] allowedOrigins = corsAllowedOrigins.split(",");
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);          // 允许携带Cookie/JWT
        configuration.setMaxAge(3600L);                   // 预检请求缓存1小时
        
        // 暴露的响应头：Authorization（JWT）、Content-Disposition（文件名）
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

package com.quxiangshe.backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Component
public class JwtUtil {
    
    @Value("${jwt.secret:quxiangshe-jwt-secret-key-2024-very-long-and-secure}")
    private String secret;
    
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 生成Token
     * @param claims 声明
     * @param expiration 过期时间（毫秒）
     * @return Token
     */
    public String createToken(Map<String, Object> claims, long expiration) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSecretKey())
                .compact();
    }
    
    /**
     * 解析Token获取声明
     * @param token Token
     * @return 声明
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.warn("Token解析失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 验证并获取声明
     * @param token Token
     * @return 声明
     */
    public Map<String, Object> validateAndGetClaimsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        
        // 检查是否过期
        if (claims.getExpiration().before(new Date())) {
            log.warn("Token已过期");
            return null;
        }
        
        return claims;
    }
    
    /**
     * 获取用户ID
     * @param token Token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        String sub = claims.getSubject();
        return sub != null ? Long.parseLong(sub) : null;
    }
    
    /**
     * 获取用户名
     * @param token Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        return claims.get("username", String.class);
    }
    
    /**
     * 检查Token是否有效
     * @param token Token
     * @return true-有效，false-无效
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims != null && !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取Token过期时间
     * @param token Token
     * @return 过期时间
     */
    public LocalDateTime getExpiration(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        Date expiration = claims.getExpiration();
        return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
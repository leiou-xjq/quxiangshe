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
 * <p>基于JJWT库封装JWT的生成、解析、验证等操作。
 * 使用HMAC-SHA算法签名，支持自定义Claims，内置过期时间校验。
 * Token过期后的异步刷新由网关层统一处理。</p>
 * 
 * @author 趣享社技术团队
 * @since 1.0
 */
@Slf4j
@Component
public class JwtUtil {
    
    /** JWT签名密钥，可通过配置文件覆盖 */
    @Value("${jwt.secret:quxiangshe-jwt-secret-key-2024-very-long-and-secure}")
    private String secret;
    
    /**
     * 根据配置密钥生成HMAC签名密钥
     * 
     * <p>每次调用均动态生成，确保密钥变更后新Token立即生效。</p>
     *
     * @return HMAC签名密钥
     */
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
     * 验证Token有效性并获取声明
     * 
     * <p>验证分为两步：1) 签名校验（防篡改），2) 过期时间校验（防重放）。
     * 任一校验不通过则返回null，调用方据此判断是否需要重新登录。</p>
     *
     * @param token JWT Token字符串
     * @return Token有效时返回声明Map，无效或过期时返回null
     */
    public Map<String, Object> validateAndGetClaimsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        
        // 检查Token是否已过期
        if (claims.getExpiration().before(new Date())) {
            log.warn("Token已过期");
            return null;
        }
        
        return claims;
    }
    
    /**
     * 从Token中提取用户ID
     * 
     * <p>用户ID存储在JWT标准字段sub（Subject）中，
     * 解析时需要先校验签名再提取，不可直接base64解码。</p>
     *
     * @param token JWT Token字符串
     * @return 用户ID，Token无效时返回null
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        // sub字段存储用户ID，解析为Long类型
        String sub = claims.getSubject();
        return sub != null ? Long.parseLong(sub) : null;
    }
    
    /**
     * 从Token中提取用户名
     * 
     * <p>用户名存储在自定义claim中，非JWT标准字段。</p>
     *
     * @param token JWT Token字符串
     * @return 用户名，Token无效时返回null
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        // 从自定义claim中获取username字段
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
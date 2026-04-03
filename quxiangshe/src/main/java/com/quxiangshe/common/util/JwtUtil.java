package com.quxiangshe.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 支持AccessToken(短有效期)和RefreshToken(长有效期)双令牌模式
 */
@Slf4j
@Component
public class JwtUtil {

    /**
     * JWT密钥
     */
    @Value("${jwt.secret:quxiangshe-secret-key-change-in-production-2024}")
    private String secret;

    /**
     * AccessToken有效期（秒）
     */
    @Value("${jwt.access-token-validity:900}")
    private long accessTokenValidity;

    /**
     * RefreshToken有效期（秒）
     */
    @Value("${jwt.refresh-token-validity:604800}")
    private long refreshTokenValidity;

    /**
     * 生成SecretKey
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成AccessToken
     * 包含用户ID、用户名、角色信息
     */
    public String generateAccessToken(Long userId, String username, String roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("roles", roles);
        claims.put("type", "access");

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidity * 1000);

        return Jwts.builder()
                .claims(claims)
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 生成RefreshToken
     * 仅包含用户ID，无业务信息
     */
    public String generateRefreshToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenValidity * 1000);

        return Jwts.builder()
                .claims(claims)
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 解析Token获取用户ID
     */
    public String extractUserId(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * 解析Token获取用户名
     */
    public String extractUsername(String token) {
        Claims claims = parseToken(token);
        return claims != null ? (String) claims.get("username") : null;
    }

    /**
     * 解析Token获取角色
     */
    public String extractRoles(String token) {
        Claims claims = parseToken(token);
        return claims != null ? (String) claims.get("roles") : null;
    }

    /**
     * 获取Token类型
     */
    public String extractTokenType(String token) {
        Claims claims = parseToken(token);
        return claims != null ? (String) claims.get("type") : null;
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims != null && !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证Token是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims != null && claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 解析Token
     */
    private Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("Token已过期");
            return null;
        } catch (JwtException e) {
            log.debug("Token解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取AccessToken过期时间
     */
    public long getAccessTokenValidity() {
        return accessTokenValidity;
    }

    /**
     * 获取RefreshToken过期时间
     */
    public long getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

    /**
     * 从HttpServletRequest中获取用户ID
     */
    public static Long getUserIdFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // 使用固定的secret key，避免实例属性为null
                String secret = "quxiangshe-secret-key-change-in-production-2024";
                SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
                String userIdStr = claims.getSubject();
                if (userIdStr != null) {
                    return Long.parseLong(userIdStr);
                }
            } catch (Exception e) {
                log.warn("从请求中获取用户ID失败: {}", e.getMessage());
            }
        }
        return null;
    }
}

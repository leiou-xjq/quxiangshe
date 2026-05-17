package com.quxiangshe.backend.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具类
 * 
 * <p>基于Spring Security BCryptPasswordEncoder实现单向散列加密，
 * BCrypt算法内置盐值（Salt）和可配置的计算强度（默认10），
 * 相同明文每次加密结果不同，可有效抵御彩虹表攻击。</p>
 * 
 * @author 趣享社技术团队
 * @since 1.0
 */
public class PasswordUtil {
    
    /** BCrypt编码器，内置随机盐，线程安全 */
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    
    /**
     * 加密原始密码
     * 
     * <p>BCrypt内部自动生成随机盐值并拼接到结果中，
     * 因此相同明文每次调用返回的密文不同。</p>
     *
     * @param rawPassword 明文密码
     * @return BCrypt加密后的密文（60字符长度）
     */
    public static String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }
    
    /**
     * 校验明文密码与密文是否匹配
     * 
     * <p>BCrypt.matches自动从密文中提取盐值和计算强度，
     * 对明文重新计算后比较结果。</p>
     *
     * @param rawPassword     明文密码
     * @param encodedPassword 数据库中存储的BCrypt密文
     * @return true-密码匹配，false-不匹配或参数为null
     */
    public static boolean verify(String rawPassword, String encodedPassword) {
        // 防御性检查：任一参数为空时直接返回false
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return encoder.matches(rawPassword, encodedPassword);
    }
}
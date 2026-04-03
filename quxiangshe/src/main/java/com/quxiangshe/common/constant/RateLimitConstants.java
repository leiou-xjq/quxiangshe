package com.quxiangshe.common.constant;

/**
 * 限流常量类
 * 集中管理限流相关的配置常量，支持动态配置扩展
 */
public class RateLimitConstants {

    private RateLimitConstants() {
    }

    // ==================== 限流Key前缀 ====================
    
    /**
     * 验证码限流Key前缀
     */
    public static final String CAPTCHA_LIMIT_KEY_PREFIX = "limit:captcha:";
    
    /**
     * 注册限流Key前缀
     */
    public static final String REGISTER_LIMIT_KEY_PREFIX = "limit:register:";
    
    /**
     * 手机号注册限流Key前缀
     */
    public static final String REGISTER_PHONE_LIMIT_KEY_PREFIX = "limit:register:phone:";

    // ==================== 验证码接口限流配置 ====================
    
    /**
     * 验证码接口默认阈值（次/窗口时间）
     */
    public static final int CAPTCHA_DEFAULT_LIMIT = 10;
    
    /**
     * 验证码接口默认窗口时间（毫秒）- 60秒
     */
    public static final long CAPTCHA_DEFAULT_WINDOW_MS = 60000L;
    
    /**
     * 验证码Key过期时间（毫秒）- 略大于窗口时间
     */
    public static final long CAPTCHA_EXPIRE_MS = 70000L;

    // ==================== 注册接口限流配置 ====================
    
    /**
     * 注册接口默认阈值（次/窗口时间）
     */
    public static final int REGISTER_DEFAULT_LIMIT = 5;
    
    /**
     * 注册接口默认窗口时间（毫秒）- 60秒
     */
    public static final long REGISTER_DEFAULT_WINDOW_MS = 60000L;
    
    /**
     * 注册Key过期时间（毫秒）
     */
    public static final long REGISTER_EXPIRE_MS = 70000L;

    // ==================== 手机号注册限流配置 ====================
    
    /**
     * 单手机号注册默认阈值（次/窗口时间）
     */
    public static final int REGISTER_PHONE_DEFAULT_LIMIT = 3;
    
    /**
     * 单手机号注册窗口时间（毫秒）- 10分钟
     */
    public static final long REGISTER_PHONE_WINDOW_MS = 600000L;
    
    /**
     * 单手机号注册Key过期时间（毫秒）
     */
    public static final long REGISTER_PHONE_EXPIRE_MS = 610000L;

    // ==================== 白名单配置 ====================
    
    /**
     * 白名单IP列表（硬编码配置，可扩展为配置中心读取）
     * 测试环境常用IP - 生产环境应为空数组
     * 注意：本地开发时如需测试限流，请将此数组置空
     */
    public static final String[] WHITE_LIST_IPS = new String[] {
        // "127.0.0.1",  // 压测时需注释掉
        // "localhost"  // 压测时需注释掉
    };

    // ==================== 限流响应配置 ====================
    
    /**
     * 限流响应码
     */
    public static final int RATE_LIMIT_CODE = 429;
    
    /**
     * 限流响应消息
     */
    public static final String RATE_LIMIT_MESSAGE = "请求过于频繁，请稍后再试";
    
    /**
     * 验证码限流响应消息
     */
    public static final String CAPTCHA_RATE_LIMIT_MESSAGE = "验证码获取过于频繁，请稍后再试";
    
    /**
     * 注册限流响应消息
     */
    public static final String REGISTER_RATE_LIMIT_MESSAGE = "注册请求过于频繁，请稍后再试";
    
    /**
     * 手机号注册限流响应消息
     */
    public static final String REGISTER_PHONE_RATE_LIMIT_MESSAGE = "该手机号注册过于频繁，请稍后再试";

    // ==================== Redis连接池配置 ====================
    
    /**
     * 最大连接池数
     */
    public static final int REDIS_MAX_TOTAL = 200;
    
    /**
     * 最大空闲连接数
     */
    public static final int REDIS_MAX_IDLE = 50;
    
    /**
     * 最小空闲连接数
     */
    public static final int REDIS_MIN_IDLE = 10;
    
    /**
     * 获取连接最大等待时间（毫秒）
     */
    public static final long REDIS_MAX_WAIT_MILLIS = 3000L;
    
    /**
     * 连接最大存活时间（毫秒）
     */
    public static final long REDIS_MAX_IDLE_TIME = 1800000L;
}
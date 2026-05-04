package com.quxiangshe.backend.annotation;

import java.lang.annotation.*;

/**
 * 限流注解
 * 用于标记需要限流的接口
 * 
 * @author 趣享社技术团队
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    
    /**
     * 限流key前缀
     */
    String key() default "";
    
    /**
     * 最大请求数
     */
    int maxRequests() default 10;
    
    /**
     * 时间窗口（秒）
     */
    int windowSeconds() default 60;
    
    /**
     * 限流类型
     */
    LimitType type() default LimitType.FIXED_WINDOW;
    
    /**
     * 错误消息
     */
    String message() default "请求过于频繁，请稍后再试";
    
    /**
     * 限流类型枚举
     */
    enum LimitType {
        /** 固定窗口限流 */
        FIXED_WINDOW,
        /** 滑动窗口限流 */
        SLIDING_WINDOW
    }
}

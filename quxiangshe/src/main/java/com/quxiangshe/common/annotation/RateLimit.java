package com.quxiangshe.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解
 * 用于标识需要限流的方法，支持传入限流阈值、窗口时间等参数
 * 
 * 使用示例：
 * <pre>
 * {@code @RateLimit(limit = 10, windowMs = 60000)}
 * public void sendVerifyCode() { ... }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流维度类型
     */
    LimitType type() default LimitType.IP;

    /**
     * 限流阈值（次/窗口时间）
     * 默认值-1表示使用常量类中的默认值
     */
    int limit() default -1;

    /**
     * 窗口时间（毫秒）
     * 默认值-1表示使用常量类中的默认值
     */
    long windowMs() default -1;

    /**
     * 限流Key前缀
     * 用于区分不同接口的限流
     */
    String keyPrefix() default "";

    /**
     * 限流触发时的提示消息
     */
    String message() default "";

    /**
     * 是否启用限流
     * 可用于临时关闭限流
     */
    boolean enabled() default true;

    /**
     * 限流维度类型枚举
     */
    enum LimitType {
        /**
         * 基于IP限流
         */
        IP,
        
        /**
         * 基于用户ID限流
         */
        USER_ID,
        
        /**
         * 基于手机号限流
         */
        PHONE,
        
        /**
         * 基于IP+接口名限流
         */
        IP_AND_INTERFACE
    }
}
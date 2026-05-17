package com.quxiangshe.backend.annotation;

import java.lang.annotation.*;

/**
 * 限流注解
 * 
 * <p>通过AOP切面在方法执行前进行限流检查，支持方法级和类级标注。
 * 提供固定窗口和滑动窗口两种限流算法，可按接口、用户维度灵活配置。
 * 支持占位符动态解析（如{userId}），实现用户级别的精细化限流。</p>
 * 
 * <p>使用示例：<pre>
 * &#64;RateLimit(key = "note:like:{userId}", maxRequests = 30, windowSeconds = 60)
 * public Result likeNote(Long noteId, Long userId) { ... }
 * </pre></p>
 * 
 * @author 趣享社技术团队
 * @since 1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    
    /**
     * 限流key前缀，支持{参数名}占位符动态替换
     * <p>例如：key = "note:{userId}" 会替换为 "note:12345"</p>
     */
    String key() default "";
    
    /**
     * 时间窗口内允许的最大请求数
     */
    int maxRequests() default 10;
    
    /**
     * 限流时间窗口大小（秒）
     */
    int windowSeconds() default 60;
    
    /**
     * 限流算法类型
     * <ul>
     *   <li>FIXED_WINDOW - 固定窗口，实现简单，性能高</li>
     *   <li>SLIDING_WINDOW - 滑动窗口，流量更平滑，成本略高</li>
     * </ul>
     */
    LimitType type() default LimitType.FIXED_WINDOW;
    
    /**
     * 触发限流时返回的错误提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
    
    /**
     * 限流类型枚举，定义支持的限流算法
     */
    enum LimitType {
        /** 固定窗口限流：基于固定时间段计数，边界处可能出现流量突增 */
        FIXED_WINDOW,
        /** 滑动窗口限流：基于ZSet + Lua脚本，流量控制更平滑 */
        SLIDING_WINDOW
    }
}

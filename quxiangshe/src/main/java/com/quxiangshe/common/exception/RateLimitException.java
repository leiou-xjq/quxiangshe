package com.quxiangshe.common.exception;

/**
 * 限流异常
 * 当接口触发限流时抛出此异常，通过全局异常处理器捕获并返回JSON结果
 * 不抛运行时异常，优雅返回限流提示
 */
public class RateLimitException extends RuntimeException {

    /**
     * 限流响应码
     */
    private final int code;
    
    /**
     * 限流响应消息
     */
    private final String message;

    /**
     * 构造函数
     * 
     * @param code    限流响应码
     * @param message 限流响应消息
     */
    public RateLimitException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造函数（使用默认响应码）
     * 
     * @param message 限流响应消息
     */
    public RateLimitException(String message) {
        this(429, message);
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
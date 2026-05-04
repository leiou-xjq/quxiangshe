package com.quxiangshe.common.exception;

import com.quxiangshe.common.dto.BusinessError;
import lombok.Getter;

/**
 * 业务异常类
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误信息
     */
    private final String message;

    /**
     * 构造业务异常
     */
    public BusinessException(String message) {
        this(BusinessError.PARAM_ERROR.getCode(), message);
    }

    /**
     * 构造业务异常（带错误码）
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造业务异常（使用枚举）
     */
    public BusinessException(BusinessError error) {
        this(error.getCode(), error.getMessage());
    }

    /**
     * 构造业务异常（使用枚举，带自定义消息）
     */
    public BusinessException(BusinessError error, String customMessage) {
        this(error.getCode(), customMessage);
    }
}

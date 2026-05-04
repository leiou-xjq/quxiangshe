package com.quxiangshe.backend.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果封装类
 * 
 * @author 趣享社技术团队
 */
@Data
@Schema(description = "统一返回结果")
public class R<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 状态码
     */
    @Schema(description = "状态码: 0-成功, 其他-失败")
    private Integer code;
    
    /**
     * 消息
     */
    @Schema(description = "消息")
    private String message;
    
    /**
     * 数据
     */
    @Schema(description = "数据")
    private T data;
    
    /**
     * 成功状态码
     */
    public static final Integer SUCCESS_CODE = 0;
    
    /**
     * 失败状态码
     */
    public static final Integer ERROR_CODE = 500;
    
    /**
     * 私有构造函数
     */
    private R() {}
    
    /**
     * 成功返回（无数据）
     */
    public static <T> R<T> ok() {
        R<T> r = new R<>();
        r.setCode(SUCCESS_CODE);
        r.setMessage("success");
        return r;
    }
    
    /**
     * 成功返回（带数据）
     */
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(SUCCESS_CODE);
        r.setMessage("success");
        r.setData(data);
        return r;
    }
    
    /**
     * 成功返回（带消息和数据）
     */
    public static <T> R<T> ok(String message, T data) {
        R<T> r = new R<>();
        r.setCode(SUCCESS_CODE);
        r.setMessage(message);
        r.setData(data);
        return r;
    }
    
    /**
     * 失败返回
     */
    public static <T> R<T> fail() {
        R<T> r = new R<>();
        r.setCode(ERROR_CODE);
        r.setMessage("服务器内部错误");
        return r;
    }
    
    /**
     * 失败返回（带消息）
     */
    public static <T> R<T> fail(String message) {
        R<T> r = new R<>();
        r.setCode(ERROR_CODE);
        r.setMessage(message);
        return r;
    }
    
    /**
     * 失败返回（带状态码和消息）
     */
    public static <T> R<T> fail(Integer code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return SUCCESS_CODE.equals(this.code);
    }
}
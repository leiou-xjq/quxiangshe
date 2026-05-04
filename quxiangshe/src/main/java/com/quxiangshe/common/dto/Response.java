package com.quxiangshe.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果包装类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> {

    /**
     * 错误码：0表示成功
     */
    private int code;

    /**
     * 错误信息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应
     */
    public static <T> Response<T> success(T data) {
        return Response.<T>builder()
                .code(0)
                .message("success")
                .data(data)
                .build();
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> Response<T> success() {
        return success(null);
    }

    /**
     * 失败响应
     */
    public static <T> Response<T> error(int code, String message) {
        return Response.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }

    /**
     * 业务异常响应
     */
    public static <T> Response<T> error(BusinessError error) {
        return error(error.getCode(), error.getMessage());
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return this.code == 0;
    }
}

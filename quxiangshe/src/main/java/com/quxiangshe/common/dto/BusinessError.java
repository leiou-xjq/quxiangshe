package com.quxiangshe.common.dto;

import lombok.Getter;

/**
 * 业务错误码枚举
 */
@Getter
public enum BusinessError {

    // 通用错误 1000-1099
    SUCCESS(0, "success"),
    SYSTEM_ERROR(5001, "系统繁忙，请稍后重试"),
    PARAM_ERROR(1001, "请求参数错误"),
    NOT_FOUND(1002, "资源不存在"),
    UNAUTHORIZED(1003, "未授权，请先登录"),
    FORBIDDEN(1004, "无权限访问"),
    RATE_LIMIT(4001, "请求频率超限，请稍后重试"),

    // 用户模块错误 2000-2099
    USER_NOT_FOUND(2001, "用户不存在"),
    USERNAME_EXISTS(2002, "用户名已存在"),
    PHONE_EXISTS(2003, "手机号已被注册"),
    EMAIL_EXISTS(2004, "邮箱已被注册"),
    PASSWORD_ERROR(2005, "用户名或密码错误"),
    USER_DISABLED(2006, "用户已被禁用"),
    ALREADY_FOLLOWED(2007, "已关注该用户"),
    NOT_FOLLOWED(2008, "未关注该用户"),

    // 认证模块错误 3000-3099
    TOKEN_EXPIRED(3001, "令牌已过期"),
    TOKEN_INVALID(3002, "令牌无效"),
    REFRESH_TOKEN_EXPIRED(3003, "刷新令牌已过期"),
    REFRESH_TOKEN_INVALID(3004, "刷新令牌无效"),
    LOGIN_RATE_LIMIT(3005, "登录请求过于频繁，请稍后重试"),
    LOGIN_RATE_LIMIT_OLD_USER(3007, "登录请求过于频繁，可使用手机号验证码登录"),
    PHONE_NOT_EXISTS(3006, "该手机号未注册"),

    // 动态模块错误 4000-4099
    POST_NOT_FOUND(4001, "动态不存在"),
    POST_FORBIDDEN(4002, "无权限操作"),
    ALREADY_LIKED(4003, "已点赞"),
    NOT_LIKED(4004, "未点赞"),

    // 评论模块错误 5000-5099
    COMMENT_NOT_FOUND(5001, "评论不存在"),
    COMMENT_FORBIDDEN(5002, "无权限操作"),
    COMMENT_CONTENT_TOO_LONG(5003, "评论内容过长"),

    // 搜索模块错误 7000-7099
    SEARCH_ERROR(7001, "搜索服务异常");

    private final int code;
    private final String message;

    BusinessError(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

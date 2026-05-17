package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体类，对应数据库表 operation_log。
 * <p>
 * 记录管理员和用户的关键操作行为，包含操作人、操作模块、
 * 请求详情（方法/路径/参数/请求体）、响应结果（状态码/响应体）、
 * 客户端信息（IP/UserAgent）和执行耗时。
 * 用于安全审计、合规追溯和系统运维分析。
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Data
@TableName("operation_log")
public class OperationLog {
    
    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 操作用户ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 操作用户名
     */
    private String username;
    
    /**
     * 操作模块
     */
    private String module;
    
    /**
     * 操作类型
     */
    private String operation;
    
    /**
     * 请求方法
     */
    private String method;
    
    /**
     * 请求路径
     */
    private String endpoint;
    
    /**
     * HTTP请求方法
     */
    @TableField("request_method")
    private String requestMethod;
    
    /**
     * 请求参数
     */
    @TableField("request_params")
    private String requestParams;
    
    /**
     * 请求体
     */
    @TableField("request_body")
    private String requestBody;
    
    /**
     * 响应状态码
     */
    @TableField("response_status")
    private Integer responseStatus;
    
    /**
     * 响应体
     */
    @TableField("response_body")
    private String responseBody;
    
    /**
     * 客户端IP地址
     */
    @TableField("ip_address")
    private String ipAddress;
    
    /**
     * 用户代理
     */
    @TableField("user_agent")
    private String userAgent;
    
    /**
     * 执行耗时（毫秒）
     */
    @TableField("execution_time")
    private Integer executionTime;
    
    /**
     * 操作状态：0-失败，1-成功
     */
    private Integer status;
    
    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt = LocalDateTime.now();
}
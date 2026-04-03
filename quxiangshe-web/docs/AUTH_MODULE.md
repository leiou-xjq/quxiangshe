# 趣享社认证模块技术文档

## 一、模块概览
Auth 模块提供用户注册、登录、验证码服务、登录态验证、登出等核心认证能力，结合滑动窗口限流、IP 黑名单等安全机制，确保系统在高并发下的稳定性与安全性。技术栈：Spring Boot、Spring Security、JWT、Redis、Lua、RabbitMQ、MyBatis-Plus。

## 二、核心功能清单
- 注册/登录/验证码服务
- 登录态校验与登出
- 限流、IP 黑名单、降级容错、持久化策略
- 全局异常处理

## 三、技术架构与组件
- 核心组件：AuthController、AuthService、AuthServiceImpl、DTO、Entity、Mapper、Filter/JWT、SecurityConfig
- 限流组件：RateLimitAspect、LoginRateLimitAspect、RateLimitService、RateLimitPersistenceService、RedisLuaRateLimiter、LocalRateLimitCache、RedisHealthManager
- 黑名单组件：BlacklistUtil、LocalBlacklistCache、BlacklistPersistenceService
- 公共基础：JwtUtil、RedisUtil、Redis健康检查、全局异常处理
- 安全与运维：Cors、Token 认证、日志与审计

## 四、数据模型与持久化
- 用户表：t_user（id, username, phone, email, password_hash, nickname, avatar_url, bio, status, create_time）
- 黑名单/限流数据：通过 Redis 与本地缓存组合持久化策略实现，必要时写入数据库
- 重试与审计：验证码、登录日志按需落库

## 五、接口设计要点
- 注册、登录、验证码、刷新、登出等接口均遵循 OpenSpec 风格
- 统一返回结构：code、message、data
- 限流与错误码覆盖：429/400/401/500 等

---

*文档更新时间：2026-04-03*

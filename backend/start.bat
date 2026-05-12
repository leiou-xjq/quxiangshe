@echo off
REM ==========================================
REM 趣享社 - 本地启动脚本
REM ==========================================

echo.
echo ========================================
echo 正在启动趣享社后端服务...
echo ========================================
echo.
echo 请确保已修改 application-local.yml 中的：
echo   - MAIL_PASSWORD (QQ邮箱授权码)
echo   - JWT_SECRET (可选，已有默认值)
echo.

cd /d %~dp0

REM 检查 JAR 文件是否存在
if not exist "target\lixiang-backend-1.0.0.jar" (
    echo [错误] 未找到 JAR 文件，请先运行: mvn package -DskipTests
    pause
    exit /b 1
)

REM 启动 Spring Boot，使用本地配置
java -jar target\lixiang-backend-1.0.0.jar --spring.profiles.active=local

pause
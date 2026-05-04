@echo off
REM ==========================================
REM 趣享社后端启动脚本
REM ==========================================

REM 设置环境变量
set DOUBAO_API_KEY=ep-20260428080756-pbltx
set DOUBAO_ENDPOINT=ark-8f29c9b7-1a77-493e-8b24-1e6c5ddf6655-4a2e0
set DOUBAO_BASE_URL=https://ark.cn-beijing.volces.com/api/v3
set DOUBAO_MODEL=doubao-1-5-lite-32k-250115
set REVIEW_ENABLED=true
set VALUE_REVIEW_ENABLED=true
set REVIEW_ASYNC_ENABLED=true

REM 启动Spring Boot应用
cd /d "%~dp0backend"
java -jar target/quxiangshe-backend-1.0.0.jar

pause
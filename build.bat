@echo off
chcp 65001 >nul
echo ========================================
echo   理享 (YouLin) - 一键构建脚本
echo ========================================
echo.

set /p MODE="构建模式 (dev=开发 / prod=生产, 默认 prod): "
if "%MODE%"=="" set MODE=prod

echo.
echo [1/3] 构建前端...
cd /d "%~dp0frontend"
if "%MODE%"=="dev" (
    call npm run build
) else (
    call npm run build
)
if %ERRORLEVEL% neq 0 (
    echo 前端构建失败!
    pause
    exit /b 1
)
echo 前端构建完成

echo.
echo [2/3] 部署前端到后端...
cd /d "%~dp0backend"
if exist "src\main\resources\static" rmdir /s /q "src\main\resources\static"
mkdir "src\main\resources\static" 2>nul
xcopy /e /y "%~dp0frontend\dist\*" "src\main\resources\static\"
echo 前端部署完成

echo.
echo [3/3] 打包后端...
call mvn package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo 后端打包失败!
    pause
    exit /b 1
)
echo 后端打包完成

echo.
echo ========================================
echo   构建成功!
echo   输出文件: backend\target\lixiang-backend-1.0.0.jar
echo.
echo   本地运行: java -jar backend\target\lixiang-backend-1.0.0.jar
echo   Docker部署: docker-compose up -d
echo ========================================
pause
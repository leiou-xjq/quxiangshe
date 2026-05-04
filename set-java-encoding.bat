@echo off
echo ========================================
echo   设置Java UTF-8编码（永久生效）
echo ========================================
echo.

:: 设置用户级环境变量（不需要管理员权限，但需要重启命令行生效）
setx JAVA_TOOL_OPTIONS "-Dfile.encoding=UTF-8"

echo.
echo ✅ 环境变量已设置！
echo.
echo 请注意：
echo 1. 需要关闭当前命令行窗口，重新打开一个命令行
echo 2. 或者重启电脑
echo 3. 设置后所有Java程序启动都会默认使用UTF-8编码
echo.
pause
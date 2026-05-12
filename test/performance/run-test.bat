@echo off
REM ============================================
REM 趣享社压力测试执行脚本 (Windows)
REM ============================================

echo ============================================
echo 趣享社 压力测试执行脚本
echo ============================================

REM 设置 JMeter 路径（请根据实际情况修改）
set JMETER_HOME=D:\apache-jmeter-5.6.3
set JMETER_BIN=%JMETER_HOME%\bin\jmeter.bat
set TEST_PLAN=%~dp0quxiangshe-test-plan.jmx
set RESULTS_DIR=%~dp0results

REM 创建结果目录
if not exist "%RESULTS_DIR%" mkdir "%RESULTS_DIR%"

echo.
echo [1/5] 检查环境...
echo.

if not exist "%JMETER_BIN%" (
    echo ERROR: JMeter not found at %JMETER_BIN%
    echo Please set JMETER_HOME correctly in this script
    exit /b 1
)

echo JMeter found: %JMETER_BIN%
echo Test Plan: %TEST_PLAN%

echo.
echo [2/5] 启动服务（Docker Compose）...
echo.

cd %~dp0..\..
docker-compose up -d
timeout /t 30 /nobreak

echo.
echo [3/5] 等待服务就绪...
echo.

REM 健康检查
:health_check
curl -s http://localhost:8080/api/auth/health > nul 2>&1
if %errorlevel% neq 0 (
    echo Waiting for backend service...
    timeout /t 5 /nobreak
    goto health_check
)

echo Backend service is ready!

echo.
echo [4/5] 执行预热测试 (100线程, 5分钟)...
echo.

"%JMETER_BIN%" -n -t "%TEST_PLAN%" ^
    -l "%RESULTS_DIR%\warmup.log" ^
    -j "%RESULTS_DIR%\warmup-jmeter.log" ^
    -Jthread.scheduler=true ^
    -Jthread.duration=300 ^
    -Jthread.num_threads=100 ^
    -e -o "%RESULTS_DIR%\warmup-report"

echo.
echo [5/5] 执行正式压测...
echo.

REM 递增压测
echo Phase 1: 200线程 (5分钟)
"%JMETER_BIN%" -n -t "%TEST_PLAN%" ^
    -l "%RESULTS_DIR%\phase1-200threads.csv" ^
    -j "%RESULTS_DIR%\phase1-jmeter.log" ^
    -Jthread.scheduler=true ^
    -Jthread.duration=300 ^
    -Jthread.num_threads=200 ^
    -e -o "%RESULTS_DIR%\phase1-report"

echo Phase 2: 500线程 (5分钟)
"%JMETER_BIN%" -n -t "%TEST_PLAN%" ^
    -l "%RESULTS_DIR%\phase2-500threads.csv" ^
    -j "%RESULTS_DIR%\phase2-jmeter.log" ^
    -Jthread.scheduler=true ^
    -Jthread.duration=300 ^
    -Jthread.num_threads=500 ^
    -e -o "%RESULTS_DIR%\phase2-report"

echo Phase 3: 1000线程 (5分钟)
"%JMETER_BIN%" -n -t "%TEST_PLAN%" ^
    -l "%RESULTS_DIR%\phase3-1000threads.csv" ^
    -j "%RESULTS_DIR%\phase3-jmeter.log" ^
    -Jthread.scheduler=true ^
    -Jthread.duration=300 ^
    -Jthread.num_threads=1000 ^
    -e -o "%RESULTS_DIR%\phase3-report"

echo.
echo ============================================
echo 压力测试完成！
echo 结果目录: %RESULTS_DIR%
echo ============================================
echo.

REM 生成汇总报告
echo 生成汇总报告...
powershell -Command "Get-ChildItem '%RESULTS_DIR%\*.csv' | ForEach-Object { Import-Csv $_.FullName } | Export-Csv '%RESULTS_DIR%\summary-all.csv' -NoTypeInformation"

echo.
echo 报告生成完成！请查看 %RESULTS_DIR% 目录
pause

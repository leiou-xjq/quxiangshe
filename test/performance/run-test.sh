#!/bin/bash
# ============================================
# 趣享社压力测试执行脚本 (Linux/Mac)
# ============================================

set -e

echo "============================================"
echo "趣享社 压力测试执行脚本"
echo "============================================"

# 设置 JMeter 路径（请根据实际情况修改）
export JMETER_HOME="${JMETER_HOME:-/opt/apache-jmeter-5.6.3}"
export JMETER_BIN="${JMETER_HOME}/bin/jmeter"
export TEST_PLAN="$(dirname "$0")/quxiangshe-test-plan.jmx"
export RESULTS_DIR="$(dirname "$0")/results"

# 创建结果目录
mkdir -p "$RESULTS_DIR"

echo ""
echo "[1/5] 检查环境..."
echo ""

if [[ ! -f "$JMETER_BIN" ]]; then
    echo "ERROR: JMeter not found at $JMETER_BIN"
    echo "Please set JMETER_HOME correctly"
    exit 1
fi

echo "JMeter found: $JMETER_BIN"
echo "Test Plan: $TEST_PLAN"

echo ""
echo "[2/5] 启动服务（Docker Compose）..."
echo ""

cd "$(dirname "$0")/../.."
docker-compose up -d
sleep 30

echo ""
echo "[3/5] 等待服务就绪..."
echo ""

# 健康检查
MAX_RETRIES=30
RETRY_COUNT=0
until curl -s http://localhost:8080/api/auth/health > /dev/null 2>&1; do
    RETRY_COUNT=$((RETRY_COUNT + 1))
    if [[ $RETRY_COUNT -ge $MAX_RETRIES ]]; then
        echo "ERROR: Backend service failed to start"
        exit 1
    fi
    echo "Waiting for backend service... ($RETRY_COUNT/$MAX_RETRIES)"
    sleep 5
done

echo "Backend service is ready!"

echo ""
echo "[4/5] 执行预热测试 (100线程, 5分钟)..."
echo ""

"$JMETER_BIN" -n \
    -t "$TEST_PLAN" \
    -l "$RESULTS_DIR/warmup.log" \
    -j "$RESULTS_DIR/warmup-jmeter.log" \
    -Jthread.scheduler=true \
    -Jthread.duration=300 \
    -Jthread.num_threads=100 \
    -e -o "$RESULTS_DIR/warmup-report" \
    2>&1 | tee "$RESULTS_DIR/warmup-output.log"

echo ""
echo "[5/5] 执行正式压测..."
echo ""

# 递增压测
echo "Phase 1: 200线程 (5分钟)"
"$JMETER_BIN" -n \
    -t "$TEST_PLAN" \
    -l "$RESULTS_DIR/phase1-200threads.csv" \
    -j "$RESULTS_DIR/phase1-jmeter.log" \
    -Jthread.scheduler=true \
    -Jthread.duration=300 \
    -Jthread.num_threads=200 \
    -e -o "$RESULTS_DIR/phase1-report"

echo "Phase 2: 500线程 (5分钟)"
"$JMETER_BIN" -n \
    -t "$TEST_PLAN" \
    -l "$RESULTS_DIR/phase2-500threads.csv" \
    -j "$RESULTS_DIR/phase2-jmeter.log" \
    -Jthread.scheduler=true \
    -Jthread.duration=300 \
    -Jthread.num_threads=500 \
    -e -o "$RESULTS_DIR/phase2-report"

echo "Phase 3: 1000线程 (5分钟)"
"$JMETER_BIN" -n \
    -t "$TEST_PLAN" \
    -l "$RESULTS_DIR/phase3-1000threads.csv" \
    -j "$RESULTS_DIR/phase3-jmeter.log" \
    -Jthread.scheduler=true \
    -Jthread.duration=300 \
    -Jthread.num_threads=1000 \
    -e -o "$RESULTS_DIR/phase3-report"

echo ""
echo "============================================"
echo "压力测试完成！"
echo "结果目录: $RESULTS_DIR"
echo "============================================"
echo ""

# 生成汇总报告
echo "生成汇总报告..."
cat "$RESULTS_DIR"/*.csv > "$RESULTS_DIR/summary-all.csv" 2>/dev/null || true

echo ""
echo "报告生成完成！请查看 $RESULTS_DIR 目录"
echo ""
echo "推荐查看:"
echo "  - HTML报告: $RESULTS_DIR/*-report/index.html"
echo "  - CSV汇总: $RESULTS_DIR/summary-all.csv"

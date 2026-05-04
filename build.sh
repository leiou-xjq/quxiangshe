#!/bin/bash
set -e

echo "========================================"
echo "  理享 (LiXiang) - 一键构建脚本"
echo "========================================"
echo ""

MODE="${1:-prod}"

echo "[1/3] 构建前端..."
cd "$(dirname "$0")/frontend"
npm run build
echo "前端构建完成"

echo ""
echo "[2/3] 部署前端到后端..."
cd "$(dirname "$0")/backend"
rm -rf src/main/resources/static
mkdir -p src/main/resources/static
cp -r "$(dirname "$0")/frontend/dist/"* src/main/resources/static/
echo "前端部署完成"

echo ""
echo "[3/3] 打包后端..."
mvn package -DskipTests
echo "后端打包完成"

echo ""
echo "========================================"
echo "  构建成功!"
echo "  输出文件: backend/target/lixiang-backend-1.0.0.jar"
echo ""
echo "  本地运行: java -jar backend/target/lixiang-backend-1.0.0.jar"
echo "  Docker部署: docker compose up -d"
echo "========================================"
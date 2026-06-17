#!/usr/bin/env bash
# ============================================================
# 一键启动(开发模式,无需 MySQL — 用内置 H2 内存数据库)
# ============================================================
set -e
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "[1/2] 启动后端(端口 8080,dev profile + H2)..."
cd backend
# 检查 jar
if [[ ! -f "target/eval-platform.jar" ]]; then
  echo "未找到 target/eval-platform.jar,先打包..."
  mvn package -DskipTests -q
fi
nohup java -jar -Dspring.profiles.active=dev target/eval-platform.jar > ../backend.log 2>&1 &
echo "  → 后端 PID: $!,日志: backend.log"

echo ""
echo "[2/2] 启动前端(端口 5173)..."
cd ../frontend
if [[ ! -d "node_modules" ]]; then
  echo "安装前端依赖..."
  npm install --silent
fi
nohup npm run dev > ../frontend.log 2>&1 &
echo "  → 前端 PID: $!,日志: frontend.log"

echo ""
echo "============================================"
echo "启动完成!"
echo "  - 前端地址:http://localhost:5173"
echo "  - 后端 API:http://localhost:8080/api"
echo "  - Swagger:http://localhost:8080/swagger-ui.html"
echo "  - 测试账号:admin / admin123"
echo ""
echo "日志:tail -f backend.log / frontend.log"
echo "停止:bash deploy/stop-dev.sh"
echo "============================================"

#!/usr/bin/env bash
# ============================================================
# 多模型回答对比与评测平台 — 一键启动脚本 (Linux/macOS)
# 用法: bash deploy/start.sh {backend|frontend|all}
# 模块负责人:刘家豪
# ============================================================
set -e
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="${1:-all}"

log() { echo -e "\033[36m[$(date +%H:%M:%S)]\033[0m $*"; }
err() { echo -e "\033[31m[$(date +%H:%M:%S)] ERROR:\033[0m $*" >&2; }

start_backend() {
  log "▶ 启动后端 (Spring Boot) ..."
  cd "$ROOT_DIR/backend"
  if [[ -f "mvnw" ]]; then
    ./mvnw spring-boot:run
  else
    mvn spring-boot:run
  fi
}

start_frontend() {
  log "▶ 启动前端 (Vue 3 + Vite) ..."
  cd "$ROOT_DIR/frontend"
  if [[ ! -d "node_modules" ]]; then
    log "首次运行,先安装依赖 ..."
    npm install
  fi
  npm run dev
}

case "$TARGET" in
  backend)  start_backend ;;
  frontend) start_frontend ;;
  all)
    log "▶ 同时启动后端 + 前端 (并行)"
    ( start_backend ) &
    BPID=$!
    ( start_frontend ) &
    FPID=$!
    trap "kill $BPID $FPID 2>/dev/null || true" EXIT INT TERM
    wait
    ;;
  *)
    err "未知参数: $TARGET (支持 backend|frontend|all)"
    exit 1
    ;;
esac

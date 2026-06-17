#!/usr/bin/env bash
# 停止开发模式
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
echo "Stopping Java (Spring Boot)..."
pkill -f eval-platform.jar 2>/dev/null || true
echo "Stopping npm (Vite)..."
pkill -f "vite" 2>/dev/null || true
sleep 1
echo "Done."

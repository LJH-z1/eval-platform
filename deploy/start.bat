@echo off
REM ============================================================
REM 多模型回答对比与评测平台 — 一键启动脚本 (Windows)
REM 用法: deploy\start.bat {backend|frontend|all}
REM 模块负责人:刘家豪
REM ============================================================
setlocal
set "ROOT_DIR=%~dp0.."
set "TARGET=%~1"
if "%TARGET%"=="" set "TARGET=all"

if /I "%TARGET%"=="backend" goto :backend
if /I "%TARGET%"=="frontend" goto :frontend
if /I "%TARGET%"=="all" goto :all
echo [ERROR] 未知参数: %TARGET% (支持 backend^|frontend^|all)
exit /b 1

:backend
echo [INFO] 启动后端 (Spring Boot) ...
cd /d "%ROOT_DIR%\backend"
mvn spring-boot:run
goto :eof

:frontend
echo [INFO] 启动前端 (Vue 3 + Vite) ...
cd /d "%ROOT_DIR%\frontend"
if not exist "node_modules" (
  echo [INFO] 首次运行,先安装依赖 ...
  call npm install
)
call npm run dev
goto :eof

:all
echo [INFO] 同时启动后端 + 前端 (需开两个终端)
start "eval-backend" cmd /k "cd /d %ROOT_DIR%\backend && mvn spring-boot:run"
timeout /t 3 /nobreak > nul
start "eval-frontend" cmd /k "cd /d %ROOT_DIR%\frontend && npm run dev"
echo [INFO] 后端窗口: eval-backend
echo [INFO] 前端窗口: eval-frontend
goto :eof

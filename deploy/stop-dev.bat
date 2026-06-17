@echo off
REM 停止开发模式
echo Stopping Java (Spring Boot)...
taskkill /F /FI "WINDOWTITLE eq eval-backend*" >nul 2>&1
taskkill /F /IM java.exe /FI "MEMUSAGE gt 50000" >nul 2>&1
echo Stopping npm (Vite)...
taskkill /F /FI "WINDOWTITLE eq eval-frontend*" >nul 2>&1
echo Done.

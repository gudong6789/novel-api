@echo off
chcp 65001 >nul
echo ========================================
echo   小说管理后台 - PC 端
echo ========================================
echo.

REM 检查 Node.js
where node >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ✗ 未找到 Node.js
    echo.
    echo 请先安装 Node.js: https://nodejs.org/
    echo.
    pause
    exit /b 1
)

echo ✓ Node.js 版本:
node --version
echo.

REM 检查依赖
if not exist "node_modules" (
    echo 📦 首次运行，正在安装依赖...
    echo.
    call npm install
    if %ERRORLEVEL% NEQ 0 (
        echo ✗ 依赖安装失败
        pause
        exit /b 1
    )
    echo.
    echo ✓ 依赖安装完成
    echo.
)

echo 🚀 启动应用...
echo.

REM 启动应用
call npm start

pause

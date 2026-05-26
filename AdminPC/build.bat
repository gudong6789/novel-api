@echo off
chcp 65001 >nul
echo ========================================
echo   小说管理后台 - 打包工具
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
    echo 📦 正在安装依赖...
    echo.
    call npm install
    if %ERRORLEVEL% NEQ 0 (
        echo ✗ 依赖安装失败
        pause
        exit /b 1
    )
    echo.
)

echo 📦 开始打包...
echo.

REM 选择打包模式
echo 请选择打包模式:
echo 1. 安装程序版 (推荐)
echo 2. 便携版 (单文件)
echo.
set /p mode="请输入选项 (1 或 2，默认为 1): "

if "%mode%" == "2" (
    echo 正在打包便携版...
    call npm run build:portable
) else (
    echo 正在打包安装程序版...
    call npm run build
)

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   ✅ 打包完成!
    echo ========================================
    echo.
    echo 输出目录：dist\
    echo.
    REM 打开输出目录
    if exist "dist" (
        explorer dist
    )
) else (
    echo.
    echo ✗ 打包失败，请检查错误信息
)

echo.
pause

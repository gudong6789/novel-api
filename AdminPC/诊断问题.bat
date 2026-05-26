@echo off
chcp 65001 >nul
echo ========================================
echo   小说管理后台 - 诊断工具
echo ========================================
echo.

cd /d "%~dp0"

echo 1️⃣ 检查 Node.js
where node >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✓ Node.js 已安装
    node --version
) else (
    echo ✗ Node.js 未安装
    echo   请安装：https://nodejs.org/
    pause
    exit /b 1
)
echo.

echo 2️⃣ 检查 npm
where npm >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✓ npm 已安装
    npm --version
) else (
    echo ✗ npm 未安装
    pause
    exit /b 1
)
echo.

echo 3️⃣ 检查依赖
if exist "node_modules\electron" (
    echo ✓ Electron 已安装
) else (
    echo ⚠ Electron 未安装，正在安装...
    call npm install
)
echo.

echo 4️⃣ 检查必要文件
for %%f in (main.js preload.js index.html styles.css renderer.js) do (
    if exist "%%f" (
        echo ✓ %%f 存在
    ) else (
        echo ✗ %%f 缺失
    )
)
echo.

echo 5️⃣ 尝试启动应用
echo.
echo 启动中...
npx electron . --enable-logging --no-sandbox

echo.
echo ========================================
echo   诊断完成
echo ========================================
echo.
pause

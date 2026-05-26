@echo off
chcp 65001 >nul
title 小说管理后台
cd /d "%~dp0"

REM 使用完整路径启动 Electron
set ELECTRON_EXE=C:\Users\dqb\AppData\Local\easyclaw\ai\tool_cache\resources\tools\win\node-24.13.0\node_modules\electron\dist\electron.exe

if exist "%ELECTRON_EXE%" (
    "%ELECTRON_EXE%" .
) else (
    echo ✗ Electron 未找到，尝试使用 npx...
    npx electron .
)

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ✗ 启动失败
    echo.
    echo 建议使用 Web 管理后台:
    echo https://novel-api-production-55af.up.railway.app/admin
    echo.
    pause
)

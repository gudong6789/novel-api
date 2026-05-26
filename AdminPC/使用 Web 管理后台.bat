@echo off
chcp 65001 >nul
title 打开 Web 管理后台
echo.
echo ========================================
echo   正在打开 Web 管理后台...
echo ========================================
echo.
echo 如果浏览器没有自动打开，请访问:
echo https://novel-api-production-55af.up.railway.app/admin
echo.

REM 打开默认浏览器
start https://novel-api-production-55af.up.railway.app/admin

echo.
echo 💡 提示:
echo   - 可以将此网页添加到浏览器书签
echo   - 也可以创建桌面快捷方式
echo.
echo 按任意键关闭此窗口...
pause >nul

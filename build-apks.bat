@echo off
chcp 65001 >nul
echo ========================================
echo   小说自动阅读系统 - APK 编译工具
echo ========================================
echo.

REM 检查 Android Studio 命令行工具
set ANDROID_STUDIO_PATH=C:\Program Files\Android\Android Studio
set GRADLE_PATH=%ANDROID_STUDIO_PATH%\gradle\gradle-8.0\bin\gradle.bat

if exist "%GRADLE_PATH%" (
    echo ✓ 找到 Android Studio Gradle
    set GRADLE_CMD="%GRADLE_PATH%"
) else (
    REM 尝试使用 PATH 中的 gradle
    where gradle >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo ✓ 使用系统 Gradle
        set GRADLE_CMD=gradle
    ) else (
        echo ✗ 未找到 Gradle
        echo.
        echo 请使用以下方法之一编译:
        echo.
        echo 方法 1: 使用 Android Studio (推荐)
        echo   1. 打开 Android Studio
        echo   2. File -^> Open -^> 选择 ReaderApp 目录
        echo   3. Build -^> Build Bundle(s) / APK(s) -^> Build APK(s)
        echo   4. 对 AdminApp 重复上述步骤
        echo.
        echo 方法 2: 安装 Gradle
        echo   下载地址：https://gradle.org/install/
        echo.
        pause
        exit /b 1
    )
)

echo.
echo 开始编译 ReaderApp...
cd /d "%~dp0ReaderApp"
%GRADLE_CMD% assembleRelease

if %ERRORLEVEL% EQU 0 (
    echo ✓ ReaderApp 编译成功
    if exist "app\build\outputs\apk\release\app-release.apk" (
        echo   APK: ReaderApp\app\build\outputs\apk\release\app-release.apk
    )
) else (
    echo ✗ ReaderApp 编译失败
)

echo.
echo 开始编译 AdminApp...
cd /d "%~dp0AdminApp"
%GRADLE_CMD% assembleRelease

if %ERRORLEVEL% EQU 0 (
    echo ✓ AdminApp 编译成功
    if exist "app\build\outputs\apk\release\app-release.apk" (
        echo   APK: AdminApp\app\build\outputs\apk\release\app-release.apk
    )
) else (
    echo ✗ AdminApp 编译失败
)

echo.
echo ========================================
echo   编译完成
echo ========================================
echo.
pause

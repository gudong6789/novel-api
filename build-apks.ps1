# 小说自动阅读系统 - APK 编译脚本
# 使用方法：.\build-apks.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  小说自动阅读系统 - APK 编译工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查 Android Studio 安装
$androidStudioPaths = @(
    "C:\Program Files\Android\Android Studio\gradle\gradle-*\bin\gradle.bat",
    "$env:LOCALAPPDATA\Android\Android Studio\gradle\gradle-*\bin\gradle.bat",
    "C:\Program Files (x86)\Android\Android Studio\gradle\gradle-*\bin\gradle.bat"
)

$gradlePath = $null
foreach ($path in $androidStudioPaths) {
    $resolved = Get-Item -Path $path -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($resolved) {
        $gradlePath = $resolved.FullName
        break
    }
}

if (-not $gradlePath) {
    # 尝试使用系统 PATH 中的 gradle
    $gradleCmd = Get-Command gradle -ErrorAction SilentlyContinue
    if ($gradleCmd) {
        $gradlePath = $gradleCmd.Source
        Write-Host "✓ 使用系统 Gradle: $gradlePath" -ForegroundColor Green
    } else {
        Write-Host "✗ 未找到 Gradle" -ForegroundColor Red
        Write-Host ""
        Write-Host "请安装 Android Studio 或 Gradle:" -ForegroundColor Yellow
        Write-Host "  1. 安装 Android Studio: https://developer.android.com/studio"
        Write-Host "  2. 或者安装 Gradle: https://gradle.org/install/"
        Write-Host ""
        Write-Host "也可以使用 Android Studio 直接构建:" -ForegroundColor Yellow
        Write-Host "  1. 打开 Android Studio"
        Write-Host "  2. File -> Open -> 选择 ReaderApp 或 AdminApp 目录"
        Write-Host "  3. Build -> Build Bundle(s) / APK(s) -> Build APK(s)"
        exit 1
    }
} else {
    Write-Host "✓ 找到 Gradle: $gradlePath" -ForegroundColor Green
}

Write-Host ""

# 编译 ReaderApp
Write-Host "开始编译 ReaderApp..." -ForegroundColor Cyan
Set-Location "$PSScriptRoot\ReaderApp"

if (Test-Path "app\build\outputs\apk\release\app-release.apk") {
    Remove-Item "app\build\outputs\apk\release\app-release.apk" -Force
}

& $gradlePath assembleRelease

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ ReaderApp 编译成功!" -ForegroundColor Green
    $apkPath = "$PSScriptRoot\ReaderApp\app\build\outputs\apk\release\app-release.apk"
    if (Test-Path $apkPath) {
        Write-Host "  APK 位置：$apkPath" -ForegroundColor Green
    } else {
        # 尝试查找 debug APK
        $debugApk = "$PSScriptRoot\ReaderApp\app\build\outputs\apk\debug\app-debug.apk"
        if (Test-Path $debugApk) {
            Write-Host "  APK 位置：$debugApk (Debug 版本)" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "✗ ReaderApp 编译失败" -ForegroundColor Red
}

Write-Host ""

# 编译 AdminApp
Write-Host "开始编译 AdminApp..." -ForegroundColor Cyan
Set-Location "$PSScriptRoot\AdminApp"

if (Test-Path "app\build\outputs\apk\release\app-release.apk") {
    Remove-Item "app\build\outputs\apk\release\app-release.apk" -Force
}

& $gradlePath assembleRelease

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ AdminApp 编译成功!" -ForegroundColor Green
    $apkPath = "$PSScriptRoot\AdminApp\app\build\outputs\apk\release\app-release.apk"
    if (Test-Path $apkPath) {
        Write-Host "  APK 位置：$apkPath" -ForegroundColor Green
    } else {
        $debugApk = "$PSScriptRoot\AdminApp\app\build\outputs\apk\debug\app-debug.apk"
        if (Test-Path $debugApk) {
            Write-Host "  APK 位置：$debugApk (Debug 版本)" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "✗ AdminApp 编译失败" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  编译完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 📱 小说自动阅读系统 - APK 编译指南

由于系统中未安装 Gradle，需要使用 **Android Studio** 来编译 APK。

---

## ✅ 方法一：使用 Android Studio (推荐 ⭐⭐⭐)

这是最简单、最可靠的方法。

### 步骤 1: 打开 Android Studio

启动 **Android Studio** (如果未安装，请先下载安装)

### 步骤 2: 打开 ReaderApp 项目

1. 点击 **File** → **Open**
2. 浏览并选择文件夹：`C:\Users\dqb\.easyclaw\workspace\ReaderApp`
3. 点击 **OK**

### 步骤 3: 等待 Gradle 同步

- Android Studio 会自动下载依赖并同步项目
- 查看底部进度条，等待同步完成
- 如果提示升级 Gradle，点击 **Upgrade**

### 步骤 4: 编译 APK

1. 点击顶部菜单 **Build**
2. 选择 **Build Bundle(s) / APK(s)**
3. 选择 **Build APK(s)**

### 步骤 5: 等待编译完成

- 编译过程会在底部显示进度
- 编译完成后会弹出通知：**APK(s) generated successfully**
- 点击通知中的 **locate** 链接查看 APK 文件

### 步骤 6: 找到 APK 文件

APK 文件位置：
```
C:\Users\dqb\.easyclaw\workspace\ReaderApp\app\build\outputs\apk\release\app-release.apk
```

### 步骤 7: 编译 AdminApp

重复上述步骤，打开文件夹：
```
C:\Users\dqb\.easyclaw\workspace\AdminApp
```

---

## ✅ 方法二：使用命令行脚本 (如果已安装 Gradle)

如果已安装 Gradle，可以直接运行脚本：

### Windows PowerShell:
```powershell
cd C:\Users\dqb\.easyclaw\workspace
.\build-apks.ps1
```

### Windows CMD:
```cmd
cd C:\Users\dqb\.easyclaw\workspace
build-apks.bat
```

---

## 📥 安装 Android Studio

如果尚未安装 Android Studio：

1. **下载地址**: https://developer.android.com/studio
2. 下载 **Android Studio Hedgehog** (2023.1.1) 或更高版本
3. 运行安装程序，按照提示安装
4. 首次启动时，SDK 会自动下载安装

---

## 📋 编译前检查清单

- [ ] 已安装 Android Studio
- [ ] 已安装 JDK 17 (Android Studio 通常会自动安装)
- [ ] 网络连接正常 (用于下载依赖)
- [ ] 磁盘空间充足 (至少 5GB)

---

## 🔧 可能遇到的问题

### 问题 1: Gradle 同步失败

**错误信息**: `Could not resolve all files`

**解决方法**:
1. 检查网络连接
2. 点击 **File** → **Invalidate Caches** → **Invalidate and Restart**
3. 重试编译

---

### 问题 2: SDK 未找到

**错误信息**: `SDK location not found`

**解决方法**:
1. 打开 Android Studio
2. 点击 **Tools** → **SDK Manager**
3. 安装 **Android SDK Platform 34**
4. 重试编译

或者创建 `local.properties` 文件：

**ReaderApp/local.properties**:
```properties
sdk.dir=C\:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk
```

**AdminApp/local.properties**:
```properties
sdk.dir=C\:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk
```

---

### 问题 3: 内存不足

**错误信息**: `OutOfMemoryError`

**解决方法**:
编辑 `gradle.properties` 文件，增加内存：
```properties
org.gradle.jvmargs=-Xmx4096m
```

---

## 📱 安装 APK 到手机

### 方法一：使用 USB 数据线

1. 手机开启 **开发者选项** 和 **USB 调试**
2. 用数据线连接手机和电脑
3. 在 Android Studio 中点击 **Run** (绿色三角形)
4. 选择你的设备，点击 **OK**

### 方法二：使用 ADB 命令

```bash
# 检查设备连接
adb devices

# 安装 ReaderApp
adb install ReaderApp/app/build/outputs/apk/release/app-release.apk

# 安装 AdminApp
adb install AdminApp/app/build/outputs/apk/release/app-release.apk
```

### 方法三：直接传输 APK

1. 将 APK 文件复制到手机
2. 在手机上打开文件管理器
3. 找到 APK 文件，点击安装
4. 允许安装未知来源应用

---

## 🎯 快速验证

安装完成后：

1. **打开 ReaderApp**
   - 检查主界面是否正常显示
   - 尝试点击各个功能按钮

2. **打开 AdminApp**
   - 检查统计界面
   - 尝试登录后查看管理功能

3. **测试云端连接**
   - ReaderApp 应该能连接到 Railway API
   - AdminApp 应该能获取授权码列表

---

## 📞 需要帮助？

如果遇到问题：

1. 查看 Android Studio 的 **Build** 输出窗口，查找错误信息
2. 检查 `BUILD_GUIDE.md` 中的详细故障排查指南
3. 确保 API 地址配置正确

---

**最后更新**: 2026-05-25  
**项目版本**: 1.0.0

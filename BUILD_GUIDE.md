# APK 编译指南

## 🚀 快速编译

### 方法一：使用 Android Studio (推荐 ⭐)

**编译 ReaderApp:**
1. 打开 **Android Studio**
2. 点击 **File** → **Open**
3. 选择 `ReaderApp` 文件夹
4. 等待 Gradle 同步完成
5. 点击 **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
6. 编译完成后会显示通知，点击 **locate** 查看 APK 文件

**编译 AdminApp:**
1. 重复上述步骤，选择 `AdminApp` 文件夹

**APK 输出位置:**
```
ReaderApp/app/build/outputs/apk/release/app-release.apk
AdminApp/app/build/outputs/apk/release/app-release.apk
```

---

### 方法二：使用命令行脚本

**Windows PowerShell:**
```powershell
cd C:\Users\dqb\.easyclaw\workspace
.\build-apks.ps1
```

**Windows CMD:**
```cmd
cd C:\Users\dqb\.easyclaw\workspace
build-apks.bat
```

**前提条件:**
- 已安装 Android Studio，或
- 已安装 Gradle 并添加到系统 PATH

---

### 方法三：手动使用 Gradle

**环境要求:**
- JDK 17
- Android SDK 34
- Gradle 8.0+

**编译 ReaderApp:**
```bash
cd ReaderApp
gradle assembleRelease
```

**编译 AdminApp:**
```bash
cd AdminApp
gradle assembleRelease
```

---

## 📋 编译问题排查

### 1. Gradle 同步失败

**错误:** `Could not find com.android.application`

**解决:**
```bash
# 清理 Gradle 缓存
gradle clean --refresh-dependencies
```

### 2. SDK 未找到

**错误:** `SDK location not found`

**解决:**
- 在 Android Studio 中安装 SDK Platform 34
- 或创建 `local.properties` 文件指定 SDK 路径:
```properties
sdk.dir=C:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk
```

### 3. 内存不足

**错误:** `OutOfMemoryError`

**解决:**
- 编辑 `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m
```

### 4. 签名问题 (Release 版本)

如果要发布签名版本，需要配置签名：

**在 `app/build.gradle` 中添加:**
```groovy
android {
    ...
    signingConfigs {
        release {
            storeFile file('your-keystore.jks')
            storePassword 'your-store-password'
            keyAlias 'your-key-alias'
            keyPassword 'your-key-password'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

---

## 📱 安装 APK

### 使用 ADB 安装

```bash
# 安装 ReaderApp
adb install ReaderApp/app/build/outputs/apk/release/app-release.apk

# 安装 AdminApp
adb install AdminApp/app/build/outputs/apk/release/app-release.apk
```

### 直接传输安装

1. 将 APK 文件传输到手机
2. 在手机上打开 APK 文件
3. 允许安装未知来源应用
4. 点击安装

---

## 🔧 配置 API 地址

编译前请确认 API 地址已正确配置：

**ReaderApp:**
- 文件：`ReaderApp/app/src/main/java/com/reader/automation/api/ApiService.kt`
- 确认：`BASE_URL = "https://novel-api-production-55af.up.railway.app/api"`

**AdminApp:**
- 文件：`AdminApp/app/src/main/java/com/admin/manager/api/ApiService.kt`
- 确认：`BASE_URL = "https://novel-api-production-55af.up.railway.app/api"`

---

## 📦 APK 文件大小

预计大小：
- **ReaderApp**: ~15-20 MB
- **AdminApp**: ~10-15 MB

如果 APK 过大，可以启用 ProGuard 代码压缩：

```groovy
buildTypes {
    release {
        minifyEnabled true
        shrinkResources true
        proguardFiles ...
    }
}
```

---

## ✅ 验证编译

编译成功后，检查以下内容：

1. **APK 文件存在**
   ```bash
   ls ReaderApp/app/build/outputs/apk/release/
   ```

2. **APK 可以安装**
   ```bash
   adb install ReaderApp/app/build/outputs/apk/release/app-release.apk
   ```

3. **应用可以启动**
   - 在设备上打开应用
   - 检查主界面是否正常显示

---

## 🆘 常见问题

### Q: 编译太慢怎么办？

A: 使用 Gradle 守护进程和缓存：
```properties
# gradle.properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
```

### Q: 如何生成 Debug 版本？

A: 
```bash
gradle assembleDebug
```
Debug APK 位置：`app/build/outputs/apk/debug/app-debug.apk`

### Q: 如何批量编译两个项目？

A: 使用提供的脚本：
```powershell
.\build-apks.ps1
```

---

**更新时间**: 2026-05-25  
**适用版本**: 1.0.0

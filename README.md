# 小说自动阅读系统 - 完整项目

## 📦 项目结构

```
workspace/
├── ReaderApp/              # 阅读客户端 APK (Android)
│   ├── app/
│   └── build.gradle
│
├── AdminApp/               # 授权管理后台 APK (Android)
│   ├── app/
│   └── build.gradle
│
├── Server/                 # 云端 API 服务 + Web 管理后台 (Node.js)
│   ├── server.js
│   ├── admin/
│   │   └── index.html      # Web 管理界面
│   └── README.md
│
├── AdminPC/                # PC 管理后台 (Electron 桌面应用) ✨
│   ├── main.js
│   ├── index.html
│   ├── styles.css
│   ├── renderer.js
│   ├── run.bat             # 快速启动
│   ├── build.bat           # 打包工具
│   ├── README.md
│   └── 使用指南.md
│
├── build-apks.bat          # APK 编译脚本
├── build-apks.ps1          # APK 编译脚本 (PowerShell)
├── BUILD_GUIDE.md          # APK 编译指南
├── 如何编译 APK.md          # 中文编译指南
└── README.md               # 本文件
```

## 🎯 功能特性

### 阅读客户端 (ReaderApp)

| 功能 | 说明 | 状态 |
|------|------|------|
| 定时启动 | 每日 7:00-9:00 随机时段自动启动 | ✅ |
| 手动操控 | 一键启停任务 | ✅ |
| 真人模拟 | 1~2 小时随机阅读时长，5~10 秒随机滑动 | ✅ |
| 多 APP 轮换 | 多款小说 APP 自动切换 | ✅ |
| 单 APP 时长限制 | 单款 APP 单日最大 12 小时 | ✅ |
| 书籍管理 | 录入书单、按作者筛选 | ✅ |
| 进度管理 | 自动识别完结、跳过已读 | ✅ |
| 前台服务保活 | 双进程守护、防杀 | ✅ |
| 静默运行 | 无悬浮窗、无弹窗 | ✅ |
| 异常防护 | 网络断开红屏提示、自动重试 | ✅ |
| 授权管理 | 本地 + 云端双重校验 | ✅ |
| 开机自启 | 支持开机自动运行 | ✅ |

### 授权管理后台 (AdminApp - Android)

| 功能 | 说明 | 状态 |
|------|------|------|
| 授权码生成 | 单个/批量生成 | ✅ |
| 期限设置 | 1 天/7 天/30 天/永久 | ✅ |
| 授权封禁 | 随时封禁已发放授权 | ✅ |
| 设备管理 | 查看在线状态、运行数据 | ✅ |
| 全局配置 | 远程修改运行参数 | ✅ |
| 数据统计 | 挂机运行数据统计 | ✅ |

### Web 管理后台 (Server/admin)

| 功能 | 说明 | 状态 |
|------|------|------|
| 浏览器访问 | 无需安装，打开网页即可使用 | ✅ |
| 响应式设计 | 支持手机/平板/电脑 | ✅ |
| 数据概览 | 实时统计数据 | ✅ |
| 授权码管理 | 生成/批量/封禁/删除 | ✅ |
| 设备管理 | 在线状态/阅读数据 | ✅ |
| 全局配置 | 远程修改参数 | ✅ |

### PC 管理后台 (AdminPC - Electron) ✨ 新增

| 功能 | 说明 | 状态 |
|------|------|------|
| 桌面应用 | Windows 原生应用，独立运行 | ✅ |
| 数据概览 | 6 个统计卡片，实时数据 | ✅ |
| 授权码管理 | 单个/批量生成，封禁/删除 | ✅ |
| 设备管理 | 设备列表，在线状态 | ✅ |
| 全局配置 | 运行参数配置 | ✅ |
| 系统设置 | API 地址配置 | ✅ |
| 桌面通知 | 操作结果实时提醒 | ✅ |
| 本地存储 | 记住登录状态 | ✅ |

## 🚀 快速开始

### 1. 部署云端服务 (Railway)

```bash
# 登录 Railway: https://railway.app/
# 创建新项目 → 连接 GitHub 仓库
# 添加 MongoDB 插件
# 配置环境变量 (见 Server/README.md)
# 运行初始化脚本
npm run seed
```

**服务地址**:
- **API**: `https://novel-api-production-55af.up.railway.app/api`
- **Web 管理后台**: `https://novel-api-production-55af.up.railway.app/admin`

### 2. 使用 PC 管理后台 (推荐 ⭐)

**方式一：直接运行**
```bash
cd AdminPC
.\run.bat
```

**方式二：打包成安装包**
```bash
cd AdminPC
.\build.bat
# 选择打包模式
# 生成的安装包在 dist/ 目录
```

### 3. 构建 Android APK

**阅读客户端:**
```bash
cd ReaderApp
./gradlew assembleRelease
```

**授权管理后台:**
```bash
cd AdminApp
./gradlew assembleRelease
```

或使用脚本：
```bash
.\build-apks.bat
```

### 4. 安装使用

1. 在目标设备安装 `ReaderApp`
2. 使用 PC 管理后台或 Web 管理后台生成授权码
3. 在 ReaderApp 中输入授权码激活
4. 在管理后台查看设备状态和数据

## 📡 API 接口概览

### 认证授权
- `POST /api/auth/verify` - 验证授权码
- `POST /api/auth/generate` - 生成授权码
- `POST /api/auth/generate-batch` - 批量生成
- `GET /api/auth/list` - 获取授权码列表
- `POST /api/auth/block` - 封禁/解封
- `DELETE /api/auth/delete/:code` - 删除授权码

### 设备管理
- `POST /api/device/report` - 上报设备信息
- `POST /api/device/heartbeat` - 心跳保活
- `GET /api/device/list` - 设备列表
- `GET /api/device/detail/:id` - 设备详情

### 配置管理
- `GET /api/config/global` - 获取全局配置
- `POST /api/config/global` - 更新全局配置

### 数据统计
- `GET /api/statistics` - 统计数据

### 管理员
- `POST /api/admin/login` - 管理员登录

**详细文档**: [Server/README.md](Server/README.md)

## 🖥️ 管理端对比

| 特性 | PC 管理后台 | Web 管理后台 | Android 管理后台 |
|------|-------------|--------------|------------------|
| 平台 | Windows | 浏览器 | Android |
| 安装 | 需要 | 不需要 | 需要 |
| 离线使用 | ❌ | ❌ | ❌ |
| 桌面通知 | ✅ | ❌ | ✅ |
| 响应速度 | ⚡ 快 | 🐢 中 | ⚡ 快 |
| 推荐场景 | 日常办公 | 临时管理 | 移动管理 |

## 🛠️ 技术栈

### Android
- **语言**: Kotlin 1.9.0
- **SDK**: Android 34
- **架构**: MVVM
- **数据库**: Room
- **网络**: Retrofit + OkHttp

### 后端
- **运行时**: Node.js 18+
- **框架**: Express
- **数据库**: MongoDB + Mongoose
- **认证**: JWT + bcrypt
- **部署**: Railway

### Web 管理后台
- **纯前端**: HTML + CSS + JavaScript
- **无需构建**: 直接部署静态文件
- **响应式**: 支持手机/平板/电脑

### PC 管理后台
- **框架**: Electron 28
- **语言**: JavaScript
- **打包**: electron-builder
- **支持**: Windows 10/11

## ⚠️ 注意事项

1. **合规使用** - 此类自动化工具可能违反小说平台的服务条款
2. **无障碍服务** - 滥用可能被系统限制
3. **反作弊机制** - 部分平台可能检测到自动化行为
4. **合法用途** - 仅用于个人学习研究

## 📖 详细文档

- [云端 API 部署指南](Server/README.md)
- [Web 管理后台使用](Server/README.md#管理后台-web-界面)
- [PC 管理后台使用](AdminPC/使用指南.md)
- [APK 编译指南](BUILD_GUIDE.md)
- [如何编译 APK.md](如何编译%20APK.md)

---

**版本**: 1.0.0  
**创建时间**: 2026-05-24  
**更新时间**: 2026-05-26  
**技术栈**: Kotlin + AndroidX + Room + Retrofit + Node.js + Express + MongoDB + Electron

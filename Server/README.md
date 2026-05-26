# 云端 API 服务部署指南

## 📦 项目结构

```
Server/
├── server.js              # 主入口文件
├── package.json           # 依赖配置
├── .env                   # 环境变量
├── scripts/
│   └── seed-admin.js      # 初始化管理员脚本
└── README.md              # 本文件
```

## 🚀 部署到 Railway

### 方式一：一键部署 (推荐)

1. 登录 [Railway](https://railway.app/)
2. 点击 **New Project** → **Deploy from GitHub repo**
3. 选择你的仓库
4. Railway 会自动检测 `package.json` 并部署

### 方式二：手动部署

1. **安装依赖**
```bash
cd Server
npm install
```

2. **配置环境变量**

在 Railway 面板中添加以下变量：

| 变量名 | 值 | 说明 |
|--------|-----|------|
| `MONGODB_URI` | `mongodb://...` | Railway 会自动提供 MongoDB 连接字符串 |
| `JWT_SECRET` | 随机字符串 | JWT 签名密钥 |
| `ADMIN_USERNAME` | `admin` | 管理员用户名 |
| `ADMIN_PASSWORD` | 强密码 | 管理员密码 |
| `PORT` | `3000` | 服务端口 |

3. **初始化数据库**

在 Railway 中打开 **Shell**，运行：
```bash
npm run seed
```

4. **查看日志**
```bash
# Railway 面板 → Logs
```

## 🔧 本地开发

### 1. 安装 MongoDB

**Windows:**
```bash
# 下载安装 MongoDB Community Server
# https://www.mongodb.com/try/download/community
```

**macOS:**
```bash
brew install mongodb-community
brew services start mongodb-community
```

**Docker:**
```bash
docker run -d -p 27017:27017 --name mongodb mongo:latest
```

### 2. 安装依赖
```bash
cd Server
npm install
```

### 3. 配置环境变量
编辑 `.env` 文件：
```env
MONGODB_URI=mongodb://localhost:27017/novel-automation
JWT_SECRET=your-secret-key-change-in-production
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123
```

### 4. 初始化管理员
```bash
npm run seed
```

### 5. 启动服务
```bash
# 开发模式 (自动重启)
npm run dev

# 生产模式
npm start
```

## 🖥️ 管理后台 Web 界面

**访问地址**:
```
https://novel-api-production-55af.up.railway.app/admin
```

### 功能特性

- ✅ 数据统计概览 (授权码/设备/阅读时长)
- ✅ 授权码管理 (生成/批量生成/封禁/删除)
- ✅ 设备管理 (在线状态/阅读数据)
- ✅ 全局配置 (远程修改运行参数)
- ✅ 响应式设计 (支持手机/平板/电脑)

### 登录方式

使用管理员账号登录：
- 用户名：`admin` (或环境变量中配置的 `ADMIN_USERNAME`)
- 密码：部署时设置的密码

---

## 📡 API 接口文档

### 基础地址

**API 接口地址**:
```
https://novel-api-production-55af.up.railway.app/api
```

**管理后台 Web 界面**:
```
https://novel-api-production-55af.up.railway.app/admin
```

### 健康检查
```http
GET /api/health
```

**响应:**
```json
{
  "success": true,
  "message": "API 服务运行正常",
  "timestamp": "2026-05-25T04:35:00.000Z"
}
```

---

### 管理员登录
```http
POST /api/admin/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**响应:**
```json
{
  "success": true,
  "message": "登录成功",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "adminInfo": {
    "id": "xxx",
    "username": "admin",
    "role": "admin",
    "permissions": ["*"]
  }
}
```

---

### 验证授权码 (阅读客户端)
```http
POST /api/auth/verify
Content-Type: application/json

{
  "authCode": "ABCD-1234-EFGH-5678",
  "deviceId": "device-uuid",
  "deviceModel": "Xiaomi 14",
  "appVersion": "1.0.0"
}
```

**响应:**
```json
{
  "success": true,
  "message": "验证成功",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expireTime": 1719360000000,
  "config": {
    "startHour": 7,
    "endHour": 9,
    "scrollIntervalMin": 5000,
    "scrollIntervalMax": 10000,
    "singleReadDurationMin": 3600000,
    "singleReadDurationMax": 7200000,
    "maxAppDailyTime": 43200000,
    "totalDailyTime": 86400000
  }
}
```

---

### 生成授权码 (管理后台)
```http
POST /api/auth/generate
Authorization: Bearer <token>
Content-Type: application/json

{
  "durationType": 30,
  "notes": "测试授权"
}
```

**durationType 说明:**
- `1` = 1 天
- `7` = 7 天
- `30` = 30 天
- `0` = 永久

**响应:**
```json
{
  "success": true,
  "message": "生成成功",
  "authCode": "ABCD-1234-EFGH-5678",
  "expireTime": 1719360000000
}
```

---

### 批量生成授权码
```http
POST /api/auth/generate-batch
Authorization: Bearer <token>
Content-Type: application/json

{
  "count": 10,
  "durationType": 30,
  "notes": "批量生成"
}
```

**响应:**
```json
{
  "success": true,
  "message": "成功生成 10 个授权码",
  "codes": [
    "AAAA-1111-BBBB-2222",
    "CCCC-3333-DDDD-4444",
    ...
  ]
}
```

---

### 获取授权码列表
```http
GET /api/auth/list?page=1&pageSize=50
Authorization: Bearer <token>
```

---

### 封禁/解封授权码
```http
POST /api/auth/block
Authorization: Bearer <token>
Content-Type: application/json

{
  "code": "ABCD-1234-EFGH-5678",
  "blocked": true
}
```

---

### 删除授权码
```http
DELETE /api/auth/delete/ABCD-1234-EFGH-5678
Authorization: Bearer <token>
```

---

### 上报设备信息
```http
POST /api/device/report
Authorization: Bearer <token>
Content-Type: application/json

{
  "deviceId": "device-uuid",
  "deviceName": "我的设备",
  "deviceModel": "Xiaomi 14",
  "androidVersion": "14",
  "appVersion": "1.0.0",
  "installedApps": ["com.qidian.QDReader", "com.fanqie.reader"],
  "authCode": "ABCD-1234-EFGH-5678"
}
```

---

### 心跳保活
```http
POST /api/device/heartbeat
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "online",
  "batteryLevel": 80,
  "networkType": "WiFi",
  "timestamp": 1716638100000
}
```

---

### 获取设备列表
```http
GET /api/device/list?page=1&pageSize=50
Authorization: Bearer <token>
```

---

### 获取设备详情
```http
GET /api/device/detail/device-uuid
Authorization: Bearer <token>
```

---

### 获取全局配置
```http
GET /api/config/global
```

---

### 更新全局配置
```http
POST /api/config/global
Authorization: Bearer <token>
Content-Type: application/json

{
  "startHour": 8,
  "endHour": 10,
  "scrollIntervalMin": 6000,
  "scrollIntervalMax": 12000,
  "singleReadDurationMin": 7200000,
  "singleReadDurationMax": 14400000,
  "maxAppDailyTime": 43200000,
  "totalDailyTime": 86400000
}
```

---

### 获取统计数据
```http
GET /api/statistics
Authorization: Bearer <token>
```

**响应:**
```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "totalCodes": 100,
    "activeCodes": 50,
    "usedCodes": 45,
    "unusedCodes": 55,
    "totalDevices": 45,
    "onlineDevices": 30,
    "totalReadTime": 3600000000,
    "todayReadTime": 144000000
  }
}
```

## 🔒 安全建议

1. **修改默认密码** - 部署后立即修改 `ADMIN_PASSWORD`
2. **使用 HTTPS** - Railway 默认提供 HTTPS
3. **定期备份数据库** - 使用 MongoDB Atlas 或定期导出
4. **监控日志** - 定期检查异常请求
5. **限制 API 访问** - 可添加 IP 白名单

## 📊 数据库集合

| 集合名 | 说明 |
|--------|------|
| `authcodes` | 授权码数据 |
| `devices` | 设备信息 |
| `admins` | 管理员账号 |
| `globalconfigs` | 全局配置 |

## 🛠️ 故障排查

### MongoDB 连接失败
```bash
# 检查 MongoDB 是否运行
mongosh --eval "db.version()"

# 检查连接字符串
echo $MONGODB_URI
```

### 端口被占用
```bash
# 修改 .env 中的 PORT
PORT=3001
```

### 查看日志
```bash
# Railway 面板 → Logs
# 或本地运行
npm run dev
```

## 📞 技术支持

如有问题，请检查：
1. Railway 日志
2. MongoDB 连接状态
3. 环境变量配置
4. API 请求格式

---

**版本**: 1.0.0  
**更新时间**: 2026-05-25

/**
 * 小说自动阅读系统 - 云端 API 服务
 * 部署平台：Railway
 */

require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');

const app = express();

// ============ 中间件 ============

// 安全头
app.use(helmet());

// CORS
app.use(cors({
  origin: '*',
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));

// 日志
app.use(morgan('combined'));

// 解析 JSON
app.use(express.json({ limit: '10mb' }));

// 速率限制
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 分钟
  max: 1000, // 每个 IP 最多 1000 请求
  message: { success: false, message: '请求过于频繁，请稍后重试' }
});
app.use('/api/', limiter);

// ============ 数据库连接 ============

const connectDB = async () => {
  try {
    await mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/novel-automation');
    console.log('✅ MongoDB 连接成功');
  } catch (error) {
    console.error('❌ MongoDB 连接失败:', error.message);
    process.exit(1);
  }
};

// ============ 数据模型 ============

const AuthCodeSchema = new mongoose.Schema({
  code: { type: String, required: true, unique: true },
  durationType: { type: Number, required: true },
  durationDays: { type: Number, required: true },
  expireTime: { type: Date },
  isActive: { type: Boolean, default: false },
  isBlocked: { type: Boolean, default: false },
  deviceId: { type: String },
  activatedTime: { type: Date },
  notes: { type: String, default: '' },
  creator: { type: String, default: 'admin' },
  createdAt: { type: Date, default: Date.now }
});

const DeviceSchema = new mongoose.Schema({
  deviceId: { type: String, required: true, unique: true },
  deviceName: { type: String },
  deviceModel: { type: String },
  androidVersion: { type: String },
  authCode: { type: String, required: true },
  isOnline: { type: Boolean, default: false },
  lastActiveTime: { type: Date, default: Date.now },
  todayReadTime: { type: Number, default: 0 },
  totalReadTime: { type: Number, default: 0 },
  installedApps: [{ type: String }],
  readingHistory: [{
    packageName: String,
    bookTitle: String,
    readProgress: Number,
    readDuration: Number,
    timestamp: Date
  }],
  createdAt: { type: Date, default: Date.now }
});

const AdminSchema = new mongoose.Schema({
  username: { type: String, required: true, unique: true },
  password: { type: String, required: true },
  role: { type: String, default: 'admin' },
  permissions: [{ type: String }],
  createdAt: { type: Date, default: Date.now }
});

const GlobalConfigSchema = new mongoose.Schema({
  startHour: { type: Number, default: 7 },
  endHour: { type: Number, default: 9 },
  scrollIntervalMin: { type: Number, default: 5000 },
  scrollIntervalMax: { type: Number, default: 10000 },
  singleReadDurationMin: { type: Number, default: 3600000 },
  singleReadDurationMax: { type: Number, default: 7200000 },
  maxAppDailyTime: { type: Number, default: 43200000 },
  totalDailyTime: { type: Number, default: 86400000 },
  updatedAt: { type: Date, default: Date.now }
});

const AuthCode = mongoose.model('AuthCode', AuthCodeSchema);
const Device = mongoose.model('Device', DeviceSchema);
const Admin = mongoose.model('Admin', AdminSchema);
const GlobalConfig = mongoose.model('GlobalConfig', GlobalConfigSchema);

// ============ 工具函数 ============

const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');

// 生成授权码
function generateAuthCode() {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  let code = '';
  for (let i = 0; i < 12; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return code.match(/.{1,4}/g).join('-');
}

// 验证 JWT token
function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];
  
  if (!token) {
    return res.status(401).json({ success: false, message: '未提供认证令牌' });
  }
  
  jwt.verify(token, process.env.JWT_SECRET, (err, user) => {
    if (err) {
      return res.status(403).json({ success: false, message: '认证令牌无效或已过期' });
    }
    req.user = user;
    next();
  });
}

// ============ 路由 ============

// --- 健康检查 ---
app.get('/api/health', (req, res) => {
  res.json({ success: true, message: 'API 服务运行正常', timestamp: new Date() });
});

// --- 管理员登录 ---
app.post('/api/admin/login', async (req, res) => {
  try {
    const { username, password } = req.body;
    
    if (!username || !password) {
      return res.status(400).json({ success: false, message: '用户名和密码不能为空' });
    }
    
    const admin = await Admin.findOne({ username });
    if (!admin) {
      return res.status(401).json({ success: false, message: '用户名或密码错误' });
    }
    
    const validPassword = await bcrypt.compare(password, admin.password);
    if (!validPassword) {
      return res.status(401).json({ success: false, message: '用户名或密码错误' });
    }
    
    const token = jwt.sign(
      { id: admin._id, username: admin.username, role: admin.role },
      process.env.JWT_SECRET,
      { expiresIn: process.env.JWT_EXPIRE }
    );
    
    res.json({
      success: true,
      message: '登录成功',
      token,
      adminInfo: {
        id: admin._id,
        username: admin.username,
        role: admin.role,
        permissions: admin.permissions
      }
    });
  } catch (error) {
    console.error('管理员登录错误:', error);
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// --- 验证授权码 ---
app.post('/api/auth/verify', async (req, res) => {
  try {
    const { authCode, deviceId, deviceModel, appVersion } = req.body;
    
    if (!authCode) {
      return res.status(400).json({ success: false, message: '授权码不能为空' });
    }
    
    const codeDoc = await AuthCode.findOne({ code: authCode });
    
    if (!codeDoc) {
      return res.status(404).json({ success: false, message: '授权码不存在' });
    }
    
    if (codeDoc.isBlocked) {
      return res.status(403).json({ success: false, message: '授权码已被封禁' });
    }
    
    const now = new Date();
    if (codeDoc.expireTime && now > codeDoc.expireTime) {
      return res.status(403).json({ success: false, message: '授权码已过期' });
    }
    
    // 激活授权码
    if (!codeDoc.isActive) {
      codeDoc.isActive = true;
      codeDoc.deviceId = deviceId;
      codeDoc.activatedTime = now;
      await codeDoc.save();
    }
    
    // 生成 token
    const token = jwt.sign(
      { deviceId, authCode, deviceModel },
      process.env.JWT_SECRET,
      { expiresIn: '365d' }
    );
    
    // 获取全局配置
    let config = await GlobalConfig.findOne();
    if (!config) {
      config = await GlobalConfig.create({});
    }
    
    res.json({
      success: true,
      message: '验证成功',
      token,
      expireTime: codeDoc.expireTime ? codeDoc.expireTime.getTime() : null,
      config: {
        startHour: config.startHour,
        endHour: config.endHour,
        scrollIntervalMin: config.scrollIntervalMin,
        scrollIntervalMax: config.scrollIntervalMax,
        singleReadDurationMin: config.singleReadDurationMin,
        singleReadDurationMax: config.singleReadDurationMax,
        maxAppDailyTime: config.maxAppDailyTime,
        totalDailyTime: config.totalDailyTime
      }
    });
  } catch (error) {
    console.error('验证授权码错误:', error);
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// --- 生成授权码 (单个) ---
app.post('/api/auth/generate', authenticateToken, async (req, res) => {
  try {
    const { durationType, notes } = req.body;
    
    const durationDays = durationType === 0 ? 0 : durationType;
    const expireTime = durationType === 0 ? null : new Date(Date.now() + durationDays * 24 * 60 * 60 * 1000);
    
    const authCode = new AuthCode({
      code: generateAuthCode(),
      durationType,
      durationDays,
      expireTime,
      notes: notes || '',
      creator: req.user.username
    });
    
    await authCode.save();
    
    res.json({
      success: true,
      message: '生成成功',
      authCode: authCode.code,
      expireTime: authCode.expireTime ? authCode.expireTime.getTime() : null
    });
  } catch (error) {
    console.error('生成授权码错误:', error);
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// --- 批量生成授权码 ---
app.post('/api/auth/generate-batch', authenticateToken, async (req, res) => {
  try {
    const { count, durationType, notes } = req.body;
    
    if (!count || count < 1 || count > 1000) {
      return res.status(400).json({ success: false, message: '生成数量必须在 1-1000 之间' });
    }
    
    const durationDays = durationType === 0 ? 0 : durationType;
    const expireTime = durationType === 0 ? null : new Date(Date.now() + durationDays * 24 * 60 * 60 * 1000);
    
    const codes = [];
    for (let i = 0; i < count; i++) {
      codes.push({
        code: generateAuthCode(),
        durationType,
        durationDays,
        expireTime,
        notes: notes || '',
        creator: req.user.username
      });
    }
    
    await AuthCode.insertMany(codes);
    
    res.json({
      success: true,
      message: `成功生成 ${count} 个授权码`,
      codes: codes.map(c => c.code)
    });
  } catch (error) {
    console.error('批量生成授权码错误:', error);
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// --- 获取授权码列表 ---
app.get('/api/auth/list', authenticateToken, async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const pageSize = parseInt(req.query.pageSize) || 50;
    
    const total = await AuthCode.countDocuments();
    const list = await AuthCode.find()
      .sort({ createdAt: -1 })
      .skip((page - 1) * pageSize)
      .limit(pageSize);
    
    res.json({
      success: true,
      message: '获取成功',
      data: {
        total,
        list: list.map(item => ({
          code: item.code,
          durationType: item.durationType,
          durationDays: item.durationDays,
          expireTime: item.expireTime ? item.expireTime.getTime() : null,
          isActive: item.isActive,
          isBlocked: item.isBlocked,
          deviceId: item.deviceId,
          activatedTime: item.activatedTime ? item.activatedTime.getTime() : null,
          notes: item.notes,
          creator: item.creator
        }))
      }
    });
  } catch (error) {
    console.error('获取授权码列表错误:', error);
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// --- 封禁/解封授权码 ---
app.post('/api/auth/block', authenticateToken, async (req, res) => {
  try {
    const { code, blocked } = req.body;
    
    const codeDoc = await AuthCode.findOne({ code });
    if (!codeDoc) {
      return res.status(404).json({ success: false, message: '授权码不存在' });
    }
    
    codeDoc.isBlocked = blocked;
    await codeDoc.save();
    
    res.json({
      success: true,
      message: blocked ? '已封禁' : '已解封'
    });
  } catch (error) {
    console.error('封禁授权码错误:', error);
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// --- 删除授权码 ---
app.delete('/api/auth/delete/:code', authenticateToken, async (req, res) => {
  try {
    const { code } = req.params;
    
    const codeDoc = await AuthCode.findOneAndDelete({ code });
    if (!codeDoc) {
      return res.status(404).json({ success: false, message: '授权码不存在' });
    }
    
    res.json({
      success: true,
      message: '删除成功'
    });
  } catch (error) {
    console.error('删除授权码错误:', error);
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// --- 上报设备信息 ---
app.post('/api/device/report', authenticateToken, async (req, res) => {
  try {
    const { deviceId, deviceName, deviceModel, androidVersion, appVersion, installedApps, authCode } = req.body;
    
    let device = await Device.findOne({ deviceId });
    
    if (device) {
      device.deviceName = deviceName || device.deviceName;
      device.deviceModel = deviceModel || device.deviceModel;
      device.androidVersion = androidVersion || device.androidVersion;
      device.installedApps = installedApps || device.installedApps;
      device.lastActiveTime = new Date();
      device.isOnline = true;
      await device.save();
    } else {
      device = await Device.create({
        deviceId,
        deviceName,
        deviceModel,
        androidVersion,
        authCode,
        installedApps,
        isOnline: true
      });
    }
    
    res.json({
      success: true,
      message: '上报成功'
    });
  } catch (error) {
    console.error('上报设备信息错误:', error);
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// --- 心跳保活 ---
app.post('/api/device/heartbeat', authenticateToken, async (req, res) => {
  try {
    const { status, batteryLevel, networkType, timestamp } = req.body;
    const deviceId = req.user.deviceId;
    
    await Device.findOneAndUpdate(
      { deviceId },
      {
        isOnline: true,
        lastActiveTime: new Date(timestamp || Date.now())
      }
    );
    
    res.json({
      success: true,
      message: '心跳成功'
    });
  } catch (error) {
    console.error('心跳保活错误:', error);
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// --- 获取设备列表 ---
app.get('/api/device/list', authenticateToken, async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const pageSize = parseInt(req.query.pageSize) || 50;
    
    const total = await Device.countDocuments();
    const list = await Device.find()
      .sort({ lastActiveTime: -1 })
      .skip((page - 1) * pageSize)
      .limit(pageSize);
    
    res.json({
      success: true,
      message: '获取成功',
      data: {
        total,
        list: list.map(item => ({
          deviceId: item.deviceId,
          deviceName: item.deviceName,
          deviceModel: item.deviceModel,
          authCode: item.authCode,
          isOnline: item.isOnline,
          lastActiveTime: item.lastActiveTime.getTime(),
          todayReadTime: item.todayReadTime,
          totalReadTime: item.totalReadTime,
          installedApps: item.installedApps
        }))
      }
    });
  } catch (error) {
    console.error('获取设备列表错误:', error);
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// --- 获取设备详情 ---
app.get('/api/device/detail/:deviceId', authenticateToken, async (req, res) => {
  try {
    const { deviceId } = req.params;
    
    const device = await Device.findOne({ deviceId });
    if (!device) {
      return res.status(404).json({ success: false, message: '设备不存在' });
    }
    
    res.json({
      success: true,
      message: '获取成功',
      device: {
        deviceId: device.deviceId,
        deviceName: device.deviceName,
        deviceModel: device.deviceModel,
        androidVersion: device.androidVersion,
        authCode: device.authCode,
        isOnline: device.isOnline,
        lastActiveTime: device.lastActiveTime.getTime(),
        todayReadTime: device.todayReadTime,
        totalReadTime: device.totalReadTime,
        installedApps: device.installedApps,
        readingHistory: device.readingHistory
      }
    });
  } catch (error) {
    console.error('获取设备详情错误:', error);
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// --- 获取全局配置 ---
app.get('/api/config/global', async (req, res) => {
  try {
    let config = await GlobalConfig.findOne();
    if (!config) {
      config = await GlobalConfig.create({});
    }
    
    res.json({
      success: true,
      config: {
        startHour: config.startHour,
        endHour: config.endHour,
        scrollIntervalMin: config.scrollIntervalMin,
        scrollIntervalMax: config.scrollIntervalMax,
        singleReadDurationMin: config.singleReadDurationMin,
        singleReadDurationMax: config.singleReadDurationMax,
        maxAppDailyTime: config.maxAppDailyTime,
        totalDailyTime: config.totalDailyTime
      }
    });
  } catch (error) {
    console.error('获取全局配置错误:', error);
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// --- 更新全局配置 ---
app.post('/api/config/global', authenticateToken, async (req, res) => {
  try {
    const {
      startHour, endHour,
      scrollIntervalMin, scrollIntervalMax,
      singleReadDurationMin, singleReadDurationMax,
      maxAppDailyTime, totalDailyTime
    } = req.body;
    
    let config = await GlobalConfig.findOne();
    if (config) {
      config.startHour = startHour ?? config.startHour;
      config.endHour = endHour ?? config.endHour;
      config.scrollIntervalMin = scrollIntervalMin ?? config.scrollIntervalMin;
      config.scrollIntervalMax = scrollIntervalMax ?? config.scrollIntervalMax;
      config.singleReadDurationMin = singleReadDurationMin ?? config.singleReadDurationMin;
      config.singleReadDurationMax = singleReadDurationMax ?? config.singleReadDurationMax;
      config.maxAppDailyTime = maxAppDailyTime ?? config.maxAppDailyTime;
      config.totalDailyTime = totalDailyTime ?? config.totalDailyTime;
      config.updatedAt = new Date();
      await config.save();
    } else {
      config = await GlobalConfig.create(req.body);
    }
    
    res.json({
      success: true,
      message: '配置已更新'
    });
  } catch (error) {
    console.error('更新全局配置错误:', error);
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// --- 获取统计数据 ---
app.get('/api/statistics', authenticateToken, async (req, res) => {
  try {
    const totalCodes = await AuthCode.countDocuments();
    const activeCodes = await AuthCode.countDocuments({ isActive: true });
    const usedCodes = await AuthCode.countDocuments({ isActive: true, isBlocked: { $ne: true } });
    const unusedCodes = totalCodes - usedCodes;
    
    const totalDevices = await Device.countDocuments();
    const onlineDevices = await Device.countDocuments({ isOnline: true });
    
    const devices = await Device.find();
    const totalReadTime = devices.reduce((sum, d) => sum + d.totalReadTime, 0);
    const todayReadTime = devices.reduce((sum, d) => sum + d.todayReadTime, 0);
    
    res.json({
      success: true,
      message: '获取成功',
      data: {
        totalCodes,
        activeCodes,
        usedCodes,
        unusedCodes,
        totalDevices,
        onlineDevices,
        totalReadTime,
        todayReadTime
      }
    });
  } catch (error) {
    console.error('获取统计数据错误:', error);
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// ============ 静态文件服务 (管理后台 Web 界面) ============

const path = require('path');

// 管理后台页面
app.get('/admin', (req, res) => {
  res.sendFile(path.join(__dirname, 'admin', 'index.html'));
});

// 静态文件
app.use('/admin', express.static(path.join(__dirname, 'admin')));

// ============ 启动服务器 ============

const PORT = process.env.PORT || 3000;

connectDB().then(() => {
  app.listen(PORT, () => {
    console.log(`🚀 服务器运行在端口 ${PORT}`);
    console.log(`📡 API 地址：http://localhost:${PORT}/api`);
    console.log(`🖥️  管理后台：http://localhost:${PORT}/admin`);
  });
});

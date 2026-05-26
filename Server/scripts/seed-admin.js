/**
 * 初始化管理员账号脚本
 * 运行：npm run seed
 */

require('dotenv').config();
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

const AdminSchema = new mongoose.Schema({
  username: { type: String, required: true, unique: true },
  password: { type: String, required: true },
  role: { type: String, default: 'admin' },
  permissions: [{ type: String }],
  createdAt: { type: Date, default: Date.now }
});

const Admin = mongoose.model('Admin', AdminSchema);

async function seedAdmin() {
  try {
    await mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/novel-automation');
    console.log('✅ MongoDB 连接成功');
    
    // 检查是否已有管理员
    const existingAdmin = await Admin.findOne({ username: process.env.ADMIN_USERNAME || 'admin' });
    if (existingAdmin) {
      console.log('⚠️  管理员账号已存在');
      process.exit(0);
    }
    
    // 创建管理员
    const hashedPassword = await bcrypt.hash(process.env.ADMIN_PASSWORD || 'admin123', 10);
    
    const admin = await Admin.create({
      username: process.env.ADMIN_USERNAME || 'admin',
      password: hashedPassword,
      role: 'admin',
      permissions: ['*']
    });
    
    console.log('✅ 管理员账号创建成功');
    console.log(`   用户名：${admin.username}`);
    console.log(`   密码：${process.env.ADMIN_PASSWORD || 'admin123'}`);
    console.log('\n⚠️  请及时修改默认密码！');
    
    process.exit(0);
  } catch (error) {
    console.error('❌ 创建管理员失败:', error.message);
    process.exit(1);
  }
}

seedAdmin();

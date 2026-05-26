/**
 * 小说自动阅读系统 - PC 管理后台 (Electron 主进程)
 */

const { app, BrowserWindow, ipcMain, dialog, Notification, shell } = require('electron');
const path = require('path');
const axios = require('axios');

// API 基础地址
const API_BASE_URL = 'https://novel-api-production-55af.up.railway.app/api';

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 1024,
    minHeight: 700,
    icon: path.join(__dirname, 'icon.ico'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false
    },
    frame: true,
    backgroundColor: '#f5f6fa'
  });

  mainWindow.loadFile('index.html');
  
  // 开发模式打开开发者工具
  if (process.argv.includes('--dev')) {
    mainWindow.webContents.openDevTools();
  }

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

// ============ IPC 处理 ============

// API 请求处理
ipcMain.handle('api-request', async (event, config) => {
  try {
    const response = await axios({
      ...config,
      baseURL: API_BASE_URL,
      timeout: 30000
    });
    return { success: true, data: response.data };
  } catch (error) {
    return { 
      success: false, 
      message: error.response?.data?.message || error.message 
    };
  }
});

// 显示通知
ipcMain.handle('show-notification', async (event, options) => {
  new Notification({
    title: options.title,
    body: options.body,
    icon: options.icon
  }).show();
});

// 打开外部链接
ipcMain.handle('open-external', async (event, url) => {
  shell.openExternal(url);
});

// 保存文件
ipcMain.handle('save-file', async (event, options) => {
  const result = await dialog.showSaveDialog(mainWindow, {
    title: options.title,
    defaultPath: options.defaultPath,
    filters: options.filters
  });
  
  if (!result.canceled && result.filePath) {
    return { success: true, filePath: result.filePath };
  }
  return { success: false };
});

// 获取配置
ipcMain.handle('get-config', async () => {
  const configPath = path.join(app.getPath('userData'), 'config.json');
  const fs = require('fs');
  
  try {
    if (fs.existsSync(configPath)) {
      const data = fs.readFileSync(configPath, 'utf8');
      return JSON.parse(data);
    }
  } catch (error) {
    console.error('读取配置失败:', error);
  }
  
  return { apiBaseUrl: API_BASE_URL };
});

// 保存配置
ipcMain.handle('save-config', async (event, config) => {
  const configPath = path.join(app.getPath('userData'), 'config.json');
  const fs = require('fs');
  
  try {
    fs.writeFileSync(configPath, JSON.stringify(config, null, 2), 'utf8');
    return { success: true };
  } catch (error) {
    return { success: false, message: error.message };
  }
});

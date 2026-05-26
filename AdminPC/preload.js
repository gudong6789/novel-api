/**
 * Electron Preload 脚本
 * 安全地暴露 API 给渲染进程
 */

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  // API 请求
  apiRequest: (config) => ipcRenderer.invoke('api-request', config),
  
  // 显示通知
  showNotification: (options) => ipcRenderer.invoke('show-notification', options),
  
  // 打开外部链接
  openExternal: (url) => ipcRenderer.invoke('open-external', url),
  
  // 保存文件
  saveFile: (options) => ipcRenderer.invoke('save-file', options),
  
  // 获取配置
  getConfig: () => ipcRenderer.invoke('get-config'),
  
  // 保存配置
  saveConfig: (config) => ipcRenderer.invoke('save-config', config)
});

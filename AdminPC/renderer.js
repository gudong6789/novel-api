/**
 * 小说自动阅读系统 - PC 管理后台 (渲染进程)
 */

// API 基础地址
let API_BASE_URL = 'https://novel-api-production-55af.up.railway.app/api';
let authToken = localStorage.getItem('adminToken');

// 初始化
document.addEventListener('DOMContentLoaded', async () => {
  // 加载配置
  if (window.electronAPI) {
    const config = await window.electronAPI.getConfig();
    if (config.apiBaseUrl) {
      API_BASE_URL = config.apiBaseUrl;
      document.getElementById('apiUrl').textContent = API_BASE_URL;
    }
  }
  
  // 检查登录状态
  if (authToken) {
    showMainView();
  } else {
    document.getElementById('loginView').classList.add('active');
  }
  
  // 绑定事件
  bindEvents();
});

// 绑定事件
function bindEvents() {
  // 登录表单
  document.getElementById('loginForm').addEventListener('submit', handleLogin);
  
  // 退出登录
  document.getElementById('logoutBtn').addEventListener('click', handleLogout);
  
  // 导航
  document.querySelectorAll('.nav-item').forEach(item => {
    item.addEventListener('click', (e) => {
      e.preventDefault();
      const view = item.dataset.view;
      switchView(view);
    });
  });
  
  // 生成表单
  document.getElementById('generateForm').addEventListener('submit', (e) => {
    e.preventDefault();
    generateAuthCode();
  });
  
  document.getElementById('batchForm').addEventListener('submit', (e) => {
    e.preventDefault();
    batchGenerate();
  });
}

// 处理登录
async function handleLogin(e) {
  e.preventDefault();
  
  const username = document.getElementById('username').value;
  const password = document.getElementById('password').value;
  const rememberMe = document.getElementById('rememberMe').checked;
  const btn = document.getElementById('loginBtn');
  
  btn.disabled = true;
  btn.textContent = '登录中...';
  
  try {
    const result = await window.electronAPI.apiRequest({
      method: 'POST',
      url: '/admin/login',
      data: { username, password }
    });
    
    if (result.success && result.data.success) {
      authToken = result.data.token;
      if (rememberMe) {
        localStorage.setItem('adminToken', authToken);
      }
      
      document.getElementById('currentUsername').textContent = username;
      showMainView();
      loadStatistics();
    } else {
      showError('loginError', result.data.message || '登录失败');
    }
  } catch (error) {
    showError('loginError', '网络错误，请稍后重试');
  } finally {
    btn.disabled = false;
    btn.textContent = '登 录';
  }
}

// 处理退出
function handleLogout() {
  authToken = null;
  localStorage.removeItem('adminToken');
  document.getElementById('mainView').style.display = 'none';
  document.getElementById('loginView').classList.add('active');
  document.getElementById('password').value = '';
}

// 显示主界面
function showMainView() {
  document.getElementById('loginView').classList.remove('active');
  document.getElementById('mainView').style.display = 'flex';
  loadStatistics();
}

// 切换视图
function switchView(viewName) {
  // 更新导航
  document.querySelectorAll('.nav-item').forEach(item => {
    item.classList.toggle('active', item.dataset.view === viewName);
  });
  
  // 更新内容
  document.querySelectorAll('.content-view').forEach(el => {
    el.style.display = 'none';
  });
  
  const viewMap = {
    'dashboard': 'dashboardView',
    'authcodes': 'authcodesView',
    'devices': 'devicesView',
    'config': 'configView',
    'settings': 'settingsView'
  };
  
  const targetId = viewMap[viewName];
  if (targetId) {
    document.getElementById(targetId).style.display = 'block';
    
    // 更新页面标题
    const titles = {
      'dashboard': '数据概览',
      'authcodes': '授权码管理',
      'devices': '设备管理',
      'config': '全局配置',
      'settings': '系统设置'
    };
    document.getElementById('pageTitle').textContent = titles[viewName];
    
    // 加载数据
    if (viewName === 'authcodes') loadAuthCodes();
    if (viewName === 'devices') loadDevices();
    if (viewName === 'config') loadConfig();
  }
}

// 加载统计数据
async function loadStatistics() {
  try {
    const result = await window.electronAPI.apiRequest({
      method: 'GET',
      url: '/statistics',
      headers: { 'Authorization': `Bearer ${authToken}` }
    });
    
    if (result.success && result.data.success) {
      const data = result.data.data;
      document.getElementById('statTotalCodes').textContent = data.totalCodes;
      document.getElementById('statActiveCodes').textContent = data.activeCodes;
      document.getElementById('statTotalDevices').textContent = data.totalDevices;
      document.getElementById('statOnlineDevices').textContent = data.onlineDevices;
      document.getElementById('statTodayRead').textContent = (data.todayReadTime / 3600000).toFixed(1);
      document.getElementById('statTotalRead').textContent = (data.totalReadTime / 3600000).toFixed(1);
    }
  } catch (error) {
    console.error('加载统计失败:', error);
  }
}

// 加载授权码列表
async function loadAuthCodes() {
  try {
    const result = await window.electronAPI.apiRequest({
      method: 'GET',
      url: '/auth/list?page=1&pageSize=100',
      headers: { 'Authorization': `Bearer ${authToken}` }
    });
    
    if (result.success && result.data.success) {
      const tbody = document.getElementById('authcodesBody');
      const list = result.data.data.list;
      
      if (list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="loading">暂无数据</td></tr>';
        return;
      }
      
      tbody.innerHTML = list.map(item => {
        const statusBadge = getStatusBadge(item);
        const expireText = item.expireTime ? formatDate(item.expireTime) : '永久';
        const createTime = formatDate(item.createdAt || Date.now());
        
        return `
          <tr>
            <td style="font-family: monospace;">${item.code}</td>
            <td>${expireText}</td>
            <td>${statusBadge}</td>
            <td>${item.deviceId || '-'}</td>
            <td>${item.notes || '-'}</td>
            <td>${createTime}</td>
            <td>
              <button class="btn btn-sm ${item.isBlocked ? 'btn-success' : 'btn-danger'}" 
                onclick="toggleBlock('${item.code}', ${!item.isBlocked})">
                ${item.isBlocked ? '解封' : '封禁'}
              </button>
              <button class="btn btn-sm btn-danger" onclick="deleteCode('${item.code}')">删除</button>
            </td>
          </tr>
        `;
      }).join('');
    }
  } catch (error) {
    console.error('加载授权码失败:', error);
    document.getElementById('authcodesBody').innerHTML = 
      '<tr><td colspan="7" class="loading">加载失败</td></tr>';
  }
}

// 加载设备列表
async function loadDevices() {
  try {
    const result = await window.electronAPI.apiRequest({
      method: 'GET',
      url: '/device/list?page=1&pageSize=100',
      headers: { 'Authorization': `Bearer ${authToken}` }
    });
    
    if (result.success && result.data.success) {
      const tbody = document.getElementById('devicesBody');
      const list = result.data.data.list;
      
      if (list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="loading">暂无数据</td></tr>';
        return;
      }
      
      tbody.innerHTML = list.map(item => {
        const statusBadge = item.isOnline 
          ? '<span class="badge badge-success">🟢 在线</span>' 
          : '<span class="badge badge-danger">⚪ 离线</span>';
        const lastActive = formatDateTime(item.lastActiveTime);
        const todayRead = (item.todayReadTime / 3600000).toFixed(1);
        const totalRead = (item.totalReadTime / 3600000).toFixed(1);
        
        return `
          <tr>
            <td>${item.deviceName || '未知设备'}</td>
            <td>${item.deviceModel || '-'}</td>
            <td>${statusBadge}</td>
            <td style="font-family: monospace;">${item.authCode}</td>
            <td>${lastActive}</td>
            <td>${todayRead} 小时</td>
            <td>${totalRead} 小时</td>
          </tr>
        `;
      }).join('');
    }
  } catch (error) {
    console.error('加载设备失败:', error);
    document.getElementById('devicesBody').innerHTML = 
      '<tr><td colspan="7" class="loading">加载失败</td></tr>';
  }
}

// 加载配置
async function loadConfig() {
  try {
    const result = await window.electronAPI.apiRequest({
      method: 'GET',
      url: '/config/global'
    });
    
    if (result.success && result.data.success && result.data.config) {
      const config = result.data.config;
      document.getElementById('startHour').value = config.startHour;
      document.getElementById('endHour').value = config.endHour;
      document.getElementById('scrollIntervalMin').value = config.scrollIntervalMin;
      document.getElementById('scrollIntervalMax').value = config.scrollIntervalMax;
      document.getElementById('singleReadDurationMin').value = config.singleReadDurationMin;
      document.getElementById('singleReadDurationMax').value = config.singleReadDurationMax;
      document.getElementById('maxAppDailyTime').value = config.maxAppDailyTime;
      document.getElementById('totalDailyTime').value = config.totalDailyTime;
    }
  } catch (error) {
    console.error('加载配置失败:', error);
  }
}

// 保存配置
async function saveConfig() {
  const config = {
    startHour: parseInt(document.getElementById('startHour').value),
    endHour: parseInt(document.getElementById('endHour').value),
    scrollIntervalMin: parseInt(document.getElementById('scrollIntervalMin').value),
    scrollIntervalMax: parseInt(document.getElementById('scrollIntervalMax').value),
    singleReadDurationMin: parseInt(document.getElementById('singleReadDurationMin').value),
    singleReadDurationMax: parseInt(document.getElementById('singleReadDurationMax').value),
    maxAppDailyTime: parseInt(document.getElementById('maxAppDailyTime').value),
    totalDailyTime: parseInt(document.getElementById('totalDailyTime').value)
  };
  
  try {
    const result = await window.electronAPI.apiRequest({
      method: 'POST',
      url: '/config/global',
      headers: { 
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      },
      data: config
    });
    
    if (result.success && result.data.success) {
      showNotification('配置已保存', 'success');
    } else {
      showNotification(result.data.message || '保存失败', 'error');
    }
  } catch (error) {
    showNotification('保存失败', 'error');
  }
}

// 显示生成模态框
function showGenerateModal() {
  document.getElementById('generateModal').classList.add('active');
}

// 显示批量生成模态框
function showBatchModal() {
  document.getElementById('batchModal').classList.add('active');
}

// 关闭模态框
function closeModal(modalId) {
  document.getElementById(modalId).classList.remove('active');
}

// 生成授权码
async function generateAuthCode() {
  const durationType = parseInt(document.getElementById('durationType').value);
  const notes = document.getElementById('generateNotes').value;
  
  try {
    const result = await window.electronAPI.apiRequest({
      method: 'POST',
      url: '/auth/generate',
      headers: { 
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      },
      data: { durationType, notes }
    });
    
    if (result.success && result.data.success) {
      closeModal('generateModal');
      showResult(`
        <div class="result-success">
          <h4>✅ 生成成功</h4>
          <p class="auth-code">${result.data.authCode}</p>
          <p>有效期：${durationType === 0 ? '永久' : durationType + ' 天'}</p>
        </div>
      `);
      loadAuthCodes();
      loadStatistics();
    } else {
      showNotification(result.data.message || '生成失败', 'error');
    }
  } catch (error) {
    showNotification('生成失败', 'error');
  }
}

// 批量生成
async function batchGenerate() {
  const count = parseInt(document.getElementById('batchCount').value);
  const durationType = parseInt(document.getElementById('batchDurationType').value);
  const notes = document.getElementById('batchNotes').value;
  
  try {
    const result = await window.electronAPI.apiRequest({
      method: 'POST',
      url: '/auth/generate-batch',
      headers: { 
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      },
      data: { count, durationType, notes }
    });
    
    if (result.success && result.data.success) {
      closeModal('batchModal');
      
      const codesList = result.data.codes.map(code => 
        `<div style="font-family: monospace; padding: 4px 0;">${code}</div>`
      ).join('');
      
      showResult(`
        <div class="result-success">
          <h4>✅ 成功生成 ${result.data.codes.length} 个授权码</h4>
          <div class="codes-list" style="max-height: 300px; overflow-y: auto; margin: 16px 0; padding: 16px; background: var(--light-bg); border-radius: 8px;">
            ${codesList}
          </div>
          <button class="btn btn-primary" onclick="copyCodes(${JSON.stringify(result.data.codes)})">
            📋 复制所有授权码
          </button>
        </div>
      `);
      loadAuthCodes();
      loadStatistics();
    } else {
      showNotification(result.data.message || '生成失败', 'error');
    }
  } catch (error) {
    showNotification('生成失败', 'error');
  }
}

// 显示结果模态框
function showResult(content) {
  document.getElementById('resultContent').innerHTML = content;
  document.getElementById('resultModal').classList.add('active');
}

// 复制授权码
async function copyCodes(codes) {
  const text = codes.join('\n');
  try {
    await navigator.clipboard.writeText(text);
    showNotification('已复制到剪贴板', 'success');
  } catch (error) {
    // 备用方法
    const textarea = document.createElement('textarea');
    textarea.value = text;
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
    showNotification('已复制到剪贴板', 'success');
  }
}

// 封禁/解封授权码
async function toggleBlock(code, blocked) {
  if (!confirm(`确定要${blocked ? '封禁' : '解封'}授权码 ${code} 吗？`)) return;
  
  try {
    const result = await window.electronAPI.apiRequest({
      method: 'POST',
      url: '/auth/block',
      headers: { 
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      },
      data: { code, blocked }
    });
    
    if (result.success && result.data.success) {
      showNotification(blocked ? '已封禁' : '已解封', 'success');
      loadAuthCodes();
    } else {
      showNotification(result.data.message || '操作失败', 'error');
    }
  } catch (error) {
    showNotification('操作失败', 'error');
  }
}

// 删除授权码
async function deleteCode(code) {
  if (!confirm(`确定要删除授权码 ${code} 吗？此操作不可恢复。`)) return;
  
  try {
    const result = await window.electronAPI.apiRequest({
      method: 'DELETE',
      url: `/auth/delete/${code}`,
      headers: { 'Authorization': `Bearer ${authToken}` }
    });
    
    if (result.success && result.data.success) {
      showNotification('删除成功', 'success');
      loadAuthCodes();
      loadStatistics();
    } else {
      showNotification(result.data.message || '删除失败', 'error');
    }
  } catch (error) {
    showNotification('删除失败', 'error');
  }
}

// 保存 API 配置
async function saveApiConfig() {
  const apiBaseUrl = document.getElementById('apiBaseUrl').value;
  
  if (window.electronAPI) {
    await window.electronAPI.saveConfig({ apiBaseUrl });
  }
  
  API_BASE_URL = apiBaseUrl;
  showNotification('API 配置已保存', 'success');
}

// 刷新所有数据
function refreshAll() {
  loadStatistics();
  loadAuthCodes();
  loadDevices();
  showNotification('数据已刷新', 'success');
}

// 工具函数
function getStatusBadge(item) {
  if (item.isBlocked) return '<span class="badge badge-danger">🚫 已封禁</span>';
  if (!item.isActive) return '<span class="badge badge-info">⭕ 未使用</span>';
  return '<span class="badge badge-success">✅ 已激活</span>';
}

function formatDate(timestamp) {
  const date = new Date(timestamp);
  return date.toLocaleDateString('zh-CN');
}

function formatDateTime(timestamp) {
  const date = new Date(timestamp);
  return date.toLocaleString('zh-CN');
}

function showError(elementId, message) {
  const el = document.getElementById(elementId);
  if (el) {
    el.textContent = message;
    el.style.display = 'block';
    setTimeout(() => el.style.display = 'none', 5000);
  }
}

function showNotification(message, type = 'info') {
  if (window.electronAPI) {
    window.electronAPI.showNotification({
      title: type === 'success' ? '✅ 成功' : '❌ 错误',
      body: message
    });
  }
  
  // 同时显示在界面上
  const notification = document.createElement('div');
  notification.style.cssText = `
    position: fixed;
    top: 20px;
    right: 20px;
    padding: 16px 24px;
    background: ${type === 'success' ? 'var(--success-color)' : 'var(--danger-color)'};
    color: white;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.2);
    z-index: 9999;
    animation: slideIn 0.3s ease-out;
  `;
  notification.textContent = message;
  document.body.appendChild(notification);
  
  setTimeout(() => {
    notification.remove();
  }, 3000);
}

// 添加动画
const style = document.createElement('style');
style.textContent = `
  @keyframes slideIn {
    from {
      transform: translateX(400px);
      opacity: 0;
    }
    to {
      transform: translateX(0);
      opacity: 1;
    }
  }
  
  .result-success {
    text-align: center;
    padding: 20px;
  }
  
  .result-success h4 {
    font-size: 20px;
    margin-bottom: 16px;
    color: var(--success-color);
  }
  
  .auth-code {
    font-size: 24px;
    font-family: monospace;
    font-weight: bold;
    color: var(--primary-color);
    background: var(--light-bg);
    padding: 16px;
    border-radius: 8px;
    margin: 16px 0;
  }
`;
document.head.appendChild(style);

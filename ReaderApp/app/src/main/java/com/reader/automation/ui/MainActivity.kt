package com.reader.automation.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.reader.automation.R
import com.reader.automation.databinding.ActivityMainBinding
import com.reader.automation.services.ReaderAccessibilityService
import com.reader.automation.services.ReaderForegroundService
import com.reader.automation.utils.Logger
import com.reader.automation.utils.PreferenceHelper

/**
 * 主界面
 * 功能：启动/停止控制、状态显示、配置入口
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferenceHelper
    
    private var isRunning = false
    private var isPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PreferenceHelper.getInstance(applicationContext)
        
        setupViews()
        checkAccessibilityService()
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        checkAccessibilityService()
    }

    private fun setupViews() {
        // 启动/停止按钮
        binding.btnStartStop.setOnClickListener {
            if (isRunning) {
                stopService()
            } else {
                startService()
            }
        }
        
        // 暂停/继续按钮
        binding.btnPauseResume.setOnClickListener {
            if (isPaused) {
                resumeService()
            } else {
                pauseService()
            }
        }
        
        // 配置按钮
        binding.btnConfig.setOnClickListener {
            startActivity(Intent(this, ConfigActivity::class.java))
        }
        
        // 书籍管理按钮
        binding.btnBooks.setOnClickListener {
            startActivity(Intent(this, BookManageActivity::class.java))
        }
        
        // 授权按钮
        binding.btnActivation.setOnClickListener {
            startActivity(Intent(this, ActivationActivity::class.java))
        }
    }

    /**
     * 检查无障碍服务是否启用
     */
    private fun checkAccessibilityService() {
        val accessibilityManager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        
        val isAccessibilityEnabled = enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == packageName
        }
        
        if (!isAccessibilityEnabled) {
            showAccessibilityDialog()
        }
    }

    /**
     * 显示无障碍服务设置对话框
     */
    private fun showAccessibilityDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要开启无障碍服务")
            .setMessage("本应用需要使用无障碍服务来模拟真人阅读操作。请在设置中开启「小说自动阅读」服务。")
            .setPositiveButton("去设置") { _, _ ->
                openAccessibilitySettings()
            }
            .setNegativeButton("取消", null)
            .setCancelable(false)
            .show()
    }

    /**
     * 打开无障碍服务设置
     */
    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Logger.e("MainActivity", "打开无障碍设置失败", e)
        }
    }

    /**
     * 启动服务
     */
    private fun startService() {
        // 检查授权
        if (!checkAuthorization()) {
            AlertDialog.Builder(this)
                .setTitle("需要激活")
                .setMessage("请先激活授权码才能使用本应用")
                .setPositiveButton("去激活") { _, _ ->
                    startActivity(Intent(this, ActivationActivity::class.java))
                }
                .setNegativeButton("取消", null)
                .show()
            return
        }
        
        // 启动前台服务
        ReaderForegroundService.start(application)
        
        // 启动无障碍服务（需要用户手动开启）
        checkAccessibilityService()
        
        isRunning = true
        isPaused = false
        updateUI()
        
        Logger.d("MainActivity", "服务已启动")
    }

    /**
     * 停止服务
     */
    private fun stopService() {
        ReaderForegroundService.stop(application)
        ReaderAccessibilityService.instance?.stopReadingTask()
        
        isRunning = false
        isPaused = false
        updateUI()
        
        Logger.d("MainActivity", "服务已停止")
    }

    /**
     * 暂停服务
     */
    private fun pauseService() {
        ReaderAccessibilityService.instance?.pauseReadingTask()
        isPaused = true
        updateUI()
    }

    /**
     * 恢复服务
     */
    private fun resumeService() {
        ReaderAccessibilityService.instance?.resumeReadingTask()
        isPaused = false
        updateUI()
    }

    /**
     * 检查授权状态
     */
    private fun checkAuthorization(): Boolean {
        val isActivated = prefs.getBoolean("is_activated", false)
        val expireTime = prefs.getLong("expire_time", 0L)
        val currentTime = System.currentTimeMillis()
        
        return isActivated && (expireTime == 0L || expireTime > currentTime)
    }

    /**
     * 更新 UI 状态
     */
    private fun updateUI() {
        // 从服务获取实际状态
        isRunning = ReaderForegroundService.isRunning
        isPaused = ReaderAccessibilityService.isPaused
        
        if (isRunning) {
            binding.btnStartStop.text = "停止运行"
            binding.btnStartStop.setBackgroundResource(R.drawable.btn_stop_bg)
            
            binding.tvStatus.text = if (isPaused) "已暂停" else "运行中"
            binding.tvStatus.setTextColor(getColor(R.color.status_running))
            
            binding.btnPauseResume.text = if (isPaused) "继续" else "暂停"
            binding.btnPauseResume.isEnabled = true
        } else {
            binding.btnStartStop.text = "开始运行"
            binding.btnStartStop.setBackgroundResource(R.drawable.btn_start_bg)
            
            binding.tvStatus.text = "已停止"
            binding.tvStatus.setTextColor(getColor(R.color.status_stopped))
            
            binding.btnPauseResume.isEnabled = false
        }
        
        // 更新统计信息
        updateStats()
    }

    /**
     * 更新统计信息
     */
    private fun updateStats() {
        // TODO: 从数据库读取今日阅读时长等统计信息
        binding.tvTodayTime.text = "今日阅读：0 小时"
        binding.tvTodayApps.text = "已用 APP: 0/0"
    }
}

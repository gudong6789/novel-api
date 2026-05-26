package com.reader.automation.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.reader.automation.databinding.ActivityErrorBinding
import com.reader.automation.services.ReaderAccessibilityService
import com.reader.automation.utils.Logger

/**
 * 异常提示界面
 * 全屏红色提示异常状态
 */
class ErrorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityErrorBinding
    private val handler = Handler(Looper.getMainLooper())
    private var checkJob: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityErrorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 全屏显示
        window?.decorView?.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        val errorType = intent.getStringExtra("error_type") ?: "unknown"
        val errorMessage = intent.getStringExtra("error_message") ?: "发生未知错误"
        
        when (errorType) {
            "network_lost" -> {
                binding.tvErrorTitle.text = "⚠️ 网络断开"
                binding.tvErrorMessage.text = errorMessage
            }
            "accessibility_disabled" -> {
                binding.tvErrorTitle.text = "⚠️ 无障碍服务已关闭"
                binding.tvErrorMessage.text = "请重新开启无障碍服务"
            }
            "auth_expired" -> {
                binding.tvErrorTitle.text = "⚠️ 授权已过期"
                binding.tvErrorMessage.text = "请重新激活授权码"
            }
            else -> {
                binding.tvErrorTitle.text = "⚠️ 发生错误"
                binding.tvErrorMessage.text = errorMessage
            }
        }
        
        Logger.e("ErrorActivity", "显示异常界面：$errorType - $errorMessage")
        
        // 启动自动检测和关闭
        startAutoCheck()
    }

    /**
     * 启动自动检测
     */
    private fun startAutoCheck() {
        checkJob = object : Runnable {
            override fun run() {
                if (shouldCloseError()) {
                    Logger.d("ErrorActivity", "异常已恢复，关闭提示界面")
                    finish()
                } else {
                    handler.postDelayed(this, 3000)
                }
            }
        }
        handler.post(checkJob!!)
    }

    /**
     * 检查是否应该关闭错误界面
     */
    private fun shouldCloseError(): Boolean {
        val errorType = intent.getStringExtra("error_type")
        
        return when (errorType) {
            "network_lost" -> isNetworkAvailable()
            "accessibility_disabled" -> ReaderAccessibilityService.instance != null
            "auth_expired" -> false // 需要用户手动处理
            else -> false
        }
    }

    /**
     * 检查网络是否可用
     */
    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    override fun onDestroy() {
        super.onDestroy()
        checkJob?.let { handler.removeCallbacks(it) }
    }

    override fun onBackPressed() {
        // 禁止返回，必须处理异常
    }
}

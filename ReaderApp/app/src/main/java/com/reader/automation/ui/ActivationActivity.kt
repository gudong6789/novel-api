package com.reader.automation.ui

import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.reader.automation.databinding.ActivityActivationBinding
import com.reader.automation.utils.Logger
import com.reader.automation.utils.PreferenceHelper
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 授权激活界面
 * 功能：输入授权码、激活验证、显示剩余时间
 */
class ActivationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityActivationBinding
    private lateinit var prefs: PreferenceHelper
    private val mainScope = CoroutineScope(Dispatchers.Main + Job())
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // 服务器地址 (需要替换为实际地址)
    private val serverUrl = "http://your-server.com/api/auth/verify"
    
    private var countdownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PreferenceHelper.getInstance(applicationContext)
        
        setupViews()
        checkActivationStatus()
    }

    private fun setupViews() {
        supportActionBar?.title = "授权激活"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // 激活按钮
        binding.btnActivate.setOnClickListener {
            val code = binding.etAuthCode.text.toString().trim()
            if (code.isNotEmpty()) {
                activateCode(code)
            } else {
                binding.tilAuthCode.error = "请输入授权码"
            }
        }
        
        // 退出登录按钮
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun checkActivationStatus() {
        val isActivated = prefs.getBoolean("is_activated", false)
        val expireTime = prefs.getLong("expire_time", 0L)
        val currentTime = System.currentTimeMillis()
        
        if (isActivated) {
            binding.layoutActivate.visibility = android.view.View.GONE
            binding.layoutActivated.visibility = android.view.View.VISIBLE
            
            if (expireTime == 0L) {
                binding.tvExpireTime.text = "永久有效"
                startCountdown(0)
            } else if (expireTime > currentTime) {
                val remaining = expireTime - currentTime
                binding.tvExpireTime.text = "剩余：${formatRemainingTime(remaining)}"
                startCountdown(remaining)
            } else {
                binding.tvExpireTime.text = "已过期"
                binding.tvExpireTime.setTextColor(getColor(R.color.status_stopped))
            }
        } else {
            binding.layoutActivate.visibility = android.view.View.VISIBLE
            binding.layoutActivated.visibility = android.view.View.GONE
        }
    }

    private fun startCountdown(remainingMs: Long) {
        countdownTimer?.cancel()
        
        if (remainingMs <= 0) return
        
        countdownTimer = object : CountDownTimer(remainingMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvExpireTime.text = "剩余：${formatRemainingTime(millisUntilFinished)}"
            }
            
            override fun onFinish() {
                binding.tvExpireTime.text = "已过期"
                binding.tvExpireTime.setTextColor(getColor(R.color.status_stopped))
            }
        }.start()
    }

    private fun formatRemainingTime(millis: Long): String {
        val days = millis / (24 * 3600 * 1000)
        val hours = (millis % (24 * 3600 * 1000)) / (3600 * 1000)
        val minutes = (millis % (3600 * 1000)) / (60 * 1000)
        val seconds = (millis % (60 * 1000)) / 1000
        
        return if (days > 0) {
            "${days}天 ${hours}小时"
        } else if (hours > 0) {
            "${hours}小时 ${minutes}分钟"
        } else if (minutes > 0) {
            "${minutes}分钟 ${seconds}秒"
        } else {
            "${seconds}秒"
        }
    }

    private fun activateCode(code: String) {
        binding.btnActivate.isEnabled = false
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        mainScope.launch {
            try {
                // 本地验证格式
                if (code.length < 8) {
                    withContext(Dispatchers.Main) {
                        binding.tilAuthCode.error = "授权码格式不正确"
                        binding.btnActivate.isEnabled = true
                        binding.progressBar.visibility = android.view.View.GONE
                    }
                    return@launch
                }
                
                // 云端验证 (可选，如果不需要云端验证可跳过)
                val isValid = verifyCodeOnline(code)
                
                if (isValid) {
                    // 保存激活状态
                    prefs.edit()
                        .putBoolean("is_activated", true)
                        .putLong("expire_time", calculateExpireTime(code))
                        .putString("auth_code", code)
                        .apply()
                    
                    withContext(Dispatchers.Main) {
                        Logger.d("ActivationActivity", "激活成功")
                        checkActivationStatus()
                        showSuccessDialog()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        binding.tilAuthCode.error = "授权码无效或已过期"
                        binding.btnActivate.isEnabled = true
                        binding.progressBar.visibility = android.view.View.GONE
                    }
                }
            } catch (e: Exception) {
                Logger.e("ActivationActivity", "激活失败", e)
                withContext(Dispatchers.Main) {
                    binding.tilAuthCode.error = "网络错误，请重试"
                    binding.btnActivate.isEnabled = true
                    binding.progressBar.visibility = android.view.View.GONE
                }
            }
        }
    }

    private suspend fun verifyCodeOnline(code: String): Boolean {
        return try {
            val request = Request.Builder()
                .url("$serverUrl/$code")
                .get()
                .build()
            
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Logger.e("ActivationActivity", "云端验证失败", e)
            true // 如果服务器不可用，暂时允许本地激活
        }
    }

    private fun calculateExpireTime(code: String): Long {
        // 根据授权码解析有效期，这里简单示例
        // 实际应该从服务器返回
        return 30L * 24 * 3600 * 1000 // 默认 30 天
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("✅ 激活成功")
            .setMessage("授权已激活，现在可以开始使用自动阅读功能了！")
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("退出登录")
            .setMessage("确定要清除授权信息吗？这将需要重新激活。")
            .setPositiveButton("确定") { _, _ ->
                prefs.edit()
                    .remove("is_activated")
                    .remove("expire_time")
                    .remove("auth_code")
                    .apply()
                checkActivationStatus()
                Logger.d("ActivationActivity", "已退出登录")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
        mainScope.cancel()
    }
}

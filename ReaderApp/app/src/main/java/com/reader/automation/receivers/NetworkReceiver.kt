package com.reader.automation.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.reader.automation.services.ReaderAccessibilityService
import com.reader.automation.ui.ErrorActivity
import com.reader.automation.utils.Logger

/**
 * 网络状态接收器
 * 检测网络断开，触发异常防护机制
 */
class NetworkReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            val isConnected = isNetworkConnected(context)
            
            if (!isConnected) {
                Logger.w("NetworkReceiver", "网络已断开")
                handleNetworkLost(context)
            } else {
                Logger.d("NetworkReceiver", "网络已恢复")
                handleNetworkRestored(context)
            }
        }
    }

    private fun isNetworkConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    /**
     * 处理网络丢失
     */
    private fun handleNetworkLost(context: Context) {
        // 如果服务正在运行，显示异常提示
        if (ReaderForegroundService.isRunning) {
            Logger.e("NetworkReceiver", "网络断开，触发异常防护")
            
            // 启动异常提示界面
            val intent = Intent(context, ErrorActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("error_type", "network_lost")
                putExtra("error_message", "网络连接已断开")
            }
            context.startActivity(intent)
            
            // 尝试重启服务
            tryRestartService(context)
        }
    }

    /**
     * 处理网络恢复
     */
    private fun handleNetworkRestored(context: Context) {
        // 关闭异常提示
        val intent = Intent(context, ErrorActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("action", "close_error")
        }
        context.startActivity(intent)
        
        // 恢复服务
        if (ReaderForegroundService.isRunning) {
            ReaderAccessibilityService.instance?.resumeReadingTask()
        }
    }

    /**
     * 尝试重启服务
     */
    private fun tryRestartService(context: Context) {
        val prefs = context.getSharedPreferences("reader_automation_prefs", Context.MODE_PRIVATE)
        var retryCount = prefs.getInt("network_retry_count", 0)
        
        if (retryCount < 3) {
            retryCount++
            prefs.edit().putInt("network_retry_count", retryCount).apply()
            
            Logger.d("NetworkReceiver", "尝试重启服务，第 $retryCount 次")
            
            // 延迟 5 秒后重启
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                ReaderForegroundService.start(context.applicationContext as android.app.Application)
            }, 5000)
        } else {
            Logger.e("NetworkReceiver", "重启次数已达上限")
            prefs.edit().putInt("network_retry_count", 0).apply()
        }
    }
}

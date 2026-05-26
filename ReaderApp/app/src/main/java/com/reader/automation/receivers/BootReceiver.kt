package com.reader.automation.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reader.automation.services.ReaderForegroundService
import com.reader.automation.utils.Logger

/**
 * 开机自启接收器
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Logger.d("BootReceiver", "开机启动广播收到")
            
            // 检查是否启用开机自启
            val prefs = context.getSharedPreferences("reader_automation_prefs", Context.MODE_PRIVATE)
            val autoStart = prefs.getBoolean("auto_start", false)
            
            if (autoStart) {
                Logger.d("BootReceiver", "启动前台服务")
                ReaderForegroundService.start(context.applicationContext as android.app.Application)
            } else {
                Logger.d("BootReceiver", "开机自启未启用")
            }
        }
    }
}

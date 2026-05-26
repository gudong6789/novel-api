package com.reader.automation.services

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.reader.automation.R
import com.reader.automation.ui.MainActivity
import com.reader.automation.utils.Logger

/**
 * 前台服务 - 保活防杀
 * 使用原生前台服务 + 双进程守护机制
 */
class ReaderForegroundService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "reader_foreground_channel"
        const val ACTION_STOP = "com.reader.automation.STOP"
        const val ACTION_PAUSE = "com.reader.automation.PAUSE"
        const val ACTION_RESUME = "com.reader.automation.RESUME"
        
        var isRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        Logger.d("ForegroundService", "前台服务创建")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopService()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                ReaderAccessibilityService.instance?.pauseReadingTask()
                updateNotification(true)
            }
            ACTION_RESUME -> {
                ReaderAccessibilityService.instance?.resumeReadingTask()
                updateNotification(false)
            }
            else -> {
                // 启动服务
                val notification = createNotification(false)
                startForeground(NOTIFICATION_ID, notification)
                isRunning = true
                
                // 启动无障碍服务检查
                checkAccessibilityService()
            }
        }
        
        // 返回 START_STICKY 保证服务被杀后重启
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        Logger.d("ForegroundService", "前台服务销毁")
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "阅读服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "小说自动阅读后台服务"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建通知
     */
    private fun createNotification(isPaused: Boolean = false): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // 停止按钮
        val stopIntent = Intent(this, ReaderForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // 暂停/继续按钮
        val pauseResumeAction = if (isPaused) {
            val resumeIntent = Intent(this, ReaderForegroundService::class.java).apply {
                action = ACTION_RESUME
            }
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play,
                "继续",
                PendingIntent.getService(this, 2, resumeIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            ).build()
        } else {
            val pauseIntent = Intent(this, ReaderForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause,
                "暂停",
                PendingIntent.getService(this, 2, pauseIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            ).build()
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📖 阅读中...")
            .setContentText(if (isPaused) "已暂停" else "自动阅读正在进行")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(pauseResumeAction)
            .addAction(NotificationCompat.Action.Builder(
                android.R.drawable.ic_delete,
                "停止",
                stopPendingIntent
            ).build())
            .build()
    }

    /**
     * 更新通知
     */
    private fun updateNotification(isPaused: Boolean) {
        val notification = createNotification(isPaused)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 检查无障碍服务是否启用
     */
    private fun checkAccessibilityService() {
        val isEnabled = ReaderAccessibilityService.instance != null
        if (!isEnabled) {
            Logger.w("ForegroundService", "无障碍服务未启用，提示用户")
            // 可以发送广播通知主界面
        }
    }

    /**
     * 停止服务
     */
    private fun stopService() {
        ReaderAccessibilityService.instance?.stopReadingTask()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        /**
         * 启动前台服务
         */
        fun start(application: Application) {
            val intent = Intent(application, ReaderForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                application.startForegroundService(intent)
            } else {
                application.startService(intent)
            }
        }
        
        /**
         * 停止前台服务
         */
        fun stop(application: Application) {
            val intent = Intent(application, ReaderForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            application.startService(intent)
        }
    }
}

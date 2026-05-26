package com.reader.automation

import android.app.Application
import com.reader.automation.managers.BookProgressManager
import com.reader.automation.managers.TimeManager
import com.reader.automation.utils.Logger

/**
 * Application 入口
 */
class ReaderApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Logger.d("ReaderApplication", "应用启动")
        
        // 初始化单例
        TimeManager.getInstance(this)
        BookProgressManager.getInstance(this)
        
        // 检查是否需要每日重置
        checkDailyReset()
    }

    /**
     * 检查每日重置
     */
    private fun checkDailyReset() {
        val timeManager = TimeManager.getInstance(this)
        val bookManager = BookProgressManager.getInstance(this)
        
        if (timeManager.shouldResetDaily()) {
            Logger.d("ReaderApplication", "执行每日重置")
            bookManager.resetDailyStats()
            timeManager.markDailyReset()
        }
    }
}

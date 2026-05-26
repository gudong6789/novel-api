package com.admin.manager

import android.app.Application
import com.admin.manager.database.AppDatabase

/**
 * Application 入口
 */
class AdminApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // 初始化数据库
        AppDatabase.getInstance(this)
    }
}

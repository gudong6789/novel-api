package com.reader.automation.managers

import android.content.Context
import android.content.Intent
import com.reader.automation.models.AppConfig
import com.reader.automation.utils.Logger
import com.reader.automation.utils.PreferenceHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * APP 轮换管理器
 * 功能：管理多款小说 APP，自动切换，时长控制
 */
class AppSwitchManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AppSwitchManager"
        private var instance: AppSwitchManager? = null
        
        fun getInstance(context: Context): AppSwitchManager {
            return instance ?: synchronized(this) {
                instance ?: AppSwitchManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = PreferenceHelper.getInstance(context)
    private val gson = Gson()
    
    // 当前正在使用的 APP 索引
    private var currentAppIndex = 0
    
    /**
     * 获取所有配置的 APP 列表
     */
    fun getConfiguredApps(): List<AppConfig> {
        val appsJson = prefs.getString("configured_apps", "[]")
        val type = object : TypeToken<List<AppConfig>>() {}.type
        return try {
            gson.fromJson(appsJson, type) ?: emptyList()
        } catch (e: Exception) {
            Logger.e(TAG, "解析 APP 列表失败", e)
            emptyList()
        }
    }

    /**
     * 保存 APP 列表
     */
    fun saveApps(apps: List<AppConfig>) {
        val appsJson = gson.toJson(apps)
        prefs.edit().putString("configured_apps", appsJson).apply()
        Logger.d(TAG, "保存 APP 列表，共 ${apps.size} 个")
    }

    /**
     * 添加 APP
     */
    fun addApp(appConfig: AppConfig) {
        val apps = getConfiguredApps().toMutableList()
        
        // 检查是否已存在
        val existingIndex = apps.indexOfFirst { it.packageName == appConfig.packageName }
        if (existingIndex >= 0) {
            apps[existingIndex] = appConfig
            Logger.d(TAG, "更新已有 APP: ${appConfig.packageName}")
        } else {
            apps.add(appConfig)
            Logger.d(TAG, "添加新 APP: ${appConfig.packageName}")
        }
        
        saveApps(apps)
    }

    /**
     * 移除 APP
     */
    fun removeApp(packageName: String) {
        val apps = getConfiguredApps().toMutableList()
        apps.removeAll { it.packageName == packageName }
        saveApps(apps)
        Logger.d(TAG, "移除 APP: $packageName")
    }

    /**
     * 切换到下一个 APP
     */
    fun switchToNextApp() {
        val apps = getConfiguredApps()
        if (apps.isEmpty()) {
            Logger.w(TAG, "没有配置的 APP")
            return
        }
        
        // 循环切换
        currentAppIndex = (currentAppIndex + 1) % apps.size
        val nextApp = apps[currentAppIndex]
        
        Logger.d(TAG, "切换到 APP: ${nextApp.appName} (${nextApp.packageName})")
        launchApp(nextApp.packageName)
    }

    /**
     * 切换到指定 APP
     */
    fun switchToApp(packageName: String) {
        val apps = getConfiguredApps()
        val appIndex = apps.indexOfFirst { it.packageName == packageName }
        
        if (appIndex >= 0) {
            currentAppIndex = appIndex
            launchApp(packageName)
        } else {
            Logger.w(TAG, "APP 不在配置列表中：$packageName")
        }
    }

    /**
     * 启动指定 APP
     */
    private fun launchApp(packageName: String) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                Logger.d(TAG, "成功启动 APP: $packageName")
            } else {
                Logger.e(TAG, "无法启动 APP: $packageName (未安装)")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "启动 APP 失败: $packageName", e)
        }
    }

    /**
     * 获取当前 APP
     */
    fun getCurrentApp(): AppConfig? {
        val apps = getConfiguredApps()
        return apps.getOrNull(currentAppIndex)
    }

    /**
     * 检查 APP 是否已读满今日时长
     */
    fun isAppReachDailyLimit(packageName: String): Boolean {
        val apps = getConfiguredApps()
        val app = apps.find { it.packageName == packageName } ?: return false
        
        // 获取今日已读时长（毫秒）
        val todayReadTime = BookProgressManager.getInstance(context).getAppTodayReadTime(packageName)
        
        // 获取该 APP 的最大时长设置
        val maxTime = app.maxDailyReadTime ?: prefs.getLong("max_app_daily_time", 43200000L)
        
        return todayReadTime >= maxTime
    }

    /**
     * 获取下一个未读满的 APP
     */
    fun getNextAvailableApp(): AppConfig? {
        val apps = getConfiguredApps()
        
        // 从当前索引开始查找
        for (i in apps.indices) {
            val index = (currentAppIndex + i) % apps.size
            val app = apps[index]
            
            if (!isAppReachDailyLimit(app.packageName)) {
                return app
            }
        }
        
        return null
    }

    /**
     * 重置所有 APP 的今日阅读时长（每日零点调用）
     */
    fun resetDailyReadTime() {
        Logger.d(TAG, "重置所有 APP 今日阅读时长")
        // BookProgressManager 会处理具体的重置逻辑
    }
}

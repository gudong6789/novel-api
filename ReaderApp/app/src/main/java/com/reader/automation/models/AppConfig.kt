package com.reader.automation.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 小说 APP 配置模型
 */
@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val iconPath: String? = null,
    val maxDailyReadTime: Long? = null, // 毫秒，null 表示使用全局设置
    val isEnabled: Boolean = true,
    val sortIndex: Int = 0,
    val createTime: Long = System.currentTimeMillis()
)

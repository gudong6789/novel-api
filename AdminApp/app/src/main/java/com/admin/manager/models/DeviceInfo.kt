package com.admin.manager.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 设备信息模型
 */
@Entity(tableName = "devices")
data class DeviceInfo(
    @PrimaryKey
    val deviceId: String,              // 设备唯一标识
    val deviceName: String = "",       // 设备名称
    val deviceModel: String = "",      // 设备型号
    val osVersion: String = "",        // 系统版本
    val appVersion: String = "",       // 应用版本
    val authCode: String = "",         // 使用的授权码
    val isActive: Boolean = false,     // 是否在线
    val lastActiveTime: Long = 0,      // 最后活跃时间
    val totalReadTime: Long = 0,       // 累计阅读时长 (毫秒)
    val todayReadTime: Long = 0,       // 今日阅读时长 (毫秒)
    val installedApps: String = "",    // 已安装的小说 APP 列表 (JSON)
    val createTime: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    /**
     * 检查是否在线 (5 分钟内有过活动)
     */
    fun isOnline(): Boolean {
        return System.currentTimeMillis() - lastActiveTime < 5 * 60 * 1000
    }
    
    /**
     * 获取授权剩余时间
     */
    fun getAuthRemainingDays(): Long {
        // 需要从 AuthCode 表关联查询
        return 0
    }
}

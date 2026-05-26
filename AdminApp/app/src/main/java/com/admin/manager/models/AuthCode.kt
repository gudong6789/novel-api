package com.admin.manager.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 授权码模型
 */
@Entity(tableName = "auth_codes")
data class AuthCode(
    @PrimaryKey
    val code: String,                    // 授权码
    val durationType: Int = 0,           // 0:1 天，1:7 天，2:30 天，3:永久
    val durationDays: Int = 0,           // 具体天数
    val isActive: Boolean = true,        // 是否激活
    val isBlocked: Boolean = false,      // 是否被封禁
    val deviceId: String? = null,        // 绑定的设备 ID
    val activateTime: Long? = null,      // 激活时间
    val expireTime: Long?,               // 过期时间 (null 表示永久)
    val createTime: Long = System.currentTimeMillis(),
    val creator: String = "",            // 创建者
    val notes: String = ""               // 备注
) {
    companion object {
        const val DURATION_1_DAY = 0
        const val DURATION_7_DAYS = 1
        const val DURATION_30_DAYS = 2
        const val DURATION_PERMANENT = 3
    }
    
    /**
     * 检查是否已过期
     */
    fun isExpired(): Boolean {
        if (expireTime == null) return false // 永久
        return System.currentTimeMillis() > expireTime
    }
    
    /**
     * 获取剩余天数
     */
    fun getRemainingDays(): Long {
        if (expireTime == null) return -1 // 永久
        val remaining = expireTime - System.currentTimeMillis()
        return if (remaining > 0) remaining / (24 * 3600 * 1000) else 0
    }
}

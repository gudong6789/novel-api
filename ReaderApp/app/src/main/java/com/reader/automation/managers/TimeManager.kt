package com.reader.automation.managers

import android.content.Context
import com.reader.automation.utils.PreferenceHelper
import com.reader.automation.utils.Logger
import java.util.*

/**
 * 时间管理器
 * 功能：定时启动控制、时间段管理
 */
class TimeManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "TimeManager"
        private var instance: TimeManager? = null
        
        fun getInstance(context: Context): TimeManager {
            return instance ?: synchronized(this) {
                instance ?: TimeManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = PreferenceHelper.getInstance(context)
    private val calendar = Calendar.getInstance()

    /**
     * 检查是否在允许的时间段内
     * 默认：每日 7:00-9:00
     */
    fun isInAllowedTimePeriod(): Boolean {
        val startHour = prefs.getInt("start_hour", 7)
        val endHour = prefs.getInt("end_hour", 9)
        
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        
        return currentHour in startHour until endHour
    }

    /**
     * 获取距离下一个允许时间段的毫秒数
     */
    fun getTimeUntilNextPeriod(): Long {
        val startHour = prefs.getInt("start_hour", 7)
        val endHour = prefs.getInt("end_hour", 9)
        
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        return when {
            // 当前在时间段内
            currentHour in startHour until endHour -> 0
            
            // 当前在时间段之前
            currentHour < startHour -> {
                val millisUntilStart = ((startHour - currentHour) * 3600000L) - 
                    (currentMinute * 60000L)
                millisUntilStart.coerceAtLeast(0)
            }
            
            // 当前在时间段之后，等待明天
            else -> {
                val hoursUntilTomorrow = (24 - currentHour) + startHour
                val millisUntilTomorrow = (hoursUntilTomorrow * 3600000L) - 
                    (currentMinute * 60000L)
                millisUntilTomorrow
            }
        }
    }

    /**
     * 获取随机启动时间（在允许时间段内）
     */
    fun getRandomStartTime(): Long {
        val startHour = prefs.getInt("start_hour", 7)
        val endHour = prefs.getInt("end_hour", 9)
        
        // 在时间段内随机选择一个时间
        val randomHour = if (startHour == endHour) {
            startHour
        } else {
            startHour + (0 until (endHour - startHour)).random()
        }
        
        val randomMinute = (0..59).random()
        
        calendar.set(Calendar.HOUR_OF_DAY, randomHour)
        calendar.set(Calendar.MINUTE, randomMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        return calendar.timeInMillis
    }

    /**
     * 设置允许的时间段
     */
    fun setAllowedTimePeriod(startHour: Int, endHour: Int) {
        prefs.edit()
            .putInt("start_hour", startHour)
            .putInt("end_hour", endHour)
            .apply()
        Logger.d(TAG, "设置时间段：$startHour:00 - $endHour:00")
    }

    /**
     * 获取当前时间段配置
     */
    fun getAllowedTimePeriod(): Pair<Int, Int> {
        val startHour = prefs.getInt("start_hour", 7)
        val endHour = prefs.getInt("end_hour", 9)
        return Pair(startHour, endHour)
    }

    /**
     * 检查是否需要每日重置
     */
    fun shouldResetDaily(): Boolean {
        val lastResetDate = prefs.getString("last_reset_date", "")
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            .format(Date())
        
        return lastResetDate != currentDate
    }

    /**
     * 记录每日重置
     */
    fun markDailyReset() {
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            .format(Date())
        prefs.edit().putString("last_reset_date", currentDate).apply()
    }

    /**
     * 获取今日剩余可用时长（毫秒）
     */
    fun getRemainingTodayTime(): Long {
        val totalDailyTime = prefs.getLong("total_daily_time", 86400000L) // 24 小时
        val usedTime = prefs.getLong("today_used_time", 0L)
        return (totalDailyTime - usedTime).coerceAtLeast(0)
    }

    /**
     * 更新今日已用时长
     */
    fun updateTodayUsedTime(durationMs: Long) {
        if (shouldResetDaily()) {
            prefs.edit().putLong("today_used_time", 0L).apply()
            markDailyReset()
        }
        
        val currentUsed = prefs.getLong("today_used_time", 0L)
        prefs.edit().putLong("today_used_time", currentUsed + durationMs).apply()
    }
}

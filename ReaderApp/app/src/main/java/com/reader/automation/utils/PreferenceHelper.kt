package com.reader.automation.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences 工具类
 */
class PreferenceHelper private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "reader_automation_prefs", 
        Context.MODE_PRIVATE
    )

    companion object {
        @Volatile
        private var instance: PreferenceHelper? = null

        fun getInstance(context: Context): PreferenceHelper {
            return instance ?: synchronized(this) {
                instance ?: PreferenceHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }

    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return prefs.getLong(key, defaultValue)
    }

    fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return prefs.getFloat(key, defaultValue)
    }

    fun putFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun contains(key: String): Boolean {
        return prefs.contains(key)
    }
}

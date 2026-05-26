package com.reader.automation.utils

import android.util.Log

/**
 * 统一日志工具
 */
object Logger {
    
    private const val TAG = "ReaderAutomation"
    private var isDebug = true
    
    fun d(tag: String, message: String) {
        if (isDebug) {
            Log.d(tag, message)
        }
    }
    
    fun i(tag: String, message: String) {
        Log.i(tag, message)
    }
    
    fun w(tag: String, message: String) {
        Log.w(tag, message)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }
    
    fun v(tag: String, message: String) {
        if (isDebug) {
            Log.v(tag, message)
        }
    }
    
    fun setDebug(debug: Boolean) {
        isDebug = debug
    }
}

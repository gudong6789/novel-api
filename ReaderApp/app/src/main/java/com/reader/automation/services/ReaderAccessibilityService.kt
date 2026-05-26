package com.reader.automation.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.reader.automation.managers.AppSwitchManager
import com.reader.automation.managers.BookProgressManager
import com.reader.automation.managers.TimeManager
import com.reader.automation.models.AppConfig
import com.reader.automation.models.BookInfo
import com.reader.automation.ui.ErrorActivity
import com.reader.automation.utils.Logger
import com.reader.automation.utils.PreferenceHelper
import kotlinx.coroutines.*
import java.util.*
import kotlin.random.Random

/**
 * 核心无障碍服务 - 模拟真人阅读操作
 * 功能：自动翻页、滑动、点击、APP 切换
 */
class ReaderAccessibilityService : AccessibilityService() {

    companion object {
        const val TAG = "ReaderAccessibility"
        var instance: ReaderAccessibilityService? = null
            private set
        
        // 当前运行的 APP 包名
        var currentAppPackage: String? = null
        
        // 服务运行状态
        var isRunning = false
        var isPaused = false
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    
    // 管理器
    private lateinit var timeManager: TimeManager
    private lateinit var appSwitchManager: AppSwitchManager
    private lateinit var bookProgressManager: BookProgressManager
    
    // 滑动任务
    private var scrollJob: Job? = null
    private var appSwitchJob: Job? = null
    
    // 配置
    private var scrollIntervalMin = 5000L  // 5 秒
    private var scrollIntervalMax = 10000L // 10 秒
    private var singleReadDurationMin = 3600000L // 1 小时
    private var singleReadDurationMax = 7200000L // 2 小时

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Logger.d(TAG, "无障碍服务已连接")
        
        timeManager = TimeManager.getInstance(this)
        appSwitchManager = AppSwitchManager.getInstance(this)
        bookProgressManager = BookProgressManager.getInstance(this)
        
        loadConfig()
        startReadingTask()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !isRunning || isPaused) return
        
        val packageName = event.packageName?.toString() ?: return
        
        // 检测当前 APP 是否变化
        if (packageName != currentAppPackage) {
            currentAppPackage = packageName
            Logger.d(TAG, "切换到 APP: $packageName")
            
            // 检查是否需要切换 APP
            checkAppSwitch(packageName)
        }
        
        // 根据事件类型处理
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleContentChanged(event)
            }
        }
    }

    override fun onInterrupt() {
        Logger.w(TAG, "服务被中断")
        isRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        Logger.d(TAG, "服务已销毁")
    }

    /**
     * 加载配置
     */
    private fun loadConfig() {
        val prefs = PreferenceHelper.getInstance(applicationContext)
        scrollIntervalMin = prefs.getLong("scroll_interval_min", 5000)
        scrollIntervalMax = prefs.getLong("scroll_interval_max", 10000)
        singleReadDurationMin = prefs.getLong("single_read_duration_min", 3600000)
        singleReadDurationMax = prefs.getLong("single_read_duration_max", 7200000)
    }

    /**
     * 开始阅读任务
     */
    fun startReadingTask() {
        if (isRunning) return
        
        isRunning = true
        isPaused = false
        
        serviceScope.launch {
            // 检查是否在允许的时间段内
            if (!timeManager.isInAllowedTimePeriod()) {
                Logger.d(TAG, "不在允许的时间段内，等待...")
                delay(timeManager.getTimeUntilNextPeriod())
            }
            
            // 启动滑动任务
            startScrollTask()
            
            // 启动 APP 轮换任务
            startAppSwitchTask()
        }
    }

    /**
     * 停止阅读任务
     */
    fun stopReadingTask() {
        isRunning = false
        scrollJob?.cancel()
        appSwitchJob?.cancel()
        Logger.d(TAG, "阅读任务已停止")
    }

    /**
     * 暂停阅读任务
     */
    fun pauseReadingTask() {
        isPaused = true
        scrollJob?.cancel()
        Logger.d(TAG, "阅读任务已暂停")
    }

    /**
     * 恢复阅读任务
     */
    fun resumeReadingTask() {
        isPaused = false
        startScrollTask()
        Logger.d(TAG, "阅读任务已恢复")
    }

    /**
     * 启动自动滑动任务
     */
    private fun startScrollTask() {
        scrollJob?.cancel()
        scrollJob = serviceScope.launch {
            while (isRunning && !isPaused) {
                try {
                    // 随机等待间隔
                    val delay = Random.nextLong(scrollIntervalMin, scrollIntervalMax)
                    delay(delay)
                    
                    if (!isPaused) {
                        performScroll()
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "滑动失败", e)
                }
            }
        }
    }

    /**
     * 执行滑动操作 - 模拟真人
     */
    private suspend fun performScroll() {
        val rootNode = rootInActiveWindow ?: return
        
        try {
            // 获取屏幕尺寸
            val rect = Rect()
            rootNode.getBoundsInScreen(rect)
            val screenWidth = rect.width()
            val screenHeight = rect.height()
            
            // 随机起始点和终点，模拟真人滑动
            val startX = screenWidth / 2 + Random.nextInt(-100, 100)
            val startY = screenHeight * 0.7f + Random.nextInt(-50, 50)
            val endX = screenWidth / 2 + Random.nextInt(-100, 100)
            val endY = screenHeight * 0.3f + Random.nextInt(-50, 50)
            
            // 创建滑动路径
            val path = Path()
            path.moveTo(startX, startY)
            path.lineTo(endX, endY)
            
            // 构建手势
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, Random.nextLong(300, 800)))
                .build()
            
            // 执行滑动
            dispatchGesture(gesture, null, null)
            Logger.d(TAG, "执行滑动：($startX,$startY) -> ($endX,$endY)")
            
            // 记录阅读进度
            bookProgressManager.recordReading(currentAppPackage ?: "", 1)
            
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * 启动 APP 轮换任务
     */
    private fun startAppSwitchTask() {
        appSwitchJob?.cancel()
        appSwitchJob = serviceScope.launch {
            while (isRunning && !isPaused) {
                try {
                    // 检查当前 APP 阅读时长
                    val currentApp = currentAppPackage ?: run {
                        delay(5000)
                        continue
                    }
                    
                    val appReadTime = bookProgressManager.getAppTodayReadTime(currentApp)
                    val maxAppTime = PreferenceHelper.getInstance(applicationContext)
                        .getLong("max_app_daily_time", 43200000L) // 12 小时
                    
                    if (appReadTime >= maxAppTime) {
                        Logger.d(TAG, "APP [$currentApp] 今日阅读已达上限，切换下一个")
                        appSwitchManager.switchToNextApp()
                    }
                    
                    // 每 5 分钟检查一次
                    delay(300000)
                    
                } catch (e: Exception) {
                    Logger.e(TAG, "APP 轮换检查失败", e)
                }
            }
        }
    }

    /**
     * 检查是否需要切换 APP
     */
    private fun checkAppSwitch(currentPackage: String) {
        serviceScope.launch {
            val apps = appSwitchManager.getConfiguredApps()
            if (apps.none { it.packageName == currentPackage }) {
                Logger.d(TAG, "当前 APP 不在配置列表中，尝试切换")
                appSwitchManager.switchToNextApp()
            }
        }
    }

    /**
     * 处理窗口状态变化
     */
    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        // 检测是否进入阅读页面
        val className = event.className?.toString() ?: return
        Logger.d(TAG, "窗口状态变化：$className")
    }

    /**
     * 处理内容变化
     */
    private fun handleContentChanged(event: AccessibilityEvent) {
        // 可以检测章节切换等
    }

    /**
     * 点击指定文本
     */
    fun clickByText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        
        try {
            val nodes = rootNode.findAccessibilityNodeInfosByText(text)
            if (nodes.isEmpty()) return false
            
            val node = nodes.firstOrNull { it.isClickable } ?: nodes.first()
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * 查找并点击指定 ID 的视图
     */
    fun clickByViewId(viewId: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        
        try {
            val node = rootNode.findAccessibilityNodeInfosByViewId(viewId).firstOrNull()
            node?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return node != null
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * 返回操作
     */
    fun performBack(): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        try {
            return rootNode.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
        } finally {
            rootNode.recycle()
        }
    }
}

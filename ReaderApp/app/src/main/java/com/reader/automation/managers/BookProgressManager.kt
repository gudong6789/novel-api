package com.reader.automation.managers

import android.content.Context
import com.reader.automation.database.AppDatabase
import com.reader.automation.models.BookInfo
import com.reader.automation.utils.Logger
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 书籍进度管理器
 * 功能：记录阅读进度、时长统计、自动跳过已完结书籍
 */
class BookProgressManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "BookProgressManager"
        private var instance: BookProgressManager? = null
        
        fun getInstance(context: Context): BookProgressManager {
            return instance ?: synchronized(this) {
                instance ?: BookProgressManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val database = AppDatabase.getInstance(context)
    private val bookDao = database.bookDao()
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    
    // 今日日期缓存
    private var todayDate: String = dateFormat.format(Date())

    /**
     * 记录阅读时长
     */
    fun recordReading(packageName: String, durationSeconds: Int) {
        managerScope.launch {
            try {
                val currentBook = getCurrentReadingBook(packageName)
                if (currentBook != null) {
                    val durationMs = durationSeconds * 1000L
                    
                    // 更新书籍阅读时长
                    bookDao.updateTodayReadTime(currentBook.id, durationMs, todayDate)
                    
                    // 更新 APP 今日总阅读时长
                    updateAppTodayReadTime(packageName, durationMs)
                    
                    Logger.d(TAG, "记录阅读：${currentBook.bookTitle} +${durationSeconds}秒")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "记录阅读失败", e)
            }
        }
    }

    /**
     * 获取 APP 今日已读时长
     */
    fun getAppTodayReadTime(packageName: String): Long {
        return try {
            val appStats = database.appStatsDao().getTodayStats(packageName, todayDate)
            appStats?.totalReadTime ?: 0L
        } catch (e: Exception) {
            Logger.e(TAG, "获取 APP 阅读时长失败", e)
            0L
        }
    }

    /**
     * 更新 APP 今日阅读时长
     */
    private fun updateAppTodayReadTime(packageName: String, durationMs: Long) {
        managerScope.launch {
            try {
                database.appStatsDao().insertOrUpdate(packageName, todayDate, durationMs)
            } catch (e: Exception) {
                Logger.e(TAG, "更新 APP 阅读时长失败", e)
            }
        }
    }

    /**
     * 获取当前正在阅读的书籍
     */
    private suspend fun getCurrentReadingBook(packageName: String): BookInfo? {
        return bookDao.getCurrentReadingBook(packageName)
    }

    /**
     * 添加书籍
     */
    fun addBook(book: BookInfo) {
        managerScope.launch {
            try {
                bookDao.insert(book)
                Logger.d(TAG, "添加书籍：${book.bookTitle}")
            } catch (e: Exception) {
                Logger.e(TAG, "添加书籍失败", e)
            }
        }
    }

    /**
     * 批量添加书籍
     */
    fun addBooks(books: List<BookInfo>) {
        managerScope.launch {
            try {
                bookDao.insertAll(books)
                Logger.d(TAG, "批量添加 ${books.size} 本书")
            } catch (e: Exception) {
                Logger.e(TAG, "批量添加书籍失败", e)
            }
        }
    }

    /**
     * 更新书籍进度
     */
    fun updateBookProgress(bookId: Long, chapter: Int, progress: Float, isCompleted: Boolean) {
        managerScope.launch {
            try {
                bookDao.updateProgress(bookId, chapter, progress, isCompleted)
                if (isCompleted) {
                    Logger.d(TAG, "书籍已完结：bookId=$bookId")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "更新进度失败", e)
            }
        }
    }

    /**
     * 标记书籍为已完结
     */
    fun markAsCompleted(bookId: Long) {
        managerScope.launch {
            try {
                bookDao.markCompleted(bookId)
                Logger.d(TAG, "标记已完结：bookId=$bookId")
            } catch (e: Exception) {
                Logger.e(TAG, "标记完结失败", e)
            }
        }
    }

    /**
     * 获取某 APP 的所有书籍
     */
    suspend fun getBooksByApp(packageName: String): List<BookInfo> {
        return try {
            bookDao.getBooksByApp(packageName)
        } catch (e: Exception) {
            Logger.e(TAG, "获取书籍列表失败", e)
            emptyList()
        }
    }

    /**
     * 获取按作者筛选的书籍
     */
    suspend fun getBooksByAuthor(packageName: String, author: String): List<BookInfo> {
        return try {
            bookDao.getBooksByAuthor(packageName, author)
        } catch (e: Exception) {
            Logger.e(TAG, "获取作者书籍失败", e)
            emptyList()
        }
    }

    /**
     * 获取未完结的书籍列表
     */
    suspend fun getActiveBooks(packageName: String): List<BookInfo> {
        return try {
            bookDao.getActiveBooks(packageName)
        } catch (e: Exception) {
            Logger.e(TAG, "获取活跃书籍失败", e)
            emptyList()
        }
    }

    /**
     * 清空所有完结进度
     */
    fun resetAllCompletedBooks() {
        managerScope.launch {
            try {
                bookDao.resetAllCompleted()
                Logger.d(TAG, "已清空所有完结进度")
            } catch (e: Exception) {
                Logger.e(TAG, "清空完结进度失败", e)
            }
        }
    }

    /**
     * 清空指定 APP 的完结进度
     */
    fun resetCompletedBooks(packageName: String) {
        managerScope.launch {
            try {
                bookDao.resetCompletedByApp(packageName)
                Logger.d(TAG, "已清空 APP 完结进度：$packageName")
            } catch (e: Exception) {
                Logger.e(TAG, "清空完结进度失败", e)
            }
        }
    }

    /**
     * 每日重置 - 清零今日阅读时长
     */
    fun resetDailyStats() {
        managerScope.launch {
            try {
                // 更新日期
                todayDate = dateFormat.format(Date())
                
                // 数据库会自动按日期统计，无需手动清零
                Logger.d(TAG, "每日统计已重置，新日期：$todayDate")
            } catch (e: Exception) {
                Logger.e(TAG, "每日重置失败", e)
            }
        }
    }

    /**
     * 检查日期是否变化
     */
    fun checkDateChanged(): Boolean {
        val currentDate = dateFormat.format(Date())
        if (currentDate != todayDate) {
            todayDate = currentDate
            return true
        }
        return false
    }
}

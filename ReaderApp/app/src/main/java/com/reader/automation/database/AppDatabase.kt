package com.reader.automation.database

import android.content.Context
import androidx.room.*
import com.reader.automation.models.AppConfig
import com.reader.automation.models.BookInfo
import java.util.*

/**
 * 书籍数据访问对象
 */
@Dao
interface BookDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookInfo)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<BookInfo>)
    
    @Update
    suspend fun update(book: BookInfo)
    
    @Delete
    suspend fun delete(book: BookInfo)
    
    @Query("SELECT * FROM book_info WHERE packageName = :packageName ORDER BY sortIndex, priority DESC")
    suspend fun getBooksByApp(packageName: String): List<BookInfo>
    
    @Query("SELECT * FROM book_info WHERE packageName = :packageName AND author = :author")
    suspend fun getBooksByAuthor(packageName: String, author: String): List<BookInfo>
    
    @Query("SELECT * FROM book_info WHERE packageName = :packageName AND isCompleted = 0 AND isEnabled = 1 ORDER BY sortIndex")
    suspend fun getActiveBooks(packageName: String): List<BookInfo>
    
    @Query("SELECT * FROM book_info WHERE packageName = :packageName AND isEnabled = 1 LIMIT 1")
    suspend fun getCurrentReadingBook(packageName: String): BookInfo?
    
    @Query("UPDATE book_info SET currentChapter = :chapter, readProgress = :progress, isCompleted = :isCompleted, updateTime = :updateTime WHERE id = :bookId")
    suspend fun updateProgress(bookId: Long, chapter: Int, progress: Float, isCompleted: Boolean, updateTime: Long = System.currentTimeMillis())
    
    @Query("UPDATE book_info SET isCompleted = 1, updateTime = :updateTime WHERE id = :bookId")
    suspend fun markCompleted(bookId: Long, updateTime: Long = System.currentTimeMillis())
    
    @Query("UPDATE book_info SET todayReadTime = todayReadTime + :durationMs, lastReadTime = :lastReadTime, lastReadDate = :date WHERE id = :bookId")
    suspend fun updateTodayReadTime(bookId: Long, durationMs: Long, date: String, lastReadTime: Long = System.currentTimeMillis())
    
    @Query("UPDATE book_info SET isCompleted = 0, currentChapter = 0, readProgress = 0 WHERE isCompleted = 1")
    suspend fun resetAllCompleted()
    
    @Query("UPDATE book_info SET isCompleted = 0, currentChapter = 0, readProgress = 0 WHERE packageName = :packageName AND isCompleted = 1")
    suspend fun resetCompletedByApp(packageName: String)
    
    @Query("SELECT * FROM book_info WHERE id = :bookId")
    suspend fun getBookById(bookId: Long): BookInfo?
}

/**
 * APP 统计数据访问对象
 */
@Dao
interface AppStatsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stats: AppStats)
    
    @Query("SELECT * FROM app_stats WHERE packageName = :packageName AND date = :date")
    suspend fun getTodayStats(packageName: String, date: String): AppStats?
    
    @Query("UPDATE app_stats SET totalReadTime = totalReadTime + :durationMs WHERE packageName = :packageName AND date = :date")
    suspend fun addReadTime(packageName: String, date: String, durationMs: Long)
    
    @Query("DELETE FROM app_stats WHERE date < :cutoffDate")
    suspend fun deleteOldStats(cutoffDate: String)
}

/**
 * 插入或更新 APP 统计数据
 */
suspend fun AppStatsDao.insertOrUpdate(packageName: String, date: String, durationMs: Long) {
    val existing = getTodayStats(packageName, date)
    if (existing != null) {
        addReadTime(packageName, date, durationMs)
    } else {
        insert(AppStats(packageName = packageName, date = date, totalReadTime = durationMs))
    }
}

/**
 * APP 统计数据模型
 */
@Entity(
    tableName = "app_stats",
    primaryKeys = ["packageName", "date"]
)
data class AppStats(
    val packageName: String,
    val date: String,           // YYYY-MM-DD
    val totalReadTime: Long = 0 // 毫秒
)

/**
 * Room 数据库
 */
@Database(
    entities = [AppConfig::class, BookInfo::class, AppStats::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun appConfigDao(): AppConfigDao
    abstract fun bookDao(): BookDao
    abstract fun appStatsDao(): AppStatsDao
    
    companion object {
        @Volatile
        private var instance: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reader_automation.db"
                ).build().also { instance = it }
            }
        }
    }
}

/**
 * APP 配置 DAO
 */
@Dao
interface AppConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: AppConfig)
    
    @Update
    suspend fun update(app: AppConfig)
    
    @Delete
    suspend fun delete(app: AppConfig)
    
    @Query("SELECT * FROM app_config ORDER BY sortIndex")
    suspend fun getAllApps(): List<AppConfig>
    
    @Query("SELECT * FROM app_config WHERE packageName = :packageName")
    suspend fun getAppByPackage(packageName: String): AppConfig?
}

/**
 * 类型转换器
 */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

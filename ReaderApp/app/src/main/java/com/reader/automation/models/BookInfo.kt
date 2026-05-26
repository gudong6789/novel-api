package com.reader.automation.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 书籍信息模型
 */
@Entity(tableName = "book_info")
data class BookInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,           // 所属 APP
    val bookTitle: String,             // 书名
    val author: String,                // 作者
    val authorPageUrl: String? = null, // 作者主页链接
    val coverUrl: String? = null,      // 封面图
    val description: String? = null,   // 简介
    val isCompleted: Boolean = false,  // 是否完结
    val currentChapter: Int = 0,       // 当前章节
    val totalChapters: Int = 0,        // 总章节数
    val readProgress: Float = 0f,      // 阅读进度 0-1
    val lastReadTime: Long = 0,        // 最后阅读时间
    val totalReadTime: Long = 0,       // 累计阅读时长 (毫秒)
    val todayReadTime: Long = 0,       // 今日阅读时长 (毫秒)
    val lastReadDate: String = "",     // 最后阅读日期 YYYY-MM-DD
    val sortIndex: Int = 0,            // 排序索引
    val isEnabled: Boolean = true,     // 是否启用
    val priority: Int = 0,             // 优先级
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis()
)

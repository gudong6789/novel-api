package com.admin.manager.database

import android.content.Context
import androidx.room.*
import com.admin.manager.models.AuthCode
import com.admin.manager.models.DeviceInfo
import java.util.*

/**
 * 授权码 DAO
 */
@Dao
interface AuthCodeDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(authCode: AuthCode)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(authCodes: List<AuthCode>)
    
    @Update
    suspend fun update(authCode: AuthCode)
    
    @Delete
    suspend fun delete(authCode: AuthCode)
    
    @Query("SELECT * FROM auth_codes ORDER BY createTime DESC")
    suspend fun getAllCodes(): List<AuthCode>
    
    @Query("SELECT * FROM auth_codes WHERE code = :code")
    suspend fun getCodeByValue(code: String): AuthCode?
    
    @Query("SELECT * FROM auth_codes WHERE isActive = 1 AND isBlocked = 0")
    suspend fun getActiveCodes(): List<AuthCode>
    
    @Query("SELECT * FROM auth_codes WHERE deviceId IS NULL AND isActive = 0")
    suspend fun getUnusedCodes(): List<AuthCode>
    
    @Query("UPDATE auth_codes SET isActive = 1, deviceId = :deviceId, activateTime = :activateTime, expireTime = :expireTime WHERE code = :code")
    suspend fun activateCode(code: String, deviceId: String, activateTime: Long, expireTime: Long?)
    
    @Query("UPDATE auth_codes SET isBlocked = :blocked WHERE code = :code")
    suspend fun blockCode(code: String, blocked: Boolean)
    
    @Query("DELETE FROM auth_codes WHERE code = :code")
    suspend fun deleteCode(code: String)
    
    @Query("SELECT COUNT(*) FROM auth_codes WHERE isActive = 1")
    suspend fun getActiveCount(): Int
    
    @Query("SELECT COUNT(*) FROM auth_codes WHERE deviceId IS NOT NULL")
    suspend fun getUsedCount(): Int
}

/**
 * 设备 DAO
 */
@Dao
interface DeviceDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceInfo)
    
    @Update
    suspend fun update(device: DeviceInfo)
    
    @Delete
    suspend fun delete(device: DeviceInfo)
    
    @Query("SELECT * FROM devices ORDER BY lastActiveTime DESC")
    suspend fun getAllDevices(): List<DeviceInfo>
    
    @Query("SELECT * FROM devices WHERE deviceId = :deviceId")
    suspend fun getDeviceById(deviceId: String): DeviceInfo?
    
    @Query("SELECT * FROM devices WHERE isActive = 1")
    suspend fun getOnlineDevices(): List<DeviceInfo>
    
    @Query("SELECT * FROM devices WHERE authCode = :authCode")
    suspend fun getDeviceByAuthCode(authCode: String): DeviceInfo?
    
    @Query("UPDATE devices SET lastActiveTime = :lastActiveTime, isActive = 1 WHERE deviceId = :deviceId")
    suspend fun updateActiveTime(deviceId: String, lastActiveTime: Long)
    
    @Query("UPDATE devices SET todayReadTime = todayReadTime + :duration WHERE deviceId = :deviceId")
    suspend fun addReadTime(deviceId: String, duration: Long)
    
    @Query("UPDATE devices SET isActive = 0 WHERE lastActiveTime < :threshold")
    suspend fun markOfflineDevices(threshold: Long)
    
    @Query("SELECT COUNT(*) FROM devices")
    suspend fun getTotalCount(): Int
    
    @Query("SELECT COUNT(*) FROM devices WHERE isActive = 1")
    suspend fun getOnlineCount(): Int
}

/**
 * Room 数据库
 */
@Database(
    entities = [AuthCode::class, DeviceInfo::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun authCodeDao(): AuthCodeDao
    abstract fun deviceDao(): DeviceDao
    
    companion object {
        @Volatile
        private var instance: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "admin_manager.db"
                ).build().also { instance = it }
            }
        }
    }
}

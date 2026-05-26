package com.admin.manager.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 云端 API 服务接口 (管理后台)
 * 基础地址：https://novel-api-production-55af.up.railway.app/api
 */

private const val BASE_URL = "https://novel-api-production-55af.up.railway.app/api"

interface ApiService {
    
    /**
     * 管理员登录
     * POST /admin/login
     */
    @retrofit2.http.POST("admin/login")
    suspend fun adminLogin(
        @retrofit2.http.Body request: AdminLoginRequest
    ): retrofit2.Response<AdminLoginResponse>
    
    /**
     * 生成授权码
     * POST /auth/generate
     */
    @retrofit2.http.POST("auth/generate")
    suspend fun generateAuthCode(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: GenerateAuthCodeRequest
    ): retrofit2.Response<GenerateAuthCodeResponse>
    
    /**
     * 批量生成授权码
     * POST /auth/generate-batch
     */
    @retrofit2.http.POST("auth/generate-batch")
    suspend fun generateAuthCodeBatch(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: GenerateAuthCodeBatchRequest
    ): retrofit2.Response<GenerateAuthCodeBatchResponse>
    
    /**
     * 获取授权码列表
     * GET /auth/list
     */
    @retrofit2.http.GET("auth/list")
    suspend fun getAuthCodeList(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Query("page") page: Int,
        @retrofit2.http.Query("pageSize") pageSize: Int
    ): retrofit2.Response<AuthCodeListResponse>
    
    /**
     * 封禁/解封授权码
     * POST /auth/block
     */
    @retrofit2.http.POST("auth/block")
    suspend fun blockAuthCode(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: BlockAuthCodeRequest
    ): retrofit2.Response<BaseResponse<Unit>>
    
    /**
     * 删除授权码
     * DELETE /auth/delete/{code}
     */
    @retrofit2.http.DELETE("auth/delete/{code}")
    suspend fun deleteAuthCode(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("code") code: String
    ): retrofit2.Response<BaseResponse<Unit>>
    
    /**
     * 获取设备列表
     * GET /device/list
     */
    @retrofit2.http.GET("device/list")
    suspend fun getDeviceList(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Query("page") page: Int,
        @retrofit2.http.Query("pageSize") pageSize: Int
    ): retrofit2.Response<DeviceListResponse>
    
    /**
     * 获取设备详情
     * GET /device/detail/{deviceId}
     */
    @retrofit2.http.GET("device/detail/{deviceId}")
    suspend fun getDeviceDetail(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("deviceId") deviceId: String
    ): retrofit2.Response<DeviceDetailResponse>
    
    /**
     * 更新全局配置
     * POST /config/global
     */
    @retrofit2.http.POST("config/global")
    suspend fun updateGlobalConfig(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: GlobalConfigRequest
    ): retrofit2.Response<BaseResponse<Unit>>
    
    /**
     * 获取统计数据
     * GET /statistics
     */
    @retrofit2.http.GET("statistics")
    suspend fun getStatistics(
        @retrofit2.http.Header("Authorization") token: String
    ): retrofit2.Response<StatisticsResponse>
}

// ============ 请求/响应模型 ============

data class AdminLoginRequest(
    val username: String,
    val password: String
)

data class AdminLoginResponse(
    val success: Boolean,
    val message: String,
    val token: String?,
    val adminInfo: AdminInfo?
)

data class AdminInfo(
    val id: String,
    val username: String,
    val role: String,
    val permissions: List<String>
)

data class GenerateAuthCodeRequest(
    val durationType: Int,  // 1=1 天，7=7 天，30=30 天，0=永久
    val notes: String
)

data class GenerateAuthCodeResponse(
    val success: Boolean,
    val message: String,
    val authCode: String?,
    val expireTime: Long?
)

data class GenerateAuthCodeBatchRequest(
    val count: Int,
    val durationType: Int,
    val notes: String
)

data class GenerateAuthCodeBatchResponse(
    val success: Boolean,
    val message: String,
    val codes: List<String>?
)

data class AuthCodeListResponse(
    val success: Boolean,
    val message: String,
    val data: AuthCodeData?
)

data class AuthCodeData(
    val total: Int,
    val list: List<AuthCodeItem>
)

data class AuthCodeItem(
    val code: String,
    val durationType: Int,
    val durationDays: Int,
    val expireTime: Long?,
    val isActive: Boolean,
    val isBlocked: Boolean,
    val deviceId: String?,
    val activatedTime: Long?,
    val notes: String,
    val creator: String
)

data class BlockAuthCodeRequest(
    val code: String,
    val blocked: Boolean
)

data class DeviceListResponse(
    val success: Boolean,
    val message: String,
    val data: DeviceData?
)

data class DeviceData(
    val total: Int,
    val list: List<DeviceItem>
)

data class DeviceItem(
    val deviceId: String,
    val deviceName: String,
    val deviceModel: String,
    val authCode: String,
    val isOnline: Boolean,
    val lastActiveTime: Long,
    val todayReadTime: Long,
    val totalReadTime: Long,
    val installedApps: List<String>
)

data class DeviceDetailResponse(
    val success: Boolean,
    val message: String,
    val device: DeviceDetail?
)

data class DeviceDetail(
    val deviceId: String,
    val deviceName: String,
    val deviceModel: String,
    val androidVersion: String,
    val authCode: String,
    val isOnline: Boolean,
    val lastActiveTime: Long,
    val todayReadTime: Long,
    val totalReadTime: Long,
    val installedApps: List<String>,
    val readingHistory: List<ReadingHistory>
)

data class ReadingHistory(
    val packageName: String,
    val bookTitle: String,
    val readProgress: Float,
    val readDuration: Long,
    val timestamp: Long
)

data class GlobalConfigRequest(
    val startHour: Int,
    val endHour: Int,
    val scrollIntervalMin: Long,
    val scrollIntervalMax: Long,
    val singleReadDurationMin: Long,
    val singleReadDurationMax: Long,
    val maxAppDailyTime: Long,
    val totalDailyTime: Long
)

data class StatisticsResponse(
    val success: Boolean,
    val message: String,
    val data: StatisticsData?
)

data class StatisticsData(
    val totalCodes: Int,
    val activeCodes: Int,
    val usedCodes: Int,
    val unusedCodes: Int,
    val totalDevices: Int,
    val onlineDevices: Int,
    val totalReadTime: Long,
    val todayReadTime: Long
)

data class BaseResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)

// ============ Retrofit 单例 ============

object ApiClient {
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}

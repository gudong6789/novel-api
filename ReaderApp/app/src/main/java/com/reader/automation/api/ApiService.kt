package com.reader.automation.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 云端 API 服务接口
 * 基础地址：https://novel-api-production-55af.up.railway.app/api
 */

private const val BASE_URL = "https://novel-api-production-55af.up.railway.app/api"

interface ApiService {
    
    /**
     * 验证授权码
     * POST /auth/verify
     */
    @retrofit2.http.POST("auth/verify")
    suspend fun verifyAuthCode(
        @retrofit2.http.Body request: AuthVerifyRequest
    ): retrofit2.Response<AuthVerifyResponse>
    
    /**
     * 上报设备信息
     * POST /device/report
     */
    @retrofit2.http.POST("device/report")
    suspend fun reportDeviceInfo(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: DeviceReportRequest
    ): retrofit2.Response<BaseResponse<Unit>>
    
    /**
     * 获取全局配置
     * GET /config/global
     */
    @retrofit2.http.GET("config/global")
    suspend fun getGlobalConfig(
        @retrofit2.http.Header("Authorization") token: String
    ): retrofit2.Response<GlobalConfigResponse>
    
    /**
     * 上报阅读进度
     * POST /reading/progress
     */
    @retrofit2.http.POST("reading/progress")
    suspend fun reportReadingProgress(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: ReadingProgressRequest
    ): retrofit2.Response<BaseResponse<Unit>>
    
    /**
     * 心跳保活
     * POST /device/heartbeat
     */
    @retrofit2.http.POST("device/heartbeat")
    suspend fun sendHeartbeat(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: HeartbeatRequest
    ): retrofit2.Response<BaseResponse<Unit>>
}

// ============ 请求/响应模型 ============

data class AuthVerifyRequest(
    val authCode: String,
    val deviceId: String,
    val deviceModel: String,
    val appVersion: String
)

data class AuthVerifyResponse(
    val success: Boolean,
    val message: String,
    val token: String?,
    val expireTime: Long?,
    val config: GlobalConfig?
)

data class DeviceReportRequest(
    val deviceId: String,
    val deviceName: String,
    val deviceModel: String,
    val androidVersion: String,
    val appVersion: String,
    val installedApps: List<String>,
    val authCode: String
)

data class ReadingProgressRequest(
    val packageName: String,
    val bookTitle: String,
    val readProgress: Float,
    val readDuration: Long,
    val timestamp: Long
)

data class HeartbeatRequest(
    val status: String,  // "online", "reading", "idle", "error"
    val batteryLevel: Int,
    val networkType: String,
    val timestamp: Long
)

data class GlobalConfigResponse(
    val success: Boolean,
    val config: GlobalConfig?
)

data class GlobalConfig(
    val startHour: Int,
    val endHour: Int,
    val scrollIntervalMin: Long,
    val scrollIntervalMax: Long,
    val singleReadDurationMin: Long,
    val singleReadDurationMax: Long,
    val maxAppDailyTime: Long,
    val totalDailyTime: Long
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

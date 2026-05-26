package com.admin.manager.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.admin.manager.database.AppDatabase
import com.admin.manager.databinding.ActivityStatisticsBinding
import com.admin.manager.utils.Logger
import kotlinx.coroutines.*

/**
 * 数据统计界面
 * 功能：查看授权码统计、设备统计、运行数据
 */
class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var database: AppDatabase
    private val mainScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = AppDatabase.getInstance(applicationContext)
        
        setupViews()
        loadStatistics()
    }

    private fun setupViews() {
        supportActionBar?.title = "数据统计"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // 刷新按钮
        binding.btnRefresh.setOnClickListener {
            loadStatistics()
        }
    }

    private fun loadStatistics() {
        mainScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val totalCodes = database.authCodeDao().getAllCodes().size
                    val activeCodes = database.authCodeDao().getActiveCount()
                    val usedCodes = database.authCodeDao().getUsedCount()
                    val unusedCodes = totalCodes - usedCodes
                    
                    val totalDevices = database.deviceDao().getTotalCount()
                    val onlineDevices = database.deviceDao().getOnlineCount()
                    
                    // 计算总阅读时长
                    val devices = database.deviceDao().getAllDevices()
                    val totalReadTime = devices.sumOf { it.totalReadTime }
                    val todayReadTime = devices.sumOf { it.todayReadTime }
                    
                    withContext(Dispatchers.Main) {
                        // 授权码统计
                        binding.tvTotalCodes.text = totalCodes.toString()
                        binding.tvActiveCodes.text = activeCodes.toString()
                        binding.tvUsedCodes.text = usedCodes.toString()
                        binding.tvUnusedCodes.text = unusedCodes.toString()
                        
                        // 设备统计
                        binding.tvTotalDevices.text = totalDevices.toString()
                        binding.tvOnlineDevices.text = onlineDevices.toString()
                        binding.tvOfflineDevices.text = (totalDevices - onlineDevices).toString()
                        
                        // 阅读统计
                        binding.tvTotalReadTime.text = "${totalReadTime / 3600000.0} 小时"
                        binding.tvTodayReadTime.text = "${todayReadTime / 3600000.0} 小时"
                    }
                }
            } catch (e: Exception) {
                Logger.e("StatisticsActivity", "加载统计失败", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadStatistics()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
}

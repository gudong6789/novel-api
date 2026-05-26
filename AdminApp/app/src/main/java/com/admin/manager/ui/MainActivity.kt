package com.admin.manager.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.admin.manager.databinding.ActivityMainBinding
import com.admin.manager.database.AppDatabase
import kotlinx.coroutines.*

/**
 * 授权管理后台 - 主界面
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val database by lazy { AppDatabase.getInstance(applicationContext) }
    private val mainScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViews()
        loadStatistics()
    }

    private fun setupViews() {
        // 授权码管理
        binding.btnAuthCodes.setOnClickListener {
            startActivity(Intent(this, AuthCodeManageActivity::class.java))
        }
        
        // 设备管理
        binding.btnDevices.setOnClickListener {
            startActivity(Intent(this, DeviceManageActivity::class.java))
        }
        
        // 全局配置
        binding.btnConfig.setOnClickListener {
            startActivity(Intent(this, GlobalConfigActivity::class.java))
        }
        
        // 数据统计
        binding.btnStatistics.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }
    }

    private fun loadStatistics() {
        mainScope.launch {
            withContext(Dispatchers.IO) {
                val totalCodes = database.authCodeDao().getAllCodes().size
                val activeCodes = database.authCodeDao().getActiveCount()
                val totalDevices = database.deviceDao().getTotalCount()
                val onlineDevices = database.deviceDao().getOnlineCount()
                
                withContext(Dispatchers.Main) {
                    binding.tvTotalCodes.text = totalCodes.toString()
                    binding.tvActiveCodes.text = activeCodes.toString()
                    binding.tvTotalDevices.text = totalDevices.toString()
                    binding.tvOnlineDevices.text = onlineDevices.toString()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadStatistics()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
}

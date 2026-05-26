package com.admin.manager.ui

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.admin.manager.databinding.ActivityGlobalConfigBinding
import com.admin.manager.utils.PreferenceHelper
import com.admin.manager.utils.Logger
import kotlinx.coroutines.*

/**
 * 全局配置界面
 * 功能：远程修改运行参数（启动时段、翻页速度、时长等）
 */
class GlobalConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGlobalConfigBinding
    private lateinit var prefs: PreferenceHelper
    private val mainScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGlobalConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PreferenceHelper.getInstance(applicationContext)
        
        setupViews()
        loadConfig()
    }

    private fun setupViews() {
        supportActionBar?.title = "全局配置"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // 保存按钮
        binding.btnSave.setOnClickListener {
            saveConfig()
        }
        
        // 同步云端按钮
        binding.btnSyncCloud.setOnClickListener {
            syncToCloud()
        }
    }

    private fun loadConfig() {
        // 启动时间段
        binding.etStartHour.setText(prefs.getInt("start_hour", 7).toString())
        binding.etEndHour.setText(prefs.getInt("end_hour", 9).toString())
        
        // 滑动间隔
        binding.etScrollMin.setText((prefs.getLong("scroll_interval_min", 5000) / 1000).toString())
        binding.etScrollMax.setText((prefs.getLong("scroll_interval_max", 10000) / 1000).toString())
        
        // 阅读时长
        binding.etSingleMin.setText((prefs.getLong("single_read_duration_min", 3600000) / 60000).toString())
        binding.etSingleMax.setText((prefs.getLong("single_read_duration_max", 7200000) / 60000).toString())
        
        // 时长限制
        binding.etMaxAppDaily.setText((prefs.getLong("max_app_daily_time", 43200000) / 3600000).toString())
        binding.etTotalDaily.setText((prefs.getLong("total_daily_time", 86400000) / 3600000).toString())
    }

    private fun saveConfig() {
        mainScope.launch {
            try {
                // 启动时间段
                val startHour = binding.etStartHour.text.toString().toIntOrNull() ?: 7
                val endHour = binding.etEndHour.text.toString().toIntOrNull() ?: 9
                
                // 滑动间隔
                val scrollMin = binding.etScrollMin.text.toString().toLongOrNull() ?: 5
                val scrollMax = binding.etScrollMax.text.toString().toLongOrNull() ?: 10
                
                // 阅读时长
                val singleMin = binding.etSingleMin.text.toString().toLongOrNull() ?: 60
                val singleMax = binding.etSingleMax.text.toString().toLongOrNull() ?: 120
                
                // 时长限制
                val maxAppDaily = binding.etMaxAppDaily.text.toString().toLongOrNull() ?: 12
                val totalDaily = binding.etTotalDaily.text.toString().toLongOrNull() ?: 24
                
                prefs.edit()
                    .putInt("start_hour", startHour.coerceIn(0, 23))
                    .putInt("end_hour", endHour.coerceIn(0, 23))
                    .putLong("scroll_interval_min", scrollMin.coerceAtLeast(1) * 1000)
                    .putLong("scroll_interval_max", scrollMax.coerceAtLeast(scrollMin) * 1000)
                    .putLong("single_read_duration_min", singleMin.coerceAtLeast(1) * 60000)
                    .putLong("single_read_duration_max", singleMax.coerceAtLeast(singleMin) * 60000)
                    .putLong("max_app_daily_time", maxAppDaily.coerceAtLeast(1) * 3600000)
                    .putLong("total_daily_time", totalDaily.coerceAtLeast(1) * 3600000)
                    .apply()
                
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(this@GlobalConfigActivity)
                        .setTitle("✅ 保存成功")
                        .setMessage("配置已保存，新设备将使用此配置。")
                        .setPositiveButton("确定", null)
                        .show()
                }
                
                Logger.d("GlobalConfigActivity", "配置已保存")
            } catch (e: Exception) {
                Logger.e("GlobalConfigActivity", "保存配置失败", e)
            }
        }
    }

    private fun syncToCloud() {
        // TODO: 同步到云端服务器
        AlertDialog.Builder(this)
            .setTitle("云端同步")
            .setMessage("此功能需要配置服务器地址。\n\n请在 ApiService.kt 中设置 BASE_URL。")
            .setPositiveButton("确定", null)
            .show()
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

package com.reader.automation.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.reader.automation.R
import com.reader.automation.databinding.ActivityConfigBinding
import com.reader.automation.utils.PreferenceHelper
import com.reader.automation.utils.Logger

/**
 * 运行配置界面
 * 功能：设置时间段、滑动间隔、阅读时长等
 */
class ConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigBinding
    private lateinit var prefs: PreferenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PreferenceHelper.getInstance(applicationContext)
        
        setupViews()
        loadConfig()
    }

    private fun setupViews() {
        supportActionBar?.title = "运行配置"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // 保存按钮
        binding.btnSave.setOnClickListener {
            saveConfig()
        }
        
        // 重置按钮
        binding.btnReset.setOnClickListener {
            resetToDefault()
        }
    }

    private fun loadConfig() {
        // 启动时间段
        val startHour = prefs.getInt("start_hour", 7)
        val endHour = prefs.getInt("end_hour", 9)
        binding.etStartHour.setText(startHour.toString())
        binding.etEndHour.setText(endHour.toString())
        
        // 滑动间隔
        val scrollMin = prefs.getLong("scroll_interval_min", 5000) / 1000
        val scrollMax = prefs.getLong("scroll_interval_max", 10000) / 1000
        binding.etScrollMin.setText(scrollMin.toString())
        binding.etScrollMax.setText(scrollMax.toString())
        
        // 单次阅读时长
        val singleMin = prefs.getLong("single_read_duration_min", 3600000) / 60000
        val singleMax = prefs.getLong("single_read_duration_max", 7200000) / 60000
        binding.etSingleMin.setText(singleMin.toString())
        binding.etSingleMax.setText(singleMax.toString())
        
        // 单日总时长
        val totalDaily = prefs.getLong("total_daily_time", 86400000) / 3600000
        binding.etTotalDaily.setText(totalDaily.toString())
        
        // 单 APP 最大时长
        val maxAppTime = prefs.getLong("max_app_daily_time", 43200000) / 3600000
        binding.etMaxAppTime.setText(maxAppTime.toString())
        
        // 开机自启
        val autoStart = prefs.getBoolean("auto_start", false)
        binding.switchAutoStart.isChecked = autoStart
    }

    private fun saveConfig() {
        try {
            // 启动时间段
            val startHour = binding.etStartHour.text.toString().toIntOrNull() ?: 7
            val endHour = binding.etEndHour.text.toString().toIntOrNull() ?: 9
            prefs.edit()
                .putInt("start_hour", startHour.coerceIn(0, 23))
                .putInt("end_hour", endHour.coerceIn(0, 23))
                .apply()
            
            // 滑动间隔 (秒转毫秒)
            val scrollMin = binding.etScrollMin.text.toString().toLongOrNull() ?: 5
            val scrollMax = binding.etScrollMax.text.toString().toLongOrNull() ?: 10
            prefs.edit()
                .putLong("scroll_interval_min", scrollMin.coerceAtLeast(1) * 1000)
                .putLong("scroll_interval_max", scrollMax.coerceAtLeast(scrollMin) * 1000)
                .apply()
            
            // 单次阅读时长 (分钟转毫秒)
            val singleMin = binding.etSingleMin.text.toString().toLongOrNull() ?: 60
            val singleMax = binding.etSingleMax.text.toString().toLongOrNull() ?: 120
            prefs.edit()
                .putLong("single_read_duration_min", singleMin.coerceAtLeast(1) * 60000)
                .putLong("single_read_duration_max", singleMax.coerceAtLeast(singleMin) * 60000)
                .apply()
            
            // 单日总时长 (小时转毫秒)
            val totalDaily = binding.etTotalDaily.text.toString().toLongOrNull() ?: 24
            prefs.edit()
                .putLong("total_daily_time", totalDaily.coerceAtLeast(1) * 3600000)
                .apply()
            
            // 单 APP 最大时长 (小时转毫秒)
            val maxAppTime = binding.etMaxAppTime.text.toString().toLongOrNull() ?: 12
            prefs.edit()
                .putLong("max_app_daily_time", maxAppTime.coerceAtLeast(1) * 3600000)
                .apply()
            
            // 开机自启
            prefs.edit()
                .putBoolean("auto_start", binding.switchAutoStart.isChecked)
                .apply()
            
            Logger.d("ConfigActivity", "配置已保存")
            finish()
        } catch (e: Exception) {
            Logger.e("ConfigActivity", "保存配置失败", e)
        }
    }

    private fun resetToDefault() {
        prefs.edit().clear().apply()
        loadConfig()
        Logger.d("ConfigActivity", "配置已重置")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

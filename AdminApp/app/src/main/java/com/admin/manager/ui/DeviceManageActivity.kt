package com.admin.manager.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.admin.manager.database.AppDatabase
import com.admin.manager.databinding.ActivityDeviceManageBinding
import com.admin.manager.databinding.ItemDeviceBinding
import com.admin.manager.models.DeviceInfo
import com.admin.manager.utils.Logger
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 设备管理界面
 * 功能：查看设备列表、在线状态、运行数据
 */
class DeviceManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceManageBinding
    private lateinit var database: AppDatabase
    private val mainScope = CoroutineScope(Dispatchers.Main + Job())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    
    private lateinit var deviceAdapter: DeviceAdapter
    private var allDevices: List<DeviceInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceManageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = AppDatabase.getInstance(applicationContext)
        
        setupViews()
        loadDevices()
    }

    private fun setupViews() {
        supportActionBar?.title = "设备管理"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // 刷新按钮
        binding.btnRefresh.setOnClickListener {
            loadDevices()
        }
        
        // 设备列表
        deviceAdapter = DeviceAdapter()
        binding.recyclerDevices.layoutManager = LinearLayoutManager(this)
        binding.recyclerDevices.adapter = deviceAdapter
    }

    private fun loadDevices() {
        mainScope.launch {
            try {
                allDevices = database.deviceDao().getAllDevices()
                withContext(Dispatchers.Main) {
                    deviceAdapter.submitList(allDevices)
                    binding.tvDeviceCount.text = "共 ${allDevices.size} 台设备"
                    
                    val onlineCount = allDevices.count { it.isOnline() }
                    binding.tvOnlineCount.text = "在线：$onlineCount"
                }
            } catch (e: Exception) {
                Logger.e("DeviceManageActivity", "加载设备失败", e)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
    
    /**
     * 设备列表适配器
     */
    inner class DeviceAdapter : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
        
        private var devices: List<DeviceInfo> = emptyList()
        
        fun submitList(newDevices: List<DeviceInfo>) {
            devices = newDevices
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return DeviceViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            holder.bind(devices[position])
        }
        
        override fun getItemCount(): Int = devices.size
        
        inner class DeviceViewHolder(private val binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root) {
            
            fun bind(device: DeviceInfo) {
                binding.tvDeviceName.text = device.deviceName.ifEmpty { "未知设备" }
                binding.tvDeviceModel.text = device.deviceModel
                binding.tvAuthCode.text = "授权：${device.authCode}"
                
                // 在线状态
                if (device.isOnline()) {
                    binding.tvOnlineStatus.text = "🟢 在线"
                    binding.tvOnlineStatus.setTextColor(getColor(R.color.status_online))
                } else {
                    binding.tvOnlineStatus.text = "⚪ 离线"
                    binding.tvOnlineStatus.setTextColor(getColor(R.color.status_offline))
                }
                
                // 最后活跃时间
                binding.tvLastActive.text = "最后活跃：${dateFormat.format(Date(device.lastActiveTime))}"
                
                // 今日阅读时长
                val todayHours = device.todayReadTime / 3600000.0
                binding.tvTodayRead.text = "今日阅读：${String.format("%.1f", todayHours)}小时"
                
                // 累计阅读时长
                val totalHours = device.totalReadTime / 3600000.0
                binding.tvTotalRead.text = "累计：${String.format("%.1f", totalHours)}小时"
                
                // 安装 APP 数量
                val appCount = try {
                    val apps = com.google.gson.Gson().fromJson(device.installedApps, Array<String>::class.java)
                    apps?.size ?: 0
                } catch (e: Exception) {
                    0
                }
                binding.tvAppCount.text = "已安装 APP: $appCount"
            }
        }
    }
}

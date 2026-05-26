package com.admin.manager.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.admin.manager.R
import com.admin.manager.database.AppDatabase
import com.admin.manager.databinding.ActivityAuthCodeManageBinding
import com.admin.manager.databinding.ItemAuthCodeBinding
import com.admin.manager.models.AuthCode
import com.admin.manager.utils.Logger
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 授权码管理界面
 * 功能：生成授权码、批量生成、封禁/解封、删除
 */
class AuthCodeManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthCodeManageBinding
    private lateinit var database: AppDatabase
    private val mainScope = CoroutineScope(Dispatchers.Main + Job())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    
    private lateinit var codeAdapter: AuthCodeAdapter
    private var allCodes: List<AuthCode> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthCodeManageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = AppDatabase.getInstance(applicationContext)
        
        setupViews()
        loadCodes()
    }

    private fun setupViews() {
        supportActionBar?.title = "授权码管理"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // 单个生成按钮
        binding.btnGenerateSingle.setOnClickListener {
            showGenerateDialog(false)
        }
        
        // 批量生成按钮
        binding.btnGenerateBatch.setOnClickListener {
            showGenerateDialog(true)
        }
        
        // 刷新按钮
        binding.btnRefresh.setOnClickListener {
            loadCodes()
        }
        
        // 列表
        codeAdapter = AuthCodeAdapter()
        binding.recyclerCodes.layoutManager = LinearLayoutManager(this)
        binding.recyclerCodes.adapter = codeAdapter
    }

    private fun loadCodes() {
        mainScope.launch {
            try {
                allCodes = database.authCodeDao().getAllCodes()
                withContext(Dispatchers.Main) {
                    codeAdapter.submitList(allCodes)
                    binding.tvCodeCount.text = "共 ${allCodes.size} 个授权码"
                }
            } catch (e: Exception) {
                Logger.e("AuthCodeManageActivity", "加载授权码失败", e)
            }
        }
    }

    private fun showGenerateDialog(isBatch: Boolean) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_generate_code, null)
        val spinnerDuration = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerDuration)
        val etCount = dialogView.findViewById<EditText>(R.id.etCount)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)
        
        // 时长选项
        val durations = arrayOf("1 天", "7 天", "30 天", "永久")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, durations)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDuration.adapter = adapter
        
        if (!isBatch) {
            etCount.visibility = View.GONE
        } else {
            etCount.visibility = View.VISIBLE
            etCount.setText("10")
        }
        
        AlertDialog.Builder(this)
            .setTitle(if (isBatch) "批量生成授权码" else "生成授权码")
            .setView(dialogView)
            .setPositiveButton("生成") { _, _ ->
                val durationType = spinnerDuration.selectedItemPosition
                val count = etCount.text.toString().toIntOrNull() ?: 1
                val notes = etNotes.text.toString().trim()
                
                if (isBatch && count > 0) {
                    generateBatchCodes(count, durationType, notes)
                } else {
                    generateSingleCode(durationType, notes)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun generateSingleCode(durationType: Int, notes: String) {
        mainScope.launch {
            try {
                val code = generateAuthCode()
                val expireTime = calculateExpireTime(durationType)
                
                val authCode = AuthCode(
                    code = code,
                    durationType = durationType,
                    durationDays = getDurationDays(durationType),
                    expireTime = expireTime,
                    notes = notes,
                    creator = "admin"
                )
                
                database.authCodeDao().insert(authCode)
                loadCodes()
                
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(this@AuthCodeManageActivity)
                        .setTitle("✅ 生成成功")
                        .setMessage("授权码：$code\n有效期：${getDurationText(durationType)}")
                        .setPositiveButton("确定", null)
                        .show()
                }
            } catch (e: Exception) {
                Logger.e("AuthCodeManageActivity", "生成授权码失败", e)
            }
        }
    }

    private fun generateBatchCodes(count: Int, durationType: Int, notes: String) {
        mainScope.launch {
            try {
                val codes = mutableListOf<AuthCode>()
                val expireTime = calculateExpireTime(durationType)
                
                repeat(count) {
                    codes.add(AuthCode(
                        code = generateAuthCode(),
                        durationType = durationType,
                        durationDays = getDurationDays(durationType),
                        expireTime = expireTime,
                        notes = notes,
                        creator = "admin"
                    ))
                }
                
                database.authCodeDao().insertAll(codes)
                loadCodes()
                
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(this@AuthCodeManageActivity)
                        .setTitle("✅ 批量生成成功")
                        .setMessage("已生成 $count 个授权码")
                        .setPositiveButton("确定", null)
                        .show()
                }
            } catch (e: Exception) {
                Logger.e("AuthCodeManageActivity", "批量生成失败", e)
            }
        }
    }

    private fun generateAuthCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..12)
            .map { chars.random() }
            .joinToString("")
            .chunked(4)
            .joinToString("-")
    }

    private fun calculateExpireTime(durationType: Int): Long? {
        if (durationType == AuthCode.DURATION_PERMANENT) return null
        
        val days = getDurationDays(durationType)
        return System.currentTimeMillis() + (days * 24 * 3600 * 1000L)
    }

    private fun getDurationDays(durationType: Int): Int {
        return when (durationType) {
            AuthCode.DURATION_1_DAY -> 1
            AuthCode.DURATION_7_DAYS -> 7
            AuthCode.DURATION_30_DAYS -> 30
            else -> 0
        }
    }

    private fun getDurationText(durationType: Int): String {
        return when (durationType) {
            AuthCode.DURATION_1_DAY -> "1 天"
            AuthCode.DURATION_7_DAYS -> "7 天"
            AuthCode.DURATION_30_DAYS -> "30 天"
            AuthCode.DURATION_PERMANENT -> "永久"
            else -> "未知"
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
     * 授权码列表适配器
     */
    inner class AuthCodeAdapter : RecyclerView.Adapter<AuthCodeAdapter.CodeViewHolder>() {
        
        private var codes: List<AuthCode> = emptyList()
        
        fun submitList(newCodes: List<AuthCode>) {
            codes = newCodes
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodeViewHolder {
            val binding = ItemAuthCodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CodeViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: CodeViewHolder, position: Int) {
            holder.bind(codes[position])
        }
        
        override fun getItemCount(): Int = codes.size
        
        inner class CodeViewHolder(private val binding: ItemAuthCodeBinding) : RecyclerView.ViewHolder(binding.root) {
            
            fun bind(code: AuthCode) {
                binding.tvCode.text = code.code
                binding.tvDuration.text = getDurationText(code.durationType)
                binding.tvNotes.text = code.notes.ifEmpty { "无备注" }
                
                // 状态
                when {
                    code.isBlocked -> {
                        binding.tvStatus.text = "🚫 已封禁"
                        binding.tvStatus.setTextColor(getColor(R.color.status_blocked))
                    }
                    code.isExpired() -> {
                        binding.tvStatus.text = "⏰ 已过期"
                        binding.tvStatus.setTextColor(getColor(R.color.status_offline))
                    }
                    code.isActive -> {
                        binding.tvStatus.text = "✅ 已激活"
                        binding.tvStatus.setTextColor(getColor(R.color.status_online))
                    }
                    else -> {
                        binding.tvStatus.text = "⭕ 未使用"
                        binding.tvStatus.setTextColor(getColor(R.color.status_offline))
                    }
                }
                
                // 设备信息
                binding.tvDevice.text = code.deviceId?.let { "设备：$it" } ?: "未绑定设备"
                
                // 过期时间
                binding.tvExpireTime.text = if (code.expireTime == null) {
                    "永久有效"
                } else {
                    "过期：${dateFormat.format(Date(code.expireTime))}"
                }
                
                // 操作按钮
                binding.btnBlock.text = if (code.isBlocked) "解封" else "封禁"
                binding.btnBlock.setOnClickListener {
                    toggleBlockCode(code)
                }
                
                binding.btnDelete.setOnClickListener {
                    deleteCode(code)
                }
            }
            
            private fun toggleBlockCode(code: AuthCode) {
                mainScope.launch {
                    try {
                        database.authCodeDao().blockCode(code.code, !code.isBlocked)
                        loadCodes()
                    } catch (e: Exception) {
                        Logger.e("AuthCodeAdapter", "封禁操作失败", e)
                    }
                }
            }
            
            private fun deleteCode(code: AuthCode) {
                AlertDialog.Builder(binding.root.context)
                    .setTitle("删除授权码")
                    .setMessage("确定要删除 ${code.code} 吗？")
                    .setPositiveButton("删除") { _, _ ->
                        mainScope.launch {
                            try {
                                database.authCodeDao().deleteCode(code.code)
                                loadCodes()
                            } catch (e: Exception) {
                                Logger.e("AuthCodeAdapter", "删除失败", e)
                            }
                        }
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }
    }
}

package com.reader.automation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.reader.automation.R
import com.reader.automation.database.AppDatabase
import com.reader.automation.databinding.ActivityBookManageBinding
import com.reader.automation.databinding.ItemBookBinding
import com.reader.automation.managers.AppSwitchManager
import com.reader.automation.models.AppConfig
import com.reader.automation.models.BookInfo
import com.reader.automation.utils.Logger
import kotlinx.coroutines.*

/**
 * 书籍管理界面
 * 功能：添加/编辑书籍、按作者筛选、排序、勾选运行
 */
class BookManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookManageBinding
    private lateinit var database: AppDatabase
    private lateinit var appSwitchManager: AppSwitchManager
    private val mainScope = CoroutineScope(Dispatchers.Main + Job())
    
    private lateinit var bookAdapter: BookAdapter
    private var currentAppPackage: String = ""
    private var allBooks: List<BookInfo> = emptyList()
    private var filteredBooks: List<BookInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookManageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = AppDatabase.getInstance(applicationContext)
        appSwitchManager = AppSwitchManager.getInstance(this)
        
        setupViews()
        loadApps()
    }

    private fun setupViews() {
        supportActionBar?.title = "书籍管理"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // APP 选择器
        binding.spinnerApps.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val app = appSwitchManager.getConfiguredApps()[position - 1]
                    currentAppPackage = app.packageName
                    loadBooks()
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        // 书籍列表
        bookAdapter = BookAdapter()
        binding.recyclerBooks.layoutManager = LinearLayoutManager(this)
        binding.recyclerBooks.adapter = bookAdapter
        
        // 添加书籍按钮
        binding.fabAddBook.setOnClickListener {
            showAddBookDialog()
        }
        
        // 作者筛选
        binding.btnFilterAuthor.setOnClickListener {
            showAuthorFilterDialog()
        }
        
        // 清空完结进度
        binding.btnResetCompleted.setOnClickListener {
            showResetCompletedDialog()
        }
    }

    private fun loadApps() {
        val apps = appSwitchManager.getConfiguredApps()
        val appNames = mutableListOf("选择小说 APP")
        appNames.addAll(apps.map { it.appName })
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, appNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBooks.adapter = adapter
    }

    private fun loadBooks() {
        if (currentAppPackage.isEmpty()) return
        
        mainScope.launch {
            try {
                allBooks = database.bookDao().getBooksByApp(currentAppPackage)
                filteredBooks = allBooks
                bookAdapter.submitList(filteredBooks)
                
                withContext(Dispatchers.Main) {
                    binding.tvBookCount.text = "共 ${filteredBooks.size} 本书"
                }
            } catch (e: Exception) {
                Logger.e("BookManageActivity", "加载书籍失败", e)
            }
        }
    }

    private fun showAddBookDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_book, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etBookTitle)
        val etAuthor = dialogView.findViewById<EditText>(R.id.etAuthor)
        val etAuthorUrl = dialogView.findViewById<EditText>(R.id.etAuthorUrl)
        
        AlertDialog.Builder(this)
            .setTitle("添加书籍")
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val title = etTitle.text.toString().trim()
                val author = etAuthor.text.toString().trim()
                val authorUrl = etAuthorUrl.text.toString().trim()
                
                if (title.isNotEmpty() && author.isNotEmpty() && currentAppPackage.isNotEmpty()) {
                    addBook(title, author, authorUrl)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun addBook(title: String, author: String, authorUrl: String) {
        mainScope.launch {
            try {
                val book = BookInfo(
                    packageName = currentAppPackage,
                    bookTitle = title,
                    author = author,
                    authorPageUrl = authorUrl.ifEmpty { null }
                )
                database.bookDao().insert(book)
                loadBooks()
                Logger.d("BookManageActivity", "添加书籍成功：$title")
            } catch (e: Exception) {
                Logger.e("BookManageActivity", "添加书籍失败", e)
            }
        }
    }

    private fun showAuthorFilterDialog() {
        val authors = allBooks.map { it.author }.distinct().sorted()
        
        AlertDialog.Builder(this)
            .setTitle("按作者筛选")
            .setItems(authors.toTypedArray()) { _, which ->
                val selectedAuthor = authors[which]
                filteredBooks = allBooks.filter { it.author == selectedAuthor }
                bookAdapter.submitList(filteredBooks)
                binding.tvBookCount.text = "筛选结果：${filteredBooks.size} 本"
            }
            .setNeutralButton("显示全部") { _, _ ->
                filteredBooks = allBooks
                bookAdapter.submitList(filteredBooks)
                binding.tvBookCount.text = "共 ${allBooks.size} 本"
            }
            .show()
    }

    private fun showResetCompletedDialog() {
        AlertDialog.Builder(this)
            .setTitle("清空完结进度")
            .setMessage("确定要清空所有已完结书籍的进度吗？这将重置所有已完结书籍的阅读状态。")
            .setPositiveButton("确定") { _, _ ->
                mainScope.launch {
                    try {
                        database.bookDao().resetCompletedByApp(currentAppPackage)
                        loadBooks()
                        Logger.d("BookManageActivity", "已清空完结进度")
                    } catch (e: Exception) {
                        Logger.e("BookManageActivity", "清空完结进度失败", e)
                    }
                }
            }
            .setNegativeButton("取消", null)
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
    
    /**
     * 书籍列表适配器
     */
    inner class BookAdapter : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {
        
        private var books: List<BookInfo> = emptyList()
        
        fun submitList(newBooks: List<BookInfo>) {
            books = newBooks
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
            val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return BookViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
            holder.bind(books[position])
        }
        
        override fun getItemCount(): Int = books.size
        
        inner class BookViewHolder(private val binding: ItemBookBinding) : RecyclerView.ViewHolder(binding.root) {
            
            fun bind(book: BookInfo) {
                binding.tvBookTitle.text = book.bookTitle
                binding.tvBookAuthor.text = "作者：${book.author}"
                binding.tvBookProgress.text = "进度：${(book.readProgress * 100).toInt()}%"
                binding.checkboxEnabled.isChecked = book.isEnabled
                
                // 完结标记
                if (book.isCompleted) {
                    binding.tvCompleted.visibility = View.VISIBLE
                    binding.root.alpha = 0.6f
                } else {
                    binding.tvCompleted.visibility = View.GONE
                    binding.root.alpha = 1.0f
                }
                
                // 启用/禁用
                binding.checkboxEnabled.setOnCheckedChangeListener { _, isChecked ->
                    mainScope.launch {
                        try {
                            val updatedBook = book.copy(isEnabled = isChecked)
                            database.bookDao().update(updatedBook)
                        } catch (e: Exception) {
                            Logger.e("BookAdapter", "更新书籍状态失败", e)
                        }
                    }
                }
                
                // 长按删除
                binding.root.setOnLongClickListener {
                    AlertDialog.Builder(binding.root.context)
                        .setTitle("删除书籍")
                        .setMessage("确定要删除《${book.bookTitle}》吗？")
                        .setPositiveButton("删除") { _, _ ->
                            mainScope.launch {
                                try {
                                    database.bookDao().delete(book)
                                    loadBooks()
                                } catch (e: Exception) {
                                    Logger.e("BookAdapter", "删除书籍失败", e)
                                }
                            }
                        }
                        .setNegativeButton("取消", null)
                        .show()
                    true
                }
            }
        }
    }
}

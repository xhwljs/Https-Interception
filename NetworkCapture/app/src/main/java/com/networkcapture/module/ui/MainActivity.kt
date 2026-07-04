package com.networkcapture.module.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.networkcapture.module.R
import com.networkcapture.module.databinding.ActivityMainBinding
import com.networkcapture.module.ui.adapters.NetworkRequestAdapter
import com.networkcapture.module.ui.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * 主界面 Activity
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: NetworkRequestAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeData()
    }

    /**
     * 设置 RecyclerView
     */
    private fun setupRecyclerView() {
        adapter = NetworkRequestAdapter { request ->
            val intent = Intent(this, DetailActivity::class.java)
            val gson = com.google.gson.Gson()
            intent.putExtra("request_json", gson.toJson(request))
            startActivity(intent)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            setHasFixedSize(true)
        }
    }

    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 抓包开关
        binding.captureSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setCaptureEnabled(isChecked)
            binding.statusTextView.text = if (isChecked) {
                getString(R.string.capture_enabled)
            } else {
                getString(R.string.capture_disabled)
            }
            binding.statusTextView.setTextColor(
                if (isChecked) {
                    getColor(R.color.accent)
                } else {
                    getColor(R.color.text_muted)
                }
            )
        }

        // 搜索框
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchKeyword(s?.toString()?.trim())
            }
        })

        // 过滤器
        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val method = when {
                checkedIds.contains(R.id.chipAll) -> null
                checkedIds.contains(R.id.chipGet) -> "GET"
                checkedIds.contains(R.id.chipPost) -> "POST"
                checkedIds.contains(R.id.chipPut) -> "PUT"
                checkedIds.contains(R.id.chipDelete) -> "DELETE"
                else -> null
            }
            viewModel.setMethodFilter(method)
        }

        // 刷新
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadRequests()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        // 导出
        binding.exportButton.setOnClickListener {
            exportLogs()
        }

        // 清空
        binding.clearButton.setOnClickListener {
            showClearConfirmDialog()
        }
    }

    /**
     * 观察数据
     */
    private fun observeData() {
        // 请求列表
        viewModel.requests.observe(this) { requests ->
            adapter.submitList(requests)
            binding.emptyStateLayout.visibility = if (requests.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
            binding.totalCountTextView.text = "总计: ${requests.size}"
            binding.todayCountTextView.text = "今日: ${requests.size}"
        }
    }

    /**
     * 导出日志
     */
    private fun exportLogs() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val logs = viewModel.exportRequests()
                if (logs.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "暂无日志可导出", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                // 保存到文件
                val fileName = "network_capture_${System.currentTimeMillis()}.txt"
                val file = File(getExternalFilesDir(null), fileName)
                FileOutputStream(file).use { output ->
                    output.write(logs.toByteArray())
                }

                // 复制到剪贴板
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("网络请求日志", logs)
                clipboard.setPrimaryClip(clip)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "已导出到 $fileName 并复制到剪贴板", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 显示清空确认对话框
     */
    private fun showClearConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("清空日志")
            .setMessage(getString(R.string.clear_confirm))
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                viewModel.clearAllRequests()
                Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}
package com.networkcapture.module.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.GsonBuilder
import com.networkcapture.module.databinding.ActivityDetailBinding
import com.networkcapture.module.data.model.NetworkRequest
import com.networkcapture.module.data.repository.NetworkRequestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 详情页面 Activity
 */
class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private var request: NetworkRequest? = null
    private val gson = GsonBuilder().setPrettyPrinting().create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取请求 ID
        val requestId = intent.getLongExtra("request_id", -1L)
        if (requestId == -1L) {
            Toast.makeText(this, "无效的请求 ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 加载请求详情
        loadRequestDetail(requestId)
    }

    /**
     * 加载请求详情
     */
    private fun loadRequestDetail(requestId: Long) {
        lifecycleScope.launch {
            try {
                val repository = NetworkRequestRepository.getRepository(this@DetailActivity)
                request = withContext(Dispatchers.IO) {
                    repository.getById(requestId)
                }

                if (request == null) {
                    Toast.makeText(this@DetailActivity, "请求不存在", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                displayRequest(request!!)
            } catch (e: Exception) {
                Toast.makeText(this@DetailActivity, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * 显示请求详情
     */
    private fun displayRequest(request: NetworkRequest) {
        binding.apply {
            // 设置方法标签
            methodTextView.text = request.method
            methodTextView.setTextColor(getMethodColor(request.method))

            // 设置状态码
            statusCodeTextView.text = request.responseCode.toString()
            statusCodeTextView.setTextColor(getStatusColor(request.responseCode))

            // 设置状态文本
            statusTextView.text = request.getStatusText()

            // 设置 URL
            urlTextView.text = request.url

            // 设置时间
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            timeTextView.text = dateFormat.format(Date(request.timestamp))

            // 设置耗时
            durationTextView.text = request.getFormattedDuration()

            // 设置请求头
            requestHeadersTextView.text = formatJson(request.requestHeaders ?: getString(R.string.empty_body))

            // 设置请求体
            requestBodyTextView.text = formatJson(request.requestBody ?: getString(R.string.empty_body))

            // 设置响应头
            responseHeadersTextView.text = formatJson(request.responseHeaders ?: getString(R.string.empty_body))

            // 设置响应体
            responseBodyTextView.text = formatJson(request.responseBody ?: getString(R.string.empty_body))

            // 显示错误信息（如果有）
            if (!request.isSuccess && request.errorMessage != null) {
                errorCard.visibility = View.VISIBLE
                errorMessageTextView.text = request.errorMessage
            }

            // 设置复制按钮
            copyRequestHeadersButton.setOnClickListener {
                copyToClipboard(request.requestHeaders ?: "")
            }

            copyRequestBodyButton.setOnClickListener {
                copyToClipboard(request.requestBody ?: "")
            }

            copyResponseHeadersButton.setOnClickListener {
                copyToClipboard(request.responseHeaders ?: "")
            }

            copyResponseBodyButton.setOnClickListener {
                copyToClipboard(request.responseBody ?: "")
            }

            // 复制全部
            copyAllButton.setOnClickListener {
                copyAllRequest()
            }
        }
    }

    /**
     * 格式化 JSON
     */
    private fun formatJson(json: String): String {
        return try {
            if (json.isEmpty() || json == getString(R.string.empty_body)) {
                json
            } else {
                gson.toJson(gson.fromJson(json, Any::class.java))
            }
        } catch (e: Exception) {
            json
        }
    }

    /**
     * 复制到剪贴板
     */
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("network_request", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    /**
     * 复制全部请求信息
     */
    private fun copyAllRequest() {
        request?.let { req ->
            val builder = StringBuilder()
            builder.append("====================================\n")
            builder.append("请求详情\n")
            builder.append("====================================\n\n")
            builder.append("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(req.timestamp))}\n")
            builder.append("方法: ${req.method}\n")
            builder.append("URL: ${req.url}\n")
            builder.append("状态码: ${req.responseCode}\n")
            builder.append("耗时: ${req.getFormattedDuration()}\n\n")

            builder.append("请求头:\n")
            builder.append(formatJson(req.requestHeaders ?: "(空)") + "\n\n")

            builder.append("请求体:\n")
            builder.append(formatJson(req.requestBody ?: "(空)") + "\n\n")

            builder.append("响应头:\n")
            builder.append(formatJson(req.responseHeaders ?: "(空)") + "\n\n")

            builder.append("响应体:\n")
            builder.append(formatJson(req.responseBody ?: "(空)") + "\n\n")

            if (!req.isSuccess && req.errorMessage != null) {
                builder.append("错误信息:\n")
                builder.append(req.errorMessage + "\n\n")
            }

            builder.append("====================================\n")

            copyToClipboard(builder.toString())
        }
    }

    /**
     * 根据请求方法获取颜色
     */
    private fun getMethodColor(method: String): Int {
        return when (method.uppercase()) {
            "GET" -> Color.parseColor("#22C55E")
            "POST" -> Color.parseColor("#3B82F6")
            "PUT" -> Color.parseColor("#F59E0B")
            "DELETE" -> Color.parseColor("#EF4444")
            "PATCH" -> Color.parseColor("#8B5CF6")
            else -> Color.parseColor("#94A3B8")
        }
    }

    /**
     * 根据状态码获取颜色
     */
    private fun getStatusColor(code: Int): Int {
        return when (code) {
            in 200..299 -> Color.parseColor("#22C55E")
            in 300..399 -> Color.parseColor("#3B82F6")
            in 400..499 -> Color.parseColor("#F59E0B")
            in 500..599 -> Color.parseColor("#EF4444")
            else -> Color.parseColor("#94A3B8")
        }
    }
}
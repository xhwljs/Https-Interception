package com.networkcapture.module.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.networkcapture.module.data.model.NetworkRequest
import com.networkcapture.module.hook.CaptureManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 主界面 ViewModel
 * 从文件读取抓包数据（跨进程通信）
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val targetPackage = "com.feiyu.stepbystepapp"

    // 网络请求列表
    private val _requests = MutableLiveData<List<NetworkRequest>>()
    val requests: LiveData<List<NetworkRequest>> = _requests

    // 抓包开关状态
    private val _isCaptureEnabled = MutableLiveData(true)
    val isCaptureEnabled: LiveData<Boolean> = _isCaptureEnabled

    // 当前过滤的方法
    private val _currentMethod = MutableLiveData<String?>()
    val currentMethod: LiveData<String?> = _currentMethod

    // 当前搜索关键词
    private val _searchKeyword = MutableLiveData<String?>()
    val searchKeyword: LiveData<String?> = _searchKeyword

    // 加载状态
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // 错误信息
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadRequests()
    }

    /**
     * 从文件加载网络请求
     */
    fun loadRequests() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    val files = CaptureManager.getCaptureFiles(targetPackage)
                    val requests = mutableListOf<NetworkRequest>()

                    for (file in files) {
                        val request = CaptureManager.readCaptureFile(file)
                        if (request != null) {
                            requests.add(request)
                        }
                    }

                    // 应用过滤
                    val method = _currentMethod.value
                    val keyword = _searchKeyword.value

                    var filtered = requests.toList()

                    if (method != null) {
                        filtered = filtered.filter { it.method == method }
                    }

                    if (keyword != null) {
                        filtered = filtered.filter {
                            it.url.contains(keyword, ignoreCase = true) ||
                            it.requestBody?.contains(keyword, ignoreCase = true) == true ||
                            it.responseBody?.contains(keyword, ignoreCase = true) == true
                        }
                    }

                    filtered
                }

                _requests.value = result
            } catch (e: Exception) {
                _errorMessage.value = "加载失败: ${e.message}"
                _requests.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setCaptureEnabled(enabled: Boolean) {
        _isCaptureEnabled.value = enabled
    }

    fun setMethodFilter(method: String?) {
        _currentMethod.value = method
        loadRequests()
    }

    fun setSearchKeyword(keyword: String?) {
        _searchKeyword.value = keyword
        loadRequests()
    }

    /**
     * 清空所有抓包文件
     */
    fun clearAllRequests() {
        viewModelScope.launch(Dispatchers.IO) {
            CaptureManager.clearAllCaptures(targetPackage)
            withContext(Dispatchers.Main) {
                loadRequests()
            }
        }
    }

    /**
     * 导出请求日志
     */
    fun exportRequests(): String {
        val requests = _requests.value ?: emptyList()
        if (requests.isEmpty()) {
            return ""
        }

        val builder = StringBuilder()
        builder.append("网络请求日志导出\n")
        builder.append("导出时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
        builder.append("总数: ${requests.size}\n\n")

        requests.forEach { request ->
            builder.append("====================================\n")
            builder.append("时间: ${request.getFormattedDate()} ${request.getFormattedTime()}\n")
            builder.append("方法: ${request.method}\n")
            builder.append("URL: ${request.url}\n")
            builder.append("状态码: ${request.responseCode}\n")
            builder.append("耗时: ${request.getFormattedDuration()}\n\n")
            builder.append("请求头:\n${request.requestHeaders ?: "(空)"}\n\n")
            builder.append("请求体:\n${request.requestBody ?: "(空)"}\n\n")
            builder.append("响应头:\n${request.responseHeaders ?: "(空)"}\n\n")
            builder.append("响应体:\n${request.responseBody ?: "(空)"}\n")
            builder.append("====================================\n\n")
        }

        return builder.toString()
    }
}

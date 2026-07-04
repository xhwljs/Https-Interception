package com.networkcapture.module.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.networkcapture.module.data.model.NetworkRequest
import com.networkcapture.module.data.repository.NetworkRequestRepository
import com.networkcapture.module.hook.CaptureManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 主界面 ViewModel
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NetworkRequestRepository.getRepository(application)

    // 网络请求列表
    private val _requests = MutableLiveData<List<NetworkRequest>>()
    val requests: LiveData<List<NetworkRequest>> = _requests

    // 今天的请求数量
    val todayCount: LiveData<Int> = repository.getTodayCount().asLiveData()

    // 总请求数量
    val totalCount: LiveData<Int> = repository.getTotalCount().asLiveData()

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

    init {
        // 初始化 CaptureManager
        CaptureManager.init(application)

        // 加载所有请求
        loadRequests()
    }

    /**
     * 加载网络请求
     */
    fun loadRequests() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val method = _currentMethod.value
                val keyword = _searchKeyword.value

                val result = withContext(Dispatchers.IO) {
                    when {
                        keyword != null && method != null -> {
                            repository.searchByMethod(method, keyword)
                        }
                        keyword != null -> {
                            repository.search(keyword)
                        }
                        method != null -> {
                            repository.getByMethod(method)
                        }
                        else -> {
                            repository.getAll()
                        }
                    }.asLiveData().value ?: emptyList()
                }

                _requests.value = result
            } catch (e: Exception) {
                _requests.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 设置抓包开关
     */
    fun setCaptureEnabled(enabled: Boolean) {
        _isCaptureEnabled.value = enabled
    }

    /**
     * 设置过滤方法
     */
    fun setMethodFilter(method: String?) {
        _currentMethod.value = method
        loadRequests()
    }

    /**
     * 设置搜索关键词
     */
    fun setSearchKeyword(keyword: String?) {
        _searchKeyword.value = keyword
        loadRequests()
    }

    /**
     * 清空所有请求
     */
    fun clearAllRequests() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAll()
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
        builder.append("总数: ${requests.size}\n")
        builder.append("\n")

        requests.forEach { request ->
            builder.append("====================================\n")
            builder.append("时间: ${request.getFormattedDate()} ${request.getFormattedTime()}\n")
            builder.append("方法: ${request.method}\n")
            builder.append("URL: ${request.url}\n")
            builder.append("状态码: ${request.responseCode}\n")
            builder.append("耗时: ${request.getFormattedDuration()}\n")
            builder.append("\n")
            builder.append("请求头:\n${request.requestHeaders ?: "(空)"}\n")
            builder.append("\n")
            builder.append("请求体:\n${request.requestBody ?: "(空)"}\n")
            builder.append("\n")
            builder.append("响应头:\n${request.responseHeaders ?: "(空)"}\n")
            builder.append("\n")
            builder.append("响应体:\n${request.responseBody ?: "(空)"}\n")
            builder.append("====================================\n\n")
        }

        return builder.toString()
    }
}
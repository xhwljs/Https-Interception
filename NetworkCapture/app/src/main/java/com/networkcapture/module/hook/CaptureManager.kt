package com.networkcapture.module.hook

import android.content.Context
import android.content.Intent
import com.networkcapture.module.data.model.NetworkRequest
import com.networkcapture.module.data.repository.NetworkRequestRepository
import de.robv.android.xposed.XposedBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 抓包管理器
 * 用于 Hook 进程和主应用之间的通信
 */
object CaptureManager {

    private const val ACTION_NEW_REQUEST = "com.networkcapture.module.NEW_REQUEST"
    private const val EXTRA_REQUEST_URL = "request_url"
    private const val EXTRA_REQUEST_METHOD = "request_method"
    private const val EXTRA_REQUEST_HEADERS = "request_headers"
    private const val EXTRA_REQUEST_BODY = "request_body"
    private const val EXTRA_RESPONSE_CODE = "response_code"
    private const val EXTRA_RESPONSE_HEADERS = "response_headers"
    private const val EXTRA_RESPONSE_BODY = "response_body"
    private const val EXTRA_TIMESTAMP = "timestamp"
    private const val EXTRA_DURATION = "duration"
    private const val EXTRA_IS_SUCCESS = "is_success"
    private const val EXTRA_ERROR_MESSAGE = "error_message"

    private var repository: NetworkRequestRepository? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * 初始化（在主应用中调用）
     */
    fun init(context: Context) {
        repository = NetworkRequestRepository.getRepository(context)
    }

    /**
     * 保存网络请求（从 Hook 进程调用）
     */
    fun saveRequest(request: NetworkRequest) {
        try {
            // 直接保存到数据库（如果是同一进程）
            repository?.let { repo ->
                scope.launch(Dispatchers.IO) {
                    try {
                        repo.insert(request)
                        XposedBridge.log("NetworkCapture: 请求已保存 - ${request.method} ${request.url}")
                    } catch (e: Exception) {
                        XposedBridge.log("NetworkCapture: 保存请求失败 - ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            XposedBridge.log("NetworkCapture: saveRequest 失败 - ${e.message}")
        }
    }

    /**
     * 从 Intent 解析网络请求
     */
    fun parseRequestFromIntent(intent: Intent): NetworkRequest? {
        try {
            return NetworkRequest(
                url = intent.getStringExtra(EXTRA_REQUEST_URL) ?: return null,
                method = intent.getStringExtra(EXTRA_REQUEST_METHOD) ?: "GET",
                requestHeaders = intent.getStringExtra(EXTRA_REQUEST_HEADERS),
                requestBody = intent.getStringExtra(EXTRA_REQUEST_BODY),
                responseCode = intent.getIntExtra(EXTRA_RESPONSE_CODE, 0),
                responseHeaders = intent.getStringExtra(EXTRA_RESPONSE_HEADERS),
                responseBody = intent.getStringExtra(EXTRA_RESPONSE_BODY),
                timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis()),
                duration = intent.getLongExtra(EXTRA_DURATION, 0),
                isSuccess = intent.getBooleanExtra(EXTRA_IS_SUCCESS, true),
                errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE)
            )
        } catch (e: Exception) {
            XposedBridge.log("NetworkCapture: 解析 Intent 失败 - ${e.message}")
            return null
        }
    }

    /**
     * 创建网络请求 Intent
     */
    fun createRequestIntent(request: NetworkRequest): Intent {
        return Intent(ACTION_NEW_REQUEST).apply {
            putExtra(EXTRA_REQUEST_URL, request.url)
            putExtra(EXTRA_REQUEST_METHOD, request.method)
            putExtra(EXTRA_REQUEST_HEADERS, request.requestHeaders)
            putExtra(EXTRA_REQUEST_BODY, request.requestBody)
            putExtra(EXTRA_RESPONSE_CODE, request.responseCode)
            putExtra(EXTRA_RESPONSE_HEADERS, request.responseHeaders)
            putExtra(EXTRA_RESPONSE_BODY, request.responseBody)
            putExtra(EXTRA_TIMESTAMP, request.timestamp)
            putExtra(EXTRA_DURATION, request.duration)
            putExtra(EXTRA_IS_SUCCESS, request.isSuccess)
            putExtra(EXTRA_ERROR_MESSAGE, request.errorMessage)
        }
    }
}
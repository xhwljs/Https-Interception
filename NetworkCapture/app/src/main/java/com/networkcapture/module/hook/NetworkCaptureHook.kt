package com.networkcapture.module.hook

import android.app.Application
import com.google.gson.Gson
import com.networkcapture.module.data.model.NetworkRequest
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * LSPosed 网络请求 Hook
 * 拦截 OkHttp 和 HttpURLConnection 的网络请求
 */
class NetworkCaptureHook : IXposedHookLoadPackage {

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val targetPackage = "com.feiyu.stepbystepapp"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 只 Hook 目标应用
        if (lpparam.packageName != targetPackage) {
            return
        }

        XposedBridge.log("NetworkCapture: 开始 Hook ${lpparam.packageName}")

        try {
            // Hook OkHttp
            hookOkHttp(lpparam)

            // Hook HttpURLConnection
            hookHttpURLConnection(lpparam)

            XposedBridge.log("NetworkCapture: Hook 成功")
        } catch (e: Throwable) {
            XposedBridge.log("NetworkCapture: Hook 失败 - ${e.message}")
            XposedBridge.log(e)
        }
    }

    /**
     * Hook OkHttp 请求
     */
    private fun hookOkHttp(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val okHttpClientBuilderClass = XposedHelpers.findClass(
                "okhttp3.OkHttpClient.Builder",
                lpparam.classLoader
            )

            // Hook build() 方法，添加拦截器
            XposedHelpers.findAndHookMethod(
                okHttpClientBuilderClass,
                "build",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val builder = param.thisObject
                        try {
                            // 添加网络拦截器
                            val interceptor = NetworkInterceptor()
                            XposedHelpers.callMethod(builder, "addNetworkInterceptor", interceptor)
                        } catch (e: Exception) {
                            XposedBridge.log("NetworkCapture: 添加拦截器失败 - ${e.message}")
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("NetworkCapture: Hook OkHttp 失败 - ${e.message}")
        }
    }

    /**
     * Hook HttpURLConnection 请求
     */
    private fun hookHttpURLConnection(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val httpURLConnectionClass = XposedHelpers.findClass(
                "java.net.HttpURLConnection",
                lpparam.classLoader
            )

            // Hook getInputStream() 方法，记录响应
            XposedHelpers.findAndHookMethod(
                httpURLConnectionClass,
                "getInputStream",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val connection = param.thisObject as HttpURLConnection
                        try {
                            captureHttpURLConnectionRequest(connection, null)
                        } catch (e: Exception) {
                            XposedBridge.log("NetworkCapture: 记录 HttpURLConnection 请求失败 - ${e.message}")
                        }
                    }
                }
            )

            // Hook getOutputStream() 方法，记录请求体
            XposedHelpers.findAndHookMethod(
                httpURLConnectionClass,
                "getOutputStream",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val connection = param.thisObject as HttpURLConnection
                        // 这里可以捕获请求体，但需要更复杂的处理
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("NetworkCapture: Hook HttpURLConnection 失败 - ${e.message}")
        }
    }

    /**
     * 捕获 HttpURLConnection 请求
     */
    private fun captureHttpURLConnectionRequest(connection: HttpURLConnection, requestBody: String?) {
        try {
            val startTime = System.currentTimeMillis()

            // 收集请求信息
            val url = connection.url.toString()
            val method = connection.requestMethod
            val requestHeaders = collectRequestHeaders(connection)

            // 收集响应信息
            val responseCode = connection.responseCode
            val responseHeaders = collectResponseHeaders(connection)
            val responseBody = try {
                connection.inputStream?.bufferedReader()?.readText() ?: ""
            } catch (e: Exception) {
                connection.errorStream?.bufferedReader()?.readText() ?: ""
            }

            val duration = System.currentTimeMillis() - startTime

            // 保存请求记录
            saveRequest(
                url = url,
                method = method,
                requestHeaders = requestHeaders,
                requestBody = requestBody,
                responseCode = responseCode,
                responseHeaders = responseHeaders,
                responseBody = responseBody,
                duration = duration,
                isSuccess = responseCode in 200..399,
                errorMessage = null
            )
        } catch (e: Exception) {
            XposedBridge.log("NetworkCapture: 捕获 HttpURLConnection 请求失败 - ${e.message}")
        }
    }

    /**
     * 收集请求头
     */
    private fun collectRequestHeaders(connection: HttpURLConnection): String {
        val headers = mutableMapOf<String, String>()
        connection.requestProperties?.forEach { (key, values) ->
            headers[key] = values.joinToString(", ")
        }
        return gson.toJson(headers)
    }

    /**
     * 收集响应头
     */
    private fun collectResponseHeaders(connection: HttpURLConnection): String {
        val headers = mutableMapOf<String, String>()
        connection.headerFields?.forEach { (key, values) ->
            if (key != null) {
                headers[key] = values.joinToString(", ")
            }
        }
        return gson.toJson(headers)
    }

    /**
     * 保存请求到数据库
     */
    private fun saveRequest(
        url: String,
        method: String,
        requestHeaders: String?,
        requestBody: String?,
        responseCode: Int,
        responseHeaders: String?,
        responseBody: String?,
        duration: Long,
        isSuccess: Boolean,
        errorMessage: String?
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val request = NetworkRequest(
                    url = url,
                    method = method,
                    requestHeaders = requestHeaders,
                    requestBody = requestBody,
                    responseCode = responseCode,
                    responseHeaders = responseHeaders,
                    responseBody = responseBody,
                    timestamp = System.currentTimeMillis(),
                    duration = duration,
                    isSuccess = isSuccess,
                    errorMessage = errorMessage
                )

                // 通知主应用保存数据
                CaptureManager.saveRequest(request)
            } catch (e: Exception) {
                XposedBridge.log("NetworkCapture: 保存请求失败 - ${e.message}")
            }
        }
    }

    /**
     * OkHttp 网络拦截器
     */
    inner class NetworkInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val startTime = System.nanoTime()

            var response: Response? = null
            var errorMessage: String? = null

            try {
                response = chain.proceed(request)

                // 收集请求信息
                val requestHeaders = collectHeaders(request.headers)
                val requestBody = try {
                    request.body?.let { body ->
                        val buffer = okio.Buffer()
                        body.writeTo(buffer)
                        buffer.readUtf8()
                    }
                } catch (e: Exception) {
                    null
                }

                // 收集响应信息
                val responseHeaders = collectHeaders(response.headers)
                val responseBody = try {
                    response.peekBody(Long.MAX_VALUE).string()
                } catch (e: Exception) {
                    null
                }

                val duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)

                // 保存请求记录
                saveRequest(
                    url = request.url.toString(),
                    method = request.method,
                    requestHeaders = requestHeaders,
                    requestBody = requestBody,
                    responseCode = response.code,
                    responseHeaders = responseHeaders,
                    responseBody = responseBody,
                    duration = duration,
                    isSuccess = response.isSuccessful,
                    errorMessage = null
                )

                return response
            } catch (e: IOException) {
                errorMessage = e.message
                val duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)

                // 保存失败的请求
                saveRequest(
                    url = request.url.toString(),
                    method = request.method,
                    requestHeaders = collectHeaders(request.headers),
                    requestBody = null,
                    responseCode = 0,
                    responseHeaders = null,
                    responseBody = null,
                    duration = duration,
                    isSuccess = false,
                    errorMessage = errorMessage
                )

                throw e
            }
        }

        private fun collectHeaders(headers: Headers): String {
            val map = mutableMapOf<String, String>()
            for (i in 0 until headers.size) {
                map[headers.name(i)] = headers.value(i)
            }
            return gson.toJson(map)
        }
    }
}
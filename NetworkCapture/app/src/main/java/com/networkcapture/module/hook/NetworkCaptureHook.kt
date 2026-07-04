package com.networkcapture.module.hook

import android.app.Application
import android.util.Log
import com.google.gson.Gson
import com.networkcapture.module.data.model.NetworkRequest
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocket

/**
 * LSPosed 网络请求 Hook
 * 使用SSL/TLS层Hook，能抓到所有Java层网络请求
 * 包括OkHttp、HttpURLConnection、WebSocket、原生Socket等
 */
class NetworkCaptureHook : IXposedHookLoadPackage {

    private val gson = Gson()
    private val targetPackage = "com.feiyu.stepbystepapp"
    private val TAG = "NetworkCapture"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!lpparam.packageName.startsWith(targetPackage)) {
            return
        }

        XposedBridge.log("NetworkCapture: ====== 开始 Hook ======")
        XposedBridge.log("NetworkCapture: 包名: ${lpparam.packageName}")
        XposedBridge.log("NetworkCapture: 进程名: ${lpparam.processName}")

        try {
            hookApplicationInit(lpparam)
            hookSSLSocket(lpparam)
            hookOkHttp(lpparam)
            hookJavaNet(lpparam)
            hookWebView(lpparam)
            XposedBridge.log("NetworkCapture: ====== Hook 注册完成 ======")
        } catch (e: Throwable) {
            XposedBridge.log("NetworkCapture: Hook 失败 - ${e.message}")
            XposedBridge.log(e)
        }
    }

    private fun hookApplicationInit(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val app = param.thisObject as Application
                            CaptureManager.init(app)
                            XposedBridge.log("NetworkCapture: CaptureManager 初始化成功")
                        } catch (e: Exception) {
                            XposedBridge.log("NetworkCapture: CaptureManager 初始化失败 - ${e.message}")
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("NetworkCapture: Hook Application 失败 - ${e.message}")
        }
    }

    /**
     * Hook SSLSocket - 抓取所有HTTPS流量
     * 这是最底层的Java层Hook，能抓到所有HTTPS请求
     */
    private fun hookSSLSocket(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook SSLSocketFactory.createSocket - 获取SSL连接信息
            val sslSocketFactoryClass = XposedHelpers.findClass(
                "javax.net.ssl.SSLSocketFactory",
                lpparam.classLoader
            )

            // Hook所有createSocket重载
            for (method in sslSocketFactoryClass.declaredMethods) {
                if (method.name == "createSocket") {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            try {
                                val socket = param.result as? SSLSocket ?: return
                                val host = socket.inetAddress?.hostAddress ?: "unknown"
                                val port = socket.port
                                XposedBridge.log("NetworkCapture: [SSL] 创建SSL连接 - $host:$port")

                                // Hook输入输出流
                                hookSocketStreams(socket, host, port)
                            } catch (e: Exception) {
                                XposedBridge.log("NetworkCapture: [SSL] 捕获失败 - ${e.message}")
                            }
                        }
                    })
                }
            }
            XposedBridge.log("NetworkCapture: [SSL] SSLSocketFactory Hook 成功")
        } catch (e: Throwable) {
            XposedBridge.log("NetworkCapture: [SSL] Hook 失败 - ${e.message}")
        }
    }

    /**
     * Hook Socket的输入输出流
     */
    private fun hookSocketStreams(socket: SSLSocket, host: String, port: Int) {
        try {
            // Hook getInputStream
            val getInputStream = Socket::class.java.getDeclaredMethod("getInputStream")
            XposedBridge.hookMethod(getInputStream, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.thisObject === socket) {
                        val inputStream = param.result as InputStream
                        XposedBridge.log("NetworkCapture: [SSL] 获取InputStream - $host:$port")
                        // 创建包装流来捕获数据
                        param.result = CapturingInputStream(inputStream, host, port, true)
                    }
                }
            })

            // Hook getOutputStream
            val getOutputStream = Socket::class.java.getDeclaredMethod("getOutputStream")
            XposedBridge.hookMethod(getOutputStream, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.thisObject === socket) {
                        val outputStream = param.result as OutputStream
                        XposedBridge.log("NetworkCapture: [SSL] 获取OutputStream - $host:$port")
                        param.result = CapturingOutputStream(outputStream, host, port)
                    }
                }
            })
        } catch (e: Exception) {
            XposedBridge.log("NetworkCapture: [SSL] Hook流失败 - ${e.message}")
        }
    }

    /**
     * Hook OkHttp - 使用多种方式
     */
    private fun hookOkHttp(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classLoader = lpparam.classLoader

        // 方式1: Hook OkHttpClient.Builder.build()
        try {
            val builderClass = XposedHelpers.findClass("okhttp3.OkHttpClient.Builder", classLoader)
            XposedHelpers.findAndHookMethod(
                builderClass, "build",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val builder = param.thisObject
                            val interceptorClass = XposedHelpers.findClass("okhttp3.Interceptor", classLoader)
                            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                                classLoader,
                                arrayOf(interceptorClass),
                                NetworkInterceptorHandler(classLoader)
                            )
                            XposedHelpers.callMethod(builder, "addNetworkInterceptor", proxy)
                            XposedBridge.log("NetworkCapture: [OkHttp1] 拦截器已添加")
                        } catch (e: Exception) {
                            XposedBridge.log("NetworkCapture: [OkHttp1] 失败 - ${e.message}")
                        }
                    }
                }
            )
            XposedBridge.log("NetworkCapture: [OkHttp1] Hook 成功")
        } catch (e: Throwable) {
            XposedBridge.log("NetworkCapture: [OkHttp1] Hook 失败 - ${e.message}")
        }

        // 方式2: Hook RealCall.execute
        try {
            val realCallClass = XposedHelpers.findClass("okhttp3.RealCall", classLoader)
            XposedHelpers.findAndHookMethod(
                realCallClass, "execute",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val realCall = param.thisObject
                            val request = realCall.javaClass.getMethod("request").invoke(realCall)
                            val url = extractUrl(request)
                            val method = extractMethod(request)
                            val response = param.result
                            val responseCode = extractResponseCode(response)
                            val responseHeaders = extractHeaders(response)
                            val responseBody = extractResponseBody(response, classLoader)

                            XposedBridge.log("NetworkCapture: [OkHttp2] execute - $method $url -> $responseCode")

                            saveRequest(url, method, null, null, responseCode, responseHeaders, responseBody, 0, true, null)
                        } catch (e: Exception) {
                            XposedBridge.log("NetworkCapture: [OkHttp2] 捕获失败 - ${e.message}")
                        }
                    }
                }
            )
            XposedBridge.log("NetworkCapture: [OkHttp2] RealCall.execute Hook 成功")
        } catch (e: Throwable) {
            XposedBridge.log("NetworkCapture: [OkHttp2] Hook 失败 - ${e.message}")
        }

        // 方式3: Hook RealCall.enqueue
        try {
            val realCallClass = XposedHelpers.findClass("okhttp3.RealCall", classLoader)
            XposedHelpers.findAndHookMethod(
                realCallClass, "execute",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val realCall = param.thisObject
                            val request = realCall.javaClass.getMethod("request").invoke(realCall)
                            val url = extractUrl(request)
                            val method = extractMethod(request)
                            XposedBridge.log("NetworkCapture: [OkHttp3] 请求 - $method $url")
                        } catch (e: Exception) {
                            XposedBridge.log("NetworkCapture: [OkHttp3] 失败 - ${e.message}")
                        }
                    }
                }
            )
        } catch (_: Throwable) {}
    }

    /**
     * Hook Java.net网络请求
     */
    private fun hookJavaNet(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Hook URL.openConnection
        try {
            val urlClass = XposedHelpers.findClass("java.net.URL", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(
                urlClass, "openConnection",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val url = param.thisObject.toString()
                            XposedBridge.log("NetworkCapture: [JavaNet] openConnection - $url")
                        } catch (_: Exception) {}
                    }
                }
            )
            XposedBridge.log("NetworkCapture: [JavaNet] URL.openConnection Hook 成功")
        } catch (e: Throwable) {
            XposedBridge.log("NetworkCapture: [JavaNet] Hook 失败 - ${e.message}")
        }

        // Hook HttpURLConnection.getResponseCode
        try {
            val connClass = XposedHelpers.findClass("java.net.HttpURLConnection", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(
                connClass, "getResponseCode",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val conn = param.thisObject
                            val url = conn.javaClass.getMethod("getURL").invoke(conn).toString()
                            val method = try {
                                conn.javaClass.getMethod("getRequestMethod").invoke(conn) as String
                            } catch (_: Exception) { "GET" }
                            val responseCode = param.result as Int

                            XposedBridge.log("NetworkCapture: [JavaNet] getResponseCode - $method $url -> $responseCode")

                            saveRequest(url, method, null, null, responseCode, null, null, 0, responseCode in 200..399, null)
                        } catch (e: Exception) {
                            XposedBridge.log("NetworkCapture: [JavaNet] 捕获失败 - ${e.message}")
                        }
                    }
                }
            )
            XposedBridge.log("NetworkCapture: [JavaNet] HttpURLConnection.getResponseCode Hook 成功")
        } catch (e: Throwable) {
            XposedBridge.log("NetworkCapture: [JavaNet] getResponseCode Hook 失败 - ${e.message}")
        }
    }

    /**
     * Hook WebView网络请求
     */
    private fun hookWebView(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val webViewClientClass = XposedHelpers.findClass(
                "android.webkit.WebViewClient",
                lpparam.classLoader
            )
            XposedHelpers.findAndHookMethod(
                webViewClientClass,
                "shouldInterceptRequest",
                android.webkit.WebView::class.java,
                android.webkit.WebResourceRequest::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val request = param.args[1]
                            val url = request.javaClass.getMethod("getUrl").invoke(request).toString()
                            val method = try {
                                request.javaClass.getMethod("getMethod").invoke(request) as String
                            } catch (_: Exception) { "GET" }
                            XposedBridge.log("NetworkCapture: [WebView] $method $url")
                            saveRequest(url, method, null, null, 0, null, null, 0, true, null)
                        } catch (_: Exception) {}
                    }
                }
            )
            XposedBridge.log("NetworkCapture: [WebView] Hook 成功")
        } catch (e: Throwable) {
            XposedBridge.log("NetworkCapture: [WebView] Hook 失败 - ${e.message}")
        }
    }

    // ==================== OkHttp拦截器 ====================

    inner class NetworkInterceptorHandler(private val classLoader: ClassLoader) : java.lang.reflect.InvocationHandler {
        override fun invoke(proxy: Any, method: java.lang.reflect.Method, args: Array<out Any>?): Any? {
            if (method.name == "intercept") {
                return intercept(args?.get(0))
            }
            return method.invoke(proxy, *(args ?: emptyArray()))
        }

        private fun intercept(chain: Any?): Any? {
            if (chain == null) return null
            val startTime = System.nanoTime()
            try {
                val request = chain.javaClass.getMethod("request").invoke(chain)
                val proceedMethod = chain.javaClass.getMethod("proceed", request.javaClass)
                val response = proceedMethod.invoke(chain, request)

                val url = extractUrl(request)
                val method = extractMethod(request)
                val requestHeaders = extractHeaders(request)
                val requestBody = extractBody(request, classLoader)
                val responseCode = extractResponseCode(response)
                val responseHeaders = extractHeaders(response)
                val responseBody = extractResponseBody(response, classLoader)

                val duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
                XposedBridge.log("NetworkCapture: [拦截器] $method $url -> $responseCode")

                saveRequest(url, method, requestHeaders, requestBody, responseCode, responseHeaders, responseBody, duration, responseCode in 200..399, null)
                return response
            } catch (e: Exception) {
                XposedBridge.log("NetworkCapture: [拦截器] 异常 - ${e.message}")
                throw e
            }
        }
    }

    // ==================== 辅助方法 ====================

    private fun extractUrl(request: Any): String = try {
        request.javaClass.getMethod("url").invoke(request).toString()
    } catch (_: Exception) { "" }

    private fun extractMethod(request: Any): String = try {
        request.javaClass.getMethod("method").invoke(request) as String
    } catch (_: Exception) { "GET" }

    private fun extractHeaders(obj: Any): String = try {
        val headers = obj.javaClass.getMethod("headers").invoke(obj)
        val size = headers.javaClass.getMethod("size").invoke(headers) as Int
        val map = mutableMapOf<String, String>()
        for (i in 0 until size) {
            val name = headers.javaClass.getMethod("name", Int::class.java).invoke(headers, i) as String
            val value = headers.javaClass.getMethod("value", Int::class.java).invoke(headers, i) as String
            map[name] = value
        }
        gson.toJson(map)
    } catch (_: Exception) { "{}" }

    private fun extractBody(request: Any, classLoader: ClassLoader): String? = try {
        val body = request.javaClass.getMethod("body").invoke(request) ?: return null
        val bufferClass = XposedHelpers.findClass("okio.Buffer", classLoader)
        val buffer = bufferClass.getDeclaredConstructor().newInstance()
        body.javaClass.getMethod("writeTo", bufferClass).invoke(body, buffer)
        buffer.javaClass.getMethod("readUtf8").invoke(buffer) as String
    } catch (_: Exception) { null }

    private fun extractResponseCode(response: Any): Int = try {
        response.javaClass.getMethod("code").invoke(response) as Int
    } catch (_: Exception) { -1 }

    private fun extractResponseBody(response: Any, classLoader: ClassLoader): String? = try {
        val peekBodyMethod = response.javaClass.getMethod("peekBody", Long::class.java)
        val peekedBody = peekBodyMethod.invoke(response, Long.MAX_VALUE)
        peekedBody.javaClass.getMethod("string").invoke(peekedBody) as String
    } catch (_: Exception) { null }

    private fun saveRequest(
        url: String, method: String, requestHeaders: String?, requestBody: String?,
        responseCode: Int, responseHeaders: String?, responseBody: String?,
        duration: Long, isSuccess: Boolean, errorMessage: String?
    ) {
        try {
            val request = NetworkRequest(
                url = url, method = method,
                requestHeaders = requestHeaders, requestBody = requestBody,
                responseCode = responseCode, responseHeaders = responseHeaders,
                responseBody = responseBody,
                timestamp = System.currentTimeMillis(), duration = duration,
                isSuccess = isSuccess, errorMessage = errorMessage
            )
            CaptureManager.saveRequest(request)
        } catch (e: Exception) {
            XposedBridge.log("NetworkCapture: 保存失败 - ${e.message}")
        }
    }
}

/**
 * 捕获InputStream数据的包装类
 */
class CapturingInputStream(
    private val inputStream: InputStream,
    private val host: String,
    private val port: Int,
    private val isResponse: Boolean
) : InputStream() {
    private val buffer = StringBuilder()
    private var totalBytes = 0

    override fun read(): Int {
        val b = inputStream.read()
        if (b >= 0) {
            buffer.append(b.toChar())
            totalBytes++
        }
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = inputStream.read(b, off, len)
        if (n > 0) {
            buffer.append(String(b, off, n))
            totalBytes += n
            // 每1KB记录一次
            if (totalBytes % 1024 == 0) {
                XposedBridge.log("NetworkCapture: [Stream] $host:$port 读取 ${totalBytes} 字节")
            }
        }
        return n
    }

    override fun close() {
        inputStream.close()
        if (buffer.isNotEmpty()) {
            XposedBridge.log("NetworkCapture: [Stream] $host:$port 总共读取 ${totalBytes} 字节")
            // 保存到文件
            try {
                val request = NetworkRequest(
                    url = "ssl://$host:$port",
                    method = "SSL",
                    requestHeaders = null,
                    requestBody = null,
                    responseCode = 200,
                    responseHeaders = null,
                    responseBody = buffer.toString().take(10000),
                    timestamp = System.currentTimeMillis(),
                    duration = 0,
                    isSuccess = true,
                    errorMessage = null
                )
                CaptureManager.saveRequest(request)
            } catch (_: Exception) {}
        }
    }
}

/**
 * 捕获OutputStream数据的包装类
 */
class CapturingOutputStream(
    private val outputStream: OutputStream,
    private val host: String,
    private val port: Int
) : OutputStream() {
    private val buffer = StringBuilder()
    private var totalBytes = 0

    override fun write(b: Int) {
        outputStream.write(b)
        buffer.append(b.toChar())
        totalBytes++
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        outputStream.write(b, off, len)
        buffer.append(String(b, off, len))
        totalBytes += len
    }

    override fun flush() {
        outputStream.flush()
    }

    override fun close() {
        outputStream.close()
        if (buffer.isNotEmpty()) {
            XposedBridge.log("NetworkCapture: [Stream] $host:$port 发送 ${totalBytes} 字节")
            // 保存请求
            try {
                val request = NetworkRequest(
                    url = "ssl://$host:$port",
                    method = "SSL_SEND",
                    requestHeaders = null,
                    requestBody = buffer.toString().take(10000),
                    responseCode = 0,
                    responseHeaders = null,
                    responseBody = null,
                    timestamp = System.currentTimeMillis(),
                    duration = 0,
                    isSuccess = true,
                    errorMessage = null
                )
                CaptureManager.saveRequest(request)
            } catch (_: Exception) {}
        }
    }
}

package com.networkcapture.module.hook

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.networkcapture.module.data.model.NetworkRequest
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 抓包管理器
 * 使用文件方式跨进程通信（最可靠）
 * Hook进程写入JSON文件，模块UI进程读取
 */
object CaptureManager {

    private const val TAG = "NetworkCapture"
    private const val CAPTURE_DIR = "network_capture_logs"
    private val gson = Gson()

    private var appContext: Context? = null

    /**
     * 初始化（在Hook进程中调用）
     */
    fun init(context: Context) {
        appContext = context
        try {
            val dir = getCaptureDir()
            if (!dir.exists()) {
                dir.mkdirs()
            }
            // 写入一个测试文件确认权限
            val testFile = File(dir, "init_test.txt")
            testFile.writeText("CaptureManager initialized at ${Date()}")
            Log.d(TAG, "CaptureManager 初始化成功, 目录: ${dir.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "CaptureManager 初始化失败 - ${e.message}", e)
        }
    }

    /**
     * 获取抓包数据目录
     * 使用 /sdcard/Android/data/<target_pkg>/files/network_capture_logs/
     */
    private fun getCaptureDir(): File {
        val ctx = appContext!!
        val dir = File(ctx.getExternalFilesDir(null), CAPTURE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 保存网络请求到文件
     */
    fun saveRequest(request: NetworkRequest) {
        try {
            val ctx = appContext ?: run {
                Log.e(TAG, "appContext 为 null")
                return
            }

            val dir = getCaptureDir()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
            val fileName = "req_${timestamp}_${System.nanoTime()}.json"
            val file = File(dir, fileName)

            val json = gson.toJson(request)
            FileWriter(file).use { it.write(json) }

            // 同时写入日志文件
            val logFile = File(dir, "capture_log.txt")
            val logEntry = "[${Date()}] ${request.method} ${request.url} -> ${request.responseCode}\n"
            FileWriter(logFile, true).use { it.append(logEntry) }

            Log.d(TAG, "请求已保存到文件: ${file.name}")
        } catch (e: Exception) {
            Log.e(TAG, "保存请求失败 - ${e.message}", e)
        }
    }

    /**
     * 获取所有抓包文件（在模块UI进程中调用）
     */
    fun getCaptureFiles(targetPackage: String): List<File> {
        return try {
            val dir = File("/sdcard/Android/data/$targetPackage/files/$CAPTURE_DIR")
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles { f -> f.name.startsWith("req_") && f.name.endsWith(".json") }
                    ?.sortedByDescending { it.lastModified() }
                    ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取抓包文件失败 - ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 读取抓包文件内容
     */
    fun readCaptureFile(file: File): NetworkRequest? {
        return try {
            val json = file.readText()
            gson.fromJson(json, NetworkRequest::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "读取文件失败 - ${e.message}", e)
            null
        }
    }

    /**
     * 删除所有抓包文件
     */
    fun clearAllCaptures(targetPackage: String): Boolean {
        return try {
            val dir = File("/sdcard/Android/data/$targetPackage/files/$CAPTURE_DIR")
            if (dir.exists()) {
                dir.listFiles()?.forEach { it.delete() }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "清除文件失败 - ${e.message}", e)
            false
        }
    }
}

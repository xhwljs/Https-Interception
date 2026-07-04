package com.networkcapture.module.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * 网络请求数据实体
 *
 * @property id 主键ID
 * @property url 请求URL
 * @property method 请求方法（GET, POST, PUT, DELETE等）
 * @property requestHeaders 请求头（JSON格式）
 * @property requestBody 请求体
 * @property responseCode 响应状态码
 * @property responseHeaders 响应头（JSON格式）
 * @property responseBody 响应体
 * @property timestamp 请求时间戳
 * @property duration 请求耗时（毫秒）
 * @property isSuccess 是否成功
 * @property errorMessage 错误信息
 */
@Entity(
    tableName = "network_requests",
    indices = [
        Index(value = ["timestamp"], name = "idx_timestamp"),
        Index(value = ["method"], name = "idx_method"),
        Index(value = ["url"], name = "idx_url")
    ]
)
data class NetworkRequest(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val url: String,
    val method: String,

    val requestHeaders: String? = null,
    val requestBody: String? = null,

    val responseCode: Int = 0,
    val responseHeaders: String? = null,
    val responseBody: String? = null,

    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long = 0,

    val isSuccess: Boolean = true,
    val errorMessage: String? = null
) {
    /**
     * 获取请求状态描述
     */
    fun getStatusText(): String {
        return when {
            !isSuccess -> "错误"
            responseCode in 200..299 -> "成功"
            responseCode in 300..399 -> "重定向"
            responseCode in 400..499 -> "客户端错误"
            responseCode in 500..599 -> "服务器错误"
            else -> "未知"
        }
    }

    /**
     * 获取简短的URL路径
     */
    fun getShortUrl(): String {
        return try {
            val uri = java.net.URI(url)
            "${uri.path}${uri.query?.let { "?$it" } ?: ""}"
        } catch (e: Exception) {
            url
        }
    }

    /**
     * 获取域名
     */
    fun getDomain(): String {
        return try {
            val uri = java.net.URI(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }

    /**
     * 获取格式化的时间
     */
    fun getFormattedTime(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    /**
     * 获取格式化的日期
     */
    fun getFormattedDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    /**
     * 获取格式化的耗时
     */
    fun getFormattedDuration(): String {
        return when {
            duration < 1000 -> "${duration}ms"
            duration < 60000 -> "${duration / 1000.0}s"
            else -> "${duration / 60000}min"
        }
    }
}
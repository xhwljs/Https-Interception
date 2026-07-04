package com.networkcapture.module.data.repository

import android.content.Context
import com.networkcapture.module.data.db.AppDatabase
import com.networkcapture.module.data.model.NetworkRequest
import kotlinx.coroutines.flow.Flow

/**
 * 网络请求仓库
 */
class NetworkRequestRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val dao = database.networkRequestDao()

    /**
     * 插入网络请求
     */
    suspend fun insert(request: NetworkRequest): Long {
        return dao.insert(request)
    }

    /**
     * 批量插入
     */
    suspend fun insertAll(requests: List<NetworkRequest>) {
        dao.insertAll(requests)
    }

    /**
     * 根据ID获取
     */
    suspend fun getById(id: Long): NetworkRequest? {
        return dao.getById(id)
    }

    /**
     * 获取所有网络请求
     */
    fun getAll(): Flow<List<NetworkRequest>> {
        return dao.getAll()
    }

    /**
     * 根据方法过滤
     */
    fun getByMethod(method: String): Flow<List<NetworkRequest>> {
        return dao.getByMethod(method)
    }

    /**
     * 搜索
     */
    fun search(keyword: String): Flow<List<NetworkRequest>> {
        return dao.search(keyword)
    }

    /**
     * 根据方法搜索
     */
    fun searchByMethod(method: String, keyword: String): Flow<List<NetworkRequest>> {
        return dao.searchByMethod(method, keyword)
    }

    /**
     * 获取今天的请求数量
     */
    fun getTodayCount(): Flow<Int> {
        val startOfDay = getStartOfDayTimestamp()
        return dao.getTodayCount(startOfDay)
    }

    /**
     * 获取总数量
     */
    fun getTotalCount(): Flow<Int> {
        return dao.getTotalCount()
    }

    /**
     * 删除所有
     */
    suspend fun deleteAll() {
        dao.deleteAll()
    }

    /**
     * 删除指定时间之前的请求
     */
    suspend fun deleteBefore(timestamp: Long) {
        dao.deleteBefore(timestamp)
    }

    /**
     * 根据ID删除
     */
    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    /**
     * 获取今天0点的时间戳
     */
    private fun getStartOfDayTimestamp(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    companion object {
        @Volatile
        private var INSTANCE: NetworkRequestRepository? = null

        fun getRepository(context: Context): NetworkRequestRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = NetworkRequestRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
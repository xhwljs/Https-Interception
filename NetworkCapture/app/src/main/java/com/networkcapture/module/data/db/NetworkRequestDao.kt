package com.networkcapture.module.data.db

import androidx.room.*
import com.networkcapture.module.data.model.NetworkRequest
import kotlinx.coroutines.flow.Flow

/**
 * 网络请求 DAO
 */
@Dao
interface NetworkRequestDao {

    /**
     * 插入网络请求
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: NetworkRequest): Long

    /**
     * 批量插入网络请求
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(requests: List<NetworkRequest>)

    /**
     * 根据ID获取网络请求
     */
    @Query("SELECT * FROM network_requests WHERE id = :id")
    suspend fun getById(id: Long): NetworkRequest?

    /**
     * 获取所有网络请求（按时间倒序）
     */
    @Query("SELECT * FROM network_requests ORDER BY timestamp DESC")
    fun getAll(): Flow<List<NetworkRequest>>

    /**
     * 根据方法过滤网络请求
     */
    @Query("SELECT * FROM network_requests WHERE method = :method ORDER BY timestamp DESC")
    fun getByMethod(method: String): Flow<List<NetworkRequest>>

    /**
     * 搜索网络请求（URL或方法）
     */
    @Query("SELECT * FROM network_requests WHERE url LIKE '%' || :keyword || '%' OR method LIKE '%' || :keyword || '%' ORDER BY timestamp DESC")
    fun search(keyword: String): Flow<List<NetworkRequest>>

    /**
     * 根据方法搜索
     */
    @Query("SELECT * FROM network_requests WHERE method = :method AND (url LIKE '%' || :keyword || '%' OR method LIKE '%' || :keyword || '%') ORDER BY timestamp DESC")
    fun searchByMethod(method: String, keyword: String): Flow<List<NetworkRequest>>

    /**
     * 获取今天的网络请求数量
     */
    @Query("SELECT COUNT(*) FROM network_requests WHERE timestamp >= :startOfDay")
    fun getTodayCount(startOfDay: Long): Flow<Int>

    /**
     * 获取总数量
     */
    @Query("SELECT COUNT(*) FROM network_requests")
    fun getTotalCount(): Flow<Int>

    /**
     * 删除所有网络请求
     */
    @Query("DELETE FROM network_requests")
    suspend fun deleteAll()

    /**
     * 删除指定时间之前的网络请求
     */
    @Query("DELETE FROM network_requests WHERE timestamp < :timestamp")
    suspend fun deleteBefore(timestamp: Long)

    /**
     * 根据ID删除网络请求
     */
    @Query("DELETE FROM network_requests WHERE id = :id")
    suspend fun deleteById(id: Long)
}
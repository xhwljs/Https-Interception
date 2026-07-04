package com.networkcapture.module.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.networkcapture.module.R
import com.networkcapture.module.data.model.NetworkRequest
import com.networkcapture.module.databinding.ItemNetworkRequestBinding

/**
 * 网络请求列表适配器
 */
class NetworkRequestAdapter(
    private val onItemClick: (NetworkRequest) -> Unit
) : ListAdapter<NetworkRequest, NetworkRequestAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNetworkRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemNetworkRequestBinding,
        private val onItemClick: (NetworkRequest) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(request: NetworkRequest) {
            binding.apply {
                // 设置方法标签
                methodTextView.text = request.method
                methodTextView.setTextColor(getMethodColor(request.method))

                // 设置时间
                timeTextView.text = request.getFormattedTime()

                // 设置 URL
                urlTextView.text = request.getShortUrl()

                // 设置状态码
                statusCodeTextView.text = request.responseCode.toString()
                statusCodeTextView.setTextColor(getStatusColor(request.responseCode))

                // 设置状态文本
                statusTextView.text = request.getStatusText()

                // 设置耗时
                durationTextView.text = request.getFormattedDuration()

                // 点击事件
                root.setOnClickListener {
                    onItemClick(request)
                }
            }
        }

        /**
         * 根据请求方法获取颜色
         */
        private fun getMethodColor(method: String): Int {
            return when (method.uppercase()) {
                "GET" -> Color.parseColor("#22C55E") // Green
                "POST" -> Color.parseColor("#3B82F6") // Blue
                "PUT" -> Color.parseColor("#F59E0B") // Orange
                "DELETE" -> Color.parseColor("#EF4444") // Red
                "PATCH" -> Color.parseColor("#8B5CF6") // Purple
                else -> Color.parseColor("#94A3B8") // Gray
            }
        }

        /**
         * 根据状态码获取颜色
         */
        private fun getStatusColor(code: Int): Int {
            return when (code) {
                in 200..299 -> Color.parseColor("#22C55E") // Success - Green
                in 300..399 -> Color.parseColor("#3B82F6") // Redirect - Blue
                in 400..499 -> Color.parseColor("#F59E0B") // Client Error - Orange
                in 500..599 -> Color.parseColor("#EF4444") // Server Error - Red
                else -> Color.parseColor("#94A3B8") // Unknown - Gray
            }
        }
    }

    /**
     * DiffUtil 回调
     */
    class DiffCallback : DiffUtil.ItemCallback<NetworkRequest>() {
        override fun areItemsTheSame(oldItem: NetworkRequest, newItem: NetworkRequest): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NetworkRequest, newItem: NetworkRequest): Boolean {
            return oldItem == newItem
        }
    }
}
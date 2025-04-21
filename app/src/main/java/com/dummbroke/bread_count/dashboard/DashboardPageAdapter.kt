package com.dummbroke.bread_count.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dummbroke.bread_count.databinding.ItemDashboardBinding

class DashboardPageAdapter : ListAdapter<DashboardItem, DashboardPageAdapter.DashboardViewHolder>(DashboardDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardViewHolder {
        val binding = ItemDashboardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DashboardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DashboardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DashboardViewHolder(
        private val binding: ItemDashboardBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DashboardItem) {
            binding.apply {
                // TODO: Bind data to views once UI is designed
            }
        }
    }

    private class DashboardDiffCallback : DiffUtil.ItemCallback<DashboardItem>() {
        override fun areItemsTheSame(oldItem: DashboardItem, newItem: DashboardItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DashboardItem, newItem: DashboardItem): Boolean {
            return oldItem == newItem
        }
    }
}

class ItemDashboardBinding {

}
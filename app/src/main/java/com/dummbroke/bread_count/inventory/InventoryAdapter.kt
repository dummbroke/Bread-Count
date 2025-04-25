package com.dummbroke.bread_count.inventory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dummbroke.bread_count.R
import com.dummbroke.bread_count.model.InventoryItem

class InventoryAdapter(
    private val onItemClick: (InventoryItem) -> Unit,
    private val onDeleteClick: (InventoryItem) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {

    private var items: List<InventoryItem> = emptyList()

    fun updateItems(newItems: List<InventoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inventory_data, parent, false)
        return InventoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class InventoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameValue: TextView = itemView.findViewById(R.id.nameValue)
        private val priceValue: TextView = itemView.findViewById(R.id.priceValue)
        private val quantityValue: TextView = itemView.findViewById(R.id.quantityValue)
        private val editButton: View = itemView.findViewById(R.id.editButton)
        private val deleteButton: View = itemView.findViewById(R.id.deleteButton)

        fun bind(item: InventoryItem) {
            nameValue.text = item.name
            priceValue.text = String.format("%.2f", item.price)
            quantityValue.text = item.quantity.toString()

            editButton.setOnClickListener { onItemClick(item) }
            deleteButton.setOnClickListener { onDeleteClick(item) }
        }
    }
} 
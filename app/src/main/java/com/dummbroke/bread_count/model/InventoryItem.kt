package com.dummbroke.bread_count.model

data class InventoryItem(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) 
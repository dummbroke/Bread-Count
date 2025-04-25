package com.dummbroke.bread_count.model

data class InventoryItem(
    val id: String = "",
    val name: String,
    val price: Double,
    val category: String,
    val quantity: Int = 0
) 
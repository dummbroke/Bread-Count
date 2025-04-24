package com.dummbroke.bread_count.model

data class BreadItem(
    val id: String = "",
    val name: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0,
    val description: String = "",
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
) 
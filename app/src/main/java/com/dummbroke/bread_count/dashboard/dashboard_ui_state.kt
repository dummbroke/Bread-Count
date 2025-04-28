package com.dummbroke.bread_count.dashboard

import com.dummbroke.bread_count.model.InventoryItem

/**
 * UI state for the DashboardPage transaction recorder.
 */
data class DashboardUiState(
    val selectedCategory: String = "",
    val items: List<InventoryItem> = emptyList(),
    val selectedItem: InventoryItem? = null,
    val quantity: Int = 1,
    val isOwner: Boolean = false,
    val ownerName: String = "",
    val isRecording: Boolean = false,
    val showConfirmation: Boolean = false
) 
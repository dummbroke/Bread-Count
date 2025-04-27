package com.dummbroke.bread_count.inventory

import com.dummbroke.bread_count.model.InventoryItem

sealed class InventoryUiState {
    object Initial : InventoryUiState()
    object Loading : InventoryUiState()
    data class Success(
        val items: List<InventoryItem>,
        val currentCategory: String
    ) : InventoryUiState()
    data class Error(val message: String) : InventoryUiState()
}
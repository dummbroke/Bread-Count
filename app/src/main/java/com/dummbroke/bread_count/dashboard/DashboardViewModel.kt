package com.dummbroke.bread_count.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Initial)
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Success(emptyList())
        }
    }

    fun refreshData() {
        loadDashboardData()
    }
}

sealed class DashboardUiState {
    object Initial : DashboardUiState()
    object Loading : DashboardUiState()
    data class Success(val items: List<DashboardItem>) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
} 
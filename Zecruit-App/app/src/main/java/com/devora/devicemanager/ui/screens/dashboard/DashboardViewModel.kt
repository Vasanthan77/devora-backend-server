package com.devora.devicemanager.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devora.devicemanager.network.DashboardStats
import com.devora.devicemanager.network.DeviceActivityResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val stats: DashboardStats? = null,
    val recentActivities: List<DeviceActivityResponse> = emptyList()
)

class DashboardViewModel(
    private val repository: DashboardRepository = DashboardRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private val dismissedActivityIds = mutableSetOf<Long>()

    init {
        refreshStats()
        startActivitiesPolling()
    }

    private fun refreshStats() {
        viewModelScope.launch {
            val stats = repository.fetchDashboardStats()
            _uiState.value = _uiState.value.copy(stats = stats)
        }
    }

    private fun startActivitiesPolling() {
        viewModelScope.launch {
            while (true) {
                val activities = repository.fetchRecentActivities(limit = 10)
                _uiState.value = _uiState.value.copy(
                    recentActivities = activities.filterNot { dismissedActivityIds.contains(it.id) }
                )
                delay(120_000L)
            }
        }
    }

    fun dismissActivity(activityId: Long) {
        viewModelScope.launch {
            val deleted = repository.deleteActivity(activityId)
            if (deleted) {
                dismissedActivityIds.add(activityId)
                _uiState.value = _uiState.value.copy(
                    recentActivities = _uiState.value.recentActivities.filterNot { it.id == activityId }
                )
            }
        }
    }
}

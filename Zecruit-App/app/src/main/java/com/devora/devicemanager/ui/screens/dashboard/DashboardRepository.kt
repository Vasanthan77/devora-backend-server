package com.devora.devicemanager.ui.screens.dashboard

import com.devora.devicemanager.data.remote.RemoteDataSource
import android.util.Log
import com.devora.devicemanager.network.DashboardStats
import com.devora.devicemanager.network.DeviceActivityResponse

class DashboardRepository {

    suspend fun fetchDashboardStats(): DashboardStats? {
        return try {
            val response = RemoteDataSource.getDashboardStats()
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.e("DashboardRepository", "Stats fetch failed: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("DashboardRepository", "Failed to fetch dashboard stats", e)
            null
        }
    }

    suspend fun fetchRecentActivities(limit: Int): List<DeviceActivityResponse> {
        return try {
            val response = RemoteDataSource.getActivities(limit = limit)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("DashboardRepository", "Failed to fetch activities", e)
            emptyList()
        }
    }

    suspend fun deleteActivity(activityId: Long): Boolean {
        return try {
            val response = RemoteDataSource.deleteActivity(activityId)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("DashboardRepository", "Failed to delete activity", e)
            false
        }
    }
}

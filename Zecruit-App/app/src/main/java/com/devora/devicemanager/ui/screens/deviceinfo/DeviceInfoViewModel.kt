package com.devora.devicemanager.ui.screens.deviceinfo

import com.devora.devicemanager.data.remote.RemoteDataSource
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devora.devicemanager.collector.AppInfo
import com.devora.devicemanager.collector.AppInventoryCollector
import com.devora.devicemanager.collector.DeviceInfo
import com.devora.devicemanager.collector.DeviceInfoCollector
import com.devora.devicemanager.data.db.AppDatabase
import com.devora.devicemanager.data.db.DeviceInfoEntity
import com.devora.devicemanager.data.db.DeviceInfoSyncLogEntity
import com.devora.devicemanager.network.DeviceInfoRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

data class DeviceInfoUiState(
    val deviceInfo: DeviceInfo? = null,
    val apps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTime: String? = null,
    val lastSyncStatus: String? = null,
    val syncMessage: String? = null,
    val errorMessage: String? = null
)

class DeviceInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).deviceInfoDao()
    private val _uiState = MutableStateFlow(DeviceInfoUiState())
    val uiState: StateFlow<DeviceInfoUiState> = _uiState.asStateFlow()

    init {
        collectDeviceInfo()
        loadLastSyncStatus()
    }

    fun collectDeviceInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val info = DeviceInfoCollector.collect(getApplication())

                // Save to Room
                dao.insert(
                    DeviceInfoEntity(
                        deviceId = info.deviceId,
                        model = info.model,
                        manufacturer = info.manufacturer,
                        brand = info.brand,
                        board = info.board,
                        osVersion = info.osVersion,
                        sdkVersion = info.sdkVersion,
                        serialNumber = info.serialNumber,
                        imei = info.imei,
                        deviceType = info.deviceType,
                        serialRestricted = info.serialRestricted,
                        imeiRestricted = info.imeiRestricted,
                        collectedAt = info.collectedAt
                    )
                )

                _uiState.value = _uiState.value.copy(
                    deviceInfo = info,
                    isLoading = false
                )

                // Collect app inventory
                val apps = AppInventoryCollector.collect(getApplication())
                _uiState.value = _uiState.value.copy(apps = apps)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Collection failed: ${e.message}"
                )
            }
        }
    }

    fun syncToBackend() {
        val info = _uiState.value.deviceInfo ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = null)
            try {
                val request = DeviceInfoRequest(
                    deviceId = info.deviceId,
                    model = info.model,
                    manufacturer = info.manufacturer,
                    osVersion = info.osVersion,
                    sdkVersion = info.sdkVersion,
                    serialNumber = info.serialNumber,
                    imei = info.imei,
                    deviceType = info.deviceType,
                    deviceOwnerSet = info.deviceOwnerSet
                )

                val response = RemoteDataSource.uploadDeviceInfo(request)
                val now = Instant.now().toString()

                if (response.isSuccessful) {
                    dao.insertSyncLog(
                        DeviceInfoSyncLogEntity(
                            deviceId = info.deviceId,
                            syncedAt = now,
                            status = "SUCCESS",
                            httpCode = response.code(),
                            errorMessage = null
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        lastSyncTime = now,
                        lastSyncStatus = "SUCCESS",
                        syncMessage = "Synced successfully"
                    )
                } else {
                    val errBody = response.errorBody()?.string() ?: "Unknown error"
                    dao.insertSyncLog(
                        DeviceInfoSyncLogEntity(
                            deviceId = info.deviceId,
                            syncedAt = now,
                            status = "FAILED",
                            httpCode = response.code(),
                            errorMessage = errBody
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        lastSyncStatus = "FAILED",
                        syncMessage = "Sync failed: HTTP ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                val now = Instant.now().toString()
                dao.insertSyncLog(
                    DeviceInfoSyncLogEntity(
                        deviceId = info.deviceId,
                        syncedAt = now,
                        status = "FAILED",
                        httpCode = null,
                        errorMessage = e.message
                    )
                )
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    lastSyncStatus = "FAILED",
                    syncMessage = "Sync failed: ${e.message}"
                )
            }
        }
    }

    private fun loadLastSyncStatus() {
        viewModelScope.launch {
            val lastSync = dao.getLastSuccessfulSync()
            if (lastSync != null) {
                _uiState.value = _uiState.value.copy(
                    lastSyncTime = lastSync.syncedAt,
                    lastSyncStatus = lastSync.status
                )
            }
        }
    }
}

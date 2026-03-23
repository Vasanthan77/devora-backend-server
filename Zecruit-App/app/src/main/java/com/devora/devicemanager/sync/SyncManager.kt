package com.devora.devicemanager.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.devora.devicemanager.collector.AppInventoryCollector
import com.devora.devicemanager.collector.DeviceInfoCollector
import com.devora.devicemanager.network.ApiConfig
import com.devora.devicemanager.network.model.AppInfoDto
import com.devora.devicemanager.network.model.BulkAppInventoryRequest
import com.devora.devicemanager.network.model.SyncDeviceInfoRequest

/**
 * Orchestrates collecting device data and syncing it to the backend.
 */
object SyncManager {

    private const val TAG = "SYNC"

    /**
     * Collects device info + app inventory, then POSTs both to the backend.
     *
     * @param context Application or Activity context.
     * @param employeeId The employee ID associated with this device.
     * @return [SyncResult] indicating success or failure with a message.
     */
    suspend fun syncDeviceData(context: Context, employeeId: String): SyncResult {
        Log.d(TAG, "=== Starting device sync for employee: $employeeId ===")

        // Pre-check: network connectivity
        if (!isNetworkAvailable(context)) {
            val msg = "No network connection available"
            Log.e(TAG, msg)
            return SyncResult(success = false, message = msg)
        }
        Log.d(TAG, "Network check passed")

        val api = ApiConfig.syncApi

        try {
            // Step 1: Collect device info
            Log.d(TAG, "Step 1: Collecting device info...")
            val deviceInfo = DeviceInfoCollector.collect(context)
            Log.d(TAG, "Device info collected — id=${deviceInfo.deviceId}, model=${deviceInfo.model}")

            // Step 2: Collect app inventory
            Log.d(TAG, "Step 2: Collecting app inventory...")
            val apps = AppInventoryCollector.collect(context)
            Log.d(TAG, "App inventory collected — ${apps.size} apps found")

            // Step 3: POST device info
            Log.d(TAG, "Step 3: Uploading device info to backend...")
            val deviceInfoRequest = SyncDeviceInfoRequest(
                deviceId = deviceInfo.deviceId,
                model = deviceInfo.model,
                manufacturer = deviceInfo.manufacturer,
                brand = deviceInfo.brand,
                board = deviceInfo.board,
                osVersion = deviceInfo.osVersion,
                sdkVersion = deviceInfo.sdkVersion,
                uniqueId = deviceInfo.deviceId,
                serialNumber = deviceInfo.serialNumber,
                imei = deviceInfo.imei,
                deviceType = deviceInfo.deviceType,
                deviceOwnerSet = deviceInfo.deviceOwnerSet
            )
            val deviceInfoResponse = api.uploadDeviceInfo(deviceInfoRequest)
            if (!deviceInfoResponse.isSuccessful) {
                val msg = "Device info upload failed: HTTP ${deviceInfoResponse.code()}"
                Log.e(TAG, msg)
                return SyncResult(success = false, message = msg)
            }
            Log.d(TAG, "Device info uploaded successfully")

            // Step 4: POST app inventory
            Log.d(TAG, "Step 4: Uploading app inventory to backend...")
            val appDtos = apps.map { app ->
                AppInfoDto(
                    appName = app.appName,
                    packageName = app.packageName,
                    versionName = app.versionName,
                    versionCode = app.versionCode,
                    installSource = app.installSource,
                    isSystemApp = app.isSystemApp,
                    isSuspended = app.isSuspended,
                    iconBase64 = app.iconBase64
                )
            }
            val appRequest = BulkAppInventoryRequest(
                deviceId = deviceInfo.deviceId,
                apps = appDtos
            )
            val appResponse = api.uploadAppInventory(appRequest)
            if (!appResponse.isSuccessful) {
                val msg = "App inventory upload failed: HTTP ${appResponse.code()}"
                Log.e(TAG, msg)
                return SyncResult(success = false, message = msg)
            }
            Log.d(TAG, "App inventory uploaded successfully")

            Log.d(TAG, "=== Sync completed successfully ===")
            return SyncResult(success = true, message = "Sync completed successfully")

        } catch (e: Exception) {
            val msg = "Sync failed: ${e.message}"
            Log.e(TAG, msg, e)
            return SyncResult(success = false, message = msg)
        }
    }

    /**
     * Checks whether the device currently has network connectivity.
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

/**
 * Simple result wrapper for sync operations.
 */
data class SyncResult(
    val success: Boolean,
    val message: String
)

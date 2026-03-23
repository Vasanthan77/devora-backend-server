package com.devora.devicemanager.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.devora.devicemanager.collector.DeviceInfoCollector
import com.devora.devicemanager.data.db.AppDatabase
import com.devora.devicemanager.data.db.DeviceInfoEntity
import com.devora.devicemanager.data.db.DeviceInfoSyncLogEntity
import com.devora.devicemanager.network.DeviceInfoRequest
import com.devora.devicemanager.network.RetrofitClient
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that periodically collects and syncs device info to backend.
 * Uses exponential backoff with 3 max retries on failure.
 */
class DeviceInfoSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "DeviceInfoSync"
        private const val WORK_NAME = "device_info_sync"
        private const val WORK_NOW_NAME = "device_info_sync_now"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<DeviceInfoSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Device info sync worker scheduled")
        }

        fun scheduleNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<DeviceInfoSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NOW_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "Immediate device info sync scheduled")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting device info sync work (attempt ${runAttemptCount + 1})")

        if (runAttemptCount >= 3) {
            Log.w(TAG, "Max retries reached, marking as failure")
            return Result.failure()
        }

        val dao = AppDatabase.getInstance(applicationContext).deviceInfoDao()

        return try {
            val info = DeviceInfoCollector.collect(applicationContext)

            // Save locally
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

            // Sync to backend
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

            val response = RetrofitClient.api.uploadDeviceInfo(request)
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
                Log.d(TAG, "Device info synced successfully")
                Result.success()
            } else {
                dao.insertSyncLog(
                    DeviceInfoSyncLogEntity(
                        deviceId = info.deviceId,
                        syncedAt = now,
                        status = "FAILED",
                        httpCode = response.code(),
                        errorMessage = response.errorBody()?.string()
                    )
                )
                Log.w(TAG, "Sync failed: HTTP ${response.code()}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync error", e)
            dao.insertSyncLog(
                DeviceInfoSyncLogEntity(
                    deviceId = "unknown",
                    syncedAt = Instant.now().toString(),
                    status = "FAILED",
                    httpCode = null,
                    errorMessage = e.message
                )
            )
            Result.retry()
        }
    }
}

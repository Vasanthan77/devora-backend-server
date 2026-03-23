package com.devora.devicemanager.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Background worker that periodically syncs device info and app inventory
 * to the MDM backend using WorkManager.
 *
 * Runs every 15 minutes (WorkManager minimum interval) even when the app
 * is in the background. Sends a notification on successful sync.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME = "devora_periodic_sync"
        private const val CHANNEL_ID = "devora_sync_channel"
        private const val NOTIFICATION_ID = 1001

        /**
         * Enqueues a periodic sync that runs every 15 minutes.
         * Uses [ExistingPeriodicWorkPolicy.KEEP] so scheduling this multiple
         * times is safe — it won't create duplicates.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            Log.d(TAG, "Periodic sync scheduled (every 15 min)")
        }

        /**
         * Cancels the periodic sync.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Periodic sync cancelled")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker started")

        return try {
            // Get employee ID from SharedPreferences
            val prefs = applicationContext.getSharedPreferences(
                "devora_enrollment", Context.MODE_PRIVATE
            )
            val employeeId = prefs.getString("employee_id", "unknown") ?: "unknown"

            val syncResult = SyncManager.syncDeviceData(applicationContext, employeeId)

            if (syncResult.success) {
                Log.d(TAG, "SyncWorker completed successfully")
                showNotification("Device synced successfully")

                // Update last_seen timestamp in prefs
                prefs.edit()
                    .putLong("last_sync_timestamp", System.currentTimeMillis())
                    .apply()

                Result.success()
            } else {
                Log.w(TAG, "SyncWorker sync failed: ${syncResult.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker exception: ${e.message}", e)
            Result.retry()
        }
    }

    private fun showNotification(message: String) {
        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        // Create channel (required for API 26+)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "DEVORA Sync",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Background device sync notifications"
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("DEVORA")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

package com.devora.devicemanager.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Retired worker kept only as a compatibility shim.
 *
 * Location uploads should be initiated explicitly by app actions/backend workflows.
 */
class LocationSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        fun schedule(context: Context) = Unit
        fun scheduleNow(context: Context) = Unit
    }

    override suspend fun doWork(): Result = Result.success()
}

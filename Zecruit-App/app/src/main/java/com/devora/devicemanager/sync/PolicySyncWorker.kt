package com.devora.devicemanager.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Retired worker kept only as a compatibility shim.
 *
 * Policy enforcement is now handled by backend AMAPI calls, not local DPM polling.
 */
class PolicySyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        fun schedule(context: Context) = Unit
        fun scheduleNow(context: Context) = Unit
    }

    override suspend fun doWork(): Result = Result.success()
}

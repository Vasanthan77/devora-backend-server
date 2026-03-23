package com.devora.devicemanager.sync

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.os.UserManager
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.devora.devicemanager.AdminReceiver
import com.devora.devicemanager.network.DeviceAppRestrictionResponse
import com.devora.devicemanager.network.RetrofitClient
import java.util.concurrent.TimeUnit

/**
 * Worker (reschedules itself every few seconds) that enforces MDM policies:
 *  1. App restrictions   — setPackagesSuspended() per restricted app
 *  2. Camera policy      — setCameraDisabled()
 *  3. Install/uninstall  — addUserRestriction(DISALLOW_INSTALL/UNINSTALL_APPS)
 *  4. Pending commands   — LOCK, WIPE, CAMERA_*, FORCE_PASSWORD_RESET, CLEAR_APP_DATA, FORCE_SYNC
 */
class PolicySyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "PolicySyncWorker"
        private const val WORK_NAME = "devora_policy_sync"

        fun schedule(context: Context) {
            enqueue(
                context,
                initialDelaySeconds = 5,
                workPolicy = ExistingWorkPolicy.APPEND
            )
        }

        fun scheduleNow(context: Context) {
            enqueue(
                context,
                initialDelaySeconds = 0,
                workPolicy = ExistingWorkPolicy.REPLACE
            )
        }

        private fun enqueue(
            context: Context,
            initialDelaySeconds: Long,
            workPolicy: ExistingWorkPolicy
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val requestBuilder = OneTimeWorkRequestBuilder<PolicySyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)

            if (initialDelaySeconds > 0) {
                requestBuilder.setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS)
            }

            val request = requestBuilder.build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                workPolicy,
                request
            )
            Log.d(TAG, "Policy sync worker scheduled (${initialDelaySeconds}s, policy=$workPolicy)")
        }
    }

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("devora_enrollment", Context.MODE_PRIVATE)
        val deviceId = prefs.getString("device_id", null) ?: return Result.success()

        val dpm = applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = AdminReceiver.getComponentName(applicationContext)
        val isDeviceOwner = dpm.isDeviceOwnerApp(applicationContext.packageName)
        val isAdminActive = dpm.isAdminActive(admin)

        if (!isAdminActive) {
            Log.w(TAG, "Admin receiver is not active — skipping policy + command execution")
            return Result.success()
        }

        if (!isDeviceOwner) {
            Log.d(TAG, "Not Device Owner — skipping owner-only policy enforcement")
        }

        if (isDeviceOwner) {
            try {
                enforceAppRestrictions(deviceId, dpm, admin)
            } catch (e: Exception) {
                Log.w(TAG, "App restriction enforcement failed: ${e.message}")
            }

            try {
                enforcePolicies(deviceId, dpm, admin)
            } catch (e: Exception) {
                Log.w(TAG, "Policy enforcement failed: ${e.message}")
            }
        }

        try {
            executePendingCommands(deviceId, dpm, admin, isDeviceOwner)
        } catch (e: Exception) {
            Log.w(TAG, "Command execution failed: ${e.message}")
        }

        schedule(applicationContext)

        return Result.success()
    }

    private suspend fun enforceAppRestrictions(
        deviceId: String,
        dpm: DevicePolicyManager,
        admin: android.content.ComponentName
    ) {
        if (!dpm.isDeviceOwnerApp(applicationContext.packageName)) {
            Log.w(TAG, "Not device owner, skipping app restrictions")
            return
        }

        val response = RetrofitClient.api.getAllAppRestrictions(deviceId)
        if (!response.isSuccessful) return

        val restrictions = response.body() ?: emptyList()
        applyAppRestrictions(dpm, admin, restrictions)
    }

    private fun applyAppRestrictions(
        dpm: DevicePolicyManager,
        adminComponent: android.content.ComponentName,
        restrictedApps: List<DeviceAppRestrictionResponse>
    ) {
        if (!dpm.isDeviceOwnerApp(applicationContext.packageName)) {
            Log.w(TAG, "Not device owner, skipping")
            return
        }

        val restrictionPrefs = applicationContext.getSharedPreferences("devora_restrictions", Context.MODE_PRIVATE)
        val previousRestricted = restrictionPrefs.getStringSet("restricted_packages", emptySet()) ?: emptySet()

        val restrictedNow = restrictedApps
            .filter { it.restricted }
            .map { it.packageName }
            .toSet()

        val explicitUnsuspend = restrictedApps
            .filter { !it.restricted }
            .map { it.packageName }
            .toSet()

        val toSuspend = restrictedNow.toTypedArray()
        val toUnsuspend = (explicitUnsuspend + (previousRestricted - restrictedNow)).toTypedArray()

        if (toSuspend.isNotEmpty()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // Call the richer overload via reflection when available on this API/SDK level.
                    try {
                        val suspendDialogInfoClass = Class.forName("android.content.pm.SuspendDialogInfo")
                        val builderClass = Class.forName("android.content.pm.SuspendDialogInfo\$Builder")
                        val builder = builderClass.getDeclaredConstructor().newInstance()
                        builderClass.getMethod("setTitle", String::class.java)
                            .invoke(builder, "App Restricted")
                        builderClass.getMethod("setMessage", String::class.java)
                            .invoke(builder, "This app has been restricted by your IT administrator. Contact your admin for access.")
                        val dialogInfo = builderClass.getMethod("build").invoke(builder)

                        val method = DevicePolicyManager::class.java.getMethod(
                            "setPackagesSuspended",
                            android.content.ComponentName::class.java,
                            Array<String>::class.java,
                            Boolean::class.javaPrimitiveType,
                            android.os.PersistableBundle::class.java,
                            android.os.PersistableBundle::class.java,
                            suspendDialogInfoClass
                        )
                        method.invoke(dpm, adminComponent, toSuspend, true, null, null, dialogInfo)
                    } catch (_: Exception) {
                        dpm.setPackagesSuspended(adminComponent, toSuspend, true)
                    }
                } else {
                    dpm.setPackagesSuspended(adminComponent, toSuspend, true)
                }
                Log.d(TAG, "Suspended ${toSuspend.size} app(s)")
            } catch (e: Exception) {
                Log.e(TAG, "Suspend failed: ${e.message}")
            }
        }

        if (toUnsuspend.isNotEmpty()) {
            try {
                dpm.setPackagesSuspended(adminComponent, toUnsuspend, false)
                Log.d(TAG, "Unsuspended ${toUnsuspend.size} app(s)")
            } catch (e: Exception) {
                Log.e(TAG, "Unsuspend failed: ${e.message}")
            }
        }

        restrictionPrefs.edit().putStringSet("restricted_packages", restrictedNow).apply()
    }

    private suspend fun enforcePolicies(
        deviceId: String,
        dpm: DevicePolicyManager,
        admin: android.content.ComponentName
    ) {
        if (!dpm.isDeviceOwnerApp(applicationContext.packageName)) {
            Log.w(TAG, "Not device owner, skipping policy enforcement")
            return
        }

        val response = RetrofitClient.api.getDevicePolicies(deviceId)
        if (!response.isSuccessful) return

        val policy = response.body() ?: return

        // Camera
        dpm.setCameraDisabled(admin, policy.cameraDisabled)
        Log.d(TAG, "Camera disabled: ${policy.cameraDisabled}")

        // Install/uninstall restrictions
        if (policy.installBlocked) {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_INSTALL_APPS)
        } else {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_INSTALL_APPS)
        }

        if (policy.uninstallBlocked) {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_UNINSTALL_APPS)
        } else {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_UNINSTALL_APPS)
        }

        Log.d(TAG, "Install blocked: ${policy.installBlocked}, Uninstall blocked: ${policy.uninstallBlocked}")
    }

    private suspend fun executePendingCommands(
        deviceId: String,
        dpm: DevicePolicyManager,
        admin: android.content.ComponentName,
        isDeviceOwner: Boolean
    ) {
        val response = RetrofitClient.api.getPendingCommands(deviceId)
        if (!response.isSuccessful) return

        val commands = response.body() ?: emptyList()
        for (cmd in commands) {
            val commandType = cmd.commandType ?: ""
            when (commandType) {
                "LOCK" -> {
                    dpm.lockNow()
                    Log.d(TAG, "Executed LOCK command ${cmd.id}")
                }
                "WIPE" -> {
                    if (!isDeviceOwner) {
                        Log.w(TAG, "Skipping WIPE command ${cmd.id}: requires Device Owner")
                        continue
                    }
                    // Acknowledge before wiping since device resets
                    try {
                        RetrofitClient.api.ackCommand(deviceId, cmd.id)
                    } catch (_: Exception) { }
                    dpm.wipeData(0)
                    return // Device is wiping, no further commands
                }
                "FORCE_PASSWORD_RESET" -> {
                    dpm.setPasswordQuality(admin, DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC)
                    dpm.setPasswordMinimumLength(admin, 6)
                    dpm.lockNow()
                    Log.d(TAG, "Executed FORCE_PASSWORD_RESET command ${cmd.id}")
                }
                "FORCE_SYNC" -> {
                    SyncWorker.schedule(applicationContext)
                    DeviceInfoSyncWorker.schedule(applicationContext)
                    LocationSyncWorker.schedule(applicationContext)
                    Log.d(TAG, "Executed FORCE_SYNC command ${cmd.id}")
                }
                "REQUEST_LOCATION" -> {
                    LocationSyncWorker.scheduleNow(applicationContext)
                    Log.d(TAG, "Executed REQUEST_LOCATION command ${cmd.id}")
                }
                "CAMERA_DISABLE" -> {
                    dpm.setCameraDisabled(admin, true)
                    Log.d(TAG, "Executed CAMERA_DISABLE command ${cmd.id}")
                }
                "CAMERA_ENABLE" -> {
                    dpm.setCameraDisabled(admin, false)
                    Log.d(TAG, "Executed CAMERA_ENABLE command ${cmd.id}")
                }
                else -> {
                    if (commandType.startsWith("CLEAR_APP_DATA:")) {
                        if (!isDeviceOwner) {
                            Log.w(TAG, "Skipping CLEAR_APP_DATA command ${cmd.id}: requires Device Owner")
                            continue
                        }
                        val packageName = commandType.removePrefix("CLEAR_APP_DATA:").trim()
                        if (packageName.isNotBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            dpm.clearApplicationUserData(
                                admin,
                                packageName,
                                applicationContext.mainExecutor
                            ) { _, success ->
                                Log.d(TAG, "CLEAR_APP_DATA for $packageName success=$success")
                            }
                            Log.d(TAG, "Executed CLEAR_APP_DATA command ${cmd.id} for $packageName")
                        } else {
                            Log.w(TAG, "Cannot execute CLEAR_APP_DATA for command ${cmd.id}")
                        }
                    }
                }
            }
            // Acknowledge command execution
            try {
                RetrofitClient.api.ackCommand(deviceId, cmd.id)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to ack command ${cmd.id}: ${e.message}")
            }
        }
    }
}

package com.devora.devicemanager.enrollment

import android.content.Context
import android.util.Log
import com.devora.devicemanager.collector.DeviceInfoCollector
import com.devora.devicemanager.collector.AppInventoryCollector
import com.devora.devicemanager.network.DeviceInfoRequest
import com.devora.devicemanager.network.EnrollRequest
import com.devora.devicemanager.network.EnrollResponse
import com.devora.devicemanager.network.EnrollmentApiService
import com.devora.devicemanager.network.RetrofitClient
import com.devora.devicemanager.network.model.AppInfoDto
import com.devora.devicemanager.network.model.BulkAppInventoryRequest
import com.devora.devicemanager.session.SessionManager
import com.devora.devicemanager.sync.PolicySyncWorker

/**
 * Enrollment states tracked during the enrollment flow.
 */
enum class EnrollmentStatus {
    IDLE,
    PENDING,
    VALIDATING_TOKEN,
    CONNECTING,
    INSTALLING_POLICIES,
    CONFIGURING_DEVICE_OWNER,
    UPLOADING_DEVICE_INFO,
    FINALIZING,
    SUCCESS,
    FAILED
}

/**
 * Holds the result of a completed enrollment.
 */
data class EnrollmentResult(
    val deviceId: String,
    val enrolledAt: String?,
    val status: String?,
    val method: String?,
    val employeeName: String? = null,
    val employeeId: String? = null,
    val errorMessage: String? = null
)

/**
 * Repository handling all enrollment server communication and validation.
 *
 * Orchestrates the multi-step enrollment flow:
 *  1. Validate the enrollment token format
 *  2. Send enrollment request to the backend
 *  3. Upload device hardware info
 *  4. Upload installed app inventory
 *  5. Persist enrollment state locally
 */
class EnrollmentRepository(
    private val context: Context,
    private val api: EnrollmentApiService = RetrofitClient.api
) {
    companion object {
        private const val TAG = "EnrollmentRepository"
        private const val PREFS_NAME = "devora_enrollment"
    }

    /**
     * Validates token format: must match DEV-XXXX-XXXX-XXXX (16 alphanumeric characters).
     */
    fun validateTokenFormat(token: String): Boolean {
        // DEV-XXXX-XXXX-XXXX = 18 chars total (3 prefix + 3 dashes + 12 body)
        return Regex("^DEV-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$")
            .matches(token.trim().uppercase())
    }

    /**
     * Executes the full enrollment flow, calling [onStepChanged] for each step.
     *
     * @param token         The enrollment token (DEV-XXXX-XXXX-XXXX)
     * @param method        "QR_CODE" or "TOKEN"
     * @param onStepChanged Called each time the enrollment step advances
     * @return [EnrollmentResult] on success, or an [EnrollmentResult] with errorMessage on failure
     */
    suspend fun enroll(
        token: String,
        method: String,
        onStepChanged: (EnrollmentStatus) -> Unit
    ): EnrollmentResult {
        val deviceInfo = DeviceInfoCollector.collect(context)
        val deviceId = deviceInfo.deviceId

        try {
            // Recovery path: if backend already has this device as enrolled/active,
            // restore local enrollment state and continue without re-enrollment.
            recoverEnrollmentFromServerIfNeeded(deviceId)?.let { recovered ->
                onStepChanged(EnrollmentStatus.FINALIZING)
                onStepChanged(EnrollmentStatus.SUCCESS)
                return recovered
            }

            // Step 1 — Validate token
            onStepChanged(EnrollmentStatus.VALIDATING_TOKEN)
            if (!validateTokenFormat(token)) {
                return EnrollmentResult(
                    deviceId = deviceId,
                    enrolledAt = null,
                    status = "FAILED",
                    method = method,
                    errorMessage = "Invalid token format. Expected DEV-XXXX-XXXX-XXXX"
                )
            }

            // Step 2 — Connect to server & enroll
            onStepChanged(EnrollmentStatus.CONNECTING)
            val enrollResponse = api.enrollDevice(
                EnrollRequest(
                    deviceId = deviceId,
                    enrollmentToken = token,
                    enrollmentMethod = method
                )
            )

            if (!enrollResponse.isSuccessful) {
                val errorBody = enrollResponse.errorBody()?.string()
                Log.e(TAG, "Enrollment failed: ${enrollResponse.code()} — $errorBody")
                return EnrollmentResult(
                    deviceId = deviceId,
                    enrolledAt = null,
                    status = "FAILED",
                    method = method,
                    errorMessage = "Server rejected enrollment (${enrollResponse.code()})"
                )
            }

            val enrollData = enrollResponse.body()

            // Persist immediately after successful enroll API so app restarts don't lose state.
            persistEnrollmentState(
                deviceId = deviceId,
                token = token,
                method = method,
                employeeId = enrollData?.employeeId,
                employeeName = enrollData?.employeeName
            )

            // Step 3 — Upload device info
            onStepChanged(EnrollmentStatus.INSTALLING_POLICIES)
            try {
                api.uploadDeviceInfo(
                    DeviceInfoRequest(
                        deviceId = deviceId,
                        model = deviceInfo.model,
                        manufacturer = deviceInfo.manufacturer,
                        osVersion = deviceInfo.osVersion,
                        sdkVersion = deviceInfo.sdkVersion,
                        serialNumber = deviceInfo.serialNumber,
                        imei = deviceInfo.imei,
                        deviceType = deviceInfo.deviceType,
                        deviceOwnerSet = deviceInfo.deviceOwnerSet,
                        employeeId = enrollData?.employeeId
                    )
                )
            } catch (oom: OutOfMemoryError) {
                Log.e(TAG, "Device info upload OOM (non-fatal)", oom)
            } catch (e: Exception) {
                Log.w(TAG, "Device info upload failed (non-fatal): ${e.message}")
            }

            // Step 4 — Upload app inventory (bulk)
            onStepChanged(EnrollmentStatus.CONFIGURING_DEVICE_OWNER)
            try {
                // During enrollment, skip icon payloads to avoid memory spikes.
                val apps = AppInventoryCollector.collect(context, includeIcons = false)
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
                val bulkRequest = BulkAppInventoryRequest(
                    deviceId = deviceId,
                    apps = appDtos
                )
                com.devora.devicemanager.network.ApiConfig.syncApi.uploadAppInventory(bulkRequest)
            } catch (oom: OutOfMemoryError) {
                Log.e(TAG, "App inventory upload OOM (non-fatal)", oom)
            } catch (e: Exception) {
                Log.w(TAG, "App inventory upload failed (non-fatal): ${e.message}")
            }

            // Step 5 — Finalize
            onStepChanged(EnrollmentStatus.FINALIZING)

            onStepChanged(EnrollmentStatus.SUCCESS)
            return EnrollmentResult(
                deviceId = deviceId,
                enrolledAt = enrollData?.enrolledAt,
                status = enrollData?.status ?: "ENROLLED",
                method = method,
                employeeName = enrollData?.employeeName,
                employeeId = enrollData?.employeeId
            )

        } catch (e: Exception) {
            Log.e(TAG, "Enrollment failed with exception", e)
            onStepChanged(EnrollmentStatus.FAILED)
            return EnrollmentResult(
                deviceId = deviceId,
                enrolledAt = null,
                status = "FAILED",
                method = method,
                errorMessage = e.message ?: "Unknown enrollment error"
            )
        }
    }

    suspend fun recoverEnrollmentFromServerIfNeeded(deviceId: String? = null): EnrollmentResult? {
        if (SessionManager.isForceReEnroll(context)) return null
        if (isEnrolled()) return null

        val resolvedDeviceId = deviceId ?: DeviceInfoCollector.collect(context).deviceId
        return try {
            val response = api.checkDevice(resolvedDeviceId)
            if (!response.isSuccessful) return null

            val device = response.body() ?: return null
            val status = (device.status ?: "").uppercase()
            val isAlreadyEnrolled = status == "ACTIVE" || status == "ENROLLED" || status == "ONLINE" || status == "OFFLINE"
            if (!isAlreadyEnrolled) return null

            val recoveredMethod = device.enrollmentMethod ?: "RECOVERED"
            val recoveredToken = getStoredToken() ?: "RECOVERED"

            persistEnrollmentState(
                deviceId = resolvedDeviceId,
                token = recoveredToken,
                method = recoveredMethod,
                employeeId = device.employeeId,
                employeeName = device.employeeName
            )

            EnrollmentResult(
                deviceId = resolvedDeviceId,
                enrolledAt = device.enrolledAt,
                status = device.status,
                method = recoveredMethod,
                employeeName = device.employeeName,
                employeeId = device.employeeId
            )
        } catch (e: Exception) {
            Log.d(TAG, "Recovery check skipped: ${e.message}")
            null
        }
    }

    /**
     * Checks whether this device has already been enrolled.
     */
    fun isEnrolled(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("is_enrolled", false)
    }

    /**
     * Returns the stored enrollment token, if any.
     */
    fun getStoredToken(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("enrollment_token", null)
    }

    /**
     * Clears saved enrollment state (for testing / re-enrollment).
     */
    fun clearEnrollmentState() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    /**
     * Deletes a device from the backend
     * Removes all associated employee data and enrollment tokens
     * Device must re-enroll from step 1
     */
    suspend fun deleteDevice(deviceId: String): Boolean {
        return try {
            val response = api.deleteDevice(deviceId)
            if (response.isSuccessful) {
                Log.i(TAG, "Device deleted successfully: $deviceId")
                // If this is the current device, clear enrollment state
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val storedDeviceId = prefs.getString("device_id", null)
                if (storedDeviceId == deviceId) {
                    clearEnrollmentState()
                }
                true
            } else {
                Log.e(TAG, "Failed to delete device: ${response.code()} — ${response.errorBody()?.string()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Device deletion failed with exception", e)
            false
        }
    }

    private fun persistEnrollmentState(
        deviceId: String,
        token: String,
        method: String,
        employeeId: String? = null,
        employeeName: String? = null
    ) {
        val committed = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_enrolled", true)
            .putString("device_id", deviceId)
            .putString("enrollment_token", token)
            .putString("enrollment_method", method)
            .putString("employee_id", employeeId)
            .putString("employee_name", employeeName)
            .putLong("enrolled_at", System.currentTimeMillis())
            .commit()

        if (!committed) {
            Log.w(TAG, "Enrollment state commit returned false for deviceId=$deviceId")
        }

        SessionManager.setForceReEnroll(context, false)
        SessionManager.setEmployeeSignedOut(context, false)

        // Ensure remote commands (e.g., LOCK) are polled immediately after enrollment.
        PolicySyncWorker.scheduleNow(context)
    }
}

package com.devora.devicemanager.collector

import com.devora.devicemanager.AdminReceiver
import android.content.Context
import android.os.Build
import java.time.Instant
import java.util.UUID

/**
 * Collection status for each device attribute.
 */
enum class FieldStatus { AVAILABLE, RESTRICTED, UNAVAILABLE }

/**
 * Holds device-level metadata with collection status for each field.
 */
data class DeviceInfo(
    val deviceId: String,           // App-scoped UUID persisted in SharedPreferences
    val model: String,              // Build.MODEL
    val manufacturer: String,       // Build.MANUFACTURER
    val brand: String,              // Build.BRAND
    val board: String,              // Build.BOARD
    val osVersion: String,          // Build.VERSION.RELEASE
    val sdkVersion: Int,            // Build.VERSION.SDK_INT
    val serialNumber: String?,      // Build.getSerial() — restricted on API 29+
    val imei: String?,              // TelephonyManager.getImei() — restricted on API 29+
    val deviceType: String,         // PHONE / TABLET / DEDICATED / UNKNOWN
    val deviceOwnerSet: Boolean,    // DevicePolicyManager.isDeviceOwnerApp(packageName)
    val serialStatus: FieldStatus,
    val imeiStatus: FieldStatus,
    val serialRestricted: Boolean,
    val imeiRestricted: Boolean,
    val collectedAt: String         // ISO-8601 timestamp
)

object DeviceInfoCollector {

    private const val PREFS_NAME = "device_manager_prefs"
    private const val KEY_DEVICE_UUID = "device_uuid"

    /**
     * Collects comprehensive device information including restricted identifiers.
     * Uses DeviceIdentifierStrategy for serial/IMEI with proper fallbacks.
     */
    fun collect(context: Context): DeviceInfo {
        val deviceId = getOrCreateDeviceId(context)
        val identifiers = DeviceIdentifierStrategy.collect(context)
        val deviceType = DeviceTypeClassifier.classify(context)
        val isDeviceOwnerSet = AdminReceiver.isDeviceOwner(context)

        return DeviceInfo(
            deviceId = deviceId,
            model = Build.MODEL.orEmpty(),
            manufacturer = Build.MANUFACTURER.orEmpty(),
            brand = Build.BRAND.orEmpty(),
            board = Build.BOARD.orEmpty(),
            osVersion = Build.VERSION.RELEASE.orEmpty(),
            sdkVersion = Build.VERSION.SDK_INT,
            serialNumber = identifiers.serialNumber,
            imei = identifiers.imei,
            deviceType = deviceType.name,
            deviceOwnerSet = isDeviceOwnerSet,
            serialStatus = when {
                identifiers.serialNumber != null -> FieldStatus.AVAILABLE
                identifiers.serialRestricted -> FieldStatus.RESTRICTED
                else -> FieldStatus.UNAVAILABLE
            },
            imeiStatus = when {
                identifiers.imei != null -> FieldStatus.AVAILABLE
                identifiers.imeiRestricted -> FieldStatus.RESTRICTED
                else -> FieldStatus.UNAVAILABLE
            },
            serialRestricted = identifiers.serialRestricted,
            imeiRestricted = identifiers.imeiRestricted,
            collectedAt = Instant.now().toString()
        )
    }

    /**
     * Returns a persistent UUID for this app installation.
     */
    private fun getOrCreateDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEVICE_UUID, null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_UUID, newId).apply()
            newId
        }
    }
}

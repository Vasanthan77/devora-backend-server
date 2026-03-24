package com.devora.devicemanager.collector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.devora.devicemanager.AdminReceiver
import java.util.UUID

/**
 * Strategy pattern for collecting device identifiers.
 * Handles all Android API restrictions and fallbacks for serial, IMEI, and UUID.
 */
object DeviceIdentifierStrategy {

    private const val TAG = "DeviceIdStrategy"
    private const val PREFS_NAME = "device_manager_prefs"
    private const val KEY_DEVICE_UUID = "device_uuid"

    data class IdentifierResult(
        val serialNumber: String?,
        val imei: String?,
        val uniqueId: String,
        val serialRestricted: Boolean,
        val imeiRestricted: Boolean
    )

    fun collect(context: Context): IdentifierResult {
        val serial = collectSerial(context)
        val imei = collectImei(context)
        val uuid = getOrCreateUuid(context)

        return IdentifierResult(
            serialNumber = serial.value,
            imei = imei.value,
            uniqueId = uuid,
            serialRestricted = serial.restricted,
            imeiRestricted = imei.restricted
        )
    }

    // ── Serial Number ──────────────────────────────────────────

    private data class FieldResult(val value: String?, val restricted: Boolean)

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun collectSerial(context: Context): FieldResult {
        // Device Owner can always read serial on API 26+
        if (isDeviceOwner(context)) {
            return try {
                val serial = Build.getSerial()
                if (serial != null && serial != Build.UNKNOWN) {
                    Log.d(TAG, "Serial obtained via Device Owner: $serial")
                    FieldResult(serial, restricted = false)
                } else {
                    FieldResult(null, restricted = false)
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Serial restricted even with Device Owner", e)
                FieldResult(null, restricted = true)
            }
        }

        // Non-Device Owner: need READ_PHONE_STATE permission (pre-API 29)
        // On API 29+ non-Device Owner apps cannot read serial at all
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d(TAG, "Serial restricted on API 29+ without Device Owner")
            return FieldResult(null, restricted = true)
        }

        // API 26-28: try with permission
        return if (hasReadPhoneStatePermission(context)) {
            try {
                @Suppress("DEPRECATION")
                val serial = Build.getSerial()
                if (serial != null && serial != Build.UNKNOWN) {
                    FieldResult(serial, restricted = false)
                } else {
                    FieldResult(null, restricted = false)
                }
            } catch (e: SecurityException) {
                FieldResult(null, restricted = true)
            }
        } else {
            FieldResult(null, restricted = true)
        }
    }

    // ── IMEI ───────────────────────────────────────────────────

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun collectImei(context: Context): FieldResult {
        // IMEI requires API 26+ and Device Owner on API 29+
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return FieldResult(null, restricted = false) // No telephony (e.g., Wi-Fi tablet)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isDeviceOwner(context)) {
            Log.d(TAG, "IMEI restricted on API 29+ without Device Owner")
            return FieldResult(null, restricted = true)
        }

        if (!hasReadPhoneStatePermission(context)) {
            return FieldResult(null, restricted = true)
        }

        return try {
            val imei = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tm.getImei(0) // Slot 0 only
            } else {
                @Suppress("DEPRECATION")
                tm.deviceId
            }
            if (imei != null) {
                Log.d(TAG, "IMEI obtained successfully")
                FieldResult(imei, restricted = false)
            } else {
                FieldResult(null, restricted = false)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "IMEI access denied", e)
            FieldResult(null, restricted = true)
        }
    }

    // ── UUID ───────────────────────────────────────────────────

    private fun getOrCreateUuid(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEVICE_UUID, null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_UUID, newId).apply()
            newId
        }
    }

    // ── Helpers ────────────────────────────────────────────────

    private fun isDeviceOwner(context: Context): Boolean {
        return AdminReceiver.isDeviceOwner(context)
    }

    private fun hasReadPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

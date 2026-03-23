package com.devora.devicemanager.enrollment

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import com.devora.devicemanager.AdminReceiver

/**
 * Helper for Device Policy Manager operations after enrollment.
 *
 * Provides a safe API layer over [DevicePolicyManager] that:
 *  - Checks Device Owner status before every call
 *  - Handles API level differences (26 vs 28 vs 33+)
 *  - Logs all policy changes for audit trail
 */
class DevicePolicyHelper(private val context: Context) {

    companion object {
        private const val TAG = "DevicePolicyHelper"
    }

    private val dpm: DevicePolicyManager
        get() = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val adminComponent: ComponentName
        get() = AdminReceiver.getComponentName(context)

    /** Whether this app is the Device Owner. */
    val isDeviceOwner: Boolean
        get() = dpm.isDeviceOwnerApp(context.packageName)

    /** Whether this app is an active device administrator. */
    val isAdminActive: Boolean
        get() = dpm.isAdminActive(adminComponent)

    // ─────────────────────────────────────
    // Lock task (kiosk) mode
    // ─────────────────────────────────────

    /**
     * Adds packages to the lock task allowlist.
     * Only permitted when this app is the Device Owner.
     */
    fun setLockTaskPackages(packages: Array<String>): Boolean {
        if (!isDeviceOwner) {
            Log.w(TAG, "setLockTaskPackages: not Device Owner")
            return false
        }
        return try {
            dpm.setLockTaskPackages(adminComponent, packages)
            Log.d(TAG, "Lock task packages set: ${packages.joinToString()}")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to set lock task packages", e)
            false
        }
    }

    /**
     * Returns the current lock task allowlisted packages.
     */
    fun getLockTaskPackages(): Array<String> {
        return if (isDeviceOwner) {
            try {
                dpm.getLockTaskPackages(adminComponent)
            } catch (e: SecurityException) {
                emptyArray()
            }
        } else emptyArray()
    }

    // ─────────────────────────────────────
    // Status bar
    // ─────────────────────────────────────

    /**
     * Enables or disables the status bar (notification shade pull-down).
     * Requires Device Owner on API 23+.
     */
    fun setStatusBarDisabled(disabled: Boolean): Boolean {
        if (!isDeviceOwner) return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dpm.setStatusBarDisabled(adminComponent, disabled)
                Log.d(TAG, "Status bar disabled: $disabled")
                true
            } else false
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to set status bar state", e)
            false
        }
    }

    // ─────────────────────────────────────
    // Keyguard features
    // ─────────────────────────────────────

    /**
     * Disables specified keyguard features on the lock screen.
     *
     * @param features Bitmask of [DevicePolicyManager.KEYGUARD_DISABLE_*] flags.
     *                 Use 0 to re-enable all features.
     */
    fun setKeyguardDisabledFeatures(features: Int): Boolean {
        if (!isAdminActive) return false
        return try {
            dpm.setKeyguardDisabledFeatures(adminComponent, features)
            Log.d(TAG, "Keyguard disabled features set: $features")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to set keyguard features", e)
            false
        }
    }

    // ─────────────────────────────────────
    // Password policies
    // ─────────────────────────────────────

    /**
     * Sets the minimum password quality and length.
     *
     * @param quality One of the [DevicePolicyManager.PASSWORD_QUALITY_*] constants
     * @param minLength Minimum password length (only effective if quality > UNSPECIFIED)
     */
    @Suppress("DEPRECATION")
    fun setPasswordPolicy(quality: Int, minLength: Int): Boolean {
        if (!isAdminActive) return false
        return try {
            dpm.setPasswordQuality(adminComponent, quality)
            dpm.setPasswordMinimumLength(adminComponent, minLength)
            Log.d(TAG, "Password policy: quality=$quality, minLength=$minLength")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to set password policy", e)
            false
        }
    }

    // ─────────────────────────────────────
    // Camera
    // ─────────────────────────────────────

    /**
     * Enables or disables all cameras on the device.
     */
    fun setCameraDisabled(disabled: Boolean): Boolean {
        if (!isAdminActive) return false
        return try {
            dpm.setCameraDisabled(adminComponent, disabled)
            Log.d(TAG, "Camera disabled: $disabled")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to set camera policy", e)
            false
        }
    }

    // ─────────────────────────────────────
    // Remote lock & wipe
    // ─────────────────────────────────────

    /**
     * Immediately locks the device screen.
     */
    fun lockDevice(): Boolean {
        if (!isAdminActive) return false
        return try {
            dpm.lockNow()
            Log.d(TAG, "Device locked")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to lock device", e)
            false
        }
    }

    /**
     * Factory-resets the device. THIS IS DESTRUCTIVE AND IRREVERSIBLE.
     *
     * @param reason A short audit string explaining why the wipe was initiated.
     */
    fun wipeDevice(reason: String): Boolean {
        if (!isDeviceOwner) {
            Log.w(TAG, "wipeDevice: not Device Owner — cannot wipe")
            return false
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dpm.wipeData(0, reason)
            } else {
                @Suppress("DEPRECATION")
                dpm.wipeData(0)
            }
            Log.d(TAG, "Device wipe initiated: $reason")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to wipe device", e)
            false
        }
    }

    // ─────────────────────────────────────
    // Owner info on lock screen
    // ─────────────────────────────────────

    /**
     * Sets the owner information message displayed on the lock screen.
     */
    fun setDeviceOwnerLockScreenInfo(info: String): Boolean {
        if (!isDeviceOwner) return false
        return try {
            dpm.setDeviceOwnerLockScreenInfo(adminComponent, info)
            Log.d(TAG, "Lock screen info set: $info")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to set lock screen info", e)
            false
        }
    }

    // ─────────────────────────────────────
    // Enrollment status queries
    // ─────────────────────────────────────

    /**
     * Returns a summary of the current device management state.
     */
    fun getManagementStatus(): Map<String, Any> = buildMap {
        put("isDeviceOwner", isDeviceOwner)
        put("isAdminActive", isAdminActive)
        put("lockTaskPackages", getLockTaskPackages().toList())
        put("passwordSufficient", try { dpm.isActivePasswordSufficient } catch (_: Exception) { false })
        put("encryptionStatus", dpm.storageEncryptionStatus)
    }
}

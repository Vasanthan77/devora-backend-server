package com.devora.devicemanager.enrollment

import android.content.Context
import com.devora.devicemanager.AdminReceiver

/**
 * AMAPI migration facade retained to keep existing call sites stable.
 *
 * Local DevicePolicyManager operations are intentionally disabled.
 */
class DevicePolicyHelper(private val context: Context) {

    /** Whether this device is AMAPI-managed in local app state. */
    val isDeviceOwner: Boolean
        get() = AdminReceiver.isDeviceOwner(context)

    /** Legacy compatibility flag. */
    val isAdminActive: Boolean
        get() = false

    fun setLockTaskPackages(packages: Array<String>): Boolean {
        return false
    }

    fun getLockTaskPackages(): Array<String> {
        return emptyArray()
    }

    fun setStatusBarDisabled(disabled: Boolean): Boolean {
        return false
    }

    fun setKeyguardDisabledFeatures(features: Int): Boolean {
        return false
    }

    fun setPasswordPolicy(quality: Int, minLength: Int): Boolean {
        return false
    }

    fun setCameraDisabled(disabled: Boolean): Boolean {
        return false
    }

    fun lockDevice(): Boolean {
        return false
    }

    fun wipeDevice(reason: String): Boolean {
        return false
    }

    fun setDeviceOwnerLockScreenInfo(info: String): Boolean {
        return false
    }

    fun getManagementStatus(): Map<String, Any> = buildMap {
        put("isAmapiManaged", isDeviceOwner)
        put("isAdminActive", isAdminActive)
        put("managementMode", "AMAPI")
    }
}

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

    /** Local lock-screen text is no longer managed by DPM in AMAPI mode. */
    fun setDeviceOwnerLockScreenInfo(info: String): Boolean {
        return false
    }

    fun getManagementStatus(): Map<String, Any> = buildMap {
        put("isAmapiManaged", isDeviceOwner)
        put("isAdminActive", false)
        put("managementMode", "AMAPI")
    }
}

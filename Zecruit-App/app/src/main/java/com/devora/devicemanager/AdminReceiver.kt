package com.devora.devicemanager

import android.content.ComponentName
import android.content.Context

/**
 * Legacy compatibility wrapper after migrating from local DPM to AMAPI.
 *
 * This object keeps old call-sites compiling while all management is now cloud-driven.
 */
object AdminReceiver {

    private const val PREFS_NAME = "devora_enrollment"
    private const val KEY_DEVICE_ID = "device_id"

    /**
     * Legacy API retained for compatibility. No local admin component is used anymore.
     */
    fun getComponentName(context: Context): ComponentName {
        return ComponentName(context.applicationContext, MainActivity::class.java)
    }

    /**
     * Returns true when the app has completed AMAPI-backed enrollment state locally.
     */
    fun isDeviceOwner(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_DEVICE_ID, null).isNullOrBlank()
    }
}

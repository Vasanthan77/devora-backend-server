package com.devora.devicemanager.collector

import android.content.Context
import android.content.res.Configuration

/**
 * Classifies the device type based on screen size and configuration.
 */
object DeviceTypeClassifier {

    enum class DeviceType { PHONE, TABLET, DEDICATED, UNKNOWN }

    fun classify(context: Context): DeviceType {
        val config = context.resources.configuration
        val screenLayout = config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK

        // Check if device is in lock task mode (dedicated/kiosk device)
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        if (am != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (am.lockTaskModeState != android.app.ActivityManager.LOCK_TASK_MODE_NONE) {
                return DeviceType.DEDICATED
            }
        }

        return when {
            screenLayout >= Configuration.SCREENLAYOUT_SIZE_XLARGE -> DeviceType.TABLET
            screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE -> DeviceType.TABLET
            else -> DeviceType.PHONE
        }
    }
}

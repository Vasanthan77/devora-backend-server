package com.devora.devicemanager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.devora.devicemanager.sync.HeartbeatService

/**
 * Reschedules command polling after reboot/app update so remote actions keep working
 * even before the user manually opens the app.
 */
class DeviceBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DeviceBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val prefs = context.getSharedPreferences("devora_enrollment", Context.MODE_PRIVATE)
        val deviceId = prefs.getString("device_id", null)

        if (deviceId.isNullOrBlank()) {
            Log.d(TAG, "Ignoring $action: device not enrolled")
            return
        }

        Log.d(TAG, "Received $action, starting heartbeat/command service")
        HeartbeatService.start(context)
    }
}

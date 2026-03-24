package com.devora.devicemanager.sync

import android.content.Context
import android.util.Log
import com.devora.devicemanager.network.RetrofitClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Optional realtime command channel.
 *
 * Backend can send data messages with keys:
 * - commandType: REQUEST_LOCATION | LOCK | WIPE_DATA | ...
 * - commandId: Long (optional)
 * - deviceId: String (optional)
 */
class DeviceCommandFirebaseService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "DeviceCmdFCM"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        // Token upload endpoint can be added server-side if direct push targeting is required.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val commandType = data["commandType"]?.uppercase().orEmpty()
        val commandId = data["commandId"]?.toLongOrNull()
        val deviceIdFromPayload = data["deviceId"]

        if (commandType.isBlank()) {
            return
        }

        val prefs = getSharedPreferences("devora_enrollment", Context.MODE_PRIVATE)
        val deviceId = deviceIdFromPayload ?: prefs.getString("device_id", null)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (commandType) {
                    "REQUEST_LOCATION" -> {
                        // Ensure heartbeat service is active to process location and command queue quickly.
                        HeartbeatService.start(this@DeviceCommandFirebaseService)
                    }
                    "LOCK", "WIPE_DATA" -> {
                        // AMAPI executes these server-side on the managed device policy layer.
                    }
                    else -> {
                        Log.d(TAG, "Unhandled FCM commandType=$commandType")
                    }
                }

                if (deviceId != null && commandId != null) {
                    runCatching {
                        RetrofitClient.api.ackCommand(deviceId, commandId)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to process FCM command: ${e.message}")
            }
        }
    }
}

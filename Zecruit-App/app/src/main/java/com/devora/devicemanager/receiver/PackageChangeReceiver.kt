package com.devora.devicemanager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.devora.devicemanager.network.NewAppNotification
import com.devora.devicemanager.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Monitors package install/update events on the employee device
 * and notifies the backend so the admin is alerted in real-time.
 */
class PackageChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PackageChangeReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return

        // Ignore our own package
        if (packageName == context.packageName) return

        val action = when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) "UPDATED"
                else "INSTALLED"
            }
            Intent.ACTION_PACKAGE_REPLACED -> "UPDATED"
            else -> return
        }

        Log.d(TAG, "Package $action: $packageName")

        // Get app info
        val pm = context.packageManager
        val appName: String
        val versionName: String
        val versionCode: Long
        val isSystemApp: Boolean

        try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            appName = appInfo.loadLabel(pm).toString()
            isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            versionName = pkgInfo.versionName ?: ""
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Package not found after install: $packageName")
            return
        }

        // Get device ID from SharedPreferences
        val prefs = context.getSharedPreferences("enrollment_prefs", Context.MODE_PRIVATE)
        val deviceId = prefs.getString("device_id", null)
        if (deviceId == null) {
            Log.w(TAG, "Device not enrolled, skipping notification")
            return
        }

        // Notify the backend
        val notification = NewAppNotification(
            deviceId = deviceId,
            appName = appName,
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            isSystemApp = isSystemApp,
            action = action
        )

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.notifyNewApp(notification)
                if (response.isSuccessful) {
                    Log.d(TAG, "Notified backend: $action $appName")
                } else {
                    Log.e(TAG, "Notification failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to notify backend: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}

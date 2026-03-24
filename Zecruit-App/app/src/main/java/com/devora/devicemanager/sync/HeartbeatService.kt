package com.devora.devicemanager.sync

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.devora.devicemanager.BlockedAppActivity
import com.devora.devicemanager.network.LocationReportRequest
import com.devora.devicemanager.network.RetrofitClient
import com.devora.devicemanager.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service that:
 *  1. Sends heartbeat every 60 seconds
 *  2. Fetches restricted apps and enforces them (Device Owner: hide, else: block via UsageStats)
 *  3. Monitors foreground app every 2 seconds to block restricted apps (UsageStats fallback)
 */
class HeartbeatService : Service() {

    companion object {
        private const val TAG = "HeartbeatService"
        private const val CHANNEL_ID = "devora_heartbeat_channel"
        private const val NOTIFICATION_ID = 2001
        private const val HEARTBEAT_INTERVAL_MS = 60_000L
        private const val MONITOR_INTERVAL_MS = 2_000L
        private const val COMMAND_POLL_INTERVAL_MS = 3_000L

        fun start(context: Context) {
            val intent = Intent(context, HeartbeatService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, HeartbeatService::class.java))
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var heartbeatJob: Job? = null
    private var monitorJob: Job? = null
    private var commandJob: Job? = null

    // Cached restricted packages: packageName -> appName
    private val restrictedAppsCache = mutableMapOf<String, String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Devora MDM")
            .setContentText("Device monitoring active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        heartbeatJob?.cancel()
        monitorJob?.cancel()
        commandJob?.cancel()

        val prefs = getSharedPreferences("devora_enrollment", Context.MODE_PRIVATE)

        // Job 1: Heartbeat + restriction fetch every 60s
        heartbeatJob = serviceScope.launch {
            while (true) {
                val deviceId = prefs.getString("device_id", null)
                if (deviceId != null) {
                    val isSignedOut = SessionManager.isEmployeeSignedOut(this@HeartbeatService)
                    try {
                        if (isSignedOut) {
                            Log.d(TAG, "Employee signed out; skipping heartbeat for $deviceId")
                        } else {
                            val response = RetrofitClient.api.sendHeartbeat(deviceId)
                            if (response.isSuccessful) {
                                Log.d(TAG, "Heartbeat sent for $deviceId")
                            } else {
                                Log.w(TAG, "Heartbeat failed for $deviceId with HTTP ${response.code()}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Heartbeat failed: ${e.message}")
                    }

                    // Fetch and enforce restrictions
                    enforceAppRestrictions(deviceId)
                }
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }

        // Job 2: Monitor foreground app every 2s (UsageStats fallback)
        monitorJob = serviceScope.launch {
            while (true) {
                delay(MONITOR_INTERVAL_MS)
                monitorForegroundApp()
            }
        }

        // Job 3: Poll pending commands for near-real-time command execution.
        commandJob = serviceScope.launch {
            while (true) {
                val deviceId = prefs.getString("device_id", null)
                if (deviceId != null) {
                    processPendingCommands(deviceId)
                }
                delay(COMMAND_POLL_INTERVAL_MS)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        heartbeatJob?.cancel()
        monitorJob?.cancel()
        commandJob?.cancel()
        super.onDestroy()
    }

    private suspend fun processPendingCommands(deviceId: String) {
        try {
            val response = RetrofitClient.api.getPendingCommands(deviceId)
            if (!response.isSuccessful) {
                return
            }

            val pending = response.body().orEmpty()
            if (pending.isEmpty()) {
                return
            }

            for (command in pending) {
                val commandType = command.commandType?.uppercase().orEmpty()
                val handled = when (commandType) {
                    "REQUEST_LOCATION" -> handleLocationCommand(deviceId)
                    "LOCK", "WIPE_DATA" -> true
                    else -> false
                }

                if (handled) {
                    runCatching {
                        RetrofitClient.api.ackCommand(deviceId, command.id)
                    }.onFailure {
                        Log.w(TAG, "Failed to ack command ${command.id}: ${it.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Pending command processing failed: ${e.message}")
        }
    }

    private suspend fun handleLocationCommand(deviceId: String): Boolean {
        val location = getLastKnownLocation() ?: return false
        return try {
            val request = LocationReportRequest(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                altitude = if (location.hasAltitude()) location.altitude else null,
                bearing = if (location.hasBearing()) location.bearing else null,
                speed = if (location.hasSpeed()) location.speed else null
            )
            val response = RetrofitClient.api.reportLocation(deviceId, request)
            response.isSuccessful
        } catch (e: Exception) {
            Log.w(TAG, "REQUEST_LOCATION failed: ${e.message}")
            false
        }
    }

    private fun getLastKnownLocation(): Location? {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            return null
        }

        return try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )

            providers
                .mapNotNull { provider ->
                    runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
                }
                .maxByOrNull { it.time }
        } catch (e: Exception) {
            Log.w(TAG, "Location fetch failed: ${e.message}")
            null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Device Monitoring",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps device monitoring active in background"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    /**
     * Fetches restricted apps from backend.
     * Fetches cloud restrictions and updates the local cache for UsageStats monitoring.
     */
    private suspend fun enforceAppRestrictions(deviceId: String) {
        try {
            val response = RetrofitClient.api.getAllAppRestrictions(deviceId)
            if (!response.isSuccessful) return

            val restrictions = response.body() ?: emptyList()
            val newRestricted = mutableMapOf<String, String>()

            for (r in restrictions) {
                if (r.restricted) {
                    newRestricted[r.packageName] = r.appName ?: r.packageName
                }
            }
            Log.d(TAG, "AMAPI restrictions cached: ${newRestricted.size} app(s)")

            // Update cache for the monitoring loop
            synchronized(restrictedAppsCache) {
                restrictedAppsCache.clear()
                restrictedAppsCache.putAll(newRestricted)
            }
        } catch (e: Exception) {
            Log.w(TAG, "App restriction enforcement failed: ${e.message}")
        }
    }

    /**
     * Checks the current foreground app via UsageStatsManager.
     * If it's a restricted app, launches the BlockedAppActivity to cover it.
     */
    private fun monitorForegroundApp() {
        val cached: Map<String, String>
        synchronized(restrictedAppsCache) {
            if (restrictedAppsCache.isEmpty()) return
            cached = restrictedAppsCache.toMap()
        }

        if (!hasUsageStatsPermission()) return

        val foregroundPkg = getForegroundPackage() ?: return
        if (foregroundPkg == packageName) return // Don't block ourselves
        if (foregroundPkg == "com.devora.devicemanager") return

        val appName = cached[foregroundPkg]
        if (appName != null) {
            Log.d(TAG, "Restricted app in foreground: $foregroundPkg — blocking")
            BlockedAppActivity.launch(this@HeartbeatService, appName, foregroundPkg)
        }
    }

    private fun getForegroundPackage(): String? {
        return try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                now - 5_000,
                now
            )
            stats?.maxByOrNull { it.lastTimeUsed }?.packageName
        } catch (e: Exception) {
            Log.d(TAG, "UsageStats query failed: ${e.message}")
            null
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }
}

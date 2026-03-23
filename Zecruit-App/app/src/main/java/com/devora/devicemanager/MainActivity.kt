package com.devora.devicemanager

import android.Manifest
import android.app.AppOpsManager
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devora.devicemanager.sync.SyncManager
import com.devora.devicemanager.sync.SyncWorker
import com.devora.devicemanager.sync.DeviceInfoSyncWorker
import com.devora.devicemanager.sync.PolicySyncWorker
import com.devora.devicemanager.sync.LocationSyncWorker
import com.devora.devicemanager.ui.navigation.AppNavigation
import com.devora.devicemanager.ui.theme.DevoraTheme
import com.devora.devicemanager.ui.theme.ThemeViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d("MainActivity", "POST_NOTIFICATIONS granted=$granted")
        }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val fineGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseGranted = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            Log.d("MainActivity", "Location permissions fine=$fineGranted, coarse=$coarseGranted")

            // Request background location separately when foreground location is available.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && (fineGranted || coarseGranted)) {
                requestBackgroundLocationIfNeeded()
            }
        }

    private val backgroundLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d("MainActivity", "ACCESS_BACKGROUND_LOCATION granted=$granted")
        }

    private val readPhoneStatePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d("MainActivity", "READ_PHONE_STATE granted=$granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // If launched after Device Owner provisioning, mark enrollment as pending
        if (intent?.getBooleanExtra("enrollment_complete", false) == true) {
            Log.d("MainActivity", "Launched after Device Owner provisioning — enrollment pending")
        }

        // Schedule background sync (15-min interval via WorkManager)
        SyncWorker.schedule(this)
        DeviceInfoSyncWorker.schedule(this)
        PolicySyncWorker.schedule(this)
        LocationSyncWorker.schedule(this)

        // Start foreground heartbeat service — sends heartbeat every 30s so the
        // backend can detect uninstall within ~30–90 seconds.
        com.devora.devicemanager.sync.HeartbeatService.start(this)

        // Request runtime permissions required by optional platform APIs.
        requestNotificationPermissionIfNeeded()
        requestLocationPermissionsIfNeeded()
        requestReadPhoneStateIfNeeded()

        // Prompt for Usage Access permission (needed for app restriction enforcement
        // when Device Owner is not available)
        requestUsageStatsPermissionIfNeeded()

        setContent {
            val themeVm: ThemeViewModel = viewModel()
            val isDark = themeVm.isDark

            // Sync status: null = not started, "syncing" / "success" / "failed:reason"
            var syncStatus by remember { mutableStateOf<String?>(null) }

            // Trigger sync on first composition if Device Owner
            LaunchedEffect(Unit) {
                val isDeviceOwner = AdminReceiver.isDeviceOwner(this@MainActivity)
                Log.d("SYNC", "Device Owner status: $isDeviceOwner")

                if (isDeviceOwner) {
                    syncStatus = "syncing"
                    val result = SyncManager.syncDeviceData(
                        context = applicationContext,
                        employeeId = getStoredEmployeeId()
                    )
                    syncStatus = if (result.success) "success" else "failed:${result.message}"
                }
            }

            // Show terminal sync status briefly, then hide banner.
            LaunchedEffect(syncStatus) {
                val status = syncStatus ?: return@LaunchedEffect
                if (status == "success" || status.startsWith("failed")) {
                    delay(3500)
                    if (syncStatus == status) {
                        syncStatus = null
                    }
                }
            }

            DevoraTheme(isDark = isDark) {
                Box {
                    // Main app navigation
                    AppNavigation(
                        isDark = isDark,
                        onThemeToggle = themeVm::toggle
                    )

                    // Sync status banner at the top
                    syncStatus?.let { status ->
                        val (text, bgColor) = when {
                            status == "syncing" -> "Syncing..." to Color(0xFF1976D2)
                            status == "success" -> "Synced" to Color(0xFF388E3C)
                            status.startsWith("failed") -> {
                                val reason = status.removePrefix("failed:")
                                "Sync Failed: $reason" to Color(0xFFD32F2F)
                            }
                            else -> return@let
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 48.dp)
                                .background(
                                    color = bgColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                                .align(Alignment.TopCenter)
                        ) {
                            Text(
                                text = text,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Retrieves the stored employee ID from SharedPreferences.
     * Falls back to "unknown" if not yet set.
     */
    private fun getStoredEmployeeId(): String {
        val prefs = getSharedPreferences("devora_enrollment", MODE_PRIVATE)
        return prefs.getString("employee_id", "unknown") ?: "unknown"
    }

    private fun requestUsageStatsPermissionIfNeeded() {
        // Only prompt on employee devices (enrolled)
        val prefs = getSharedPreferences("devora_enrollment", MODE_PRIVATE)
        val isEnrolled = prefs.getString("device_id", null) != null

        if (!isEnrolled) return

        // Check if Device Owner — if so, no need for UsageStats
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        if (dpm.isDeviceOwnerApp(packageName)) return

        // Check if already granted
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        if (mode == AppOpsManager.MODE_ALLOWED) return

        // Open Usage Access settings
        try {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            Log.w("MainActivity", "Could not open Usage Access settings")
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun requestLocationPermissionsIfNeeded() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestBackgroundLocationIfNeeded()
        }
    }

    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val bgGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!bgGranted) {
            backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    private fun requestReadPhoneStateIfNeeded() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        val isDeviceOwner = dpm.isDeviceOwnerApp(packageName)

        // On API 29+, non-Device Owner apps cannot access IMEI/serial anyway.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isDeviceOwner) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            readPhoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
    }
}

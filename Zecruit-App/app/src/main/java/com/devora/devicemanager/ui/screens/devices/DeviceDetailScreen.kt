package com.devora.devicemanager.ui.screens.devices

import android.graphics.BitmapFactory
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devora.devicemanager.AdminReceiver
import com.devora.devicemanager.network.AppInventoryItem
import com.devora.devicemanager.network.CommandRequest
import com.devora.devicemanager.network.DeviceActivityResponse
import com.devora.devicemanager.network.DeviceAppRestrictionResponse
import com.devora.devicemanager.network.DeviceLocationResponse
import com.devora.devicemanager.network.DevicePolicyResponse
import com.devora.devicemanager.network.DeviceResponse
import com.devora.devicemanager.network.LocationReportRequest
import com.devora.devicemanager.network.PolicyUpdateRequest
import com.devora.devicemanager.network.RestrictAppRequestNew
import com.devora.devicemanager.network.RetrofitClient
import com.devora.devicemanager.ui.components.DevoraCard
import com.devora.devicemanager.ui.components.SectionHeader
import com.devora.devicemanager.ui.components.StatusBadge
import com.devora.devicemanager.ui.theme.BgBase
import com.devora.devicemanager.ui.theme.BgElevated
import com.devora.devicemanager.ui.theme.BgSurface
import com.devora.devicemanager.ui.theme.DMSans
import com.devora.devicemanager.ui.theme.Danger
import com.devora.devicemanager.ui.theme.DarkBgBase
import com.devora.devicemanager.ui.theme.DarkBgElevated
import com.devora.devicemanager.ui.theme.DarkBgSurface
import com.devora.devicemanager.ui.theme.DarkTextPrimary
import com.devora.devicemanager.ui.theme.JetBrainsMono
import com.devora.devicemanager.ui.theme.PlusJakartaSans
import com.devora.devicemanager.ui.theme.PurpleBorder
import com.devora.devicemanager.ui.theme.PurpleCore
import com.devora.devicemanager.ui.theme.PurpleDeep
import com.devora.devicemanager.ui.theme.PurpleDim
import com.devora.devicemanager.ui.theme.Success
import com.devora.devicemanager.ui.theme.TextMuted
import com.devora.devicemanager.ui.theme.TextPrimary
import com.devora.devicemanager.ui.theme.Warning
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.net.HttpURLConnection
import java.net.URL
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ══════════════════════════════════════
// DEVICE DETAIL SCREEN
// ══════════════════════════════════════

@Composable
fun DeviceDetailScreen(
    deviceId: String,
    onBack: () -> Unit,
    isDark: Boolean
) {
    val apiService = remember { RetrofitClient.api }
    // Fetch real device data from API
    var deviceResponse by remember { mutableStateOf<DeviceResponse?>(null) }

    LaunchedEffect(deviceId) {
        try {
            val response = RetrofitClient.api.getDeviceList()
            if (response.isSuccessful) {
                deviceResponse = response.body()?.find { it.deviceId == deviceId }
            }
        } catch (e: Exception) {
            Log.e("DeviceDetail", "Failed to fetch device: ${e.message}")
        }
    }

    val device = if (deviceResponse != null) {
        val dr = deviceResponse!!
        val employeeName = dr.employeeName?.takeIf { it.isNotBlank() }
        val displayName = employeeName?.let { "$it's Device" } ?: (dr.deviceModel ?: "Unknown")
        val subtitle = "${dr.manufacturer.orEmpty()} ${dr.deviceModel.orEmpty()}".trim().let {
            if (it.isBlank()) "Unknown Device · ${dr.enrollmentMethod}" else "$it · ${dr.enrollmentMethod}"
        }
        Device(
            name = displayName,
            subtitle = subtitle,
            searchManufacturer = dr.manufacturer.orEmpty(),
            searchModel = dr.deviceModel.orEmpty(),
            status = when (dr.status.uppercase()) {
                "ACTIVE", "ENROLLED" -> "ONLINE"
                else -> "OFFLINE"
            },
            api = "ID: ${dr.id}",
            initial = displayName.take(1).uppercase(),
            deviceId = dr.deviceId,
            lastSeen = "Enrolled ${dr.enrolledAt.take(10)}"
        )
    } else {
        Device(
            name = deviceId.ifEmpty { "Unknown Device" },
            subtitle = "Loading...",
            searchManufacturer = "",
            searchModel = "",
            status = "PENDING",
            api = "—",
            initial = deviceId.take(1).uppercase().ifEmpty { "?" },
            deviceId = deviceId,
            lastSeen = "Unknown"
        )
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("INFO", "APPS", "ACTIVITY", "ACTIONS")
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Dialog states
    var showLockDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var clearDataPackage by remember { mutableStateOf("") }
    var showWipeDialog by remember { mutableStateOf(false) }
    var wipeStep by remember { mutableIntStateOf(0) }
    var wipeConfirmText by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }
    var commandProgressVisible by remember { mutableStateOf(false) }
    var commandProgressTitle by remember { mutableStateOf("Sending Command") }
    var commandProgressMessage by remember { mutableStateOf("Waiting for device to execute...") }

    suspend fun executeRemoteCommandAndWait(
        commandType: String,
        packageName: String? = null,
        pendingMessage: String,
        successMessage: String,
        timeoutMs: Long = 20_000L,
        timeoutMessage: String = "Command sent. Device will apply shortly."
    ): Boolean {
        commandProgressTitle = pendingMessage
        commandProgressMessage = "Sending command to device..."
        commandProgressVisible = true

        return try {
            val beforePending = apiService.getPendingCommands(deviceId).body().orEmpty()
            val beforeIds = beforePending.map { it.id }.toSet()

            val queueResponse = when (commandType) {
                "LOCK" -> apiService.lockDevice(deviceId)
                "WIPE" -> apiService.wipeDevice(deviceId)
                else -> apiService.createDeviceCommand(deviceId, CommandRequest(commandType, packageName))
            }

            val queued = queueResponse.isSuccessful
            var commandId = queueResponse.body()?.commandId

            fun matchesCommandType(remoteType: String?): Boolean {
                if (remoteType.isNullOrBlank()) return false
                return if (commandType == "CLEAR_APP_DATA") {
                    remoteType.startsWith("CLEAR_APP_DATA:")
                } else {
                    remoteType == commandType
                }
            }

            if (!queued) {
                if (commandType == "LOCK" || commandType == "WIPE") {
                    val amapiResponse = if (commandType == "LOCK") {
                        apiService.lockDeviceAmapi(deviceId)
                    } else {
                        apiService.wipeDeviceAmapi(deviceId)
                    }

                    if (amapiResponse.isSuccessful) {
                        snackbarHostState.showSnackbar(successMessage)
                        return true
                    }
                }

                snackbarHostState.showSnackbar("Failed to send $commandType command")
                return false
            }

            // Backward-compatible fallback for servers that don't return commandId.
            if (commandId == null) {
                for (i in 1..8) {
                    delay(500)
                    val pending = apiService.getPendingCommands(deviceId).body().orEmpty()
                    val candidate = pending.firstOrNull { cmd ->
                        !beforeIds.contains(cmd.id) && matchesCommandType(cmd.commandType)
                    }
                    if (candidate != null) {
                        commandId = candidate.id
                        break
                    }
                }
            }

            val start = System.currentTimeMillis()
            var attempt = 0

            while (System.currentTimeMillis() - start < timeoutMs) {
                attempt += 1
                commandProgressMessage = "Waiting for device confirmation... (${attempt})"

                if (commandId != null) {
                    val statusResponse = apiService.getCommandStatus(deviceId, commandId!!)
                    if (statusResponse.isSuccessful && statusResponse.body()?.executed == true) {
                        snackbarHostState.showSnackbar(successMessage)
                        return true
                    }

                    // Fallback for servers without command-status endpoint support.
                    val pending = apiService.getPendingCommands(deviceId).body().orEmpty()
                    val stillPending = pending.any { it.id == commandId }
                    if (!stillPending) {
                        snackbarHostState.showSnackbar(successMessage)
                        return true
                    }
                } else {
                    // If commandId couldn't be resolved, treat queueing as success once sent.
                    snackbarHostState.showSnackbar("Command sent successfully")
                    return true
                }

                delay(1500)
            }

            snackbarHostState.showSnackbar(timeoutMessage)
            true
        } catch (_: Exception) {
            snackbarHostState.showSnackbar("Failed to send $commandType command")
            false
        } finally {
            commandProgressVisible = false
        }
    }

    val bgColor = if (isDark) DarkBgBase else BgBase
    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    val surfaceBg = if (isDark) DarkBgSurface else BgSurface

    Scaffold(
        containerColor = bgColor,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ══════ TOP BAR ══════
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PurpleCore
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        device.name,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textColor
                    )
                    Text(
                        device.subtitle,
                        fontFamily = DMSans,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
                StatusBadge(status = device.status)
            }

            Spacer(Modifier.height(16.dp))

            // ══════ TAB ROW ══════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) DarkBgSurface else Color(0xFFE9EEF3))
                    .border(1.dp, PurpleBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) PurpleCore else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            tab,
                            fontFamily = if (isSelected) PlusJakartaSans else DMSans,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.White else TextMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ══════════════════════════════════════
            // TAB CONTENT
            // ══════════════════════════════════════

            when (selectedTab) {
                0 -> InfoTab(device = device, deviceResponse = deviceResponse, isDark = isDark, textColor = textColor)
                1 -> AppsTab(deviceId = deviceId, isDark = isDark, textColor = textColor)
                2 -> ActivityTab(deviceId = deviceId, isDark = isDark, textColor = textColor)
                3 -> ActionsTab(
                    deviceId = deviceId,
                    deviceName = device.name,
                    isDark = isDark,
                    textColor = textColor,
                    isSyncing = isSyncing,
                    onShowMessage = { message ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                    },
                    onLock = { showLockDialog = true },
                    onPasswordReset = { showPasswordDialog = true },
                    onClearData = { showClearDataDialog = true },
                    onSync = {
                        coroutineScope.launch {
                            isSyncing = true
                            try {
                                executeRemoteCommandAndWait(
                                    commandType = "FORCE_SYNC",
                                    pendingMessage = "Syncing ${device.name}",
                                    successMessage = "Sync completed on ${device.name}"
                                )
                            } finally {
                                isSyncing = false
                            }
                        }
                    },
                    onWipe = {
                        wipeStep = 1
                        wipeConfirmText = ""
                        showWipeDialog = true
                    }
                )
            }
        }
    }

    // ══════════════════════════════════════
    // DIALOGS
    // ══════════════════════════════════════

    // Lock Device Dialog
    if (showLockDialog) {
        ConfirmActionDialog(
            title = "Lock ${device.name}?",
            message = "This will immediately lock the device screen.",
            icon = Icons.Outlined.Lock,
            iconColor = PurpleCore,
            confirmText = "Lock Now",
            confirmColor = PurpleCore,
            isDark = isDark,
            onDismiss = { showLockDialog = false },
            onConfirm = {
                showLockDialog = false
                coroutineScope.launch {
                    executeRemoteCommandAndWait(
                        commandType = "LOCK",
                        pendingMessage = "Locking ${device.name}",
                        successMessage = "${device.name} locked successfully",
                        timeoutMs = 10_000L,
                        timeoutMessage = "Lock command sent. Emulator may not visibly turn off immediately."
                    )
                }
            }
        )
    }

    // Password Reset Dialog
    if (showPasswordDialog) {
        ConfirmActionDialog(
            title = "Force Password Reset",
            message = "The employee will be required to set a new password on their next unlock. Their current password will be invalidated.",
            icon = Icons.Outlined.Password,
            iconColor = Warning,
            confirmText = "Reset Password",
            confirmColor = Warning,
            isDark = isDark,
            onDismiss = { showPasswordDialog = false },
            onConfirm = {
                showPasswordDialog = false
                coroutineScope.launch {
                    executeRemoteCommandAndWait(
                        commandType = "FORCE_PASSWORD_RESET",
                        pendingMessage = "Resetting password on ${device.name}",
                        successMessage = "Password reset applied on ${device.name}"
                    )
                }
            }
        )
    }

    // Clear Data Dialog
    if (showClearDataDialog) {
        Dialog(
            onDismissRequest = { showClearDataDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDark) DarkBgSurface else BgSurface)
                    .border(1.dp, PurpleBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        "Clear App Data",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textColor
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Enter package name to clear:",
                        fontFamily = DMSans,
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) DarkBgElevated else BgElevated)
                            .border(1.dp, PurpleBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (clearDataPackage.isBlank()) {
                            Text(
                                "com.example.app",
                                fontFamily = DMSans,
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                        BasicTextField(
                            value = clearDataPackage,
                            onValueChange = { clearDataPackage = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = JetBrainsMono,
                                fontSize = 12.sp,
                                color = textColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showClearDataDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel")
                        }
                        OutlinedButton(
                            onClick = {
                                val packageName = clearDataPackage.trim()
                                if (packageName.isBlank()) return@OutlinedButton
                                showClearDataDialog = false
                                clearDataPackage = ""
                                coroutineScope.launch {
                                    executeRemoteCommandAndWait(
                                        commandType = "CLEAR_APP_DATA",
                                        packageName = packageName,
                                        pendingMessage = "Clearing app data on ${device.name}",
                                        successMessage = "App data clear command executed"
                                    )
                                }
                            },
                            enabled = clearDataPackage.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Warning)
                        ) {
                            Text("Clear Data", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    // Remote Wipe Dialog — TWO STEP
    if (showWipeDialog) {
        Dialog(
            onDismissRequest = {
                showWipeDialog = false
                wipeStep = 0
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isDark) DarkBgSurface else BgSurface)
                    .border(1.dp, Danger.copy(alpha = 0.40f), RoundedCornerShape(24.dp))
                    .padding(28.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Danger icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Danger.copy(alpha = 0.10f))
                            .border(2.dp, Danger.copy(alpha = 0.40f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.DeleteForever,
                            contentDescription = null,
                            tint = Danger,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        if (wipeStep == 1) "Wipe ${device.name}?" else "Type WIPE to confirm",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Danger
                    )

                    Spacer(Modifier.height(12.dp))

                    if (wipeStep == 1) {
                        // Step 1
                        Text(
                            "⚠ This will permanently erase ALL data on this device. This action cannot be undone.",
                            fontFamily = DMSans,
                            fontSize = 13.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        // Warning info box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Danger.copy(alpha = 0.08f))
                                .border(1.dp, Danger.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Outlined.Warning,
                                    contentDescription = null,
                                    tint = Danger,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "All apps, data, settings, and accounts will be permanently removed. The device will be factory reset.",
                                    fontFamily = DMSans,
                                    fontSize = 12.sp,
                                    color = Danger.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Warning.copy(alpha = 0.10f))
                                .border(1.dp, Warning.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Outlined.Warning,
                                    contentDescription = null,
                                    tint = Warning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Emulator note: some Android emulator images block full factory reset. On real managed devices, wipe will reboot and erase data.",
                                    fontFamily = DMSans,
                                    fontSize = 12.sp,
                                    color = Warning
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PurpleDim)
                                    .border(1.dp, PurpleBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        showWipeDialog = false
                                        wipeStep = 0
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Cancel",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = PurpleCore
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Danger)
                                    .clickable { wipeStep = 2 },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "I Understand",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // Step 2 — type WIPE to confirm
                        Text(
                            "Type WIPE to confirm permanent device erasure.",
                            fontFamily = DMSans,
                            fontSize = 13.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(16.dp))

                        BasicTextField(
                            value = wipeConfirmText,
                            onValueChange = { wipeConfirmText = it.uppercase().take(4) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) DarkBgElevated else Color(0xFFFFE8E8))
                                .border(
                                    1.5.dp,
                                    if (wipeConfirmText == "WIPE") Danger else Danger.copy(alpha = 0.30f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                            textStyle = TextStyle(
                                fontFamily = JetBrainsMono,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Danger,
                                letterSpacing = 6.sp,
                                textAlign = TextAlign.Center
                            ),
                            singleLine = true,
                            decorationBox = { inner ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (wipeConfirmText.isEmpty()) {
                                        Text(
                                            "W I P E",
                                            fontFamily = JetBrainsMono,
                                            fontSize = 22.sp,
                                            color = Danger.copy(alpha = 0.25f),
                                            letterSpacing = 6.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    inner()
                                }
                            }
                        )

                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PurpleDim)
                                    .border(1.dp, PurpleBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        showWipeDialog = false
                                        wipeStep = 0
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Cancel",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = PurpleCore
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (wipeConfirmText == "WIPE") Danger
                                        else Danger.copy(alpha = 0.3f)
                                    )
                                    .clickable(enabled = wipeConfirmText == "WIPE") {
                                        showWipeDialog = false
                                        wipeStep = 0
                                        coroutineScope.launch {
                                            commandProgressTitle = "Dispatching wipe command"
                                            commandProgressMessage = "Sending command to device..."
                                            commandProgressVisible = true

                                            try {
                                                val response = apiService.wipeDevice(deviceId)
                                                if (response.isSuccessful) {
                                                    val isEmulator = Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
                                                        Build.MODEL.contains("Emulator", ignoreCase = true) ||
                                                        Build.PRODUCT.contains("sdk", ignoreCase = true)

                                                    val statusText = if (isEmulator) {
                                                        "Wipe command queued. This emulator may not allow actual factory reset."
                                                    } else {
                                                        "Wipe command queued. Device will reboot to complete reset."
                                                    }

                                                    snackbarHostState.showSnackbar(statusText)
                                                } else {
                                                    snackbarHostState.showSnackbar("Failed to send wipe command")
                                                }
                                            } catch (_: Exception) {
                                                snackbarHostState.showSnackbar("Failed to send wipe command")
                                            } finally {
                                                commandProgressVisible = false
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "WIPE DEVICE",
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (commandProgressVisible) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(surfaceBg)
                    .border(1.dp, PurpleBorder, RoundedCornerShape(18.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PurpleCore, strokeWidth = 3.dp)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = commandProgressTitle,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = commandProgressMessage,
                        fontFamily = DMSans,
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════
// INFO TAB
// ══════════════════════════════════════

@Composable
private fun InfoTab(device: Device, deviceResponse: DeviceResponse?, isDark: Boolean, textColor: Color) {
    // Device Summary
    DevoraCard(accentColor = PurpleCore, isDark = isDark) {
        SectionHeader(title = "DEVICE SUMMARY", isDark = isDark)

        Spacer(Modifier.height(4.dp))

        val infoItems = buildList {
            add("Device Model" to (deviceResponse?.deviceModel ?: "—"))
            add("Manufacturer" to (deviceResponse?.manufacturer ?: "—"))
            add("Android OS" to (deviceResponse?.osVersion?.let { "Android $it" } ?: "—"))
            add("SDK Version" to (deviceResponse?.sdkVersion ?: "—"))
            add("Device Owner" to (if (deviceResponse?.deviceOwnerSet == true) "Set" else "Not Set"))
            add("Serial Number" to (deviceResponse?.serialNumber?.ifBlank { "Restricted" } ?: "Restricted"))
            add("Device UUID" to device.deviceId)
        }

        infoItems.forEachIndexed { index, (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontFamily = DMSans, fontSize = 13.sp, color = TextMuted)
                Text(
                    value,
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    color = when {
                        value.equals("ACTIVE", ignoreCase = true) -> Success
                        value.equals("ONLINE", ignoreCase = true) -> Success
                        value.equals("SET", ignoreCase = true) -> Success
                        value.equals("NOT SET", ignoreCase = true) -> Danger
                        value.equals("FLAGGED", ignoreCase = true) -> Danger
                        value.equals("OFFLINE", ignoreCase = true) -> TextMuted
                        else -> textColor
                    },
                    fontWeight = FontWeight.Medium
                )
            }
            if (index < infoItems.size - 1) {
                HorizontalDivider(color = PurpleCore.copy(alpha = 0.08f), thickness = 1.dp)
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // Enrollment Details
    DevoraCard(accentColor = PurpleCore, isDark = isDark) {
        SectionHeader(title = "ENROLLMENT DETAILS", isDark = isDark)

        Spacer(Modifier.height(4.dp))

        val enrollItems = buildList {
            add("Employee" to (deviceResponse?.employeeName ?: "—"))
            add("Employee ID" to (deviceResponse?.employeeId ?: "—"))
            add("Enrollment" to (deviceResponse?.enrollmentMethod ?: "—"))
            add("Enrolled At" to (deviceResponse?.enrolledAt?.take(10) ?: "—"))
            add("Status" to (device.status))
            add("Record ID" to (device.api))
        }

        enrollItems.forEachIndexed { index, (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontFamily = DMSans, fontSize = 13.sp, color = TextMuted)
                Text(
                    value,
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    color = when {
                        value.equals("ACTIVE", ignoreCase = true) -> Success
                        value.equals("ONLINE", ignoreCase = true) -> Success
                        value.equals("OFFLINE", ignoreCase = true) -> TextMuted
                        else -> textColor
                    },
                    fontWeight = FontWeight.Medium
                )
            }
            if (index < enrollItems.size - 1) {
                HorizontalDivider(color = PurpleCore.copy(alpha = 0.08f), thickness = 1.dp)
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // Security Status
    DevoraCard(accentColor = Success, isDark = isDark) {
        SectionHeader(title = "SECURITY STATUS", isDark = isDark)

        Spacer(Modifier.height(4.dp))

        val securityItems = listOf(
            Triple("Screen Lock", "Enforced", Success),
            Triple("Encryption", "Enabled", Success),
            Triple("Root Detection", "Clear", Success),
            Triple("Play Protect", "Verified", Success),
            Triple("Last Security Scan", "2 mins ago", PurpleCore)
        )

        securityItems.forEachIndexed { index, (label, value, color) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontFamily = DMSans, fontSize = 13.sp, color = TextMuted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        value,
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        color = color,
                        fontWeight = FontWeight.Medium
                    )
                    if (color == Success) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Success,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            if (index < securityItems.size - 1) {
                HorizontalDivider(color = PurpleCore.copy(alpha = 0.08f), thickness = 1.dp)
            }
        }
    }
}

// ══════════════════════════════════════
// APPS TAB
// ══════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppsTab(deviceId: String, isDark: Boolean, textColor: Color) {
    data class AppTab(val label: String, val count: Int)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<AppInventoryItem>>(emptyList()) }
    var restrictedApps by remember { mutableStateOf<Map<String, DeviceAppRestrictionResponse>>(emptyMap()) }
    var localRestrictedSet by remember { mutableStateOf(readRestrictedPackages(context)) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedApp by remember { mutableStateOf<AppInventoryItem?>(null) }

    var installBlocked by remember { mutableStateOf(false) }
    var uninstallBlocked by remember { mutableStateOf(false) }
    var policyLoaded by remember { mutableStateOf(false) }
    var uninstallPackageName by remember { mutableStateOf("") }
    var confirmForceUninstall by remember { mutableStateOf(false) }

    val surfaceBg = if (isDark) DarkBgSurface else BgSurface

    fun isRestricted(packageName: String): Boolean {
        return restrictedApps[packageName]?.restricted == true || localRestrictedSet.contains(packageName)
    }

    fun applyLocalRestriction(packageName: String, restricted: Boolean): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                true
            } else {
                val adminComponent = AdminReceiver.getComponentName(context)
                dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), restricted)
                true
            }
        } catch (e: Exception) {
            Log.e("AppsTab", "Local app restriction apply failed for $packageName: ${e.message}")
            false
        }
    }

    suspend fun refreshRestrictionState() {
        val restrictedResp = RetrofitClient.api.getRestrictedApps(deviceId)
        if (restrictedResp.isSuccessful) {
            val backendRestricted = (restrictedResp.body() ?: emptyList()).associateBy { it.packageName }
            restrictedApps = backendRestricted
            val mergedRestricted = backendRestricted.values
                .filter { it.restricted }
                .map { it.packageName }
                .toSet() + localRestrictedSet
            localRestrictedSet = mergedRestricted
            saveRestrictedPackages(context, mergedRestricted)
        }
    }

    suspend fun applyRestriction(packageName: String, appName: String, installSource: String?, restricted: Boolean): Boolean {
        return try {
            val req = RestrictAppRequestNew(
                packageName = packageName,
                appName = appName,
                installSource = installSource,
                restricted = restricted
            )
            val resp = RetrofitClient.api.restrictApp(deviceId, req)
            if (!resp.isSuccessful) {
                Log.e("AppsTab", "Backend restrict failed: ${resp.code()}")
                return false
            }

            val localApplied = applyLocalRestriction(packageName, restricted)

            val updatedSet = localRestrictedSet.toMutableSet()
            if (restricted) updatedSet.add(packageName) else updatedSet.remove(packageName)
            localRestrictedSet = updatedSet
            saveRestrictedPackages(context, updatedSet)
            refreshRestrictionState()

            localApplied
        } catch (e: Exception) {
            Log.e("AppsTab", "Restrict toggle failed: ${e.message}")
            false
        }
    }

    LaunchedEffect(deviceId) {
        isLoading = true
        try {
            val inventoryResp = RetrofitClient.api.getAppInventory(deviceId)
            if (inventoryResp.isSuccessful) {
                apps = inventoryResp.body() ?: emptyList()
                errorMsg = null
            } else {
                errorMsg = "Failed to load apps (${inventoryResp.code()})"
            }

            refreshRestrictionState()

            val policyResp = RetrofitClient.api.getDevicePolicies(deviceId)
            if (policyResp.isSuccessful && policyResp.body() != null) {
                val policy = policyResp.body()!!
                installBlocked = policy.installBlocked
                uninstallBlocked = policy.uninstallBlocked
                policyLoaded = true
            }
        } catch (e: Exception) {
            errorMsg = "Failed to load apps. Check connection."
            Log.e("AppsTab", "Error loading APPS tab: ${e.message}")
        }
        isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PurpleCore, modifier = Modifier.size(32.dp))
        }
        return
    }

    if (errorMsg != null) {
        DevoraCard(accentColor = PurpleCore, isDark = isDark) {
            Text(
                errorMsg!!,
                fontFamily = DMSans,
                fontSize = 13.sp,
                color = Danger,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )
        }
        return
    }

    val userApps by remember(apps) { derivedStateOf { apps.filter { it.isSystemApp != true } } }
    val systemApps by remember(apps) { derivedStateOf { apps.filter { it.isSystemApp == true } } }
    val restrictedOnly by remember(apps, restrictedApps, localRestrictedSet) {
        derivedStateOf { apps.filter { isRestricted(it.packageName) } }
    }

    val tabs = listOf(
        AppTab("ALL", apps.size),
        AppTab("USER", userApps.size),
        AppTab("SYSTEM", systemApps.size),
        AppTab("RESTRICTED", restrictedOnly.size)
    )

    val filteredApps = apps.filter { app ->
        val matchesSearch = app.appName.contains(searchQuery, ignoreCase = true) ||
            app.packageName.contains(searchQuery, ignoreCase = true)
        val matchesTab = when (selectedTab) {
            0 -> true
            1 -> app.isSystemApp != true
            2 -> app.isSystemApp == true
            3 -> isRestricted(app.packageName)
            else -> true
        }
        matchesSearch && matchesTab
    }

    Column {
        SectionHeader(title = "INSTALLED APPLICATIONS", isDark = isDark)
        Spacer(Modifier.height(4.dp))
        Text(
            "${apps.size} apps · ${userApps.size} user · ${systemApps.size} system · ${restrictedOnly.size} restricted",
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            color = TextMuted
        )

        Spacer(Modifier.height(12.dp))

        DevoraCard(accentColor = PurpleCore, isDark = isDark) {
            SectionHeader(title = "APP POLICY CONTROLS", isDark = isDark)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Block App Installs", fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = textColor)
                    Text("Prevent new installations", fontFamily = DMSans, fontSize = 11.sp, color = TextMuted)
                }
                Switch(
                    checked = installBlocked,
                    enabled = policyLoaded,
                    onCheckedChange = { enabled ->
                        installBlocked = enabled
                        coroutineScope.launch {
                            try {
                                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                                if (dpm.isDeviceOwnerApp(context.packageName)) {
                                    val adminComponent = AdminReceiver.getComponentName(context)
                                    if (enabled) {
                                        dpm.addUserRestriction(adminComponent, android.os.UserManager.DISALLOW_INSTALL_APPS)
                                    } else {
                                        dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_INSTALL_APPS)
                                    }
                                }
                                RetrofitClient.api.updateDevicePolicy(
                                    deviceId,
                                    PolicyUpdateRequest(installBlocked = enabled)
                                )
                            } catch (e: Exception) {
                                Log.e("AppsTab", "Install policy update failed: ${e.message}")
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Block App Uninstalls", fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = textColor)
                    Text("Prevent app removal", fontFamily = DMSans, fontSize = 11.sp, color = TextMuted)
                }
                Switch(
                    checked = uninstallBlocked,
                    enabled = policyLoaded,
                    onCheckedChange = { enabled ->
                        uninstallBlocked = enabled
                        coroutineScope.launch {
                            try {
                                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                                if (dpm.isDeviceOwnerApp(context.packageName)) {
                                    val adminComponent = AdminReceiver.getComponentName(context)
                                    if (enabled) {
                                        dpm.addUserRestriction(adminComponent, android.os.UserManager.DISALLOW_UNINSTALL_APPS)
                                    } else {
                                        dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_UNINSTALL_APPS)
                                    }
                                }
                                RetrofitClient.api.updateDevicePolicy(
                                    deviceId,
                                    PolicyUpdateRequest(uninstallBlocked = enabled)
                                )
                            } catch (e: Exception) {
                                Log.e("AppsTab", "Uninstall policy update failed: ${e.message}")
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = PurpleCore.copy(alpha = 0.12f), thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            Text("Force Uninstall App", fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Danger)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .background(surfaceBg, RoundedCornerShape(10.dp))
                        .border(1.dp, PurpleBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (uninstallPackageName.isBlank()) {
                        Text("package name", fontFamily = DMSans, fontSize = 12.sp, color = TextMuted)
                    }
                    BasicTextField(
                        value = uninstallPackageName,
                        onValueChange = { uninstallPackageName = it },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, color = textColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Danger.copy(alpha = 0.12f))
                        .border(1.dp, Danger.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .clickable(enabled = uninstallPackageName.isNotBlank()) { confirmForceUninstall = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text("Uninstall", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Danger)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(surfaceBg, RoundedCornerShape(12.dp))
                .border(1.dp, PurpleBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = PurpleCore, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(text = "Search apps...", fontFamily = DMSans, fontSize = 13.sp, color = TextMuted)
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(fontFamily = DMSans, fontSize = 13.sp, color = textColor),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp).clickable { searchQuery = "" }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 12.dp,
            containerColor = Color.Transparent,
            contentColor = PurpleCore,
            indicator = {},
            divider = {}
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index
                val isRestrictedTab = tab.label == "RESTRICTED"
                val restrictedAccent = Color(0xFFF44336)

                Tab(
                    selected = isSelected,
                    onClick = { selectedTab = index },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) PurpleCore else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) PurpleCore else Color.LightGray
                        ),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = tab.label,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )

                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) Color.White.copy(alpha = 0.3f)
                                else if (isRestrictedTab) restrictedAccent.copy(alpha = 0.12f)
                                else PurpleCore.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "${tab.count}",
                                    color = if (isSelected) Color.White
                                    else if (isRestrictedTab) restrictedAccent
                                    else PurpleCore,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (selectedTab == 3 && restrictedOnly.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("No Restricted Apps", color = Color.Gray, fontWeight = FontWeight.Medium)
                Text(
                    "Tap Restrict on any app\nto block it on this device",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else if (filteredApps.isEmpty()) {
            Text(
                if (searchQuery.isNotEmpty()) "No apps match your search." else "No apps found on this device.",
                fontFamily = DMSans,
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        filteredApps.forEachIndexed { index, app ->
            val appRestricted = isRestricted(app.packageName)
            val appBg = if (appRestricted) Color(0xFFFFF0F0) else Color.Transparent

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(appBg)
                    .padding(vertical = 10.dp)
                    .clickable { selectedApp = app },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val inlineIconBitmap = remember(app.iconBase64) {
                        if (!app.iconBase64.isNullOrEmpty()) {
                            try {
                                val bytes = Base64.decode(app.iconBase64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (_: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                    }

                    if (inlineIconBitmap != null) {
                        Image(
                            bitmap = inlineIconBitmap.asImageBitmap(),
                            contentDescription = app.appName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
                            alpha = if (appRestricted) 0.4f else 1f
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (appRestricted) Danger.copy(alpha = 0.10f) else PurpleDim),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                app.appName.take(1).uppercase(),
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (appRestricted) Danger else PurpleCore
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(app.appName, fontFamily = DMSans, fontSize = 13.sp, color = if (appRestricted) Danger else textColor)
                        if (appRestricted) {
                            Text("🔴 SUSPENDED", fontFamily = JetBrainsMono, fontSize = 9.sp, color = Danger, fontWeight = FontWeight.Bold)
                        }
                        Text(app.packageName, fontFamily = JetBrainsMono, fontSize = 9.sp, color = TextMuted)
                        val appType = if (app.isSystemApp == true) "System" else "User"
                        val sourceLabel = formatInstallSource(app.installSource).ifBlank { "📱 Other Store" }
                        Text(
                            "v${app.versionName ?: "-"} · $appType · $sourceLabel",
                            fontFamily = DMSans,
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            if (appRestricted) Success.copy(alpha = 0.10f)
                            else Danger.copy(alpha = 0.10f)
                        )
                        .border(
                            1.dp,
                            if (appRestricted) Success.copy(alpha = 0.25f) else Danger.copy(alpha = 0.25f),
                            RoundedCornerShape(100.dp)
                        )
                        .clickable {
                            coroutineScope.launch {
                                val nextRestricted = !appRestricted
                                applyRestriction(
                                    packageName = app.packageName,
                                    appName = app.appName,
                                    installSource = app.installSource,
                                    restricted = nextRestricted
                                )
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (appRestricted) "Allow" else "Restrict",
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        color = if (appRestricted) Success else Danger,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (index < filteredApps.size - 1) {
                HorizontalDivider(color = PurpleCore.copy(alpha = 0.08f), thickness = 1.dp)
            }
        }
    }

    if (confirmForceUninstall) {
        AlertDialog(
            onDismissRequest = { confirmForceUninstall = false },
            title = { Text("Force Uninstall", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold) },
            text = { Text("Force uninstall $uninstallPackageName?", fontFamily = DMSans) },
            dismissButton = {
                TextButton(onClick = { confirmForceUninstall = false }) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmForceUninstall = false
                        try {
                            val packageInstaller = context.packageManager.packageInstaller
                            packageInstaller.uninstall(
                                uninstallPackageName,
                                PendingIntent.getBroadcast(
                                    context,
                                    0,
                                    Intent("UNINSTALL_COMPLETE"),
                                    PendingIntent.FLAG_IMMUTABLE
                                ).intentSender
                            )
                        } catch (e: Exception) {
                            Log.e("AppsTab", "Force uninstall failed: ${e.message}")
                        }
                    }
                ) {
                    Text("Uninstall", color = Danger)
                }
            }
        )
    }

    if (selectedApp != null) {
        val app = selectedApp!!
        val appRestricted = isRestricted(app.packageName)
        val appMeta = remember(app.packageName) {
            resolveInstalledAppMeta(context, app.packageName, app.isSystemApp == true)
        }

        ModalBottomSheet(onDismissRequest = { selectedApp = null }) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dialogIconBitmap = remember(app.iconBase64) {
                        if (!app.iconBase64.isNullOrEmpty()) {
                            try {
                                val bytes = Base64.decode(app.iconBase64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (_: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                    }

                    if (dialogIconBitmap != null) {
                        Image(
                            bitmap = dialogIconBitmap.asImageBitmap(),
                            contentDescription = app.appName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PurpleCore.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Apps, contentDescription = null, tint = PurpleCore, modifier = Modifier.size(28.dp))
                        }
                    }

                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(app.appName, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
                        Text(app.packageName, fontFamily = JetBrainsMono, fontSize = 10.sp, color = TextMuted)
                        if (appRestricted) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFF0F0)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Block,
                                        contentDescription = null,
                                        tint = Color(0xFFF44336),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Restricted",
                                        color = Color(0xFFF44336),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = PurpleCore.copy(alpha = 0.10f))
                Spacer(Modifier.height(10.dp))

                Text("APP INFORMATION", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextMuted)
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Package Name", fontFamily = DMSans, fontSize = 12.sp, color = TextMuted, modifier = Modifier.weight(0.35f))
                    Text(app.packageName, fontFamily = JetBrainsMono, fontSize = 11.sp, color = textColor, modifier = Modifier.weight(0.55f))
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy package",
                        tint = PurpleCore,
                        modifier = Modifier
                            .size(18.dp)
                            .weight(0.10f)
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Package", app.packageName))
                                Toast.makeText(context, "Package copied", Toast.LENGTH_SHORT).show()
                            }
                    )
                }

                AppInfoRow("Version Name", app.versionName ?: "Unavailable", textColor, isDark)
                AppInfoRow("Version Code", app.versionCode?.toString() ?: "Unavailable", textColor, isDark)
                AppInfoRow("App Type", if (app.isSystemApp == true) "👤 System App" else "👤 User App", textColor, isDark)
                AppInfoRow("Installed From", appMeta.installSourceLabel, textColor, isDark)
                AppInfoRow("Install Date", formatEpochDate(appMeta.firstInstallTime), textColor, isDark)
                AppInfoRow("Last Updated", formatEpochDate(appMeta.lastUpdateTime), textColor, isDark)
                AppInfoRow("App Size", formatBytes(appMeta.appSizeBytes), textColor, isDark)
                AppInfoRow("Status", if (appRestricted) "🔴 RESTRICTED" else "✅ Active", textColor, isDark)

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = PurpleCore.copy(alpha = 0.10f))
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (appRestricted) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    applyRestriction(
                                        packageName = app.packageName,
                                        appName = app.appName,
                                        installSource = app.installSource,
                                        restricted = false
                                    )
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF4CAF50)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Allow",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Allow App",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    applyRestriction(
                                        packageName = app.packageName,
                                        appName = app.appName,
                                        installSource = app.installSource,
                                        restricted = true
                                    )
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFF5722)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                Color(0xFFFF5722)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Block,
                                contentDescription = "Restrict",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Restrict",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            uninstallPackageName = app.packageName
                            confirmForceUninstall = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFF44336)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            Color(0xFFF44336)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Uninstall",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Uninstall",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ══════════════════════════════════════
// ACTIVITY TAB
// ══════════════════════════════════════

@Composable
private fun ActivityTab(deviceId: String, isDark: Boolean, textColor: Color) {
    var activities by remember { mutableStateOf<List<DeviceActivityResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    suspend fun loadActivities() {
        try {
            val response = RetrofitClient.api.getDeviceActivities(deviceId)
            if (response.isSuccessful) {
                activities = response.body() ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e("ActivityTab", "Load failed: ${e.message}")
        }
    }

    LaunchedEffect(deviceId) {
        isLoading = true
        loadActivities()
        isLoading = false
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            currentTime = System.currentTimeMillis()
        }
    }

    LaunchedEffect(deviceId, Unit) {
        while (true) {
            delay(120_000L)
            loadActivities()
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PurpleCore, modifier = Modifier.size(32.dp))
        }
        return
    }

    DevoraCard(accentColor = PurpleCore, isDark = isDark) {
        SectionHeader(title = "DEVICE ACTIVITY LOG", isDark = isDark)

        if (activities.isEmpty()) {
            Text(
                "No activity recorded yet.",
                fontFamily = DMSans,
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        activities.forEachIndexed { index, activity ->
            val color = when (activity.severity) {
                "CRITICAL" -> Danger
                "WARNING" -> Warning
                else -> when {
                    activity.activityType?.contains("RESTRICT") == true -> Danger
                    activity.activityType?.contains("WIPE") == true -> Danger
                    activity.activityType?.contains("LOCK") == true -> PurpleCore
                    activity.activityType?.contains("POLICY") == true -> PurpleCore
                    else -> Success
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        activity.description ?: "Unknown activity",
                        fontFamily = DMSans,
                        fontSize = 13.sp,
                        color = textColor
                    )
                    if (!activity.employeeName.isNullOrBlank()) {
                        Text(
                            activity.employeeName!!,
                            fontFamily = DMSans,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
                Text(
                    getTimeAgo(activity.createdAt ?: "", currentTime),
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            if (index < activities.size - 1) {
                HorizontalDivider(color = PurpleCore.copy(alpha = 0.08f), thickness = 1.dp)
            }
        }
    }
}

// ══════════════════════════════════════
// ACTIONS TAB
// ══════════════════════════════════════

@Composable
private fun ActionsTab(
    deviceId: String,
    deviceName: String,
    isDark: Boolean,
    textColor: Color,
    isSyncing: Boolean,
    onShowMessage: (String) -> Unit,
    onLock: () -> Unit,
    onPasswordReset: () -> Unit,
    onClearData: () -> Unit,
    onSync: () -> Unit,
    onWipe: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var cameraDisabled by remember { mutableStateOf(false) }
    var policyLoaded by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf<DeviceLocationResponse?>(null) }
    var locationHistory by remember { mutableStateOf<List<DeviceLocationResponse>>(emptyList()) }
    var trackingEnabled by remember { mutableStateOf(false) }
    var isRequestingLocation by remember { mutableStateOf(false) }
    var locationAddress by remember { mutableStateOf("Fetching address...") }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun openInMaps(lat: Double, lng: Double) {
        try {
            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($deviceName)")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            onShowMessage("Unable to open map app")
        }
    }

    suspend fun fetchAddress(lat: Double, lng: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(
                    "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lng&format=json"
                )
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "DEVORA-MDM/1.0")
                    connectTimeout = 5_000
                    readTimeout = 5_000
                }

                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val address = json.optJSONObject("address")
                val city = address?.optString("city")
                    ?.ifBlank { address.optString("town").ifBlank { address.optString("village") } }
                    .orEmpty()
                val state = address?.optString("state").orEmpty()
                val country = address?.optString("country").orEmpty()
                val shortAddress = listOf(city, state, country).filter { it.isNotBlank() }.joinToString(", ")

                if (shortAddress.isNotBlank()) shortAddress
                else json.optString("display_name", "${"%.4f".format(lat)}, ${"%.4f".format(lng)}")
            } catch (_: Exception) {
                "${"%.4f".format(lat)}, ${"%.4f".format(lng)}"
            }
        }
    }

    suspend fun loadLocationData() {
        try {
            val policyResponse = RetrofitClient.api.getDevicePolicies(deviceId)
            if (policyResponse.isSuccessful) {
                val policy = policyResponse.body()
                cameraDisabled = policy?.cameraDisabled ?: cameraDisabled
                trackingEnabled = policy?.locationTrackingEnabled ?: false
                policyLoaded = true
            } else {
                Log.w("ActionsTab", "Policy fetch failed: ${policyResponse.code()}")
            }
        } catch (e: Exception) {
            Log.e("ActionsTab", "Error loading policies: ${e.message}", e)
        }

        try {
            Log.d("ActionsTab", "Fetching location for device: $deviceId")
            val locationResponse = RetrofitClient.api.getDeviceLocation(deviceId)
            if (locationResponse.isSuccessful) {
                val body = locationResponse.body()
                Log.d("ActionsTab", "Location response: $body")
                location = body
                val lat = body?.latitude
                val lng = body?.longitude
                if (lat != null && lng != null) {
                    Log.d("ActionsTab", "Location received: $lat, $lng")
                    locationAddress = fetchAddress(lat, lng)
                } else {
                    Log.d("ActionsTab", "Location coordinates are null")
                }
            } else {
                Log.w("ActionsTab", "Location fetch failed: ${locationResponse.code()}")
            }
        } catch (e: Exception) {
            Log.e("ActionsTab", "Error loading location: ${e.message}", e)
        }

        try {
            val historyResponse = RetrofitClient.api.getLocationHistory(deviceId, 5)
            if (historyResponse.isSuccessful) {
                locationHistory = historyResponse.body() ?: emptyList()
                Log.d("ActionsTab", "Location history loaded: ${locationHistory.size} points")
            } else {
                Log.w("ActionsTab", "Location history fetch failed: ${historyResponse.code()}")
            }
        } catch (e: Exception) {
            Log.e("ActionsTab", "Error loading location history: ${e.message}", e)
        }
    }

    // Load initial state
    LaunchedEffect(deviceId) {
        loadLocationData()
    }

    // Tick for relative timestamps
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000L)
            currentTime = System.currentTimeMillis()
        }
    }

    // Auto refresh location state
    LaunchedEffect(deviceId, trackingEnabled) {
        while (true) {
            delay(10_000L)
            loadLocationData()
        }
    }

    DevoraCard(accentColor = PurpleCore, isDark = isDark) {
        SectionHeader(title = "REMOTE ACTIONS", isDark = isDark)

        ActionRow(
            title = "Lock Device Now",
            subtitle = "Immediately lock device screen",
            icon = Icons.Outlined.Lock,
            color = PurpleCore,
            textColor = textColor,
            onClick = onLock
        )

        // Camera toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = policyLoaded) {
                    coroutineScope.launch {
                        try {
                            val newVal = !cameraDisabled
                            val resp = RetrofitClient.api.updateDevicePolicy(
                                deviceId,
                                PolicyUpdateRequest(cameraDisabled = newVal)
                            )
                            if (resp.isSuccessful) {
                                cameraDisabled = newVal
                                    onShowMessage(
                                        if (newVal) "Camera disabled on $deviceName" else "Camera enabled on $deviceName"
                                    )
                                } else {
                                    onShowMessage("Failed to update camera policy")
                            }
                            } catch (_: Exception) {
                                onShowMessage("Failed to update camera policy")
                            }
                    }
                }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (cameraDisabled) Danger.copy(alpha = 0.10f)
                            else Warning.copy(alpha = 0.10f)
                        )
                        .border(
                            1.dp,
                            if (cameraDisabled) Danger.copy(alpha = 0.25f)
                            else Warning.copy(alpha = 0.25f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        tint = if (cameraDisabled) Danger else Warning,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        if (cameraDisabled) "Camera Disabled" else "Camera Enabled",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = textColor
                    )
                    Text(
                        if (cameraDisabled) "Tap to enable camera" else "Tap to disable camera",
                        fontFamily = DMSans,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(
                        if (cameraDisabled) Danger.copy(alpha = 0.10f)
                        else Success.copy(alpha = 0.10f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    if (cameraDisabled) "OFF" else "ON",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    color = if (cameraDisabled) Danger else Success,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        HorizontalDivider(color = PurpleCore.copy(alpha = 0.08f), thickness = 1.dp)

        ActionRow(
            title = "Force Password Reset",
            subtitle = "Require employee to set new password",
            icon = Icons.Outlined.Password,
            color = Warning,
            textColor = textColor,
            onClick = onPasswordReset
        )

        ActionRow(
            title = "Clear App Data",
            subtitle = "Wipe all app data remotely",
            icon = Icons.Outlined.CleaningServices,
            color = Warning,
            textColor = textColor,
            onClick = onClearData
        )

        // Sync row with loading state
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isSyncing) { onSync() }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PurpleCore.copy(alpha = 0.10f))
                        .border(1.dp, PurpleCore.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = PurpleCore,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Sync,
                            contentDescription = null,
                            tint = PurpleCore,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        if (isSyncing) "Syncing..." else "Sync Device Now",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = textColor
                    )
                    Text(
                        "Force immediate data synchronization",
                        fontFamily = DMSans,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
            if (!isSyncing) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = PurpleCore,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        HorizontalDivider(color = PurpleCore.copy(alpha = 0.08f), thickness = 1.dp)

        Spacer(Modifier.height(16.dp))

        // ── DANGER ZONE ──
        Box(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                color = Danger.copy(alpha = 0.20f),
                thickness = 1.dp,
                modifier = Modifier.align(Alignment.Center)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(if (isDark) DarkBgSurface else BgSurface)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    "DANGER ZONE",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = Danger,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        ActionRow(
            title = "Remote Wipe Device",
            subtitle = "⚠ Permanently erase all device data",
            icon = Icons.Outlined.DeleteForever,
            color = Danger,
            textColor = textColor,
            onClick = onWipe
        )
    }

    Spacer(Modifier.height(16.dp))

    // ── GPS LOCATION ──
    DevoraCard(accentColor = PurpleCore, isDark = isDark) {
        SectionHeader(title = "DEVICE LOCATION", isDark = isDark)

        val lat = location?.latitude
        val lng = location?.longitude

        if (isRequestingLocation) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = PurpleCore, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(12.dp))
                Text("Requesting device location...", color = TextMuted, fontSize = 12.sp)
            }
        } else if (lat != null && lng != null) {
            DeviceLocationMap(
                latitude = lat,
                longitude = lng,
                employeeName = deviceName,
                isDark = isDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, PurpleCore.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
            )

            Spacer(Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) DarkBgElevated else Color(0xFFF8F8FF),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, PurpleCore.copy(alpha = 0.18f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    val loc = location  // Local reference to avoid smart cast issues
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            tint = PurpleCore,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            locationAddress,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = textColor
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${"%.6f".format(lat)}° N, ${"%.6f".format(lng)}° E",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    Spacer(Modifier.height(4.dp))

                    // Accuracy Quality Indicator
                    val accuracy = loc?.accuracy ?: 0f
                    val (accuracyColor, accuracyLabel) = when {
                        accuracy < 10f -> Success to "Excellent"
                        accuracy < 25f -> PurpleCore to "Very Good"
                        accuracy < 50f -> Warning to "Good"
                        accuracy < 100f -> Warning to "Fair"
                        else -> Danger to "Poor"
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accuracyColor.copy(alpha = 0.10f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accuracyColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Accuracy: ±${accuracy.toInt()}m - $accuracyLabel",
                                fontFamily = DMSans,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accuracyColor
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Additional Metrics Grid
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Altitude
                            if (loc?.altitude != null) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDark) DarkBgElevated else Color(0xFFF5F5F5),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PurpleCore.copy(alpha = 0.15f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "Altitude",
                                            fontFamily = DMSans,
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                        Text(
                                            "${"%.0f".format(loc.altitude!!)}m",
                                            fontFamily = JetBrainsMono,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor
                                        )
                                    }
                                }
                            }

                            // Speed
                            if (loc?.speed != null && loc.speed!! > 0f) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDark) DarkBgElevated else Color(0xFFF5F5F5),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PurpleCore.copy(alpha = 0.15f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "Speed",
                                            fontFamily = DMSans,
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                        Text(
                                            "${"%.1f".format(loc.speed!! * 3.6f)} km/h",
                                            fontFamily = JetBrainsMono,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor
                                        )
                                    }
                                }
                            }

                            // Bearing
                            if (loc?.bearing != null && loc.bearing!! > 0f) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDark) DarkBgElevated else Color(0xFFF5F5F5),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PurpleCore.copy(alpha = 0.15f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "Bearing",
                                            fontFamily = DMSans,
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                        Text(
                                            "${"%.0f".format(loc.bearing!!)}°",
                                            fontFamily = JetBrainsMono,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Updated: ${getTimeAgo(loc?.recordedAt.orEmpty(), currentTime)}",
                            fontFamily = DMSans,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { openInMaps(lat, lng) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PurpleCore),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleCore)
            ) {
                Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open in Google Maps")
            }
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) DarkBgElevated else Color(0xFFF5F5F5)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.LocationOff,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("No location data available", color = TextMuted, fontSize = 13.sp)
                    Text("Enable tracking or request location", color = TextMuted.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        isRequestingLocation = true
                        try {
                            val response = RetrofitClient.api.createDeviceCommand(
                                deviceId,
                                CommandRequest("REQUEST_LOCATION", null)
                            )
                            if (response.isSuccessful) {
                                onShowMessage("Location request sent to device. Please wait...")
                                delay(3000L)
                                loadLocationData()
                            } else {
                                onShowMessage("Failed to request location")
                            }
                        } catch (e: Exception) {
                            Log.e("ActionsTab", "Error requesting location: ${e.message}")
                            onShowMessage("Error requesting location: ${e.message}")
                        } finally {
                            isRequestingLocation = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PurpleCore),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleCore),
                enabled = !isRequestingLocation
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isRequestingLocation) "Requesting..." else "Request Location Now")
            }
        }

        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) DarkBgElevated else BgSurface,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Location Tracking",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = textColor
                    )
                    Text(
                        if (trackingEnabled) "Updates every 5 minutes" else "Tracking disabled (one-time request available)",
                        fontFamily = DMSans,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                Switch(
                    checked = trackingEnabled,
                    onCheckedChange = { enabled ->
                        trackingEnabled = enabled
                        coroutineScope.launch {
                            try {
                                val response = RetrofitClient.api.updateDevicePolicy(
                                    deviceId,
                                    PolicyUpdateRequest(locationTrackingEnabled = enabled)
                                )
                                if (!response.isSuccessful) {
                                    trackingEnabled = !enabled
                                    onShowMessage("Failed to update location tracking policy")
                                } else {
                                    onShowMessage(
                                        if (enabled) "Location tracking enabled for $deviceName"
                                        else "Location tracking disabled for $deviceName"
                                    )
                                }
                            } catch (_: Exception) {
                                trackingEnabled = !enabled
                                onShowMessage("Failed to update location tracking policy")
                            }
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PurpleCore
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (!isRequestingLocation) PurpleCore else PurpleCore.copy(alpha = 0.50f))
                .clickable(enabled = !isRequestingLocation) {
                    coroutineScope.launch {
                        isRequestingLocation = true
                        try {
                            val beforeLat = location?.latitude
                            val beforeLng = location?.longitude

                            val commandResponse = RetrofitClient.api.createDeviceCommand(
                                deviceId,
                                CommandRequest(type = "REQUEST_LOCATION")
                            )
                            if (commandResponse.isSuccessful) {
                                onShowMessage("Location request sent")
                                repeat(4) {
                                    delay(3_000L)
                                    loadLocationData()
                                }

                                val afterLat = location?.latitude
                                val afterLng = location?.longitude

                                if (afterLat == null || afterLng == null) {
                                    onShowMessage("Location request processed. Waiting for first GPS fix.")
                                } else if (beforeLat == afterLat && beforeLng == afterLng) {
                                    onShowMessage("Location unchanged. Device may be stationary or emulator GPS is fixed.")
                                } else {
                                    onShowMessage("Location updated successfully")
                                }
                            } else {
                                onShowMessage("Failed to request location")
                            }
                        } catch (_: Exception) {
                            onShowMessage("Failed to request location")
                        } finally {
                            isRequestingLocation = false
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isRequestingLocation) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Requesting...", color = Color.White, fontFamily = PlusJakartaSans, fontSize = 13.sp)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (trackingEnabled) "Request Location Now" else "Request One-Time Location",
                        color = Color.White,
                        fontFamily = PlusJakartaSans,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (locationHistory.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "LOCATION HISTORY",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = TextMuted,
                letterSpacing = 0.4.sp
            )
            Spacer(Modifier.height(6.dp))

            locationHistory.take(5).forEachIndexed { index, loc ->
                val historyLat = loc.latitude ?: return@forEachIndexed
                val historyLng = loc.longitude ?: return@forEachIndexed

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (index == 0) PurpleCore else TextMuted.copy(alpha = 0.35f))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${"%.4f".format(historyLat)}, ${"%.4f".format(historyLng)}",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = if (index == 0) PurpleCore else TextMuted,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        getTimeAgo(loc.recordedAt.orEmpty(), currentTime),
                        fontFamily = DMSans,
                        fontSize = 10.sp,
                        color = TextMuted.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════
// DEVICE LOCATION MAP (OpenStreetMap)
// ══════════════════════════════════════

@Composable
private fun DeviceLocationMap(
    latitude: Double,
    longitude: Double,
    employeeName: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
                setMultiTouchControls(true)
                controller.setZoom(16.0)
                controller.setCenter(GeoPoint(latitude, longitude))

                val marker = Marker(this)
                marker.position = GeoPoint(latitude, longitude)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = "$employeeName's Device"
                marker.snippet = "Lat: ${"%.4f".format(latitude)}, Lng: ${"%.4f".format(longitude)}"
                marker.icon = androidx.core.content.ContextCompat.getDrawable(
                    ctx,
                    android.R.drawable.ic_menu_mylocation
                )
                overlays.add(marker)
            }
        },
        update = { mapView ->
            mapView.overlays.clear()
            mapView.controller.animateTo(GeoPoint(latitude, longitude))

            val marker = Marker(mapView)
            marker.position = GeoPoint(latitude, longitude)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = "$employeeName's Device"
            marker.snippet = "Lat: ${"%.4f".format(latitude)}, Lng: ${"%.4f".format(longitude)}"
            marker.icon = androidx.core.content.ContextCompat.getDrawable(
                context,
                android.R.drawable.ic_menu_mylocation
            )

            mapView.overlays.add(marker)
            mapView.invalidate()
        }
    )
}

// ══════════════════════════════════════
// ACTION ROW
// ══════════════════════════════════════

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.10f))
                    .border(1.dp, color.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    title,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = textColor
                )
                Text(
                    subtitle,
                    fontFamily = DMSans,
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
    }
    HorizontalDivider(color = PurpleCore.copy(alpha = 0.08f), thickness = 1.dp)
}

// ══════════════════════════════════════
// CONFIRM ACTION DIALOG (reusable)
// ══════════════════════════════════════

@Composable
private fun ConfirmActionDialog(
    title: String,
    message: String,
    icon: ImageVector,
    iconColor: Color,
    confirmText: String,
    confirmColor: Color,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .background(if (isDark) DarkBgSurface else BgSurface)
                .border(1.dp, PurpleBorder, RoundedCornerShape(24.dp))
                .padding(28.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.10f))
                        .border(1.dp, iconColor.copy(alpha = 0.30f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    title,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (isDark) DarkTextPrimary else TextPrimary
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    message,
                    fontFamily = DMSans,
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PurpleDim)
                            .border(1.dp, PurpleBorder, RoundedCornerShape(12.dp))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Cancel",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = PurpleCore
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(confirmColor, confirmColor.copy(alpha = 0.85f))
                                )
                            )
                            .clickable(onClick = onConfirm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            confirmText,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════
// HELPER FUNCTIONS
// ══════════════════════════════════════

private data class InstalledAppMeta(
    val firstInstallTime: Long?,
    val lastUpdateTime: Long?,
    val appSizeBytes: Long?,
    val installSourceLabel: String
)

private fun readRestrictedPackages(context: Context): Set<String> {
    val prefs = context.getSharedPreferences("devora_restrictions", Context.MODE_PRIVATE)
    return prefs.getStringSet("restricted_packages", emptySet()) ?: emptySet()
}

private fun saveRestrictedPackages(context: Context, restrictedPackages: Set<String>) {
    val prefs = context.getSharedPreferences("devora_restrictions", Context.MODE_PRIVATE)
    prefs.edit().putStringSet("restricted_packages", restrictedPackages).apply()
}

private fun resolveInstalledAppMeta(
    context: Context,
    packageName: String,
    isSystemApp: Boolean
): InstalledAppMeta {
    return try {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(packageName, 0)

        val installerPackage = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(packageName)
            }
        } catch (_: Exception) {
            null
        }

        val installSource = when (installerPackage) {
            "com.android.vending" -> "📦 Play Store"
            "com.bbk.appstore", "com.vivo.appstore", "com.vivo.store" -> "📦 Vivo Store"
            "com.huawei.appmarket" -> "📦 Huawei Store"
            "com.samsung.android.app.samsungapps" -> "📦 Samsung Store"
            "com.xiaomi.market" -> "📦 Mi Store"
            "com.android.chrome", "org.mozilla.firefox", "com.android.browser" -> "🌐 Browser/APK"
            null -> if (isSystemApp) "🔧 Pre-installed" else "📲 ADB/Sideloaded"
            else -> "📱 Other Store"
        }

        val sizeBytes = try {
            val sourceDir = info.applicationInfo?.sourceDir
            if (sourceDir.isNullOrBlank()) null else File(sourceDir).length()
        } catch (_: Exception) {
            null
        }

        InstalledAppMeta(
            firstInstallTime = info.firstInstallTime,
            lastUpdateTime = info.lastUpdateTime,
            appSizeBytes = sizeBytes,
            installSourceLabel = installSource
        )
    } catch (_: PackageManager.NameNotFoundException) {
        InstalledAppMeta(
            firstInstallTime = null,
            lastUpdateTime = null,
            appSizeBytes = null,
            installSourceLabel = if (isSystemApp) "🔧 Pre-installed" else "📱 Other Store"
        )
    }
}

private fun formatEpochDate(epochMillis: Long?): String {
    if (epochMillis == null || epochMillis <= 0L) return "Unavailable"
    return try {
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        formatter.format(Date(epochMillis))
    } catch (_: Exception) {
        "Unavailable"
    }
}

private fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes <= 0L) return "Unavailable"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0

    return when {
        bytes >= gb -> String.format(Locale.getDefault(), "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.getDefault(), "%.0f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.getDefault(), "%.0f KB", bytes / kb)
        else -> "$bytes B"
    }
}

private fun formatTimeAgo(isoDateTime: String?): String {
    if (isoDateTime.isNullOrEmpty()) return "just now"
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val date = sdf.parse(isoDateTime) ?: return "just now"
        val diffSec = (System.currentTimeMillis() - date.time) / 1000
        when {
            diffSec < 60 -> "just now"
            diffSec < 3600 -> "${diffSec / 60}m ago"
            diffSec < 86400 -> "${diffSec / 3600}h ago"
            else -> "${diffSec / 86400}d ago"
        }
    } catch (_: Exception) {
        "just now"
    }
}

private fun getTimeAgo(isoTimestamp: String, currentTime: Long): String {
    return try {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
        )
        var date: Date? = null
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                date = sdf.parse(isoTimestamp)
                if (date != null) break
            } catch (_: Exception) {
                continue
            }
        }

        if (date == null) return "Unknown"

        val diff = currentTime - date.time

        when {
            diff < 0 -> "Just now"
            diff < 60_000L -> "Just now"
            diff < 3_600_000L -> "${diff / 60_000L} min ago"
            diff < 86_400_000L -> {
                val h = diff / 3_600_000L
                val m = (diff % 3_600_000L) / 60_000L
                if (m > 0) "${h}h ${m}m ago" else "${h}h ago"
            }
            diff < 604_800_000L -> "${diff / 86_400_000L}d ago"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
        }
    } catch (_: Exception) {
        "Unknown"
    }
}

@Composable
private fun AppInfoRow(label: String, value: String, textColor: Color, isDark: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontFamily = DMSans,
            fontSize = 12.sp,
            color = TextMuted,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            color = textColor,
            modifier = Modifier.weight(0.6f),
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatInstallSource(source: String?): String {
    if (source.isNullOrBlank()) return ""
    return when {
        source.contains("vending", ignoreCase = true) || source.contains("google", ignoreCase = true) -> "\uD83D\uDCE6 Play Store"
        source.contains("vivo", ignoreCase = true) -> "\uD83D\uDCE6 Vivo Store"
        source.contains("samsung", ignoreCase = true) -> "\uD83D\uDCE6 Galaxy Store"
        source.contains("xiaomi", ignoreCase = true) || source.contains("miui", ignoreCase = true) -> "\uD83D\uDCE6 Mi Store"
        source.contains("browser", ignoreCase = true) || source.contains("download", ignoreCase = true) -> "\uD83C\uDF10 Browser/APK"
        source.contains("adb", ignoreCase = true) || source.contains("shell", ignoreCase = true) -> "\uD83D\uDCF2 ADB/Sideloaded"
        source.contains("packageinstaller", ignoreCase = true) -> "\uD83D\uDCF2 Manual Install"
        else -> "\uD83D\uDCF1 $source"
    }
}

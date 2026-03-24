package com.devora.devicemanager.ui.screens.employeedashboard

import com.devora.devicemanager.data.remote.RemoteDataSource
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.devora.devicemanager.AdminReceiver
import com.devora.devicemanager.collector.DeviceInfoCollector
import com.devora.devicemanager.network.AppInventoryItem
import com.devora.devicemanager.session.SessionManager
import com.devora.devicemanager.sync.DeviceInfoSyncWorker
import com.devora.devicemanager.sync.HeartbeatService
import com.devora.devicemanager.sync.SyncManager
import com.devora.devicemanager.ui.components.DevoraCard
import com.devora.devicemanager.ui.components.SectionHeader
import com.devora.devicemanager.ui.theme.BgBase
import com.devora.devicemanager.ui.theme.BgSurface
import com.devora.devicemanager.ui.theme.DMSans
import com.devora.devicemanager.ui.theme.Danger
import com.devora.devicemanager.ui.theme.DarkBgBase
import com.devora.devicemanager.ui.theme.DarkBgSurface
import com.devora.devicemanager.ui.theme.DarkTextPrimary
import com.devora.devicemanager.ui.theme.JetBrainsMono
import com.devora.devicemanager.ui.theme.PlusJakartaSans
import com.devora.devicemanager.ui.theme.PurpleBorder
import com.devora.devicemanager.ui.theme.PurpleCore
import com.devora.devicemanager.ui.theme.PurpleDim
import com.devora.devicemanager.ui.theme.Success
import com.devora.devicemanager.ui.theme.TextMuted
import com.devora.devicemanager.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Internal activity data class for unified UI
private data class UIActivity(
    val icon: ImageVector,
    val description: String,
    val timestamp: Long,
    val createdAtStr: String? = null
)

@Composable
fun EmployeeDashboardScreen(
    onSignOut: () -> Unit,
    onEnrollmentRevoked: () -> Unit,
    isDark: Boolean,
    onThemeToggle: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deviceInfo = remember { DeviceInfoCollector.collect(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var showSignOutDialog by remember { mutableStateOf(false) }
    var checkTick by remember { mutableIntStateOf(0) }
    val latestOnEnrollmentRevoked by rememberUpdatedState(onEnrollmentRevoked)

    // FIX 2: Get real employee name from SessionManager
    val employeePrefs = remember { context.getSharedPreferences("devora_enrollment", Context.MODE_PRIVATE) }
    val employeeName = remember { employeePrefs.getString("employee_name", "Employee") ?: "Employee" }
    val enrolledDeviceId = remember {
        employeePrefs.getString("device_id", null)?.takeIf { it.isNotBlank() }
    }
    val activeDeviceId = enrolledDeviceId ?: deviceInfo.deviceId
    val displayFirstName = employeeName.substringBefore(" ")
    val avatarInitial = employeeName.firstOrNull()?.uppercaseChar() ?: 'E'

    // FIX 3: Activities state
    var activities by remember { mutableStateOf<List<UIActivity>>(emptyList()) }
    var isAppInventoryLoading by remember { mutableStateOf(true) }
    var appInventory by remember { mutableStateOf<List<AppInventoryItem>>(emptyList()) }
    var restrictedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    var appSearchQuery by remember { mutableStateOf("") }
    var appFilter by remember { mutableStateOf("ALL") }
    var lockParentScrollForApps by remember { mutableStateOf(false) }
    var isQuickSyncRunning by remember { mutableStateOf(false) }

    fun getTimeAgo(timestamp: Long): String {
        val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
        return when {
            diff < 60_000L -> "Just now"
            diff < 3_600_000L -> {
                val mins = diff / 60_000L
                if (mins == 1L) "1 min ago" else "$mins mins ago"
            }
            diff < 86_400_000L -> {
                val hours = diff / 3_600_000L
                if (hours == 1L) "1 hr ago" else "$hours hrs ago"
            }
            else -> {
                val days = diff / 86_400_000L
                if (days == 1L) "1 day ago" else "$days days ago"
            }
        }
    }

    fun parseISO(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null

        return try {
            Instant.parse(dateStr).toEpochMilli()
        } catch (_: DateTimeParseException) {
            try {
                DateTimeFormatter.ISO_OFFSET_DATE_TIME
                    .parse(dateStr, Instant::from)
                    .toEpochMilli()
            } catch (_: DateTimeParseException) {
                try {
                    val localDateTime = LocalDateTime.parse(
                        dateStr,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]")
                    )
                    localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    suspend fun loadActivities() {
        val list = mutableListOf<UIActivity>()

        // 1. Fetch from Backend
        try {
            val response = RemoteDataSource.getDeviceActivities(activeDeviceId)
            if (response.isSuccessful) {
                response.body()?.forEach { res ->
                    val icon = when (res.activityType) {
                        "ENROLLED" -> Icons.Outlined.Shield
                        "APP_RESTRICTED" -> Icons.Outlined.Block
                        "DEVICE_LOCKED" -> Icons.Outlined.Lock
                        "CAMERA_DISABLED" -> Icons.Outlined.CameraAlt
                        "APP_INSTALLED" -> Icons.Outlined.PhoneAndroid
                        "LOCATION_UPDATED" -> Icons.Outlined.LocationOn
                        else -> Icons.Filled.Info
                    }
                    val parsedTime = parseISO(res.createdAt)
                    if (parsedTime != null) {
                        list.add(UIActivity(icon, res.description ?: "", parsedTime, res.createdAt))
                    }
                }
            }
        } catch (e: Exception) {
            // Backend failed, fallback to local only
        }

        // 2. Local App Detection
        try {
            val pm = context.packageManager
            val installedApps = pm.getInstalledPackages(0)
            val enrolledAt = employeePrefs.getLong("enrolled_at", 0L)
            
            // Just for demonstration if not enrolled today, use some recent time
            val compareTime = if (enrolledAt > 0) enrolledAt else System.currentTimeMillis() - 86400000

            installedApps.filter { pkg ->
                val appInfo = pkg.applicationInfo
                pkg.firstInstallTime > compareTime && 
                appInfo != null &&
                (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0
            }.forEach { pkg ->
                val appName = pkg.applicationInfo?.loadLabel(pm)?.toString() ?: "Unknown App"
                list.add(UIActivity(Icons.Outlined.PhoneAndroid, "$appName installed", pkg.firstInstallTime))
            }

            // Enrollment activity if just enrolled
            if (enrolledAt > (System.currentTimeMillis() - 3600000)) {
                list.add(UIActivity(Icons.Outlined.Shield, "Device enrolled in DEVORA MDM", enrolledAt))
            }

        } catch (e: Exception) { }

        activities = list
            .sortedWith(compareByDescending<UIActivity> { it.timestamp }.thenBy { it.description })
            .take(10)
    }

    suspend fun loadAppInventory() {
        isAppInventoryLoading = true
        try {
            val inventoryResponse = RemoteDataSource.getAppInventory(activeDeviceId)
            if (inventoryResponse.isSuccessful) {
                appInventory = inventoryResponse.body() ?: emptyList()
            }
        } catch (_: Exception) {
            // Keep the last successful app inventory; retry on next refresh.
        }

        try {
            val restrictedResponse = RemoteDataSource.getRestrictedApps(activeDeviceId)
            if (restrictedResponse.isSuccessful) {
                restrictedPackages = (restrictedResponse.body() ?: emptyList())
                    .filter { it.restricted }
                    .map { it.packageName }
                    .toSet()
            }
        } catch (_: Exception) {
            // Keep the last known restriction state on transient failures.
        }
        isAppInventoryLoading = false
    }

    suspend fun verifyDeviceStillActive() {
        try {
            val response = RemoteDataSource.checkDevice(activeDeviceId)
            if (response.code() == 404) {
                SessionManager.clearDeviceEnrollment(context)
                Toast.makeText(
                    context,
                    "Your device enrollment has been revoked. Please re-enroll.",
                    Toast.LENGTH_LONG
                ).show()
                latestOnEnrollmentRevoked()
            }
        } catch (_: Exception) {
            // Ignore transient network errors and retry on next interval.
        }
    }

    LaunchedEffect(Unit) {
        // Employee is actively using dashboard; resume heartbeat and clear signed-out flag.
        SessionManager.setEmployeeSignedOut(context, false)
        HeartbeatService.start(context)

        loadActivities()
        loadAppInventory()
        while (true) {
            verifyDeviceStillActive()
            loadAppInventory()
            delay(30_000)
        }
    }

    LaunchedEffect(checkTick) {
        if (checkTick > 0) {
            verifyDeviceStillActive()
            loadActivities()
            loadAppInventory()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val bgColor = if (isDark) DarkBgBase else BgBase
    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    val isDeviceOwnerSet = remember(context) {
        runCatching { AdminReceiver.isDeviceOwner(context) }.getOrDefault(false)
    }
    val userApps = appInventory.filter { it.isSystemApp != true }
    val systemApps = appInventory.filter { it.isSystemApp == true }
    val filteredApps = when (appFilter) {
        "USER" -> userApps
        "SYSTEM" -> systemApps
        else -> appInventory
    }.filter {
        appSearchQuery.isBlank() ||
            it.appName.contains(appSearchQuery, ignoreCase = true) ||
            it.packageName.contains(appSearchQuery, ignoreCase = true)
    }

    Scaffold(
        containerColor = bgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState(), enabled = !lockParentScrollForApps)
                .padding(16.dp)
        ) {
            // ══════════════════════════════════════
            // TOP BAR
            // ══════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "DEVORA",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = PurpleCore,
                        letterSpacing = 2.sp
                    )
                    
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        enabled = !isQuickSyncRunning,
                        onClick = {
                            scope.launch {
                                isQuickSyncRunning = true
                                try {
                                    val employeeId = employeePrefs
                                        .getString("employee_id", "unknown")
                                        ?.takeIf { it.isNotBlank() }
                                        ?: "unknown"

                                    // Immediate full sync, then ensure heartbeat/command processing is active.
                                    val syncResult = SyncManager.syncDeviceData(context, employeeId)
                                    DeviceInfoSyncWorker.scheduleNow(context)
                                    HeartbeatService.start(context)

                                    loadActivities()
                                    loadAppInventory()

                                    val msg = if (syncResult.success) {
                                        "Synced latest apps and Android info"
                                    } else {
                                        "Sync sent. Some data may update shortly"
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Sync failed: ${e.message ?: "Unknown error"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } finally {
                                    isQuickSyncRunning = false
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = "Sync and refresh location",
                            tint = if (isQuickSyncRunning) PurpleCore.copy(alpha = 0.45f) else PurpleCore
                        )
                    }

                    IconButton(onClick = onThemeToggle) {
                        Icon(
                            imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle theme",
                            tint = PurpleCore
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ══════════════════════════════════════
            // WELCOME CARD
            // ══════════════════════════════════════
            DevoraCard(showTopAccent = true, accentColor = Success, isDark = isDark) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Hi, $displayFirstName!",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = textColor
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val pulse by rememberInfiniteTransition(label = "pulse")
                                .animateFloat(
                                    initialValue = 0.6f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        tween(1000),
                                        RepeatMode.Reverse
                                    ),
                                    label = "pulseAlpha"
                                )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Success.copy(alpha = pulse))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Device is being monitored",
                                fontFamily = DMSans,
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${deviceInfo.manufacturer.replaceFirstChar { it.uppercase() }} · Android ${deviceInfo.osVersion}",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Success, Color(0xFF2AB87A)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            avatarInitial.toString(),
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ══════════════════════════════════════
            // DEVICE INFORMATION CARD
            // ══════════════════════════════════════
            DevoraCard(accentColor = PurpleCore, isDark = isDark) {
                Column {
                SectionHeader(title = "DEVICE INFORMATION", isDark = isDark)

                val infoItems = listOf(
                    Triple("Model", deviceInfo.model, textColor),
                    Triple(
                        "Manufacturer",
                        deviceInfo.manufacturer.replaceFirstChar { it.uppercase() },
                        textColor
                    ),
                    Triple("Android", deviceInfo.osVersion, textColor),
                    Triple("SDK Level", deviceInfo.sdkVersion.toString(), textColor),
                    Triple("Status", "Enrolled", Success),
                    Triple("Device Owner", if (isDeviceOwnerSet) "Set" else "Not Set", if (isDeviceOwnerSet) Success else Danger),
                    Triple("Server", "Connected", Success)
                )

                infoItems.forEachIndexed { index, (label, value, valueColor) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label,
                            fontFamily = DMSans,
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                        Text(
                            value,
                            fontFamily = JetBrainsMono,
                            fontSize = 13.sp,
                            color = valueColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (index < infoItems.size - 1) {
                        HorizontalDivider(
                            color = PurpleCore.copy(alpha = 0.08f),
                            thickness = 1.dp
                        )
                    }
                }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ══════════════════════════════════════
            // APP INVENTORY CARD
            // ══════════════════════════════════════
            DevoraCard(accentColor = PurpleCore, isDark = isDark) {
                Column {
                SectionHeader(title = "APP INVENTORY ON THIS DEVICE", isDark = isDark)

                Text(
                    "${appInventory.size} apps · ${userApps.size} user · ${systemApps.size} system",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(if (isDark) DarkBgSurface else BgSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, PurpleBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = PurpleCore,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (appSearchQuery.isEmpty()) {
                                Text(
                                    "Search apps...",
                                    fontFamily = DMSans,
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            }
                            BasicTextField(
                                value = appSearchQuery,
                                onValueChange = { appSearchQuery = it },
                                textStyle = TextStyle(
                                    fontFamily = DMSans,
                                    fontSize = 13.sp,
                                    color = textColor
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (appSearchQuery.isNotEmpty()) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Clear",
                                tint = TextMuted,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { appSearchQuery = "" }
                                    )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "ALL" to appInventory.size,
                        "USER" to userApps.size,
                        "SYSTEM" to systemApps.size
                    ).forEach { (filter, count) ->
                        val isSelected = appFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) PurpleCore.copy(alpha = 0.12f)
                                    else if (isDark) DarkBgSurface else BgSurface,
                                    RoundedCornerShape(20.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) PurpleCore.copy(alpha = 0.40f)
                                    else PurpleCore.copy(alpha = 0.15f),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { appFilter = filter }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "$filter ($count)",
                                fontFamily = if (isSelected) PlusJakartaSans else DMSans,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 11.sp,
                                color = if (isSelected) PurpleCore else TextMuted
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (isAppInventoryLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Loading app inventory...",
                            fontFamily = DMSans,
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                } else if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Apps,
                                contentDescription = null,
                                tint = PurpleCore.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (appInventory.isEmpty()) {
                                    "No app inventory available yet"
                                } else {
                                    "No apps match your search"
                                },
                                fontFamily = DMSans,
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        lockParentScrollForApps = event.changes.any { it.pressed }
                                    }
                                }
                            },
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(filteredApps, key = { it.id }) { app ->
                            val isRestricted = app.packageName in restrictedPackages
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isRestricted) Danger.copy(alpha = 0.10f) else PurpleDim),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isRestricted) {
                                        Icon(
                                            Icons.Outlined.Block,
                                            contentDescription = null,
                                            tint = Danger,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Text(
                                            app.appName.take(1).uppercase(),
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = PurpleCore
                                        )
                                    }
                                }

                                Spacer(Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        app.appName,
                                        fontFamily = DMSans,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isRestricted) Danger else textColor
                                    )
                                    Text(
                                        app.packageName,
                                        fontFamily = JetBrainsMono,
                                        fontSize = 9.sp,
                                        color = TextMuted
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(
                                            if (isRestricted) Danger.copy(alpha = 0.10f)
                                            else Success.copy(alpha = 0.10f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isRestricted) Danger.copy(alpha = 0.25f)
                                            else Success.copy(alpha = 0.25f),
                                            RoundedCornerShape(100.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        if (isRestricted) "Restricted" else "Allowed",
                                        fontFamily = JetBrainsMono,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRestricted) Danger else Success
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = PurpleCore.copy(alpha = 0.06f),
                                thickness = 1.dp
                            )
                        }
                    }
                }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ══════════════════════════════════════
            // RECENT ACTIVITY
            // ══════════════════════════════════════
            SectionHeader(title = "MY RECENT ACTIVITY", isDark = isDark)

            Spacer(Modifier.height(8.dp))

            DevoraCard(isDark = isDark) {
                if (activities.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = TextMuted.copy(alpha = 0.5f),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No recent activity",
                                fontFamily = DMSans,
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                    }
                } else {
                    Column {
                        activities.forEachIndexed { index, activity ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(PurpleDim),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = activity.icon,
                                        contentDescription = null,
                                        tint = PurpleCore,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activity.description,
                                        fontFamily = DMSans,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textColor
                                    )
                                    Text(
                                        text = getTimeAgo(activity.timestamp),
                                        fontFamily = JetBrainsMono,
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                            if (index < activities.size - 1) {
                                HorizontalDivider(
                                    color = PurpleCore.copy(alpha = 0.08f),
                                    thickness = 1.dp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ══════════════════════════════════════
            // SIGN OUT
            // ══════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Danger.copy(alpha = 0.06f))
                    .border(1.dp, Danger.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .clickable { showSignOutDialog = true }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        tint = Danger,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Sign Out",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Danger
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Your IT team monitors this device for security",
                fontFamily = DMSans,
                fontSize = 11.sp,
                color = TextMuted.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    // ══════════════════════════════════════
    // SIGN OUT DIALOG
    // ══════════════════════════════════════
    if (showSignOutDialog) {
        Dialog(
            onDismissRequest = { showSignOutDialog = false },
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
                    // Icon circle
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Danger.copy(alpha = 0.10f))
                            .border(1.dp, Danger.copy(alpha = 0.30f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            tint = Danger,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Sign Out?",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = if (isDark) DarkTextPrimary else TextPrimary
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Your device will remain enrolled and managed. You can sign back in anytime.",
                        fontFamily = DMSans,
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(20.dp))

                    // Buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PurpleDim)
                                .border(1.dp, PurpleBorder, RoundedCornerShape(12.dp))
                                .clickable { showSignOutDialog = false },
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

                        // Sign Out
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Danger, Color(0xFFE54545))
                                    )
                                )
                                .clickable {
                                    showSignOutDialog = false
                                    // Notify backend, mark signed out, then sign out locally.
                                    scope.launch {
                                        try {
                                            val response = RemoteDataSource.signOutDevice(activeDeviceId)
                                            if (!response.isSuccessful) {
                                                Toast.makeText(
                                                    context,
                                                    "Server sign-out failed (${response.code()}); device may stay online briefly",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                "Network error during sign-out; offline status may be delayed",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }

                                        SessionManager.setForceReEnroll(context, true)
                                        SessionManager.setEmployeeSignedOut(context, true)
                                        SessionManager.clearDeviceEnrollment(context)
                                        HeartbeatService.stop(context)
                                        onSignOut()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.Logout,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Sign Out",
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
    }
}

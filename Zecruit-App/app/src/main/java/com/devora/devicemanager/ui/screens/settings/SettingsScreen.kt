package com.devora.devicemanager.ui.screens.settings

import com.devora.devicemanager.data.remote.RemoteDataSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ripple
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.app.admin.DevicePolicyManager
import android.os.UserManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.devora.devicemanager.enrollment.DevicePolicyHelper
import com.devora.devicemanager.session.SessionManager
import com.devora.devicemanager.sync.SyncManager
import kotlinx.coroutines.launch
import com.devora.devicemanager.ui.components.ButtonVariant
import com.devora.devicemanager.ui.components.DevoraBottomNav
import com.devora.devicemanager.ui.components.DevoraButton
import com.devora.devicemanager.ui.components.DevoraCard
import com.devora.devicemanager.ui.components.SectionHeader
import com.devora.devicemanager.ui.theme.BadgeShape
import com.devora.devicemanager.ui.theme.BgBase
import com.devora.devicemanager.ui.theme.BgElevated
import com.devora.devicemanager.ui.theme.DMSans
import com.devora.devicemanager.ui.theme.Danger
import com.devora.devicemanager.ui.theme.DarkBgBase
import com.devora.devicemanager.ui.theme.DarkBgElevated
import com.devora.devicemanager.ui.theme.DarkTextPrimary
import com.devora.devicemanager.ui.theme.InputShape
import com.devora.devicemanager.ui.theme.JetBrainsMono
import com.devora.devicemanager.ui.theme.PlusJakartaSans
import com.devora.devicemanager.ui.theme.PurpleBorder
import com.devora.devicemanager.ui.theme.PurpleCore
import com.devora.devicemanager.ui.theme.PurpleDeep
import com.devora.devicemanager.ui.theme.Success
import com.devora.devicemanager.ui.theme.TextMuted
import com.devora.devicemanager.ui.theme.TextPrimary

// ══════════════════════════════════════
// POLICY ITEM DATA
// ══════════════════════════════════════

private data class PolicyItem(
    val icon: ImageVector,
    val title: String,
    val desc: String,
    val key: String
)

// ══════════════════════════════════════
// SETTINGS SCREEN
// ══════════════════════════════════════

@Composable
fun SettingsScreen(
    isDark: Boolean,
    onThemeToggle: () -> Unit,
    navController: NavHostController,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val adminName = remember { SessionManager.getAdminName(context).ifEmpty { "Administrator" } }
    val adminEmail = remember { SessionManager.getAdminEmail(context).ifEmpty { "admin@enterprise.com" } }
    val adminInitial = remember { adminName.firstOrNull()?.uppercase() ?: "A" }
    val policyHelper = remember { DevicePolicyHelper(context) }
    val coroutineScope = rememberCoroutineScope()

    var darkMode by remember { mutableStateOf(isDark) }
    var factoryReset by remember { mutableStateOf(true) }
    var screenLock by remember { mutableStateOf(true) }
    var appRestrict by remember { mutableStateOf(false) }
    var cameraDisable by remember { mutableStateOf(false) }
    var bgSync by remember { mutableStateOf(true) }
    var wifiOnly by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf("https://devora-backend-server-production.up.railway.app/") }
    var syncInterval by remember { mutableStateOf("15m") }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }
    var lastSyncTime by remember { mutableStateOf<String?>(null) }
    var connectionLatency by remember { mutableStateOf<String?>(null) }

    val bgColor = if (isDark) DarkBgBase else BgBase
    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    val inputBg = if (isDark) DarkBgElevated else BgElevated
    val dividerColor = if (isDark) PurpleCore.copy(alpha = 0.10f) else BgElevated

    val switchColors = SwitchDefaults.colors(
        checkedTrackColor = PurpleCore,
        uncheckedTrackColor = if (isDark) DarkBgElevated else BgElevated,
        checkedThumbColor = Color.White,
        uncheckedThumbColor = Color.White
    )

    val policies = listOf(
        PolicyItem(Icons.Filled.Shield, "Factory Reset Protection", "Prevent unauthorized reset", "factoryReset"),
        PolicyItem(Icons.Filled.Lock, "Screen Lock Enforcement", "Require PIN/pattern", "screenLock"),
        PolicyItem(Icons.Filled.Block, "App Install Restriction", "Control app installation", "appRestrict"),
        PolicyItem(Icons.Filled.CameraAlt, "Camera Disable", "Disable device camera", "cameraDisable")
    )

    Scaffold(
        bottomBar = {
            DevoraBottomNav(
                currentRoute = "settings",
                onNavigate = onNavigate,
                isDark = isDark
            )
        },
        containerColor = bgColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── PROFILE CARD ──
            item {
                DevoraCard(showTopAccent = true, isDark = isDark) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(PurpleCore, PurpleDeep)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = adminInitial,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = adminName,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = textColor
                            )
                            Text(
                                text = "Device Owner",
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = PurpleCore
                            )
                            Text(
                                text = adminEmail,
                                fontFamily = DMSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                        TextButton(onClick = { }) {
                            Text(
                                text = "Edit",
                                fontFamily = DMSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = PurpleCore
                            )
                        }
                    }
                }
            }

            // ── APPEARANCE ──
            item {
                SectionHeader(title = "▸ APPEARANCE", isDark = isDark)
            }
            item {
                DevoraCard(isDark = isDark) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (darkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                contentDescription = null,
                                tint = PurpleCore,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Dark Mode",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = textColor
                                )
                                Text(
                                    text = "Switch appearance",
                                    fontFamily = DMSans,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                        Switch(
                            checked = darkMode,
                            onCheckedChange = {
                                darkMode = it
                                onThemeToggle()
                            },
                            colors = switchColors
                        )
                    }
                }
            }

            // ── DEVICE POLICIES ──
            item {
                SectionHeader(title = "▸ DEVICE POLICIES", isDark = isDark)
            }
            item {
                DevoraCard(isDark = isDark) {
                    Column {
                        policies.forEachIndexed { index, policy ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = policy.icon,
                                    contentDescription = null,
                                    tint = PurpleCore,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = policy.title,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = textColor
                                    )
                                    Text(
                                        text = policy.desc,
                                        fontFamily = DMSans,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                                val checked = when (policy.key) {
                                    "factoryReset" -> factoryReset
                                    "screenLock" -> screenLock
                                    "appRestrict" -> appRestrict
                                    "cameraDisable" -> cameraDisable
                                    else -> false
                                }
                                Switch(
                                    checked = checked,
                                    onCheckedChange = { value ->
                                        when (policy.key) {
                                            "factoryReset" -> {
                                                factoryReset = value
                                                Toast.makeText(context, "Factory Reset Protection ${if (value) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                                            }
                                            "screenLock" -> {
                                                if (policyHelper.isDeviceOwner) {
                                                    try {
                                                        val dpm = context.getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                                                        val adminComponent = com.devora.devicemanager.AdminReceiver.getComponentName(context)
                                                        if (value) {
                                                            dpm.setMaximumTimeToLock(adminComponent, 30000L)
                                                        } else {
                                                            dpm.setMaximumTimeToLock(adminComponent, 0L)
                                                        }
                                                        screenLock = value
                                                        Toast.makeText(context, "Screen Lock ${if (value) "enforced (30s)" else "relaxed"}", Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    screenLock = value
                                                    Toast.makeText(context, "Not Device Owner — policy simulated", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            "appRestrict" -> {
                                                if (policyHelper.isDeviceOwner) {
                                                    try {
                                                        val dpm = context.getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                                                        val adminComponent = com.devora.devicemanager.AdminReceiver.getComponentName(context)
                                                        if (value) {
                                                            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
                                                        } else {
                                                            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
                                                        }
                                                        appRestrict = value
                                                        Toast.makeText(context, "App Install Restriction ${if (value) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    appRestrict = value
                                                    Toast.makeText(context, "Not Device Owner — policy simulated", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            "cameraDisable" -> {
                                                val success = policyHelper.setCameraDisabled(value)
                                                if (success) {
                                                    cameraDisable = value
                                                    Toast.makeText(context, "Camera ${if (value) "disabled" else "enabled"}", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    cameraDisable = value
                                                    Toast.makeText(context, "Not Device Owner — policy simulated", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    colors = switchColors
                                )
                            }
                            if (index < policies.size - 1) {
                                HorizontalDivider(thickness = 1.dp, color = dividerColor)
                            }
                        }
                    }
                }
            }

            // ── BACKEND CONFIGURATION ──
            item {
                SectionHeader(title = "▸ BACKEND CONFIGURATION", isDark = isDark)
            }
            item {
                DevoraCard(isDark = isDark) {
                    Column {
                        Text(
                            text = "Server URL",
                            fontFamily = DMSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = PurpleCore
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(inputBg, InputShape)
                                .border(1.dp, PurpleCore.copy(alpha = 0.30f), InputShape)
                                .padding(12.dp)
                        ) {
                            BasicTextField(
                                value = serverUrl,
                                onValueChange = { serverUrl = it },
                                textStyle = TextStyle(
                                    fontFamily = JetBrainsMono,
                                    fontSize = 13.sp,
                                    color = textColor
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DevoraButton(
                                text = if (connectionStatus == "testing") "Testing..." else "Test Connection",
                                onClick = {
                                    coroutineScope.launch {
                                        connectionStatus = "testing"
                                        val startTime = System.currentTimeMillis()
                                        try {
                                            val response = RemoteDataSource.getDashboardStats()
                                            val elapsed = System.currentTimeMillis() - startTime
                                            connectionLatency = "${elapsed}ms"
                                            connectionStatus = if (response.isSuccessful) "connected" else "failed"
                                        } catch (e: Exception) {
                                            connectionStatus = "failed"
                                            connectionLatency = null
                                        }
                                    }
                                },
                                variant = ButtonVariant.OUTLINE,
                                isDark = isDark
                            )
                            connectionStatus?.let { status ->
                                Text(
                                    text = when (status) {
                                        "testing" -> "● Testing..."
                                        "connected" -> "● Connected ${connectionLatency ?: ""}"
                                        else -> "● Connection Failed"
                                    },
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = when (status) {
                                        "testing" -> PurpleCore
                                        "connected" -> Success
                                        else -> Danger
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Sync Interval",
                            fontFamily = DMSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = PurpleCore
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("15m", "30m", "1h").forEach { interval ->
                                val isSelected = syncInterval == interval
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) PurpleCore else Color.Transparent,
                                            BadgeShape
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) PurpleCore else PurpleBorder,
                                            BadgeShape
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { syncInterval = interval }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = interval,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else PurpleCore
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── SYNC STATUS ──
            item {
                SectionHeader(title = "▸ SYNC STATUS", isDark = isDark)
            }
            item {
                DevoraCard(isDark = isDark) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = lastSyncTime ?: "No sync yet",
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = textColor
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            thickness = 1.dp,
                            color = dividerColor
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Background Sync",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = textColor
                            )
                            Switch(
                                checked = bgSync,
                                onCheckedChange = { bgSync = it },
                                colors = switchColors
                            )
                        }

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = dividerColor
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WiFi Only",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = textColor
                            )
                            Switch(
                                checked = wifiOnly,
                                onCheckedChange = { wifiOnly = it },
                                colors = switchColors
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            thickness = 1.dp,
                            color = dividerColor
                        )

                        DevoraButton(
                            text = if (isSyncing) "Syncing..." else "Sync Now",
                            onClick = {
                                if (!isSyncing) {
                                    coroutineScope.launch {
                                        isSyncing = true
                                        try {
                                            val result = SyncManager.syncDeviceData(context, "admin")
                                            val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                                            lastSyncTime = "Today ${timeFormat.format(java.util.Date())}"
                                            if (result.success) {
                                                Toast.makeText(context, "Sync completed", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Sync failed: ${result.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Sync error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                        isSyncing = false
                                    }
                                }
                            },
                            variant = ButtonVariant.OUTLINE,
                            modifier = Modifier.fillMaxWidth(),
                            isDark = isDark
                        )
                    }
                }
            }

            // ── SIGN OUT ──
            item {
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            color = Danger.copy(alpha = 0.06f)
                        )
                        .border(
                            width = 1.dp,
                            color = Danger.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = Danger)
                        ) { showSignOutDialog = true }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            tint = Danger,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Sign Out",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Danger
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        null,
                        tint = TextMuted.copy(alpha = 0.6f),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Session secured with enterprise encryption",
                        fontFamily = DMSans,
                        fontSize = 11.sp,
                        color = TextMuted.copy(alpha = 0.6f)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "DEVORA v1.0.0  ·  Enterprise MDM  ·  © 2026",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    color = TextMuted.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Sign Out Dialog
    if (showSignOutDialog) {
        Dialog(
            onDismissRequest = { showSignOutDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(PurpleCore.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            tint = Color(0xFFFF4444),
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Sign Out",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF1A1A2E)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Are you sure you want to\nsign out of DEVORA?\nYou will need to log in\nagain to access the app.",
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, PurpleCore, RoundedCornerShape(12.dp))
                                .clickable { showSignOutDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
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
                                .background(Color(0xFFFF4444))
                                .clickable {
                                    showSignOutDialog = false
                                    SessionManager.logout(context)
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign Out",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
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

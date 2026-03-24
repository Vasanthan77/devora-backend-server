package com.devora.devicemanager.ui.screens.policies

import com.devora.devicemanager.data.remote.RemoteDataSource
import com.devora.devicemanager.network.PolicyUpdateRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devora.devicemanager.ui.components.DevoraCard
import com.devora.devicemanager.ui.components.SectionHeader
import com.devora.devicemanager.ui.theme.BgBase
import com.devora.devicemanager.ui.theme.Danger
import com.devora.devicemanager.ui.theme.DarkBgBase
import com.devora.devicemanager.ui.theme.DarkTextPrimary
import com.devora.devicemanager.ui.theme.PlusJakartaSans
import com.devora.devicemanager.ui.theme.PurpleCore
import com.devora.devicemanager.ui.theme.PurpleDeep
import com.devora.devicemanager.ui.theme.Success
import com.devora.devicemanager.ui.theme.TextMuted
import com.devora.devicemanager.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════
// POLICIES SCREEN
// ══════════════════════════════════════

@Composable
fun PoliciesScreen(
    onBack: () -> Unit,
    isDark: Boolean
) {
    val bgColor = if (isDark) DarkBgBase else BgBase
    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Policy settings state
    var screenLockRequired by remember { mutableStateOf(true) }
    var minPasswordLength by remember { mutableFloatStateOf(8f) }
    var passwordComplexity by remember { mutableStateOf("Strong") }
    var autoLockTimeout by remember { mutableStateOf("5min") }
    var passwordExpiry by remember { mutableStateOf("90 Days") }

    var cameraDisabled by remember { mutableStateOf(false) }
    var usbStorageBlocked by remember { mutableStateOf(false) }
    var usbDebuggingBlocked by remember { mutableStateOf(true) }
    var unknownAppInstallBlocked by remember { mutableStateOf(true) }
    var factoryResetProtection by remember { mutableStateOf(true) }
    var screenshotBlocked by remember { mutableStateOf(false) }

    var wifiOnlyMode by remember { mutableStateOf(false) }
    var forceVpn by remember { mutableStateOf(false) }
    var bluetoothRestriction by remember { mutableStateOf("Allow All") }
    var browserRestriction by remember { mutableStateOf("Allow All Sites") }

    var kioskModeEnabled by remember { mutableStateOf(false) }
    var silentAppInstall by remember { mutableStateOf(true) }
    var nonCompliantAction by remember { mutableStateOf("Notify Admin + Lock Device") }
    var gracePeriod by remember { mutableStateOf("4hrs") }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var isApplying by remember { mutableStateOf(false) }
    var lastApplySuccess by remember { mutableStateOf(0) }
    var lastApplyTotal by remember { mutableStateOf(0) }
    var totalDevices by remember { mutableStateOf(0) }
    var violations by remember { mutableStateOf(0) }
    val lastUpdated = remember {
        SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date())
    }

    LaunchedEffect(Unit) {
        try {
            val response = RemoteDataSource.getDashboardStats()
            if (response.isSuccessful) {
                val stats = response.body()
                totalDevices = stats?.totalDevices ?: 0
                violations = stats?.violations ?: 0
            }
        } catch (_: Exception) {
            // Keep defaults when server is temporarily unavailable.
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = bgColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // TOP BAR
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = PurpleCore
                            )
                        }
                        Column {
                            Text(
                                text = "Device Policies",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = textColor
                            )
                            Text(
                                text = "Last modified: $lastUpdated",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = "Save",
                            tint = PurpleCore
                        )
                    }
                }
            }

            // POLICY STATUS BANNER
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            color = Success.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.5.dp,
                            color = Success,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = Success,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "🛡️ POLICY ACTIVE",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = textColor
                            )
                            Text(
                                text = "Applied to $totalDevices devices • $violations violations",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            Text(
                                text = "Last pushed: $lastUpdated",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ═══════════════════════════════════
            // SECTION 1: PASSWORD & AUTHENTICATION
            // ═══════════════════════════════════
            item {
                SectionHeader("Password & Authentication")
            }

            item {
                PolicyToggleCard(
                    title = "🔒 Screen Lock Required",
                    description = "Require device screen lock (PIN/Pattern/Password)",
                    riskLevel = "HIGH",
                    isEnabled = screenLockRequired,
                    onToggle = { screenLockRequired = it },
                    isDark = isDark
                )
            }

            item {
                PolicySliderCard(
                    title = "🔑 Minimum Password Length",
                    minValue = 4f,
                    maxValue = 16f,
                    currentValue = minPasswordLength,
                    onValueChange = { minPasswordLength = it },
                    suffix = "chars",
                    isDark = isDark
                )
            }

            item {
                PolicyDropdownCard(
                    title = "🔐 Password Complexity",
                    currentValue = passwordComplexity,
                    options = listOf("PIN Only", "Alphanumeric", "Strong", "Biometric"),
                    onValueChange = { passwordComplexity = it },
                    isDark = isDark
                )
            }

            item {
                PolicyChipSelectorCard(
                    title = "⏱️ Auto-Lock Timeout",
                    selectedValue = autoLockTimeout,
                    options = listOf("1min", "5min", "15min", "30min", "Never"),
                    onValueChange = { autoLockTimeout = it },
                    isDark = isDark
                )
            }

            item {
                PolicyDropdownCard(
                    title = "🔄 Password Expiry",
                    currentValue = passwordExpiry,
                    options = listOf("Never", "30 Days", "60 Days", "90 Days"),
                    onValueChange = { passwordExpiry = it },
                    isDark = isDark
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // ═══════════════════════════════════
            // SECTION 2: DEVICE RESTRICTIONS
            // ═══════════════════════════════════
            item {
                SectionHeader("Device Restrictions")
            }

            item {
                PolicyToggleCard(
                    title = "🚫 Disable Camera",
                    description = "Prevent access to device camera",
                    isEnabled = cameraDisabled,
                    onToggle = { cameraDisabled = it },
                    isDark = isDark
                )
            }

            item {
                PolicyToggleCard(
                    title = "💾 Block USB Storage",
                    description = "Prevent USB file transfer",
                    isEnabled = usbStorageBlocked,
                    onToggle = { usbStorageBlocked = it },
                    isDark = isDark
                )
            }

            item {
                PolicyToggleCard(
                    title = "🔧 Block USB Debugging",
                    description = "Prevent ADB developer access",
                    riskLevel = "HIGH",
                    isEnabled = usbDebuggingBlocked,
                    onToggle = { usbDebuggingBlocked = it },
                    isDark = isDark
                )
            }

            item {
                PolicyToggleCard(
                    title = "📦 Restrict App Installation",
                    description = "Block unknown source installations",
                    isEnabled = unknownAppInstallBlocked,
                    onToggle = { unknownAppInstallBlocked = it },
                    isDark = isDark
                )
            }

            item {
                PolicyToggleCard(
                    title = "🏭 Factory Reset Protection",
                    description = "Prevent unauthorized factory reset",
                    riskLevel = "CRITICAL",
                    isEnabled = factoryResetProtection,
                    onToggle = { factoryResetProtection = it },
                    isDark = isDark
                )
            }

            item {
                PolicyToggleCard(
                    title = "📸 Block Screenshots",
                    description = "Prevent screen capture on managed apps",
                    isEnabled = screenshotBlocked,
                    onToggle = { screenshotBlocked = it },
                    isDark = isDark
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // ═══════════════════════════════════
            // SECTION 3: NETWORK & CONNECTIVITY
            // ═══════════════════════════════════
            item {
                SectionHeader("Network & Connectivity")
            }

            item {
                PolicyToggleCard(
                    title = "📶 WiFi Only Mode",
                    description = "Restrict data usage to WiFi networks only",
                    isEnabled = wifiOnlyMode,
                    onToggle = { wifiOnlyMode = it },
                    isDark = isDark
                )
            }

            item {
                PolicyToggleCard(
                    title = "🔒 Force VPN",
                    description = "Require VPN connection for corporate data",
                    isEnabled = forceVpn,
                    onToggle = { forceVpn = it },
                    isDark = isDark
                )
            }

            item {
                PolicyDropdownCard(
                    title = "📡 Bluetooth Restrictions",
                    currentValue = bluetoothRestriction,
                    options = listOf("Allow All", "Disable", "Managed Only"),
                    onValueChange = { bluetoothRestriction = it },
                    isDark = isDark
                )
            }

            item {
                PolicyDropdownCard(
                    title = "🌐 Browser Restrictions",
                    currentValue = browserRestriction,
                    options = listOf("Allow All Sites", "Block Adult", "Whitelist Only"),
                    onValueChange = { browserRestriction = it },
                    isDark = isDark
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // ═══════════════════════════════════
            // SECTION 4: APPLICATION MANAGEMENT
            // ═══════════════════════════════════
            item {
                SectionHeader("Application Management")
            }

            item {
                PolicyToggleCard(
                    title = "📱 Kiosk Mode",
                    description = "Lock device to single app",
                    isEnabled = kioskModeEnabled,
                    onToggle = { kioskModeEnabled = it },
                    isDark = isDark
                )
            }

            item {
                PolicyToggleCard(
                    title = "🔕 Silent App Install",
                    description = "Allow remote app deployment",
                    isEnabled = silentAppInstall,
                    onToggle = { silentAppInstall = it },
                    isDark = isDark
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurpleCore.copy(alpha = 0.1f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Apps,
                            contentDescription = null,
                            tint = PurpleCore,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Manage Allowed Apps →",
                            fontFamily = PlusJakartaSans,
                            color = PurpleCore,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
                Text(
                    text = "12 apps whitelisted",
                    fontFamily = PlusJakartaSans,
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // ═══════════════════════════════════
            // SECTION 5: COMPLIANCE ACTIONS
            // ═══════════════════════════════════
            item {
                SectionHeader("Compliance Actions")
            }

            item {
                PolicyDropdownCard(
                    title = "Non-Compliant Action",
                    currentValue = nonCompliantAction,
                    options = listOf(
                        "Notify Admin Only",
                        "Notify Admin + Lock Device",
                        "Wipe Device",
                        "Block Corporate Access"
                    ),
                    onValueChange = { nonCompliantAction = it },
                    isDark = isDark
                )
            }

            item {
                PolicyChipSelectorCard(
                    title = "Grace Period",
                    selectedValue = gracePeriod,
                    options = listOf("1hr", "4hrs", "24hrs", "48hrs"),
                    onValueChange = { gracePeriod = it },
                    isDark = isDark
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            // ═══════════════════════════════════
            // ACTION BUTTONS
            // ═══════════════════════════════════
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            isApplying = true
                            scope.launch {
                                val request = PolicyUpdateRequest(
                                    cameraDisabled = cameraDisabled,
                                    screenLockRequired = screenLockRequired,
                                    installBlocked = unknownAppInstallBlocked,
                                    uninstallBlocked = usbStorageBlocked,
                                    locationTrackingEnabled = !wifiOnlyMode
                                )

                                var total = 0
                                var success = 0
                                try {
                                    val devicesResponse = RemoteDataSource.getDeviceList()
                                    val devices = if (devicesResponse.isSuccessful) {
                                        devicesResponse.body().orEmpty()
                                    } else {
                                        emptyList()
                                    }

                                    total = devices.size
                                    for (device in devices) {
                                        val response = RemoteDataSource.updateDevicePolicy(device.deviceId, request)
                                        if (response.isSuccessful) {
                                            success++
                                        }
                                    }

                                    lastApplyTotal = total
                                    lastApplySuccess = success

                                    if (total == 0) {
                                        snackbarHostState.showSnackbar("No devices found for policy push")
                                    } else {
                                        snackbarHostState.showSnackbar("Policy pushed to $success/$total devices")
                                    }
                                } catch (e: Exception) {
                                    lastApplyTotal = 0
                                    lastApplySuccess = 0
                                    snackbarHostState.showSnackbar("Policy push failed: ${e.message}")
                                }

                                delay(300)
                                isApplying = false
                                showSuccessDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurpleCore
                        ),
                        enabled = !isApplying
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(
                                color = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "🛡️ Apply Policies to All Devices",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PurpleCore
                        )
                    ) {
                        Text(
                            text = "Save as Policy Template",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }

        // SUCCESS DIALOG
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                confirmButton = {
                    Button(
                        onClick = { showSuccessDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleCore)
                    ) {
                        Text(
                            text = "OK",
                            fontFamily = PlusJakartaSans,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                },
                title = {
                    Text(
                        text = "✅ Policies Applied Successfully",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = if (lastApplyTotal > 0) {
                                "Pushed to $lastApplySuccess/$lastApplyTotal devices"
                            } else {
                                "Policy push attempted"
                            },
                            fontFamily = PlusJakartaSans,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Next sync: 15 minutes",
                            fontFamily = PlusJakartaSans,
                            fontSize = 14.sp
                        )
                    }
                }
            )
        }
    }
}

// ══════════════════════════════════════
// COMPOSABLE COMPONENTS
// ══════════════════════════════════════

@Composable
fun PolicyToggleCard(
    title: String,
    description: String? = null,
    riskLevel: String? = null,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    val riskColor = when (riskLevel) {
        "HIGH" -> Danger
        "CRITICAL" -> Danger
        else -> Success
    }

    DevoraCard(isDark = isDark) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = textColor
                )
                if (description != null) {
                    Text(
                        text = description,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                if (riskLevel != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Risk Level: $riskLevel",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = riskColor
                    )
                }
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
fun PolicySliderCard(
    title: String,
    minValue: Float,
    maxValue: Float,
    currentValue: Float,
    onValueChange: (Float) -> Unit,
    suffix: String = "",
    isDark: Boolean
) {
    val textColor = if (isDark) DarkTextPrimary else TextPrimary

    DevoraCard(isDark = isDark) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = textColor
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Slider(
                    value = currentValue,
                    onValueChange = onValueChange,
                    valueRange = minValue..maxValue,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Current: ${currentValue.toInt()} $suffix",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = PurpleCore
                )
            }
        }
    }
}

@Composable
fun PolicyDropdownCard(
    title: String,
    currentValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    var expanded by remember { mutableStateOf(false) }

    DevoraCard(isDark = isDark) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = textColor
            )
            Box {
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "▼ $currentValue",
                        fontFamily = PlusJakartaSans,
                        color = PurpleCore,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    fontFamily = PlusJakartaSans
                                )
                            },
                            onClick = {
                                onValueChange(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PolicyChipSelectorCard(
    title: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) DarkTextPrimary else TextPrimary

    DevoraCard(isDark = isDark) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = textColor
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEach { option ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (option == selectedValue) PurpleCore
                                else PurpleCore.copy(alpha = 0.1f)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clickable { onValueChange(option) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = if (option == selectedValue) androidx.compose.ui.graphics.Color.White else PurpleCore
                        )
                    }
                }
            }
        }
    }
}

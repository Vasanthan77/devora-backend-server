package com.devora.devicemanager.ui.screens.deviceinfo

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devora.devicemanager.collector.AppInfo
import com.devora.devicemanager.collector.FieldStatus
import com.devora.devicemanager.ui.theme.BgBase
import com.devora.devicemanager.ui.theme.BgElevated
import com.devora.devicemanager.ui.theme.BgSurface
import com.devora.devicemanager.ui.theme.DMSans
import com.devora.devicemanager.ui.theme.DarkBgBase
import com.devora.devicemanager.ui.theme.DarkBgSurface
import com.devora.devicemanager.ui.theme.DarkTextPrimary
import com.devora.devicemanager.ui.theme.Danger
import com.devora.devicemanager.ui.theme.PlusJakartaSans
import com.devora.devicemanager.ui.theme.PurpleBright
import com.devora.devicemanager.ui.theme.PurpleCore
import com.devora.devicemanager.ui.theme.PurpleDeep
import com.devora.devicemanager.ui.theme.PurpleDim
import com.devora.devicemanager.ui.theme.Success
import com.devora.devicemanager.ui.theme.TextMuted
import com.devora.devicemanager.ui.theme.TextPrimary
import com.devora.devicemanager.ui.theme.TextSecondary
import com.devora.devicemanager.ui.theme.Warning

@Composable
fun DeviceInfoScreen(
    onBack: () -> Unit,
    isDark: Boolean,
    viewModel: DeviceInfoViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val bg = if (isDark) DarkBgBase else BgBase
    val cardBg = if (isDark) DarkBgSurface else BgSurface
    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    val mutedColor = if (isDark) Color(0xFF6B7D8D) else TextMuted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // ── Top Bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor
                )
            }
            Text(
                "Device Information",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { viewModel.collectDeviceInfo() },
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = PurpleCore
                    )
                } else {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = PurpleCore
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ── Sync Status Banner ──
            SyncStatusBanner(
                lastSyncTime = state.lastSyncTime,
                lastSyncStatus = state.lastSyncStatus,
                syncMessage = state.syncMessage,
                isDark = isDark
            )

            Spacer(Modifier.height(16.dp))

            val info = state.deviceInfo
            if (info != null) {
                // ── Mandatory Fields Card ──
                SectionCard(
                    title = "Hardware Info",
                    icon = Icons.Filled.Smartphone,
                    cardBg = cardBg,
                    textColor = textColor,
                    isDark = isDark
                ) {
                    InfoRow("Device Model", info.model, FieldStatus.AVAILABLE, mutedColor, textColor)
                    InfoRow("Manufacturer", info.manufacturer, FieldStatus.AVAILABLE, mutedColor, textColor)
                    InfoRow("Brand", info.brand, FieldStatus.AVAILABLE, mutedColor, textColor)
                    InfoRow("Board", info.board, FieldStatus.AVAILABLE, mutedColor, textColor)
                    InfoRow("Android Version", info.osVersion, FieldStatus.AVAILABLE, mutedColor, textColor)
                    InfoRow("SDK Version", info.sdkVersion.toString(), FieldStatus.AVAILABLE, mutedColor, textColor)
                    InfoRow("Device ID (UUID)", info.deviceId, FieldStatus.AVAILABLE, mutedColor, textColor)
                }

                Spacer(Modifier.height(12.dp))

                // ── Restricted Identifiers Card ──
                SectionCard(
                    title = "Restricted Identifiers",
                    icon = Icons.Filled.Security,
                    cardBg = cardBg,
                    textColor = textColor,
                    isDark = isDark
                ) {
                    InfoRow(
                        "Serial Number",
                        info.serialNumber ?: statusLabel(info.serialStatus),
                        info.serialStatus,
                        mutedColor,
                        textColor
                    )
                    InfoRow(
                        "IMEI",
                        info.imei ?: statusLabel(info.imeiStatus),
                        info.imeiStatus,
                        mutedColor,
                        textColor
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Device Classification Card ──
                SectionCard(
                    title = "Classification",
                    icon = Icons.Filled.DeviceHub,
                    cardBg = cardBg,
                    textColor = textColor,
                    isDark = isDark
                ) {
                    InfoRow("Device Type", info.deviceType, FieldStatus.AVAILABLE, mutedColor, textColor)
                    InfoRow("Collected At", formatTimestamp(info.collectedAt), FieldStatus.AVAILABLE, mutedColor, textColor)
                }

                Spacer(Modifier.height(12.dp))

                // ── API Restrictions Summary ──
                SectionCard(
                    title = "API Restriction Status",
                    icon = Icons.Filled.Lock,
                    cardBg = cardBg,
                    textColor = textColor,
                    isDark = isDark
                ) {
                    RestrictionRow("Serial Number", info.serialRestricted, mutedColor, textColor)
                    RestrictionRow("IMEI", info.imeiRestricted, mutedColor, textColor)
                }

                Spacer(Modifier.height(12.dp))

                // ── App Search ──
                var appSearchQuery by remember { mutableStateOf("") }
                val allApps = state.apps
                val filteredApps = if (appSearchQuery.isBlank()) allApps
                    else allApps.filter {
                        it.appName.contains(appSearchQuery, ignoreCase = true) ||
                        it.packageName.contains(appSearchQuery, ignoreCase = true)
                    }
                val userApps = filteredApps.filter { !it.isSystemApp }
                val systemApps = filteredApps.filter { it.isSystemApp }

                OutlinedTextField(
                    value = appSearchQuery,
                    onValueChange = { appSearchQuery = it },
                    placeholder = {
                        Text(
                            "Search apps...",
                            fontFamily = DMSans,
                            fontSize = 14.sp,
                            color = mutedColor
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = PurpleCore)
                    },
                    trailingIcon = {
                        if (appSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { appSearchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = mutedColor)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleCore,
                        unfocusedBorderColor = mutedColor.copy(alpha = 0.3f),
                        cursorColor = PurpleCore,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
                )

                // ── User Apps Card ──
                SectionCard(
                    title = "User Apps (${userApps.size})",
                    icon = Icons.Filled.Info,
                    cardBg = cardBg,
                    textColor = textColor,
                    isDark = isDark
                ) {
                    if (userApps.isEmpty()) {
                        Text(
                            if (appSearchQuery.isNotBlank()) "No matching user apps" else "No user apps",
                            fontFamily = DMSans, fontSize = 13.sp, color = mutedColor
                        )
                    } else {
                        userApps.forEach { app ->
                            AppRow(app.appName, app.packageName, app.versionName, mutedColor, textColor)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── System Apps Card ──
                SectionCard(
                    title = "System Apps (${systemApps.size})",
                    icon = Icons.Filled.PhoneAndroid,
                    cardBg = cardBg,
                    textColor = textColor,
                    isDark = isDark
                ) {
                    if (systemApps.isEmpty()) {
                        Text(
                            if (appSearchQuery.isNotBlank()) "No matching system apps" else "No system apps",
                            fontFamily = DMSans, fontSize = 13.sp, color = mutedColor
                        )
                    } else {
                        systemApps.forEach { app ->
                            AppRow(app.appName, app.packageName, app.versionName, mutedColor, textColor)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Sync Button ──
                Button(
                    onClick = { viewModel.syncToBackend() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !state.isSyncing,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleCore,
                        disabledContainerColor = PurpleDim
                    )
                ) {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Syncing...", color = Color.White, fontFamily = DMSans)
                    } else {
                        Icon(Icons.Filled.Sync, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Sync to Backend",
                            color = Color.White,
                            fontFamily = DMSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // ── Error Message ──
            state.errorMessage?.let { error ->
                Text(
                    error,
                    color = Danger,
                    fontFamily = DMSans,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// COMPONENTS
// ═══════════════════════════════════════════════════════════

@Composable
private fun SyncStatusBanner(
    lastSyncTime: String?,
    lastSyncStatus: String?,
    syncMessage: String?,
    isDark: Boolean
) {
    val (bannerColor, bannerIcon, bannerText) = when (lastSyncStatus) {
        "SUCCESS" -> Triple(
            Success.copy(alpha = 0.12f),
            Icons.Filled.CloudDone,
            "Last synced: ${formatTimestamp(lastSyncTime ?: "")}"
        )
        "FAILED" -> Triple(
            Danger.copy(alpha = 0.12f),
            Icons.Filled.CloudOff,
            syncMessage ?: "Sync failed"
        )
        else -> Triple(
            PurpleDim,
            Icons.Filled.Cloud,
            "Not yet synced"
        )
    }

    val iconTint = when (lastSyncStatus) {
        "SUCCESS" -> Success
        "FAILED" -> Danger
        else -> PurpleCore
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bannerColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(bannerIcon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            bannerText,
            fontFamily = DMSans,
            fontSize = 13.sp,
            color = if (isDark) Color.White.copy(alpha = 0.8f) else TextSecondary
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    cardBg: Color,
    textColor: Color,
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(PurpleDim),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PurpleCore, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = textColor
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    status: FieldStatus,
    mutedColor: Color,
    textColor: Color
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                fontFamily = DMSans,
                fontSize = 13.sp,
                color = mutedColor
            )
            StatusBadge(status)
        }
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            fontFamily = DMSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = textColor
        )
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = mutedColor.copy(alpha = 0.2f))
    }
}

@Composable
private fun StatusBadge(status: FieldStatus) {
    val (text, color) = when (status) {
        FieldStatus.AVAILABLE -> "AVAILABLE" to Success
        FieldStatus.RESTRICTED -> "RESTRICTED" to Warning
        FieldStatus.UNAVAILABLE -> "UNAVAILABLE" to mutedTextColor()
    }
    Text(
        text,
        fontFamily = DMSans,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun RestrictionRow(
    label: String,
    restricted: Boolean,
    mutedColor: Color,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontFamily = DMSans, fontSize = 14.sp, color = textColor)
        Text(
            if (restricted) "BLOCKED" else "ALLOWED",
            fontFamily = DMSans,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (restricted) Danger else Success,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (restricted) Danger.copy(alpha = 0.12f) else Success.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun mutedTextColor(): Color = TextMuted

@Composable
private fun AppRow(
    appName: String,
    packageName: String,
    versionName: String,
    mutedColor: Color,
    textColor: Color
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            appName,
            fontFamily = DMSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "$packageName  •  v$versionName",
            fontFamily = DMSans,
            fontSize = 11.sp,
            color = mutedColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = mutedColor.copy(alpha = 0.15f))
    }
}

private fun statusLabel(status: FieldStatus): String = when (status) {
    FieldStatus.AVAILABLE -> "Available"
    FieldStatus.RESTRICTED -> "Restricted by Android API"
    FieldStatus.UNAVAILABLE -> "Not available on this device"
}

private fun formatTimestamp(iso: String): String {
    if (iso.isBlank()) return "—"
    return try {
        val instant = java.time.Instant.parse(iso)
        val zdt = instant.atZone(java.time.ZoneId.systemDefault())
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")
        zdt.format(formatter)
    } catch (_: Exception) {
        iso
    }
}

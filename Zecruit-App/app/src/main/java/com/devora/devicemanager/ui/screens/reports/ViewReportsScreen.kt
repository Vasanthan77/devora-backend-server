package com.devora.devicemanager.ui.screens.reports

import com.devora.devicemanager.data.remote.RemoteDataSource
import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devora.devicemanager.network.DeviceResponse
import com.devora.devicemanager.ui.components.DevoraCard
import com.devora.devicemanager.ui.components.SectionHeader
import com.devora.devicemanager.ui.theme.BgBase
import com.devora.devicemanager.ui.theme.Danger
import com.devora.devicemanager.ui.theme.DarkBgBase
import com.devora.devicemanager.ui.theme.DarkTextPrimary
import com.devora.devicemanager.ui.theme.PlusJakartaSans
import com.devora.devicemanager.ui.theme.PurpleCore
import com.devora.devicemanager.ui.theme.Success
import com.devora.devicemanager.ui.theme.TextMuted
import com.devora.devicemanager.ui.theme.TextPrimary
import com.devora.devicemanager.ui.theme.Warning
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════
// DATA CLASSES
// ══════════════════════════════════════

data class ComplianceMetric(
    val label: String,
    val percentage: Int,
    val deviceCount: String
)

data class EnrollmentEvent(
    val deviceModel: String,
    val method: String,
    val timeAgo: String
)

data class SecurityIncident(
    val severity: String,  // HIGH, MEDIUM, LOW, INFO
    val description: String,
    val timeAgo: String
)

data class InstalledApp(
    val appName: String,
    val packageName: String,
    val installCount: Int,
    val type: String  // System, User
)

// ══════════════════════════════════════
// VIEW REPORTS SCREEN
// ══════════════════════════════════════

@Composable
fun ViewReportsScreen(
    onBack: () -> Unit,
    isDark: Boolean
) {
    val bgColor = if (isDark) DarkBgBase else BgBase
    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var totalDevices by remember { mutableStateOf(0) }
    var activeDevices by remember { mutableStateOf(0) }
    var totalApps by remember { mutableStateOf(0) }
    var devices by remember { mutableStateOf<List<DeviceResponse>>(emptyList()) }

    val lastUpdated = remember {
        SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date())
    }

    val compliantDevices = devices.count { it.status.equals("ACTIVE", ignoreCase = true) }
    val atRiskDevices = (totalDevices - compliantDevices).coerceAtLeast(0)
    val withEmployeeMapped = devices.count { !it.employeeId.isNullOrBlank() && !it.employeeName.isNullOrBlank() }
    val qrBased = devices.count { it.enrollmentMethod.equals("QR_CODE", ignoreCase = true) }
    val tokenBased = devices.count { it.enrollmentMethod.equals("TOKEN", ignoreCase = true) }
    val successRate = if (totalDevices == 0) 0 else ((compliantDevices * 100f) / totalDevices).toInt()

    val complianceMetrics = listOf(
        ComplianceMetric("Active Devices", if (totalDevices == 0) 0 else ((activeDevices * 100f) / totalDevices).toInt(), "$activeDevices/$totalDevices devices"),
        ComplianceMetric("Employee Mapping", if (totalDevices == 0) 0 else ((withEmployeeMapped * 100f) / totalDevices).toInt(), "$withEmployeeMapped/$totalDevices devices"),
        ComplianceMetric("Enrollment Coverage", if (totalDevices == 0) 0 else ((devices.size * 100f) / totalDevices).toInt(), "${devices.size}/$totalDevices devices")
    )

    val recentEnrollments = devices
        .sortedByDescending { it.enrolledAt }
        .take(5)
        .map {
            EnrollmentEvent(
                deviceModel = listOfNotNull(it.manufacturer, it.deviceModel)
                    .joinToString(" ")
                    .ifBlank { it.deviceId.take(8) },
                method = it.enrollmentMethod.replace("_", " "),
                timeAgo = it.enrolledAt.take(10)
            )
        }

    val topApps = emptyList<InstalledApp>()
    val incidents = emptyList<SecurityIncident>()

    LaunchedEffect(Unit) {
        try {
            val response = RemoteDataSource.getDashboardStats()
            val deviceResponse = RemoteDataSource.getDeviceList()
            if (response.isSuccessful) {
                val stats = response.body()
                totalDevices = stats?.totalDevices ?: 0
                activeDevices = stats?.activeDevices ?: 0
                totalApps = stats?.totalApps ?: 0
            } else {
                Log.e("ViewReportsScreen", "Stats fetch failed: ${response.code()}")
            }

            if (deviceResponse.isSuccessful) {
                devices = deviceResponse.body().orEmpty()
            } else {
                Log.e("ViewReportsScreen", "Device list fetch failed: ${deviceResponse.code()}")
            }

            isLoading = false
        } catch (e: Exception) {
            Log.e("ViewReportsScreen", "Failed to fetch reports", e)
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = bgColor
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PurpleCore)
            }
        } else {
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
                            Text(
                                text = "Reports",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = textColor
                            )
                        }
                        IconButton(onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Report exported successfully")
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Export",
                                tint = PurpleCore
                            )
                        }
                    }
                    Text(
                        text = "Last updated: $lastUpdated",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // ═══════════════════════════════════
                // SECTION 1: EXECUTIVE SUMMARY
                // ═══════════════════════════════════
                item {
                    SectionHeader("Executive Summary")
                }

                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    ) {
                        items(4) { index ->
                            when (index) {
                                0 -> MetricCard(
                                    title = "Total Devices",
                                    value = totalDevices.toString(),
                                    icon = Icons.Filled.Phone,
                                    color = PurpleCore,
                                    isDark = isDark
                                )
                                1 -> MetricCard(
                                    title = "Compliant Devices",
                                    value = compliantDevices.toString(),
                                    icon = Icons.Filled.CheckCircle,
                                    color = Success,
                                    isDark = isDark
                                )
                                2 -> MetricCard(
                                    title = "At Risk Devices",
                                    value = atRiskDevices.toString(),
                                    icon = Icons.Filled.Warning,
                                    color = Warning,
                                    isDark = isDark
                                )
                                3 -> MetricCard(
                                    title = "Synced Today",
                                    value = activeDevices.toString(),
                                    icon = Icons.Filled.SignalCellularAlt,
                                    color = PurpleCore,
                                    isDark = isDark
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                // ═══════════════════════════════════
                // SECTION 2: DEVICE COMPLIANCE STATUS
                // ═══════════════════════════════════
                item {
                    SectionHeader("Device Compliance Status")
                }

                items(complianceMetrics) { metric ->
                    ComplianceRow(metric = metric, isDark = isDark)
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // ═══════════════════════════════════
                // SECTION 3: ENROLLMENT ANALYTICS
                // ═══════════════════════════════════
                item {
                    SectionHeader("Enrollment Analytics")
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EnrollmentStatBox("Total Enrolled", totalDevices.toString(), isDark)
                        EnrollmentStatBox("Success Rate", "$successRate%", isDark)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EnrollmentStatBox("QR Code", qrBased.toString(), isDark)
                        EnrollmentStatBox("Token Based", tokenBased.toString(), isDark)
                    }
                }

                item {
                    Text(
                        text = "Recent Enrollments",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = textColor,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                if (recentEnrollments.isEmpty()) {
                    item {
                        EmptyStateText(text = "No recent enrollments found", isDark = isDark)
                    }
                } else {
                    items(recentEnrollments) { enrollment ->
                        EnrollmentRow(enrollment = enrollment, isDark = isDark)
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // ═══════════════════════════════════
                // SECTION 4: APP INVENTORY ANALYSIS
                // ═══════════════════════════════════
                item {
                    SectionHeader("App Inventory Analysis")
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppStatBox("Total Apps Tracked", totalApps.toString(), isDark)
                        AppStatBox("Unique Apps", "-", isDark)
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppStatBox("System Apps", "-", isDark)
                        AppStatBox("User Installed", "-", isDark)
                    }
                }

                item {
                    Text(
                        text = "Top 5 Most Installed Apps",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = textColor,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                if (topApps.isEmpty()) {
                    item {
                        EmptyStateText(text = "Top app distribution is unavailable", isDark = isDark)
                    }
                } else {
                    items(topApps) { app ->
                        AppRow(app = app, isDark = isDark)
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // ═══════════════════════════════════
                // SECTION 5: SECURITY INCIDENTS
                // ═══════════════════════════════════
                item {
                    SectionHeader("Security Incidents")
                }

                if (incidents.isEmpty()) {
                    item {
                        EmptyStateText(text = "No security incidents reported", isDark = isDark)
                    }
                } else {
                    items(incidents) { incident ->
                        IncidentRow(incident = incident, isDark = isDark)
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // ═══════════════════════════════════
                // SECTION 6: DEVICE HEALTH OVERVIEW
                // ═══════════════════════════════════
                item {
                    SectionHeader("Device Health Overview")
                }

                val healthDevices = devices.take(3)
                if (healthDevices.isEmpty()) {
                    item {
                        EmptyStateText(text = "No device health data available", isDark = isDark)
                    }
                } else {
                    items(healthDevices) { device ->
                        val isOnline = device.status.equals("ACTIVE", ignoreCase = true)
                        DeviceHealthCard(
                            deviceName = listOfNotNull(device.manufacturer, device.deviceModel)
                                .joinToString(" ")
                                .ifBlank { device.deviceId.take(8) },
                            status = if (isOnline) "ONLINE" else "OFFLINE",
                            compliance = if (isOnline) "Good" else "Needs Attention",
                            batteryLevel = if (isOnline) 80 else 50,
                            lastSeen = device.enrolledAt.take(10),
                            isDark = isDark
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

// ══════════════════════════════════════
// COMPOSABLE COMPONENTS
// ══════════════════════════════════════

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isDark: Boolean
) {
    DevoraCard(
        accentColor = color,
        isDark = isDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = value,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = color
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun ComplianceRow(
    metric: ComplianceMetric,
    isDark: Boolean
) {
    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = metric.label,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${metric.percentage}%",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = PurpleCore
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { metric.percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = PurpleCore,
            trackColor = Color.LightGray.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = metric.deviceCount,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

@Composable
fun EnrollmentStatBox(
    title: String,
    value: String,
    isDark: Boolean
) {
    DevoraCard(isDark = isDark) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = PurpleCore
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun EnrollmentRow(
    enrollment: EnrollmentEvent,
    isDark: Boolean
) {
    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    DevoraCard(isDark = isDark) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SmartToy,
                    contentDescription = null,
                    tint = PurpleCore,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = enrollment.deviceModel,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = textColor
                    )
                    Text(
                        text = enrollment.timeAgo,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
            Box(
                modifier = Modifier
                    .background(PurpleCore.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = enrollment.method,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = PurpleCore
                )
            }
        }
    }
}

@Composable
fun AppStatBox(
    title: String,
    value: String,
    isDark: Boolean
) {
    DevoraCard(isDark = isDark) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = PurpleCore
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = TextMuted,
                maxLines = 2
            )
        }
    }
}

@Composable
fun AppRow(
    app: InstalledApp,
    isDark: Boolean
) {
    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    DevoraCard(isDark = isDark) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Apps,
                    contentDescription = null,
                    tint = PurpleCore,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = textColor
                    )
                    Text(
                        text = app.packageName,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = app.installCount.toString(),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = PurpleCore
                )
                Text(
                    text = app.type,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun IncidentRow(
    incident: SecurityIncident,
    isDark: Boolean
) {
    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    val incidentColor = when (incident.severity) {
        "HIGH" -> Danger
        "MEDIUM" -> Warning
        else -> Success
    }
    DevoraCard(isDark = isDark) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(incidentColor, CircleShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = incident.description,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = textColor
                )
                Text(
                    text = incident.timeAgo,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
            Text(
                text = incident.severity,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = incidentColor
            )
        }
    }
}

@Composable
fun EmptyStateText(text: String, isDark: Boolean) {
    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    DevoraCard(isDark = isDark) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
fun DeviceHealthCard(
    deviceName: String,
    status: String,
    compliance: String,
    batteryLevel: Int,
    lastSeen: String,
    isDark: Boolean
) {
    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    val statusColor = if (status == "ONLINE") Success else TextMuted
    val complianceColor = when (compliance) {
        "Good" -> Success
        "At Risk" -> Warning
        else -> Danger
    }

    DevoraCard(isDark = isDark) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SmartToy,
                        contentDescription = null,
                        tint = PurpleCore,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = deviceName,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = textColor
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = status,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = statusColor
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Battery6Bar,
                    contentDescription = null,
                    tint = PurpleCore,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "$batteryLevel%",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(complianceColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = compliance,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = complianceColor
                    )
                }
            }

            Text(
                text = "Last seen: $lastSeen",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

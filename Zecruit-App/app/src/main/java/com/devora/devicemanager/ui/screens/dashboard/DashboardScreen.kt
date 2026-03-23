package com.devora.devicemanager.ui.screens.dashboard

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devora.devicemanager.ui.components.DevoraBottomNav
import com.devora.devicemanager.ui.components.DevoraCard
import com.devora.devicemanager.ui.components.SectionHeader
import com.devora.devicemanager.network.DeviceActivityResponse
import com.devora.devicemanager.data.remote.RemoteDataSource
import com.devora.devicemanager.session.SessionManager
import com.devora.devicemanager.ui.theme.BgBase
import com.devora.devicemanager.ui.theme.BgElevated
import com.devora.devicemanager.ui.theme.DMSans
import com.devora.devicemanager.ui.theme.Danger
import com.devora.devicemanager.ui.theme.DarkBgBase
import com.devora.devicemanager.ui.theme.JetBrainsMono
import com.devora.devicemanager.ui.theme.PlusJakartaSans
import com.devora.devicemanager.ui.theme.PurpleBright
import com.devora.devicemanager.ui.theme.PurpleBorder
import com.devora.devicemanager.ui.theme.PurpleCore
import com.devora.devicemanager.ui.theme.PurpleDeep
import com.devora.devicemanager.ui.theme.PurpleDim
import com.devora.devicemanager.ui.theme.Success
import com.devora.devicemanager.ui.theme.TextMuted
import com.devora.devicemanager.ui.theme.TextPrimary
import com.devora.devicemanager.ui.theme.DarkTextPrimary
import com.devora.devicemanager.ui.theme.Warning as WarningColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.viewmodel.compose.viewModel

// ══════════════════════════════════════
// STAT DATA CLASS
// ══════════════════════════════════════

private data class Stat(
    val value: String,
    val label: String,
    val icon: ImageVector,
    val color: Color
)

// ══════════════════════════════════════
// ACTIVITY DATA
// ══════════════════════════════════════

private data class Activity(
    val id: Long,
    val description: String,
    val device: String,
    val time: String,
    val color: Color
)

private fun getTimeAgo(timestamp: String?, currentTime: Long): String {
    if (timestamp.isNullOrEmpty()) return "Just now"
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.parse(timestamp)
        val diff = currentTime - (date?.time ?: currentTime)

        when {
            diff < 60_000L -> "Just now"
            diff < 3_600_000L -> "${diff / 60_000L} min ago"
            diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
            diff < 604_800_000L -> "${diff / 86_400_000L}d ago"
            else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(date ?: Date())
        }
    } catch (e: Exception) {
        "Unknown"
    }
}

// ══════════════════════════════════════
// DASHBOARD SCREEN
// ══════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    isDark: Boolean,
    onThemeToggle: () -> Unit
) {
    val context = LocalContext.current
    val dashboardViewModel: DashboardViewModel = viewModel()
    val dashboardUiState by dashboardViewModel.uiState.collectAsState()

    val bgColor = if (isDark) DarkBgBase else BgBase
    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    val adminName = remember { SessionManager.getAdminName(context).trim() }
    val displayAdminName = adminName.ifBlank { "Admin" }
    val adminInitial = displayAdminName.take(1).uppercase()
    val currentDate = remember {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
    }

    // Keep a clock ticking so timestamps auto-update
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L) // update every 1 minute
            currentTime = System.currentTimeMillis()
        }
    }

    val dashboardStats = dashboardUiState.stats
    val recentActivities: List<DeviceActivityResponse> = dashboardUiState.recentActivities
    var activities by remember { mutableStateOf(recentActivities) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(recentActivities) {
        activities = recentActivities
    }

    suspend fun loadActivities() {
        try {
            val response = RemoteDataSource.getActivities(limit = 10)
            if (response.isSuccessful) {
                activities = response.body() ?: emptyList()
            }
        } catch (_: Exception) {
            // Keep current list if reload fails.
        }
    }

    suspend fun deleteActivity(activityId: Long) {
        try {
            val response = RemoteDataSource.deleteActivity(activityId)
            if (response.isSuccessful) {
                snackbarHostState.showSnackbar(
                    message = "Activity removed",
                    duration = SnackbarDuration.Short
                )
            } else {
                loadActivities()
            }
        } catch (_: Exception) {
            loadActivities()
        }
    }

    val totalDevices = dashboardStats?.totalDevices ?: 0
    val activeDevices = dashboardStats?.activeDevices ?: 0
    val inactiveDevices = (totalDevices - activeDevices).coerceAtLeast(0)
    val onlineRatio = if (totalDevices > 0) activeDevices.toFloat() / totalDevices else 0f
    val onlinePercent = (onlineRatio * 100).toInt()

    val stats = listOf(
        Stat(totalDevices.toString(), "TOTAL DEVICES", Icons.Filled.Devices, PurpleCore),
        Stat(activeDevices.toString(), "ACTIVE NOW", Icons.Filled.CheckCircle, Success),
        Stat(inactiveDevices.toString(), "OFFLINE", Icons.Filled.Schedule, WarningColor)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            DevoraBottomNav(
                currentRoute = "dashboard",
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
                    Column {
                        Text(
                            text = "DEVORA",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = PurpleCore
                        )
                        Text(
                            text = "Admin Panel",
                            fontFamily = DMSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onThemeToggle) {
                            Icon(
                                imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "Toggle theme",
                                tint = PurpleCore
                            )
                        }
                    }
                }
            }

            // GREETING CARD
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    DevoraCard(showTopAccent = true, isDark = isDark) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Hi $displayAdminName",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentDate,
                                    fontFamily = DMSans,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
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
                                    fontSize = 20.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // STATS ROW
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(stats) { stat ->
                        Box(modifier = Modifier.width(150.dp).height(100.dp)) {
                            DevoraCard(
                                accentColor = stat.color,
                                isDark = isDark
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Icon top-right
                                    Icon(
                                        imageVector = stat.icon,
                                        contentDescription = null,
                                        tint = stat.color.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .align(Alignment.TopEnd)
                                    )
                                    // Value and label bottom-left
                                    Column(
                                        modifier = Modifier.align(Alignment.BottomStart)
                                    ) {
                                        Text(
                                            text = stat.value,
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 30.sp,
                                            color = stat.color
                                        )
                                        Text(
                                            text = stat.label,
                                            fontFamily = JetBrainsMono,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 10.sp,
                                            color = TextMuted,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // DEVICE HEALTH
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    DevoraCard(
                        accentColor = PurpleCore,
                        isDark = isDark
                    ) {
                        Column {
                            SectionHeader(
                                title = "DEVICE HEALTH",
                                actionText = "$totalDevices Total",
                                isDark = isDark
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$activeDevices ONLINE",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Success
                                )
                                Text(
                                    text = "$inactiveDevices OFFLINE",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Health bar
                            val barBg = if (isDark) com.devora.devicemanager.ui.theme.DarkBgElevated else BgElevated
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .drawBehind {
                                        drawRoundRect(
                                            color = barBg,
                                            cornerRadius = CornerRadius(6.dp.toPx())
                                        )
                                        drawRoundRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(PurpleCore, PurpleBright)
                                            ),
                                            size = Size(size.width * onlineRatio, size.height),
                                            cornerRadius = CornerRadius(6.dp.toPx())
                                        )
                                    }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$onlinePercent%",
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "${100 - onlinePercent}%",
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // RECENT ACTIVITY HEADER
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader(
                        title = "RECENT ACTIVITY",
                        actionText = "See All",
                        onActionClick = { },
                        isDark = isDark
                    )
                }
            }

            // ACTIVITY ROWS
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                                        imageVector = Icons.Filled.Schedule,
                                        contentDescription = null,
                                        tint = TextMuted.copy(alpha = 0.5f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No recent activity",
                                        fontFamily = DMSans,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        } else {
                            Column {
                                activities.forEachIndexed { index, activity ->
                                    key(activity.id) {
                                        val dismissState = rememberDismissState(
                                            confirmStateChange = { dismissValue ->
                                                if (dismissValue == DismissValue.DismissedToStart) {
                                                    coroutineScope.launch {
                                                        deleteActivity(activity.id)
                                                    }
                                                    true
                                                } else {
                                                    false
                                                }
                                            }
                                        )

                                        if (dismissState.isDismissed(DismissDirection.EndToStart)) {
                                            LaunchedEffect(activity.id) {
                                                activities = activities.filter { it.id != activity.id }
                                            }
                                        }

                                        SwipeToDismiss(
                                            state = dismissState,
                                            directions = setOf(DismissDirection.EndToStart),
                                            background = {
                                                val showBackground =
                                                    dismissState.dismissDirection == DismissDirection.EndToStart
                                                val color by animateColorAsState(
                                                    targetValue = if (showBackground) {
                                                        Color(0xFFF44336)
                                                    } else {
                                                        Color.Transparent
                                                    },
                                                    label = "swipe_bg"
                                                )
                                                val alpha by animateFloatAsState(
                                                    targetValue = if (showBackground) 1f else 0f,
                                                    label = "swipe_alpha"
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(color)
                                                        .padding(end = 20.dp),
                                                    contentAlignment = Alignment.CenterEnd
                                                ) {
                                                    if (alpha > 0f) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.DeleteOutline,
                                                                contentDescription = "Delete",
                                                                tint = Color.White.copy(alpha = alpha),
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "Delete",
                                                                color = Color.White.copy(alpha = alpha),
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            dismissContent = {
                                                ActivityItemCard(
                                                    activity = activity,
                                                    currentTime = currentTime
                                                )
                                            }
                                        )
                                    }
                                    if (index < activities.size - 1) {
                                        HorizontalDivider(
                                            thickness = 1.dp,
                                            color = if (isDark) {
                                                PurpleCore.copy(alpha = 0.10f)
                                            } else {
                                                BgElevated
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "\u2190 Swipe left to remove",
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, top = 4.dp, bottom = 8.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

}

@Composable
private fun ActivityItemCard(
    activity: DeviceActivityResponse,
    currentTime: Long
) {
    val severityColor = when (activity.severity) {
        "CRITICAL" -> Color(0xFFF44336)
        "WARNING" -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(severityColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.description ?: "Unknown activity",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = activity.deviceId
                        ?.takeLast(8)
                        ?.let { "Device \u00b7\u00b7\u00b7$it" }
                        ?: activity.employeeName
                        ?: "",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = getTimeAgo(activity.createdAt, currentTime),
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.End
            )
        }
    }
}

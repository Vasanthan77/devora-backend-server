package com.devora.devicemanager.ui.screens.devices

import com.devora.devicemanager.data.remote.RemoteDataSource
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devora.devicemanager.network.DeviceResponse
import com.devora.devicemanager.ui.components.DevoraBottomNav
import com.devora.devicemanager.ui.components.DevoraCard
import com.devora.devicemanager.ui.components.StatusBadge
import com.devora.devicemanager.ui.theme.BgBase
import com.devora.devicemanager.ui.theme.BgElevated
import com.devora.devicemanager.ui.theme.BgSurface
import com.devora.devicemanager.ui.theme.ButtonShape
import com.devora.devicemanager.ui.theme.ChipShape
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
import com.devora.devicemanager.ui.theme.Success
import com.devora.devicemanager.ui.theme.TextMuted
import com.devora.devicemanager.ui.theme.TextPrimary
import kotlinx.coroutines.launch

// ══════════════════════════════════════
// DEVICE DATA (mapped from API)
// ══════════════════════════════════════

data class Device(
    val name: String,
    val subtitle: String,
    val searchManufacturer: String,
    val searchModel: String,
    val status: String,
    val api: String,
    val initial: String,
    val deviceId: String,
    val lastSeen: String
)

// ══════════════════════════════════════
// DEVICE LIST SCREEN
// ══════════════════════════════════════

@Composable
fun DeviceListScreen(
    onDeviceClick: (String) -> Unit,
    onEnrollClick: () -> Unit,
    onNavigate: (String) -> Unit,
    isDark: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    val filters = listOf("ALL", "ONLINE", "OFFLINE")

    val bgColor = if (isDark) DarkBgBase else BgBase
    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    val surfaceBg = if (isDark) DarkBgSurface else BgSurface

    // ── Delete state ──
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteTargetDevice by remember { mutableStateOf<Device?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Live data state ──
    var isLoading by remember { mutableStateOf(true) }
    var enrolledDevices by remember { mutableStateOf<List<Device>>(emptyList()) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }

    // ── Fetch from API ──
    LaunchedEffect(refreshTick) {
        isLoading = enrolledDevices.isEmpty()
        try {
            val response = RemoteDataSource.getDeviceList()
            if (response.isSuccessful) {
                enrolledDevices = (response.body() ?: emptyList()).map { it.toDevice() }
                fetchError = null
                Log.d("DeviceList", "Fetched ${enrolledDevices.size} devices")
            } else {
                fetchError = "Failed to load devices (${response.code()})"
                Log.e("DeviceList", "Device fetch failed: ${response.code()}")
            }
        } catch (e: Exception) {
            fetchError = "Failed to load devices. Check connection."
            Log.e("DeviceList", "Failed to fetch devices: ${e.message}")
        }
        isLoading = false
    }

    LaunchedEffect(fetchError) {
        fetchError?.let { snackbarHostState.showSnackbar(it) }
    }

    // ── Shimmer ──
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by shimmerTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    val filteredDevices = enrolledDevices.filter { device ->
        val matchesSearch = device.name.contains(searchQuery, ignoreCase = true) ||
                device.searchManufacturer.contains(searchQuery, ignoreCase = true) ||
                device.searchModel.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "ALL" -> true
            "ONLINE" -> device.status == "ONLINE"
            "OFFLINE" -> device.status == "OFFLINE"
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Scaffold(
        bottomBar = {
            DevoraBottomNav(
                currentRoute = "device_list",
                onNavigate = onNavigate,
                isDark = isDark
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onEnrollClick,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                containerColor = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(PurpleCore, PurpleDeep)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Enroll device",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        containerColor = bgColor,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Devices",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = textColor
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Refresh button
                    IconButton(onClick = { refreshTick++ }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = PurpleCore,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = "Filter",
                        tint = PurpleCore,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(surfaceBg, ButtonShape)
                    .border(1.dp, PurpleBorder, ButtonShape)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = PurpleCore,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search by employee name or model...",
                                fontFamily = DMSans,
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(
                                fontFamily = DMSans,
                                fontSize = 13.sp,
                                color = textColor
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { searchQuery = "" }
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    val count = when (filter) {
                        "ALL" -> enrolledDevices.size
                        "ONLINE" -> enrolledDevices.count { it.status == "ONLINE" }
                        "OFFLINE" -> enrolledDevices.count { it.status == "OFFLINE" }
                        else -> 0
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) PurpleCore.copy(alpha = 0.12f) else surfaceBg,
                                ChipShape
                            )
                            .border(
                                1.dp,
                                if (isSelected) PurpleCore.copy(alpha = 0.40f) else PurpleCore.copy(alpha = 0.15f),
                                ChipShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { selectedFilter = filter }
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "$filter ($count)",
                            fontFamily = if (isSelected) PlusJakartaSans else DMSans,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = if (isSelected) PurpleCore else TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Device list
            if (isLoading) {
                // Shimmer loading
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(4) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                .background(
                                    (if (isDark) DarkBgElevated else BgElevated)
                                        .copy(alpha = shimmerAlpha)
                                )
                        )
                    }
                }
            } else if (filteredDevices.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    PurpleCore.copy(alpha = 0.10f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = PurpleCore,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No devices match your search"
                                   else "No devices enrolled yet",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try a different search term"
                                   else "Tap + to enroll your first device",
                            fontFamily = DMSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredDevices) { device ->
                        val stripColor = when (device.status) {
                            "ONLINE" -> Success
                            "FLAGGED" -> Danger
                            else -> TextMuted
                        }

                        Box(
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onDeviceClick(device.deviceId) }
                            )
                        ) {
                            DevoraCard(accentColor = stripColor, isDark = isDark) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                if (device.status == "ONLINE") {
                                                    Brush.linearGradient(
                                                        listOf(PurpleCore, PurpleDeep)
                                                    )
                                                } else {
                                                    Brush.linearGradient(
                                                        listOf(
                                                            if (isDark) DarkBgElevated else BgElevated,
                                                            if (isDark) DarkBgElevated else BgElevated
                                                        )
                                                    )
                                                },
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = device.initial,
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (device.status == "ONLINE") Color.White else TextMuted
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Device info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = device.name,
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = textColor
                                        )
                                        Text(
                                            text = device.subtitle,
                                            fontFamily = DMSans,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 12.sp,
                                            color = TextMuted
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.AccessTime,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = device.lastSeen,
                                                fontFamily = DMSans,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 11.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }

                                    // Right column
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        StatusBadge(status = device.status)
                                        Text(
                                            text = device.api,
                                            fontFamily = JetBrainsMono,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                        IconButton(
                                            onClick = {
                                                deleteTargetDevice = device
                                                showDeleteDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.DeleteOutline,
                                                contentDescription = "Delete device",
                                                tint = Danger.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── DELETE DEVICE DIALOG ──
    if (showDeleteDialog && deleteTargetDevice != null) {
        val device = deleteTargetDevice!!
        AlertDialog(
            onDismissRequest = { 
                if (!isDeleting) showDeleteDialog = false 
            },
            containerColor = if (isDark) Color(0xFF1A1A24) else Color.White,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Delete Device",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textColor
                )
            },
            text = {
                Text(
                    "Are you sure? This will remove ${device.name.removeSuffix("'s Device")}'s device and all data permanently. The employee will need to re-enroll.",
                    fontFamily = DMSans,
                    fontSize = 13.sp,
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        coroutineScope.launch {
                            try {
                                val response = RemoteDataSource.deleteDevice(device.deviceId)
                                if (response.isSuccessful) {
                                    enrolledDevices = enrolledDevices.filter { it.deviceId != device.deviceId }
                                    showDeleteDialog = false
                                    deleteTargetDevice = null
                                    snackbarHostState.showSnackbar("Device deleted successfully")
                                    Log.i("DeviceList", "Device ${device.name} deleted successfully")
                                } else {
                                    snackbarHostState.showSnackbar("Failed to delete device (${response.code()})")
                                    Log.e("DeviceList", "Delete failed: ${response.code()}")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Failed to delete device")
                                Log.e("DeviceList", "Delete error: ${e.message}")
                            } finally {
                                isDeleting = false
                            }
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = Danger),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 1.5.dp
                        )
                    } else {
                        Text(
                            "Delete",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("Cancel", fontFamily = DMSans, fontSize = 14.sp, color = PurpleCore)
                }
            }
        )
    }
}

/**
 * Maps a [DeviceResponse] from the backend to a UI [Device].
 */
private fun DeviceResponse.toDevice(): Device {
    val statusStr = when (status.uppercase()) {
        "ACTIVE", "ENROLLED" -> "ONLINE"
        "PENDING" -> "OFFLINE"
        else -> "OFFLINE"
    }
    val enrolledStr = enrolledAt.take(10)
    
    val modelFallback = deviceModel?.takeIf { it.isNotBlank() } ?: deviceId.take(8)
    val displayName = if (!employeeName.isNullOrBlank()) {
        "${employeeName!!}'s Device"
    } else {
        modelFallback
    }

    val manufacturerLabel = manufacturer.orEmpty().trim()
    val modelLabel = deviceModel.orEmpty().trim()
    val methodLabel = enrollmentMethod.replace("_", " ")
    val subtitle = "${manufacturerLabel} ${modelLabel}".trim().let {
        if (it.isBlank()) "Unknown Device · $methodLabel" else "$it · $methodLabel"
    }

    val initialSource = employeeName?.takeIf { it.isNotBlank() } ?: modelFallback
    val firstLetter = initialSource.take(1).uppercase()

    return Device(
        name = displayName,
        subtitle = subtitle,
        searchManufacturer = manufacturer.orEmpty(),
        searchModel = modelLabel.ifBlank { modelFallback },
        status = statusStr,
        api = "ID: ${deviceId.take(8)}",
        initial = firstLetter,
        deviceId = deviceId,
        lastSeen = "Enrolled $enrolledStr"
    )
}

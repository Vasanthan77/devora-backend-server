package com.devora.devicemanager.ui.screens.appinventory

import com.devora.devicemanager.data.remote.RemoteDataSource
import android.util.Log
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devora.devicemanager.network.DeviceResponse
import com.devora.devicemanager.ui.components.DevoraCard
import com.devora.devicemanager.ui.components.StatusBadge
import com.devora.devicemanager.ui.theme.BgBase
import com.devora.devicemanager.ui.theme.DMSans
import com.devora.devicemanager.ui.theme.DarkBgBase
import com.devora.devicemanager.ui.theme.DarkBgElevated
import com.devora.devicemanager.ui.theme.DarkTextPrimary
import com.devora.devicemanager.ui.theme.JetBrainsMono
import com.devora.devicemanager.ui.theme.PlusJakartaSans
import com.devora.devicemanager.ui.theme.PurpleCore
import com.devora.devicemanager.ui.theme.PurpleDeep
import com.devora.devicemanager.ui.theme.Success
import com.devora.devicemanager.ui.theme.TextMuted
import com.devora.devicemanager.ui.theme.TextPrimary

@Composable
fun DeviceAppInventoryListScreen(
    onDeviceClick: (String) -> Unit,
    onBack: () -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) DarkTextPrimary else TextPrimary

    var isLoading by remember { mutableStateOf(true) }
    var devices by remember { mutableStateOf<List<DeviceResponse>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val response = RemoteDataSource.getDeviceList()
            if (response.isSuccessful) {
                devices = response.body()?.filter {
                    it.status.equals("ACTIVE", ignoreCase = true) ||
                    it.status.equals("ENROLLED", ignoreCase = true)
                } ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e("AppInventoryList", "Failed to fetch devices: ${e.message}")
        }
        isLoading = false
    }

    Scaffold(
        containerColor = if (isDark) DarkBgBase else BgBase
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Top bar
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
                Column {
                    Text(
                        "App Inventory",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textColor
                    )
                    Text(
                        "${devices.size} active devices",
                        fontFamily = DMSans,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PurpleCore)
                }
            } else if (devices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(PurpleCore.copy(alpha = 0.10f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Devices,
                                contentDescription = null,
                                tint = PurpleCore,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No active devices",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = textColor
                        )
                        Text(
                            "Enroll a device to view its app inventory",
                            fontFamily = DMSans,
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(devices, key = { it.deviceId }) { device ->
                        DevoraCard(accentColor = Success, isDark = isDark) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDeviceClick(device.deviceId) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Device icon
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            Brush.linearGradient(listOf(PurpleCore, PurpleDeep)),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.PhoneAndroid,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = device.employeeName ?: device.deviceModel ?: "Unknown",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = textColor
                                    )
                                    Text(
                                        text = buildString {
                                            append(device.deviceModel ?: "Unknown Device")
                                            if (!device.manufacturer.isNullOrEmpty()) {
                                                append(" · ${device.manufacturer}")
                                            }
                                        },
                                        fontFamily = DMSans,
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "ID: ${device.deviceId.take(8)}",
                                        fontFamily = JetBrainsMono,
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }

                                StatusBadge(status = "ONLINE")

                                Spacer(Modifier.width(4.dp))

                                Icon(
                                    Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

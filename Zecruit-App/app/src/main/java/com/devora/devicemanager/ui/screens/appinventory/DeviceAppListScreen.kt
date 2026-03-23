package com.devora.devicemanager.ui.screens.appinventory

import com.devora.devicemanager.data.remote.RemoteDataSource
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devora.devicemanager.network.AppInventoryItem
import com.devora.devicemanager.network.RestrictAppRequestNew
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
import com.devora.devicemanager.ui.theme.Warning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DeviceAppListScreen(
    deviceId: String,
    onBack: () -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    val surfaceBg = if (isDark) DarkBgSurface else BgSurface

    var isLoading by remember { mutableStateOf(true) }
    var allApps by remember { mutableStateOf<List<AppInventoryItem>>(emptyList()) }
    var restrictedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(deviceId) {
        val inventoryResponse = try {
            RemoteDataSource.getAppInventory(deviceId)
        } catch (e: Exception) {
            Log.e("DeviceAppList", "Failed to fetch app inventory: ${e.message}")
            null
        }

        if (inventoryResponse?.isSuccessful == true) {
            allApps = inventoryResponse.body() ?: emptyList()
        }
        isLoading = false

        try {
            val rsp = RemoteDataSource.getRestrictedApps(deviceId)
            if (rsp.isSuccessful) {
                restrictedPackages = (rsp.body() ?: emptyList())
                    .filter { it.restricted }
                    .map { it.packageName }.toSet()
            }
        } catch (e: Exception) {
            Log.e("DeviceAppList", "Failed to fetch restricted apps: ${e.message}")
        }
    }

    val userApps = allApps.filter { it.isSystemApp != true }
    val systemApps = allApps.filter { it.isSystemApp == true }

    val filteredApps = when (selectedFilter) {
        "USER" -> userApps
        "SYSTEM" -> systemApps
        else -> allApps
    }.filter {
        searchQuery.isEmpty() ||
        it.appName.contains(searchQuery, ignoreCase = true) ||
        it.packageName.contains(searchQuery, ignoreCase = true)
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
                        "Installed Apps",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textColor
                    )
                    Text(
                        "${allApps.size} apps · ${userApps.size} user · ${systemApps.size} system",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(surfaceBg, RoundedCornerShape(12.dp))
                    .border(1.dp, PurpleBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp),
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
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search apps...",
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
                            Icons.Filled.Close,
                            contentDescription = "Clear",
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

            Spacer(Modifier.height(12.dp))

            // Filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "ALL" to allApps.size,
                    "USER" to userApps.size,
                    "SYSTEM" to systemApps.size
                ).forEach { (filter, count) ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) PurpleCore.copy(alpha = 0.12f) else surfaceBg,
                                RoundedCornerShape(20.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) PurpleCore.copy(alpha = 0.40f)
                                else PurpleCore.copy(alpha = 0.15f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { selectedFilter = filter }
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "$filter ($count)",
                            fontFamily = if (isSelected) PlusJakartaSans else DMSans,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = if (isSelected) PurpleCore else TextMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PurpleCore)
                }
            } else if (allApps.isEmpty()) {
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
                                Icons.Outlined.Apps,
                                contentDescription = null,
                                tint = PurpleCore,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No app inventory available",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = textColor
                        )
                        Text(
                            "Device hasn't synced its apps yet",
                            fontFamily = DMSans,
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    items(filteredApps, key = { it.id }) { app ->
                        val isRestricted = app.packageName in restrictedPackages
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isRestricted) Modifier.background(Danger.copy(alpha = 0.05f))
                                    else Modifier
                                )
                                .padding(vertical = 10.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // App icon — real icon from base64 or fallback
                            val iconBitmap = rememberDecodedIconBitmap(app.iconBase64)

                            if (iconBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isRestricted) Danger.copy(alpha = 0.08f)
                                            else Color.Transparent
                                        )
                                ) {
                                    Image(
                                        bitmap = iconBitmap.asImageBitmap(),
                                        contentDescription = app.appName,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        alpha = if (isRestricted) 0.4f else 1f
                                    )
                                    if (isRestricted) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Outlined.Block,
                                                contentDescription = null,
                                                tint = Danger,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Fallback: letter initial icon
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isRestricted) Danger.copy(alpha = 0.10f)
                                            else PurpleDim
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isRestricted) {
                                        Icon(
                                            Icons.Outlined.Block,
                                            contentDescription = null,
                                            tint = Danger,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Text(
                                            app.appName.take(1).uppercase(),
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = PurpleCore
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    app.appName,
                                    fontFamily = DMSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isRestricted) Danger else textColor
                                )
                                Text(
                                    app.packageName,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 9.sp,
                                    color = TextMuted
                                )
                                // Version + Version Code
                                val versionInfo = buildString {
                                    if (!app.versionName.isNullOrEmpty()) append("v${app.versionName}")
                                    if (app.versionCode != null) {
                                        if (isNotEmpty()) append(" · ")
                                        append("code ${app.versionCode}")
                                    }
                                }
                                if (versionInfo.isNotEmpty()) {
                                    Text(
                                        versionInfo,
                                        fontFamily = JetBrainsMono,
                                        fontSize = 9.sp,
                                        color = TextMuted
                                    )
                                }
                                // Install source
                                val source = formatInstallSource(app.installSource)
                                if (source.isNotEmpty()) {
                                    Text(
                                        source,
                                        fontFamily = JetBrainsMono,
                                        fontSize = 9.sp,
                                        color = PurpleCore.copy(alpha = 0.70f)
                                    )
                                }
                            }

                            // Restrict / Allow button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(
                                        if (isRestricted) Success.copy(alpha = 0.10f)
                                        else Danger.copy(alpha = 0.10f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isRestricted) Success.copy(alpha = 0.30f)
                                        else Danger.copy(alpha = 0.25f),
                                        RoundedCornerShape(100.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        scope.launch {
                                            try {
                                                val rsp = RemoteDataSource.restrictApp(
                                                    deviceId,
                                                    RestrictAppRequestNew(
                                                        packageName = app.packageName,
                                                        appName = app.appName,
                                                        installSource = app.installSource ?: "",
                                                        restricted = !isRestricted
                                                    )
                                                )
                                                if (rsp.isSuccessful) {
                                                    restrictedPackages = if (isRestricted) {
                                                        restrictedPackages - app.packageName
                                                    } else {
                                                        restrictedPackages + app.packageName
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Log.e("DeviceAppList", "Restrict toggle failed: ${e.message}")
                                            }
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isRestricted) Icons.Filled.Check else Icons.Outlined.Block,
                                        contentDescription = null,
                                        tint = if (isRestricted) Success else Danger,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (isRestricted) "Allow" else "Restrict",
                                        fontFamily = JetBrainsMono,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRestricted) Success else Danger
                                    )
                                }
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
}

@Composable
private fun rememberDecodedIconBitmap(iconBase64: String?): android.graphics.Bitmap? {
    val bitmapState = produceState<android.graphics.Bitmap?>(initialValue = null, key1 = iconBase64) {
        value = if (iconBase64.isNullOrEmpty()) {
            null
        } else {
            withContext(Dispatchers.Default) {
                runCatching {
                    val bytes = Base64.decode(iconBase64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }.getOrNull()
            }
        }
    }
    return bitmapState.value
}

private fun formatInstallSource(source: String?): String {
    if (source.isNullOrBlank()) return ""
    return when {
        source.contains("vending", ignoreCase = true) -> "\uD83D\uDED2 Play Store"
        source.contains("google.android.packageinstaller", ignoreCase = true) -> "\uD83D\uDCE6 Package Installer"
        source.contains("vivo", ignoreCase = true) -> "\uD83D\uDCF1 Vivo Store"
        source.contains("samsung", ignoreCase = true) -> "\uD83C\uDF1F Galaxy Store"
        source.contains("oppo", ignoreCase = true) || source.contains("heytap", ignoreCase = true) -> "\uD83D\uDCF1 OPPO Store"
        source.contains("xiaomi", ignoreCase = true) || source.contains("miui", ignoreCase = true) -> "\uD83D\uDCF1 Mi Store"
        source.contains("huawei", ignoreCase = true) -> "\uD83D\uDCF1 Huawei Store"
        source.contains("amazon", ignoreCase = true) -> "\uD83D\uDED2 Amazon Store"
        source.contains("browser", ignoreCase = true) || source.contains("chrome", ignoreCase = true) -> "\uD83C\uDF10 Browser / APK"
        source == "com.android.shell" || source == "adb" -> "\u2699\uFE0F ADB / Sideloaded"
        else -> "\uD83D\uDCE6 $source"
    }
}

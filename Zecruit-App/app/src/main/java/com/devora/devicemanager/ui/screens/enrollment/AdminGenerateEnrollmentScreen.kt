package com.devora.devicemanager.ui.screens.enrollment

import com.devora.devicemanager.data.remote.RemoteDataSource
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devora.devicemanager.ui.components.DevoraCard
import com.devora.devicemanager.ui.components.DevoraBottomNav
import com.devora.devicemanager.ui.components.SectionHeader
import com.devora.devicemanager.ui.theme.BgBase
import com.devora.devicemanager.ui.theme.BgElevated
import com.devora.devicemanager.ui.theme.DMSans
import com.devora.devicemanager.ui.theme.Danger
import com.devora.devicemanager.ui.theme.DarkBgBase
import com.devora.devicemanager.ui.theme.DarkBgElevated
import com.devora.devicemanager.ui.theme.JetBrainsMono
import com.devora.devicemanager.ui.theme.PlusJakartaSans
import com.devora.devicemanager.ui.theme.PurpleBorder
import com.devora.devicemanager.ui.theme.PurpleBright
import com.devora.devicemanager.ui.theme.PurpleCore
import com.devora.devicemanager.ui.theme.PurpleDim
import com.devora.devicemanager.ui.theme.Success
import com.devora.devicemanager.ui.theme.TextMuted
import com.devora.devicemanager.ui.theme.TextPrimary
import com.devora.devicemanager.ui.theme.Warning
import com.devora.devicemanager.enrollment.QrProvisioningHelper
import com.devora.devicemanager.network.GenerateEnrollmentTokenRequest
import com.devora.devicemanager.network.EnrollmentTokenResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ══════════════════════════════════════
// DATA CLASS
// ══════════════════════════════════════

data class EnrollmentSession(
    val id: String,
    val deviceLabel: String,
    val assignedEmployee: String,
    val department: String,
    val deviceType: String,
    val token: String,
    val validityHours: Int,
    val createdAt: Long,
    val expiresAt: Long,
    val expiresAtRaw: String,
    val status: String,
    val deviceId: String?
)

sealed class TokenExpiryState {
    data class Valid(val text: String) : TokenExpiryState()
    data class Warning(val text: String) : TokenExpiryState()
    data class Critical(val text: String) : TokenExpiryState()
    data class Expired(val text: String) : TokenExpiryState()
    object Unknown : TokenExpiryState()
}

private fun parseTokenExpiryMillis(expiresAt: String): Long? {
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss"
    )
    formats.forEach { format ->
        runCatching {
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(expiresAt)?.time
        }.getOrNull()?.let { return it }
    }
    return runCatching { Instant.parse(expiresAt).toEpochMilli() }.getOrNull()
}

private fun getTokenExpiryState(expiresAt: String, currentTick: Long): TokenExpiryState {
    val expiry = parseTokenExpiryMillis(expiresAt) ?: return TokenExpiryState.Unknown
    val diff = expiry - currentTick
    return when {
        diff <= 0L -> {
            val ago = currentTick - expiry
            val expiredText = if (ago < 3_600_000L) {
                "Expired ${ago / 60_000L}m ago"
            } else {
                val h = ago / 3_600_000L
                val m = (ago % 3_600_000L) / 60_000L
                "Expired ${h}h ${m}m ago"
            }
            TokenExpiryState.Expired(expiredText)
        }
        diff < 300_000L -> {
            val m = diff / 60_000L
            val s = (diff % 60_000L) / 1_000L
            TokenExpiryState.Critical("Expires in ${m}m ${s}s")
        }
        diff < 1_800_000L -> {
            val m = diff / 60_000L
            val s = (diff % 60_000L) / 1_000L
            TokenExpiryState.Warning("Expires in ${m}m ${s}s")
        }
        else -> {
            val h = diff / 3_600_000L
            val m = (diff % 3_600_000L) / 60_000L
            val s = (diff % 60_000L) / 1_000L
            val text = if (h > 0L) {
                "Expires in ${h}h ${m}m ${s}s"
            } else {
                "Expires in ${m}m ${s}s"
            }
            TokenExpiryState.Valid(text)
        }
    }
}

// ══════════════════════════════════════
// SCREEN
// ══════════════════════════════════════

@Composable
fun AdminGenerateEnrollmentScreen(
    onNavigate: (String) -> Unit,
    isDark: Boolean
) {
    var screenState by rememberSaveable { mutableStateOf("FORM") }
    var deviceLabel by rememberSaveable { mutableStateOf("") }
    var assignedEmployee by rememberSaveable { mutableStateOf("") }
    var employeeName by rememberSaveable { mutableStateOf("") }
    var selectedDepartment by rememberSaveable { mutableStateOf("") }
    var selectedDeviceType by rememberSaveable { mutableStateOf("") }
    val selectedValidity = "1h"
    var enrollType by rememberSaveable { mutableStateOf("QR") }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedToken by rememberSaveable { mutableStateOf("") }
    var showDeptDropdown by remember { mutableStateOf(false) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var showRevokeDialog by remember { mutableStateOf(false) }
    var revokeTargetId by remember { mutableStateOf("") }
    var isRevoking by remember { mutableStateOf(false) }
    var showPayload by remember { mutableStateOf(false) }
    var showAppQrDialog by remember { mutableStateOf(false) }
    var generatedQrPayload by rememberSaveable { mutableStateOf("") }
    var isLoadingSessions by remember { mutableStateOf(false) }
    var sessionsError by remember { mutableStateOf<String?>(null) }
    var sessionsRefreshTick by remember { mutableStateOf(0) }
    val locallyRevokedIds = remember { mutableStateListOf<String>() }

    // Tick trigger for expiry countdown updates
    var tickTrigger by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L) // update every second
            tickTrigger = System.currentTimeMillis()
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val textColor = if (isDark) Color(0xFFF0F2F5) else TextPrimary
    val inputBg = if (isDark) DarkBgElevated else BgElevated

    val activeEnrollments = remember { mutableStateListOf<EnrollmentSession>() }
    val provisioningQrBitmap: Bitmap? = remember(generatedToken, generatedQrPayload) {
        if (generatedToken.isBlank() && generatedQrPayload.isBlank()) {
            null
        } else if (generatedQrPayload.isNotBlank()) {
            QrProvisioningHelper.generateQrBitmap(generatedQrPayload)
        } else {
            QrProvisioningHelper.generateDeviceOwnerProvisioningQr(
                enrollmentToken = generatedToken
            )
        }
    }

    val visibleEnrollments = activeEnrollments
        .filterNot { locallyRevokedIds.contains(it.id) }
        .filter { token ->
            val state = getTokenExpiryState(token.expiresAtRaw, tickTrigger)
            state !is TokenExpiryState.Expired || run {
                val expiryMillis = parseTokenExpiryMillis(token.expiresAtRaw) ?: return@run true
                (tickTrigger - expiryMillis) < 600_000L
            }
        }

    LaunchedEffect(sessionsRefreshTick) {
        isLoadingSessions = true
        sessionsError = null
        try {
            val response = RemoteDataSource.getActiveEnrollments()
            if (response.isSuccessful) {
                val sessions = (response.body() ?: emptyList()).map { it.toEnrollmentSession() }
                activeEnrollments.clear()
                activeEnrollments.addAll(sessions)
            } else {
                sessionsError = "Failed to load active sessions (${response.code()})"
            }
        } catch (_: Exception) {
            sessionsError = "Failed to load active sessions"
        } finally {
            isLoadingSessions = false
        }
    }

    Scaffold(
        containerColor = if (isDark) DarkBgBase else BgBase,
        bottomBar = {
            DevoraBottomNav(
                currentRoute = "admin_generate_enrollment",
                onNavigate = onNavigate,
                isDark = isDark
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ══════ TOP BAR ══════
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = if (screenState == "FORM") "New Enrollment" else "Enrollment Created",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textColor
                    )
                    Spacer(Modifier.weight(1f))
                    if (screenState == "GENERATED") {
                        IconButton(onClick = {
                            screenState = "FORM"
                            deviceLabel = ""
                            assignedEmployee = ""
                            employeeName = ""
                            selectedDepartment = ""
                            selectedDeviceType = ""
                            generatedToken = ""
                            showPayload = false
                        }) {
                            Icon(Icons.Outlined.Add, tint = PurpleCore, contentDescription = "New", modifier = Modifier.size(24.dp))
                        }
                    } else {
                        Spacer(Modifier.width(48.dp))
                    }
                }
            }

            // ══════════════════════════════════════
            // FORM STATE
            // ══════════════════════════════════════

            if (screenState == "FORM") {

                // ── ENROLLMENT CONFIGURATION CARD ──
                item {
                    DevoraCard(isDark = isDark, showTopAccent = true, accentColor = PurpleCore) {
                        Column {
                            SectionHeader(title = "ENROLLMENT CONFIGURATION", isDark = isDark)

                            Spacer(Modifier.height(8.dp))

                            // Employee ID
                            Text("Employee ID", fontFamily = DMSans, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = PurpleCore)
                            Spacer(Modifier.height(6.dp))
                            BasicTextField(
                                value = assignedEmployee,
                                onValueChange = { assignedEmployee = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(inputBg)
                                    .padding(14.dp),
                                textStyle = TextStyle(fontFamily = DMSans, fontSize = 14.sp, color = textColor),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (assignedEmployee.isEmpty()) {
                                        Text("Enter Employee ID", fontFamily = DMSans, fontSize = 14.sp, color = TextMuted)
                                    }
                                    inner()
                                }
                            )

                            Spacer(Modifier.height(12.dp))

                            // Employee Name
                            Text("Employee Name", fontFamily = DMSans, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = PurpleCore)
                            Spacer(Modifier.height(6.dp))
                            BasicTextField(
                                value = employeeName,
                                onValueChange = { employeeName = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(inputBg)
                                    .padding(14.dp),
                                textStyle = TextStyle(fontFamily = DMSans, fontSize = 14.sp, color = textColor),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (employeeName.isEmpty()) {
                                        Text("Enter Employee Name", fontFamily = DMSans, fontSize = 14.sp, color = TextMuted)
                                    }
                                    inner()
                                }
                            )

                            Spacer(Modifier.height(12.dp))
                            Text("Enrollment Type", fontFamily = DMSans, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = PurpleCore)
                            Spacer(Modifier.height(8.dp))
                            listOf(
                                "QR" to "QR Code (Recommended)",
                                "TOKEN" to "Token Only"
                            ).forEach { (value, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (enrollType == value) PurpleDim else Color.Transparent
                                        )
                                        .border(
                                            1.dp,
                                            if (enrollType == value) Color(0x407B61FF) else Color(0x1A7B61FF),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { enrollType = value }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = enrollType == value,
                                        onClick = { enrollType = value },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = PurpleCore,
                                            unselectedColor = TextMuted
                                        )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        label,
                                        fontFamily = DMSans,
                                        fontSize = 14.sp,
                                        color = if (enrollType == value) PurpleCore else textColor,
                                        fontWeight = if (enrollType == value) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                            }

                            Spacer(Modifier.height(6.dp))

                            Button(
                                onClick = { showAppQrDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PurpleDim),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.QrCode2,
                                        contentDescription = null,
                                        tint = PurpleCore,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Open Application QR",
                                        fontFamily = DMSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = PurpleCore
                                    )
                                }
                            }
                        }
                    }
                }

                // ── GENERATE BUTTON ──
                item {
                    Button(
                        onClick = {
                            if (assignedEmployee.isBlank()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Please enter Employee ID")
                                }
                                return@Button
                            }
                            if (employeeName.isBlank()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Please enter Employee Name")
                                }
                                return@Button
                            }
                            coroutineScope.launch {
                                isGenerating = true
                                try {
                                    val response = RemoteDataSource.generateEnrollmentToken(
                                        GenerateEnrollmentTokenRequest(
                                            employeeId = assignedEmployee.trim(),
                                            employeeName = employeeName.trim(),
                                            type = enrollType
                                        )
                                    )
                                    if (!response.isSuccessful || response.body()?.token.isNullOrBlank()) {
                                        snackbarHostState.showSnackbar("Failed to generate enrollment token")
                                        isGenerating = false
                                        return@launch
                                    }

                                    val body = response.body()!!
                                    val newToken = body.token
                                    generatedToken = newToken
                                    generatedQrPayload = body.qrCode?.trim().orEmpty()
                                    sessionsRefreshTick += 1
                                    screenState = "GENERATED"
                                } catch (_: Exception) {
                                    snackbarHostState.showSnackbar("Failed to generate enrollment token")
                                }
                                isGenerating = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleCore),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.QrCode2, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Generate Enrollment",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // ── ACTIVE ENROLLMENTS ──
                item {
                    SectionHeader(
                        title = "ACTIVE ENROLLMENT SESSIONS",
                        actionText = "${visibleEnrollments.size} Active",
                        isDark = isDark
                    )
                }

                if (isLoadingSessions) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PurpleCore)
                        }
                    }
                } else if (visibleEnrollments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "No Active Sessions",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = textColor
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Generate enrollment to create a new session",
                                    fontFamily = DMSans,
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(visibleEnrollments.toList(), key = { it.id }) { session ->
                        ActiveEnrollmentCard(
                            session = session,
                            currentTick = tickTrigger,
                            isDark = isDark,
                            textColor = textColor,
                            onOpenQr = {
                                generatedToken = session.token
                                generatedQrPayload = ""
                                employeeName = session.deviceLabel
                                assignedEmployee = session.assignedEmployee.substringAfterLast("(").substringBefore(")")
                                screenState = "GENERATED"
                            },
                            onRevoke = {
                                revokeTargetId = session.id
                                showRevokeDialog = true
                            }
                        )
                    }
                }

                if (!sessionsError.isNullOrBlank()) {
                    item {
                        Text(
                            text = sessionsError!!,
                            fontFamily = DMSans,
                            fontSize = 12.sp,
                            color = Danger
                        )
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }

            // ══════════════════════════════════════
            // GENERATED STATE
            // ══════════════════════════════════════

            if (screenState == "GENERATED") {

                // ── SUCCESS BANNER ──
                item {
                    DevoraCard(isDark = isDark, showTopAccent = true, accentColor = Success) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Success.copy(alpha = 0.12f))
                                    .border(1.dp, Success.copy(alpha = 0.30f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Enrollment Created!",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Success
                                )
                                Text(
                                    "Valid for $selectedValidity",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                // ── QR CODE CARD ──
                item {
                    DevoraCard(isDark = isDark, accentColor = PurpleCore) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SectionHeader(title = "DEVICE OWNER PROVISIONING QR", isDark = isDark)
                            Spacer(Modifier.height(16.dp))

                            // Device Owner provisioning QR with full payload for auto-install
                            Box(
                                modifier = Modifier
                                    .size(260.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .border(2.dp, PurpleCore, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (provisioningQrBitmap != null) {
                                    Image(
                                        bitmap = provisioningQrBitmap.asImageBitmap(),
                                        contentDescription = "Enrollment QR Code",
                                        modifier = Modifier
                                            .size(230.dp)
                                            .padding(8.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    CircularProgressIndicator(
                                        color = PurpleCore,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                // Center branding circle overlay
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(1.5.dp, PurpleCore.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "D",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = PurpleCore
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                "Factory reset device \u2192 tap Welcome screen 6 times \u2192\nscan this QR to auto-install & enroll",
                                fontFamily = DMSans,
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(16.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Share
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PurpleDim)
                                        .border(1.dp, PurpleBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            if (provisioningQrBitmap == null) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("QR image not ready")
                                                }
                                                return@clickable
                                            }

                                            val ok = shareQrBitmap(
                                                context = context,
                                                bitmap = provisioningQrBitmap,
                                                token = generatedToken,
                                                label = employeeName.ifBlank { deviceLabel }
                                            )
                                            if (!ok) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Failed to share QR image")
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Share, contentDescription = null, tint = PurpleCore, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Share QR", fontFamily = DMSans, fontSize = 13.sp, color = PurpleCore, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── TOKEN CARD ──
                item {
                    DevoraCard(isDark = isDark, accentColor = Warning) {
                        Column {
                            SectionHeader(title = "ENROLLMENT TOKEN", isDark = isDark)

                            Text(
                                "Alternative to QR code — employee enters this manually",
                                fontFamily = DMSans,
                                fontSize = 12.sp,
                                color = TextMuted
                            )

                            Spacer(Modifier.height(12.dp))

                            // Token display
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF14141E) else Color(0xFFF7F8FA))
                                    .border(1.5.dp, PurpleBorder, RoundedCornerShape(12.dp))
                                    .padding(vertical = 20.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = generatedToken,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp,
                                    color = PurpleCore,
                                    letterSpacing = 3.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Copy Token
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, PurpleBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                                            clipboard.setPrimaryClip(ClipData.newPlainText("token", generatedToken))
                                            Toast.makeText(context, "Token copied!", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = PurpleCore, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Copy Token", fontFamily = DMSans, fontSize = 13.sp, color = PurpleCore, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                // Share Token
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PurpleDim)
                                        .border(1.dp, PurpleBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "DEVORA Enrollment Token: $generatedToken")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share"))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Share, contentDescription = null, tint = PurpleCore, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Share Token", fontFamily = DMSans, fontSize = 13.sp, color = PurpleCore, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── QR PAYLOAD (ADVANCED) ──
                item {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFF1A1A24) else Color(0xFFF7F8FA))
                                .border(1.dp, PurpleBorder, RoundedCornerShape(10.dp))
                                .clickable { showPayload = !showPayload }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Code, contentDescription = null, tint = PurpleCore, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("QR Payload (Advanced)", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PurpleCore)
                                }
                                Icon(
                                    imageVector = if (showPayload) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = PurpleCore,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (showPayload) {
                            Spacer(Modifier.height(8.dp))

                            val jsonPayload = if (generatedQrPayload.isNotBlank()) {
                                generatedQrPayload
                            } else {
                                QrProvisioningHelper.buildProvisioningPayload(
                                    enrollmentToken = generatedToken
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color(0xFF0F0F14) else Color(0xFF1D1D21))
                                    .padding(16.dp)
                            ) {
                                SelectionContainer {
                                    Text(
                                        jsonPayload,
                                        fontFamily = JetBrainsMono,
                                        fontSize = 10.sp,
                                        color = PurpleBright,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, PurpleBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                                        clipboard.setPrimaryClip(ClipData.newPlainText("payload", jsonPayload))
                                        Toast.makeText(context, "JSON copied!", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = PurpleCore, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Copy JSON", fontFamily = DMSans, fontSize = 12.sp, color = PurpleCore, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // ── ACTIVE ENROLLMENTS ──
                item {
                    SectionHeader(
                        title = "ACTIVE ENROLLMENT SESSIONS",
                        actionText = "${visibleEnrollments.size} Active",
                        isDark = isDark
                    )
                }

                if (isLoadingSessions) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PurpleCore)
                        }
                    }
                } else if (visibleEnrollments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "No Active Sessions",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = textColor
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Generate enrollment to create a new session",
                                    fontFamily = DMSans,
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(visibleEnrollments.toList(), key = { it.id }) { session ->
                        ActiveEnrollmentCard(
                            session = session,
                            currentTick = tickTrigger,
                            isDark = isDark,
                            textColor = textColor,
                            onOpenQr = {
                                generatedToken = session.token
                                employeeName = session.deviceLabel
                                assignedEmployee = session.assignedEmployee.substringAfterLast("(").substringBefore(")")
                                screenState = "GENERATED"
                            },
                            onRevoke = {
                                revokeTargetId = session.id
                                showRevokeDialog = true
                            }
                        )
                    }
                }

                if (!sessionsError.isNullOrBlank()) {
                    item {
                        Text(
                            text = sessionsError!!,
                            fontFamily = DMSans,
                            fontSize = 12.sp,
                            color = Danger
                        )
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    // ── REVOKE DIALOG ──
    if (showRevokeDialog) {
        AlertDialog(
            onDismissRequest = { showRevokeDialog = false },
            containerColor = if (isDark) Color(0xFF1A1A24) else Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Revoke Enrollment Session?",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textColor
                )
            },
            text = {
                Text(
                    "This token will immediately expire and can no longer be used for enrollment.",
                    fontFamily = DMSans,
                    fontSize = 13.sp,
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isRevoking = true
                        coroutineScope.launch {
                            try {
                                val sessionToDelete = activeEnrollments.find { it.id == revokeTargetId }
                                if (sessionToDelete != null) {
                                    val tokenId = revokeTargetId.toLongOrNull()
                                    if (tokenId == null) {
                                        snackbarHostState.showSnackbar("Invalid session id")
                                    } else {
                                        val revokeResponse = RemoteDataSource.revokeEnrollmentToken(tokenId)
                                        if (revokeResponse.isSuccessful || revokeResponse.code() == 404) {
                                            locallyRevokedIds.add(revokeTargetId)
                                            activeEnrollments.removeIf { it.id == revokeTargetId }
                                            snackbarHostState.showSnackbar("Enrollment session revoked")
                                        } else {
                                            snackbarHostState.showSnackbar("Failed to revoke session (${revokeResponse.code()})")
                                        }
                                    }
                                }
                                showRevokeDialog = false
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Failed to revoke session: ${e.message}")
                            } finally {
                                isRevoking = false
                            }
                        }
                    },
                    enabled = !isRevoking,
                    colors = ButtonDefaults.buttonColors(containerColor = Danger),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isRevoking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 1.5.dp
                        )
                    } else {
                        Text("Revoke Session", fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeDialog = false }, enabled = !isRevoking) {
                    Text("Cancel", fontFamily = DMSans, fontSize = 14.sp, color = PurpleCore)
                }
            }
        )
    }

    if (showAppQrDialog) {
        val appQrResId = remember {
            context.resources.getIdentifier("devora_qr", "drawable", context.packageName)
                .takeIf { it != 0 }
                ?: context.resources.getIdentifier("devora_app_qr", "drawable", context.packageName)
                .takeIf { it != 0 }
                ?: context.resources.getIdentifier("DEVORA_APP_QR", "drawable", context.packageName)
        }

        AlertDialog(
            onDismissRequest = { showAppQrDialog = false },
            containerColor = if (isDark) Color(0xFF171824) else Color.White,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = "DEVORA Application QR",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        PurpleDim.copy(alpha = 0.65f),
                                        Color(0xFFF4F6FF),
                                        PurpleDim.copy(alpha = 0.35f)
                                    )
                                )
                            )
                            .border(1.5.dp, PurpleBorder, RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(268.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White)
                                .border(2.dp, PurpleCore.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (appQrResId != 0) {
                                Image(
                                    painter = painterResource(id = appQrResId),
                                    contentDescription = "DEVORA application QR",
                                    modifier = Modifier
                                        .size(238.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = "QR image not found in drawable",
                                    fontFamily = DMSans,
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Scan this QR to open the DEVORA application link.",
                        fontFamily = DMSans,
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAppQrDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleCore),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Close",
                        fontFamily = DMSans,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        )
    }
}

private fun EnrollmentTokenResponse.toEnrollmentSession(): EnrollmentSession {
    val createdAtMillis = createdAt.parseIsoMillisOrNow()
    val expiresAtMillis = expiresAt.parseIsoMillisOrNow()
    val validityHours = ((expiresAtMillis - createdAtMillis) / (60L * 60L * 1000L)).toInt().coerceAtLeast(0)

    return EnrollmentSession(
        id = id.toString(),
        deviceLabel = employeeName,
        assignedEmployee = "$employeeName ($employeeId)",
        department = "",
        deviceType = "Android",
        token = token,
        validityHours = validityHours,
        createdAt = createdAtMillis,
        expiresAt = expiresAtMillis,
        expiresAtRaw = expiresAt ?: "",
        status = status,
        deviceId = deviceId
    )
}

private fun String?.parseIsoMillisOrNow(): Long {
    if (this.isNullOrBlank()) return System.currentTimeMillis()
    return runCatching { Instant.parse(this).toEpochMilli() }
        .recoverCatching {
            parseTokenExpiryMillis(this)?.let { millis ->
                Instant.ofEpochMilli(millis).toEpochMilli()
            } ?: System.currentTimeMillis()
        }
        .getOrElse { System.currentTimeMillis() }
}

// ══════════════════════════════════════
// ACTIVE ENROLLMENT CARD
// ══════════════════════════════════════

@Composable
private fun ActiveEnrollmentCard(
    session: EnrollmentSession,
    currentTick: Long,
    isDark: Boolean,
    textColor: Color,
    onOpenQr: () -> Unit,
    onRevoke: () -> Unit
) {
    DevoraCard(
        isDark = isDark,
        accentColor = Warning,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenQr() }
    ) {
        val expiryState = remember(session.expiresAtRaw, currentTick) {
            getTokenExpiryState(session.expiresAtRaw, currentTick)
        }
        val blinkAlpha by animateFloatAsState(
            targetValue = when (expiryState) {
                is TokenExpiryState.Critical -> if ((currentTick / 500L) % 2L == 0L) 1f else 0.3f
                else -> 1f
            },
            animationSpec = tween(300),
            label = "blink"
        )
        val (expiryText, expiryColor) = when (expiryState) {
            is TokenExpiryState.Valid -> expiryState.text to Color(0xFF4CAF50)
            is TokenExpiryState.Warning -> expiryState.text to Color(0xFFFF9800)
            is TokenExpiryState.Critical -> expiryState.text to Color(0xFFF44336)
            is TokenExpiryState.Expired -> expiryState.text to Color(0xFFF44336)
            TokenExpiryState.Unknown -> "Unknown" to Color.Gray
        }
        val statusText = if (expiryState is TokenExpiryState.Expired) "EXPIRED" else "PENDING"
        val statusColor = if (expiryState is TokenExpiryState.Expired) Color(0xFFF44336) else Color(0xFFFF9800)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Warning.copy(alpha = 0.12f))
                    .border(1.dp, Warning.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Warning, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    session.deviceLabel,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = textColor
                )
                Text(
                    session.assignedEmployee,
                    fontFamily = DMSans,
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Spacer(Modifier.height(4.dp))

                // Token badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PurpleDim)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(session.token, fontFamily = JetBrainsMono, fontSize = 10.sp, color = PurpleCore)
                }

                Spacer(Modifier.height(4.dp))

                // Countdown timer
                Text(
                    expiryText,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = expiryColor.copy(alpha = blinkAlpha),
                    fontWeight = when (expiryState) {
                        is TokenExpiryState.Critical,
                        is TokenExpiryState.Expired -> FontWeight.Bold
                        else -> FontWeight.Normal
                    }
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                if (expiryState is TokenExpiryState.Expired) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFFF0F0),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF44336))
                    ) {
                        Text(
                            text = "EXPIRED",
                            color = Color(0xFFF44336),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .border(
                                width = 1.dp,
                                color = statusColor.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                IconButton(
                    onClick = onRevoke,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Revoke",
                        tint = Danger.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun shareQrBitmap(
    context: Context,
    bitmap: Bitmap,
    token: String,
    label: String
): Boolean {
    return try {
        val file = File(context.cacheDir, "enrollment_qr_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "DEVORA Enrollment QR\nToken: $token\nEmployee: $label")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Enrollment QR"))
        true
    } catch (_: Exception) {
        false
    }
}

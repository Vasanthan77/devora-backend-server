package com.devora.devicemanager.ui.screens.enrollment

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devora.devicemanager.ui.components.ButtonVariant
import com.devora.devicemanager.ui.components.DevoraBottomNav
import com.devora.devicemanager.ui.components.DevoraButton
import com.devora.devicemanager.ui.components.DevoraCard
import com.devora.devicemanager.ui.theme.BgBase
import com.devora.devicemanager.ui.theme.BgElevated
import com.devora.devicemanager.ui.theme.BgSurface
import com.devora.devicemanager.ui.theme.DMSans
import com.devora.devicemanager.ui.theme.DarkBgBase
import com.devora.devicemanager.ui.theme.DarkBgElevated
import com.devora.devicemanager.ui.theme.DarkBgSurface
import com.devora.devicemanager.ui.theme.DarkTextPrimary
import com.devora.devicemanager.ui.theme.InputShape
import com.devora.devicemanager.ui.theme.JetBrainsMono
import com.devora.devicemanager.ui.theme.PlusJakartaSans
import com.devora.devicemanager.ui.theme.PurpleBright
import com.devora.devicemanager.ui.theme.PurpleCore
import com.devora.devicemanager.ui.theme.PurpleDeep
import com.devora.devicemanager.ui.theme.TextMuted
import com.devora.devicemanager.ui.theme.TextPrimary
import kotlinx.coroutines.delay

// ══════════════════════════════════════
// ENROLLMENT SCREEN
// ══════════════════════════════════════

@Composable
fun EnrollmentScreen(
    onClose: () -> Unit,
    onNavigate: (String) -> Unit,
    isDark: Boolean
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var token by remember { mutableStateOf("") }
    var enrollStep by remember { mutableIntStateOf(-1) }

    val bgColor = if (isDark) DarkBgBase else BgBase
    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    val surfaceBg = if (isDark) DarkBgSurface else BgSurface
    val inputBg = if (isDark) DarkBgElevated else BgElevated

    Scaffold(
        bottomBar = {
            DevoraBottomNav(
                currentRoute = "enrollment",
                onNavigate = onNavigate,
                isDark = isDark
            )
        },
        containerColor = bgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = PurpleCore
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Device Enrollment",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Custom Tab Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(surfaceBg, RoundedCornerShape(12.dp))
                    .border(1.dp, PurpleCore.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
            ) {
                listOf("QR Code", "Token Entry").forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                            .background(
                                if (isSelected) PurpleCore else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedTab = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            fontFamily = if (isSelected) PlusJakartaSans else DMSans,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (isSelected) Color.White else TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // QR TAB
            if (selectedTab == 0) {
                QrTabContent(isDark = isDark, inputBg = inputBg)
            }

            // TOKEN TAB - Not started
            if (selectedTab == 1 && enrollStep == -1) {
                TokenInputContent(
                    token = token,
                    onTokenChange = { token = it },
                    onEnroll = { enrollStep = 0 },
                    textColor = textColor,
                    inputBg = inputBg,
                    isDark = isDark
                )
            }

            // STEP PROGRESS
            if (enrollStep in 0..3) {
                StepProgressContent(
                    enrollStep = enrollStep,
                    onStepAdvance = { enrollStep = it },
                    isDark = isDark,
                    textColor = textColor
                )
            }

            // SUCCESS
            if (enrollStep == 4) {
                SuccessContent(
                    onClose = onClose,
                    textColor = textColor
                )
            }
        }
    }
}

// ══════════════════════════════════════
// QR TAB CONTENT
// ══════════════════════════════════════

@Composable
private fun QrTabContent(isDark: Boolean, inputBg: Color) {
    // Info card
    DevoraCard(showTopAccent = true, accentColor = PurpleCore, isDark = isDark) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = PurpleCore,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Scan during initial device setup",
                fontFamily = DMSans,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = TextMuted
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // QR Box
    val infiniteTransition = rememberInfiniteTransition(label = "qrScan")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLine"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .background(inputBg, RoundedCornerShape(16.dp))
                .border(2.dp, PurpleCore, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                val L = 22.dp.toPx()
                val sw = 3.dp.toPx()
                val purpleColor = PurpleCore

                // Top-left corner
                drawLine(purpleColor, Offset(0f, sw / 2), Offset(L, sw / 2), sw, StrokeCap.Round)
                drawLine(purpleColor, Offset(sw / 2, 0f), Offset(sw / 2, L), sw, StrokeCap.Round)

                // Top-right corner
                drawLine(purpleColor, Offset(size.width - L, sw / 2), Offset(size.width, sw / 2), sw, StrokeCap.Round)
                drawLine(purpleColor, Offset(size.width - sw / 2, 0f), Offset(size.width - sw / 2, L), sw, StrokeCap.Round)

                // Bottom-left corner
                drawLine(purpleColor, Offset(0f, size.height - sw / 2), Offset(L, size.height - sw / 2), sw, StrokeCap.Round)
                drawLine(purpleColor, Offset(sw / 2, size.height - L), Offset(sw / 2, size.height), sw, StrokeCap.Round)

                // Bottom-right corner
                drawLine(purpleColor, Offset(size.width - L, size.height - sw / 2), Offset(size.width, size.height - sw / 2), sw, StrokeCap.Round)
                drawLine(purpleColor, Offset(size.width - sw / 2, size.height - L), Offset(size.width - sw / 2, size.height), sw, StrokeCap.Round)

                // Animated scan line
                val scanY = scanProgress * size.height
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, PurpleCore, Color.Transparent)
                    ),
                    topLeft = Offset(0f, scanY),
                    size = Size(size.width, 2.dp.toPx()),
                    alpha = 0.7f
                )

                // 7x7 QR grid
                val cell = 8.dp.toPx()
                val gap = 10.dp.toPx()
                val gridWidth = 7 * gap
                val gx = size.width / 2 - gridWidth / 2
                val gy = size.height / 2 - gridWidth / 2

                for (row in 0 until 7) {
                    for (col in 0 until 7) {
                        drawRoundRect(
                            color = BgElevated,
                            topLeft = Offset(gx + col * gap, gy + row * gap),
                            size = Size(cell, cell),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Point camera at enrollment QR code",
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = TextMuted,
        modifier = Modifier.fillMaxWidth(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Action buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(inputBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.FlashlightOn,
                contentDescription = "Flashlight",
                tint = PurpleCore,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(inputBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Image,
                contentDescription = "Gallery",
                tint = PurpleCore,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ══════════════════════════════════════
// TOKEN INPUT CONTENT
// ══════════════════════════════════════

@Composable
private fun TokenInputContent(
    token: String,
    onTokenChange: (String) -> Unit,
    onEnroll: () -> Unit,
    textColor: Color,
    inputBg: Color,
    isDark: Boolean
) {
    Text(
        text = "Enrollment Token",
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = PurpleCore
    )

    Spacer(modifier = Modifier.height(8.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(inputBg, InputShape)
            .border(1.dp, PurpleCore.copy(alpha = 0.30f), InputShape)
            .padding(16.dp)
    ) {
        if (token.isEmpty()) {
            Text(
                text = "Enter enrollment token...",
                fontFamily = JetBrainsMono,
                fontSize = 16.sp,
                color = TextMuted
            )
        }
        BasicTextField(
            value = token,
            onValueChange = onTokenChange,
            textStyle = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize = 16.sp,
                color = textColor
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    DevoraButton(
        text = "Enroll Device →",
        onClick = onEnroll,
        modifier = Modifier.fillMaxWidth(),
        isDark = isDark
    )
}

// ══════════════════════════════════════
// STEP PROGRESS CONTENT
// ══════════════════════════════════════

@Composable
private fun StepProgressContent(
    enrollStep: Int,
    onStepAdvance: (Int) -> Unit,
    isDark: Boolean,
    textColor: Color
) {
    val steps = listOf(
        "Validating Token" to "Checking token validity...",
        "Enrolling Device" to "Registering with server...",
        "Applying Policies" to "Configuring device policies...",
        "Sync Complete" to "Device is ready to use"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Auto advance steps
    LaunchedEffect(enrollStep) {
        if (enrollStep in 0..2) {
            delay(1500)
            onStepAdvance(enrollStep + 1)
        } else if (enrollStep == 3) {
            delay(500)
            onStepAdvance(4)
        }
    }

    Column {
        steps.forEachIndexed { index, (title, subtitle) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                // Step indicator
                when {
                    index < enrollStep -> {
                        // Done - checkmark
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(PurpleCore, PurpleDeep)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(14.dp)) {
                                val path = Path().apply {
                                    moveTo(size.width * 0.2f, size.height * 0.5f)
                                    lineTo(size.width * 0.4f, size.height * 0.7f)
                                    lineTo(size.width * 0.8f, size.height * 0.3f)
                                }
                                drawPath(
                                    path = path,
                                    color = Color.White,
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }
                    }
                    index == enrollStep -> {
                        // Current - pulsing with progress
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .scale(pulse)
                                .border(2.dp, PurpleCore, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = PurpleCore,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    else -> {
                        // Pending
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    if (isDark) DarkBgElevated else BgElevated,
                                    CircleShape
                                )
                                .border(1.dp, TextMuted, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = textColor
                    )
                    Text(
                        text = subtitle,
                        fontFamily = DMSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            // Dashed connector line
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .width(1.dp)
                        .height(24.dp)
                        .background(PurpleCore.copy(alpha = 0.25f))
                )
            }
        }
    }
}

// ══════════════════════════════════════
// SUCCESS CONTENT
// ══════════════════════════════════════

@Composable
private fun SuccessContent(
    onClose: () -> Unit,
    textColor: Color
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(500),
        label = "successScale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Animated checkmark circle
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(scale)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(PurpleCore, PurpleDeep)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(40.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.2f, size.height * 0.5f)
                    lineTo(size.width * 0.4f, size.height * 0.7f)
                    lineTo(size.width * 0.8f, size.height * 0.3f)
                }
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Device Enrolled!",
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = textColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Galaxy S24 Ultra",
            fontFamily = DMSans,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = PurpleCore
        )

        Spacer(modifier = Modifier.height(20.dp))

        DevoraButton(
            text = "Go to Dashboard",
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

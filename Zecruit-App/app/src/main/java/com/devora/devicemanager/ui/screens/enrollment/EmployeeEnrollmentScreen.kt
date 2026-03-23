package com.devora.devicemanager.ui.screens.enrollment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devora.devicemanager.enrollment.EnrollmentViewModel
import com.devora.devicemanager.session.SessionManager
import com.devora.devicemanager.ui.theme.DMSans
import com.devora.devicemanager.ui.theme.JetBrainsMono
import com.devora.devicemanager.ui.theme.PlusJakartaSans
import com.devora.devicemanager.ui.theme.PurpleCore
import com.devora.devicemanager.ui.theme.PurpleDeep
import kotlinx.coroutines.launch

// ══════════════════════════════════════
// EMPLOYEE ENROLLMENT SCREEN
// Always dark background (#08080F)
// ══════════════════════════════════════

private val DarkBase = Color(0xFF0F0F14)
private val DarkSurface = Color(0xFF1A1A24)
private val DarkElevated = Color(0xFF242430)
private val DarkBorder = Color(0xFF343444)
private val Purple = Color(0xFF7B61FF)
private val PurpleLight = Color(0xFFC0B3FF)
private val PurpleFaint = Color(0xFFC0B3FF)
private val TextWhite = Color(0xFFF0F2F5)
private val TextDim = Color(0xFF6B7D8D)
private val TextSub = Color(0xFF8899A6)

@Composable
fun EmployeeEnrollmentScreen(
    onEnrollSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val enrollmentVm: EnrollmentViewModel = viewModel()
    val enrollmentState by enrollmentVm.uiState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var tokenInput by remember { mutableStateOf(TextFieldValue("")) }
    // enrollStep is now driven by the ViewModel's stepIndex
    var enrollStep by remember { mutableIntStateOf(-1) }
    var showHowTo by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    fun formatToken(input: String): String {
        val raw = input.uppercase().filter { it.isLetterOrDigit() }
        if (raw.isEmpty()) return ""
        // Still typing the DEV prefix
        if (raw.length <= 3) return raw.take(3)
        // DEV + up to 12 body chars (3 groups of 4)
        val body = raw.substring(3).take(12)
        return "DEV-" + body.chunked(4).joinToString("-")
    }

    // Sync the ViewModel's step index to the local enrollStep for unchanged UI rendering
    enrollStep = enrollmentState.stepIndex

    LaunchedEffect(Unit) {
        // If user explicitly signed out, enforce clean enrollment state on screen entry.
        if (SessionManager.isForceReEnroll(context)) {
            enrollmentVm.resetEnrollment()
        }
    }

    // Navigate to dashboard only after genuine successful enrollment, not stale state.
    LaunchedEffect(enrollStep) {
        if (enrollStep == 5 && !SessionManager.isForceReEnroll(context)) {
            onEnrollSuccess()
        }
    }

    // Show error messages from the ViewModel
    val isExpiredError = enrollmentState.errorMessage
        ?.contains("expired", ignoreCase = true) == true
    if (enrollmentState.errorMessage != null) {
        LaunchedEffect(enrollmentState.errorMessage) {
            if (!isExpiredError) {
                snackbarHostState.showSnackbar(enrollmentState.errorMessage ?: "Enrollment error")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBase)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ══════ BACK BUTTON ══════
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PurpleLight,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // ══════ DEVORA LOGO ══════
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Purple.copy(alpha = 0.12f))
                    .border(1.dp, Purple.copy(alpha = 0.30f), CircleShape)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(50.dp)) {
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Purple, PurpleFaint)),
                        startAngle = 30f,
                        sweepAngle = 300f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawCircle(PurpleLight, 4.dp.toPx(), Offset(size.width * 0.35f, size.height * 0.42f))
                    drawCircle(PurpleLight, 4.dp.toPx(), Offset(size.width * 0.65f, size.height * 0.42f))
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "DEVORA",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = TextWhite,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                letterSpacing = 2.sp
            )

            Text(
                "Enterprise Device Manager",
                fontFamily = DMSans,
                fontSize = 13.sp,
                color = TextDim,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(20.dp))

            if (isExpiredError) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF44336)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFF44336)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Token Expired",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF44336),
                                fontFamily = PlusJakartaSans
                            )
                            Text(
                                text = "This enrollment token has expired. Please ask your IT admin to generate a new token.",
                                fontSize = 12.sp,
                                color = Color(0xFFF44336),
                                fontFamily = DMSans
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ══════ SECURITY INFO CARD ══════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .border(1.dp, Color(0x337B61FF), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Purple.copy(alpha = 0.12f))
                            .border(1.dp, Purple.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Security, contentDescription = null, tint = PurpleLight, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Your device will be enrolled into your company's Mobile Device Management system.",
                        fontFamily = DMSans,
                        fontSize = 13.sp,
                        color = TextSub,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ══════ TAB ROW ══════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, Color(0x267B61FF), RoundedCornerShape(12.dp))
            ) {
                listOf("Scan QR Code", "Enter Token").forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Purple else Color.Transparent)
                            .clickable { selectedTab = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            tab,
                            fontFamily = if (isSelected) PlusJakartaSans else DMSans,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (isSelected) Color.White else TextDim
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ══════════════════════════════════════
            // NOT ENROLLING YET
            // ══════════════════════════════════════

            if (enrollStep == -1) {

                // ── SCAN QR TAB ──
                if (selectedTab == 0) {

                    // Instruction box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .border(1.dp, Color(0x1A7B61FF), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = PurpleLight, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Ask your IT Administrator for the enrollment QR code and have it ready on their screen.",
                                fontFamily = DMSans,
                                fontSize = 13.sp,
                                color = TextSub,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── REAL CAMERA VIEWFINDER ──
                    val lifecycleOwner = LocalLifecycleOwner.current
                    var hasCameraPermission by remember {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                    PackageManager.PERMISSION_GRANTED
                        )
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        hasCameraPermission = granted
                    }

                    LaunchedEffect(Unit) {
                        if (!hasCameraPermission) {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(1.dp, Color(0x407B61FF), RoundedCornerShape(16.dp))
                    ) {
                        if (hasCameraPermission) {
                            // Real CameraX preview with ML Kit QR scanning
                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx).apply {
                                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    }

                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()

                                        val preview = androidx.camera.core.Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }

                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()
                                            .also { analysis ->
                                                analysis.setAnalyzer(
                                                    ContextCompat.getMainExecutor(ctx),
                                                    QrCodeAnalyzer { qrValue ->
                                                        Log.d("QRScan", "Detected QR: $qrValue")
                                                        enrollmentVm.enrollWithQrCode(qrValue)
                                                    }
                                                )
                                            }

                                        try {
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                CameraSelector.DEFAULT_BACK_CAMERA,
                                                preview,
                                                imageAnalysis
                                            )
                                        } catch (e: Exception) {
                                            Log.e("QRScan", "Camera bind failed: ${e.message}")
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))

                                    previewView
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Corner brackets overlay
                            Canvas(Modifier.fillMaxSize()) {
                                val bracketLen = 28.dp.toPx()
                                val sw = 3.dp.toPx()
                                val m = 20.dp.toPx()

                                drawLine(Purple, Offset(m, m + bracketLen), Offset(m, m), sw, StrokeCap.Round)
                                drawLine(Purple, Offset(m, m), Offset(m + bracketLen, m), sw, StrokeCap.Round)
                                drawLine(Purple, Offset(size.width - m - bracketLen, m), Offset(size.width - m, m), sw, StrokeCap.Round)
                                drawLine(Purple, Offset(size.width - m, m), Offset(size.width - m, m + bracketLen), sw, StrokeCap.Round)
                                drawLine(Purple, Offset(m, size.height - m - bracketLen), Offset(m, size.height - m), sw, StrokeCap.Round)
                                drawLine(Purple, Offset(m, size.height - m), Offset(m + bracketLen, size.height - m), sw, StrokeCap.Round)
                                drawLine(Purple, Offset(size.width - m - bracketLen, size.height - m), Offset(size.width - m, size.height - m), sw, StrokeCap.Round)
                                drawLine(Purple, Offset(size.width - m, size.height - m - bracketLen), Offset(size.width - m, size.height - m), sw, StrokeCap.Round)
                            }

                            // Animated scan line
                            val infiniteTransition = rememberInfiniteTransition(label = "scan")
                            val scanY by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    tween(2000, easing = LinearEasing),
                                    RepeatMode.Reverse
                                ),
                                label = "scanY"
                            )

                            Canvas(Modifier.fillMaxSize()) {
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Purple.copy(alpha = 0.8f),
                                            Purple,
                                            Purple.copy(alpha = 0.8f),
                                            Color.Transparent
                                        )
                                    ),
                                    topLeft = Offset(0f, size.height * scanY),
                                    size = Size(size.width, 3.dp.toPx())
                                )
                            }
                        } else {
                            // No camera permission — show placeholder
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Purple.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Camera permission required",
                                    fontFamily = DMSans,
                                    fontSize = 12.sp,
                                    color = TextDim
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Tap to grant permission",
                                    fontFamily = DMSans,
                                    fontSize = 11.sp,
                                    color = Purple,
                                    modifier = Modifier.clickable {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── HOW TO GET QR ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface)
                            .border(1.dp, Color(0x1A7B61FF), RoundedCornerShape(10.dp))
                            .clickable { showHowTo = !showHowTo }
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "▸ How to get QR from IT Admin",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = PurpleLight
                                )
                                Icon(
                                    imageVector = if (showHowTo) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = PurpleLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (showHowTo) {
                                Spacer(Modifier.height(12.dp))
                                listOf(
                                    "Contact your IT Administrator",
                                    "They will generate an enrollment QR code",
                                    "Display it on their screen or print it",
                                    "Point this device camera at the QR code",
                                    "Enrollment will start automatically"
                                ).forEachIndexed { i, step ->
                                    Row(
                                        modifier = Modifier.padding(bottom = 6.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(Purple.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "${i + 1}",
                                                fontFamily = JetBrainsMono,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = PurpleLight
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(step, fontFamily = DMSans, fontSize = 13.sp, color = TextSub)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Simulate QR scan (demo)
                    Button(
                        onClick = { enrollmentVm.simulateQrScan() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Simulate QR Scan (Demo)",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // ── ENTER TOKEN TAB ──
                if (selectedTab == 1) {

                    Text(
                        "Contact your IT Administrator for your enrollment token",
                        fontFamily = DMSans,
                        fontSize = 13.sp,
                        color = TextSub,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Enrollment Token",
                        fontFamily = DMSans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = PurpleLight
                    )

                    Spacer(Modifier.height(8.dp))

                    // Large token input
                    BasicTextField(
                        value = tokenInput,
                        onValueChange = { value ->
                            // Update selection to match formatted text length
                            val formatted = formatToken(value.text)
                            tokenInput = TextFieldValue(
                                text = formatted,
                                selection = TextRange(formatted.length)
                            )
                            enrollmentVm.onTokenChanged(formatted)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkElevated)
                            .border(
                                1.5.dp,
                                if (tokenInput.text.isNotEmpty()) Purple else Color(0x357B61FF),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(20.dp),
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMono,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite,
                            letterSpacing = 3.sp,
                            textAlign = TextAlign.Center
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters
                        ),
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (tokenInput.text.isEmpty()) {
                                    Text(
                                        "DEV-XXXX-XXXX-XXXX",
                                        fontFamily = JetBrainsMono,
                                        fontSize = 18.sp,
                                        color = TextDim,
                                        letterSpacing = 3.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                inner()
                            }
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Format: DEV-XXXX-XXXX-XXXX (provided by IT Admin)",
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        color = TextDim,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))

                    // Validate & Enroll button
                    Button(
                        onClick = {
                            if (!tokenInput.text.startsWith("DEV-") || tokenInput.text.length != 18) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Invalid token format")
                                }
                                return@Button
                            }
                            enrollmentVm.onTokenChanged(tokenInput.text)
                            enrollmentVm.enrollWithToken()
                        },
                        enabled = tokenInput.text.length == 18,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple,
                            disabledContainerColor = Color(0xFF2A2345)
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Validate & Enroll",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // ══════════════════════════════════════
            // ENROLLMENT IN PROGRESS (steps 0-4)
            // ══════════════════════════════════════

            if (enrollStep in 0..4) {

                val steps = listOf(
                    "Validating Enrollment Token" to "Checking token with Devora server...",
                    "Connecting to Devora Server" to "Establishing secure connection...",
                    "Installing Device Policies" to "Applying enterprise security policies...",
                    "Configuring Device Owner" to "Setting up admin privileges...",
                    "Finalizing Enrollment" to "Completing registration..."
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pulsing progress indicator
                    val pulseAnim by rememberInfiniteTransition(label = "pulse")
                        .animateFloat(
                            initialValue = 0.85f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                tween(800),
                                RepeatMode.Reverse
                            ),
                            label = "pulseScale"
                        )

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(pulseAnim)
                            .clip(CircleShape)
                            .background(Purple.copy(alpha = 0.12f))
                            .border(2.dp, Color(0x407B61FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(44.dp),
                            color = Purple,
                            strokeWidth = 3.dp
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "Enrolling Device...",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextWhite
                    )

                    Spacer(Modifier.height(24.dp))

                    // Vertical stepper
                    steps.forEachIndexed { index, (title, subtitle) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = if (index < enrollStep) Alignment.CenterVertically else Alignment.Top
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Step indicator
                                when {
                                    index < enrollStep -> {
                                        // Completed
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(listOf(PurpleCore, PurpleDeep))
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    index == enrollStep -> {
                                        // Current — pulsing
                                        val stepPulse by rememberInfiniteTransition(label = "stepPulse$index")
                                            .animateFloat(
                                                initialValue = 1f,
                                                targetValue = 1.2f,
                                                animationSpec = infiniteRepeatable(
                                                    tween(600),
                                                    RepeatMode.Reverse
                                                ),
                                                label = "stepScale$index"
                                            )
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .scale(stepPulse)
                                                .clip(CircleShape)
                                                .background(Purple.copy(alpha = 0.15f))
                                                .border(2.dp, Purple, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = Purple,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                    else -> {
                                        // Pending
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(DarkElevated)
                                                .border(1.dp, DarkBorder, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "${index + 1}",
                                                fontFamily = JetBrainsMono,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = TextDim
                                            )
                                        }
                                    }
                                }

                                // Connecting line
                                if (index < steps.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(32.dp)
                                            .background(
                                                if (index < enrollStep) PurpleCore.copy(0.4f) else DarkBorder
                                            )
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(top = 4.dp)
                            ) {
                                Text(
                                    title,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = when {
                                        index < enrollStep -> Purple
                                        index == enrollStep -> TextWhite
                                        else -> TextDim
                                    }
                                )
                                Text(
                                    subtitle,
                                    fontFamily = DMSans,
                                    fontSize = 12.sp,
                                    color = TextDim
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }

        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

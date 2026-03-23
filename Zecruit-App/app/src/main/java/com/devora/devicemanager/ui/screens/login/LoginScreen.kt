package com.devora.devicemanager.ui.screens.login

import com.devora.devicemanager.data.remote.RemoteDataSource
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import com.devora.devicemanager.R
import com.devora.devicemanager.network.AdminLoginRequest
import com.devora.devicemanager.session.SessionManager
import com.devora.devicemanager.ui.components.ButtonVariant
import com.devora.devicemanager.ui.components.DevoraButton
import com.devora.devicemanager.ui.theme.BgElevated
import com.devora.devicemanager.ui.theme.BgSurface
import com.devora.devicemanager.ui.theme.CardShape
import com.devora.devicemanager.ui.theme.DMSans
import com.devora.devicemanager.ui.theme.DarkBgBase
import com.devora.devicemanager.ui.theme.DarkBgElevated
import com.devora.devicemanager.ui.theme.DarkBgSurface
import com.devora.devicemanager.ui.theme.DarkTextPrimary
import com.devora.devicemanager.ui.theme.InputShape
import com.devora.devicemanager.ui.theme.PlusJakartaSans
import com.devora.devicemanager.ui.theme.PurpleCore
import com.devora.devicemanager.ui.theme.TextMuted
import com.devora.devicemanager.ui.theme.TextPrimary
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onAdminRegister: () -> Unit = {},
    onEmployeeRegister: () -> Unit = {},
    onEmployeeEnroll: () -> Unit = {},
    isDark: Boolean,
    onThemeToggle: () -> Unit
) {
    var selectedRole by remember { mutableIntStateOf(0) } // 0=Admin, 1=Employee
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Reset fields when LoginScreen reappears (e.g., after sign out)
    LaunchedEffect(Unit) {
        email = ""
        password = ""
        showPassword = false
    }

    val bgColor = if (isDark) DarkBgBase else Color(0xFFF7F8FA)
    val cardBg = if (isDark) DarkBgSurface else BgSurface
    val inputBg = if (isDark) DarkBgElevated else BgElevated
    val textColor = if (isDark) DarkTextPrimary else TextPrimary

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = bgColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Login card centered
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isDark) {
                                Modifier.border(
                                    width = 1.dp,
                                    color = PurpleCore.copy(alpha = 0.25f),
                                    shape = CardShape
                                )
                            } else {
                                Modifier
                            }
                        ),
                    shape = CardShape,
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDark) 0.dp else 4.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Logo image
                        Image(
                            painter = painterResource(id = R.drawable.devora_nobg),
                            contentDescription = "Devora Logo",
                            modifier = Modifier.size(80.dp),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. "DEVORA"
                        Text(
                            text = "DEVORA",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            color = PurpleCore
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 3. "Enterprise Device Manager"
                        Text(
                            text = "Enterprise Device Manager",
                            fontFamily = DMSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // 4. Divider
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = PurpleCore.copy(alpha = 0.15f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Role toggle: Admin / Employee
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(
                                    if (isDark) Color(0xFF242430) else Color(0xFFE9EEF3),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    PurpleCore.copy(alpha = 0.20f),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            listOf("Admin Login" to Icons.Filled.Shield, "Employee Login" to Icons.Filled.PersonAdd).forEachIndexed { index, (label, icon) ->
                                val isSelected = selectedRole == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(4.dp)
                                        .background(
                                            if (isSelected) PurpleCore else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedRole = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else TextMuted
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (selectedRole == 0) {
                            // ══════ ADMIN LOGIN FIELDS ══════

                            // 5. Email label
                            Text(
                                text = "Email",
                                fontFamily = DMSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = PurpleCore,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 6. Email TextField
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .background(inputBg, InputShape)
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Email,
                                        contentDescription = null,
                                        tint = PurpleCore,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (email.isEmpty()) {
                                            Text(
                                                text = "Enter your email",
                                                fontFamily = DMSans,
                                                fontSize = 14.sp,
                                                color = TextMuted
                                            )
                                        }
                                        BasicTextField(
                                            value = email,
                                            onValueChange = { email = it },
                                            textStyle = TextStyle(
                                                fontFamily = DMSans,
                                                fontSize = 14.sp,
                                                color = textColor
                                            ),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 7. Password label
                            Text(
                                text = "Password",
                                fontFamily = DMSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = PurpleCore,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 8. Password TextField
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .background(inputBg, InputShape)
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = PurpleCore,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (password.isEmpty()) {
                                            Text(
                                                text = "Enter password",
                                                fontFamily = DMSans,
                                                fontSize = 14.sp,
                                                color = TextMuted
                                            )
                                        }
                                        BasicTextField(
                                            value = password,
                                            onValueChange = { password = it },
                                            textStyle = TextStyle(
                                                fontFamily = DMSans,
                                                fontSize = 14.sp,
                                                color = textColor
                                            ),
                                            singleLine = true,
                                            visualTransformation = if (showPassword) {
                                                VisualTransformation.None
                                            } else {
                                                PasswordVisualTransformation()
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    IconButton(
                                        onClick = { showPassword = !showPassword },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showPassword) {
                                                Icons.Filled.Visibility
                                            } else {
                                                Icons.Filled.VisibilityOff
                                            },
                                            contentDescription = "Toggle password visibility",
                                            tint = PurpleCore,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // 9. Sign In button
                            DevoraButton(
                                text = "Sign In",
                                onClick = {
                                    if (email.isBlank() || password.isBlank()) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Email and password are required")
                                        }
                                        return@DevoraButton
                                    }
                                    isLoading = true
                                    scope.launch {
                                        try {
                                            val normalizedEmail = email.trim().lowercase()
                                            val response = RemoteDataSource.loginAdmin(
                                                AdminLoginRequest(
                                                    email = normalizedEmail,
                                                    password = password
                                                )
                                            )
                                            if (response.isSuccessful && response.body()?.success == true) {
                                                val adminName = response.body()?.name ?: ""
                                                SessionManager.saveSession(context, adminName, normalizedEmail)
                                                onLoginSuccess()
                                            } else {
                                                val msg = response.body()?.message
                                                    ?: "Invalid credentials. Use Admin email and password."
                                                snackbarHostState.showSnackbar(msg)
                                            }
                                        } catch (e: Exception) {
                                            Log.e("LoginScreen", "Login failed: ${e.message}")
                                            snackbarHostState.showSnackbar("Network error: ${e.message}")
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                variant = ButtonVariant.PRIMARY,
                                isLoading = isLoading,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // 10. Register link
                            Text(
                                text = "Don't have an account? Register",
                                fontFamily = DMSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = PurpleCore,
                                modifier = Modifier.clickable { onAdminRegister() }
                            )
                        } else {
                            // ══════ EMPLOYEE LOGIN ══════
                            Text(
                                text = "New employee? Register your account and request a device.",
                                fontFamily = DMSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = TextMuted
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Spacer(modifier = Modifier.height(12.dp))

                            DevoraButton(
                                text = "Enroll Device →",
                                onClick = onEmployeeEnroll,
                                variant = ButtonVariant.OUTLINE,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Already registered? Sign in from Admin tab",
                                fontFamily = DMSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom: "Secured by Enterprise MDM"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Secured by Enterprise MDM",
                        fontFamily = DMSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // Theme toggle top-right (on top of scrollable content)
            IconButton(
                onClick = onThemeToggle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Toggle theme",
                    tint = PurpleCore
                )
            }
        }
    }
}

package com.devora.devicemanager.ui.screens.register

import com.devora.devicemanager.data.remote.RemoteDataSource
import android.util.Log
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devora.devicemanager.R
import com.devora.devicemanager.network.AdminRegisterRequest
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
fun AdminRegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    isDark: Boolean
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val bgColor = if (isDark) DarkBgBase else Color(0xFFF7F8FA)
    val cardBg = if (isDark) DarkBgSurface else BgSurface
    val inputBg = if (isDark) DarkBgElevated else BgElevated
    val textColor = if (isDark) DarkTextPrimary else TextPrimary

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = bgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                    // Logo
                    Image(
                        painter = painterResource(id = R.drawable.devora_nobg),
                        contentDescription = "Devora Logo",
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "DEVORA",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        color = PurpleCore
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Admin Registration",
                        fontFamily = DMSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Divider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = PurpleCore.copy(alpha = 0.15f)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Name field
                    InputField(
                        label = "Full Name",
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "Enter your name",
                        icon = Icons.Filled.Person,
                        inputBg = inputBg,
                        textColor = textColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email field
                    InputField(
                        label = "Email",
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Enter your email",
                        icon = Icons.Filled.Email,
                        inputBg = inputBg,
                        textColor = textColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password field
                    PasswordField(
                        label = "Password",
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Enter password",
                        showPassword = showPassword,
                        onToggle = { showPassword = !showPassword },
                        inputBg = inputBg,
                        textColor = textColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Password field
                    PasswordField(
                        label = "Confirm Password",
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = "Confirm password",
                        showPassword = showConfirmPassword,
                        onToggle = { showConfirmPassword = !showConfirmPassword },
                        inputBg = inputBg,
                        textColor = textColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Register button
                    DevoraButton(
                        text = "Register",
                        onClick = {
                            when {
                                name.isBlank() || email.isBlank() || password.isBlank() -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("All fields are required")
                                    }
                                }
                                password != confirmPassword -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Passwords do not match")
                                    }
                                }
                                password.length < 6 -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Password must be at least 6 characters")
                                    }
                                }
                                else -> {
                                    isLoading = true
                                    scope.launch {
                                        try {
                                            val response = RemoteDataSource.registerAdmin(
                                                AdminRegisterRequest(
                                                    name = name.trim(),
                                                    email = email.trim(),
                                                    password = password
                                                )
                                            )
                                            if (response.isSuccessful && response.body()?.success == true) {
                                                snackbarHostState.showSnackbar("Registration successful! Please login.")
                                                onRegisterSuccess()
                                            } else {
                                                val msg = response.body()?.message
                                                    ?: "Registration failed"
                                                snackbarHostState.showSnackbar(msg)
                                            }
                                        } catch (e: Exception) {
                                            Log.e("AdminRegister", "Register failed: ${e.message}")
                                            snackbarHostState.showSnackbar("Network error: ${e.message}")
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        },
                        variant = ButtonVariant.PRIMARY,
                        isLoading = isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Back to login link
                    Text(
                        text = "Already have an account? Sign In",
                        fontFamily = DMSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = PurpleCore,
                        modifier = Modifier.clickable { onBackToLogin() }
                    )
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
    }
}

@Composable
private fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    inputBg: Color,
    textColor: Color
) {
    Text(
        text = label,
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = PurpleCore,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
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
                imageVector = icon,
                contentDescription = null,
                tint = PurpleCore,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontFamily = DMSans,
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
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
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    showPassword: Boolean,
    onToggle: () -> Unit,
    inputBg: Color,
    textColor: Color
) {
    Text(
        text = label,
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = PurpleCore,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
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
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontFamily = DMSans,
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
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
                onClick = onToggle,
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
}

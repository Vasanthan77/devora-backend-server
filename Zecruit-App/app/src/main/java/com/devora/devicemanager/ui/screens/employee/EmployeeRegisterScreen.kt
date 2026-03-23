package com.devora.devicemanager.ui.screens.employee

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devora.devicemanager.ui.components.ButtonVariant
import com.devora.devicemanager.ui.components.DevoraButton
import com.devora.devicemanager.ui.components.DevoraCard
import com.devora.devicemanager.ui.components.SectionHeader
import com.devora.devicemanager.ui.theme.BgBase
import com.devora.devicemanager.ui.theme.DarkBgBase
import com.devora.devicemanager.ui.theme.DarkBgElevated
import com.devora.devicemanager.ui.theme.DMSans
import com.devora.devicemanager.ui.theme.Danger
import com.devora.devicemanager.ui.theme.InputShape
import com.devora.devicemanager.ui.theme.PlusJakartaSans
import com.devora.devicemanager.ui.theme.PurpleBright
import com.devora.devicemanager.ui.theme.PurpleCore
import com.devora.devicemanager.ui.theme.Success
import com.devora.devicemanager.ui.theme.TextMuted
import com.devora.devicemanager.ui.theme.Warning

@Composable
fun EmployeeRegisterScreen(
    onBack: () -> Unit,
    onRegistrationSuccess: () -> Unit,
    isDark: Boolean,
    viewModel: EmployeeRegisterViewModel = viewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val scrollState = rememberScrollState()

    val bgColor = if (isDark) DarkBgBase else BgBase
    val textColor = if (isDark) Color(0xFFF0F2F5) else Color(0xFF1D1D21)
    val inputBg = if (isDark) DarkBgElevated else Color(0xFFE9EEF3)

    // Step tracking (visual only — all fields on one page)
    val currentStep = when {
        formState.isSuccess -> 2
        formState.fullName.isNotBlank() && formState.employeeId.isNotBlank() && formState.email.isNotBlank() -> 1
        else -> 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // ══════ TOP BAR ══════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PurpleCore,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Employee Registration",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textColor
            )
        }

        // ══════ PROGRESS INDICATOR ══════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Register", "Verify", "Enroll").forEachIndexed { index, label ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    index < currentStep -> Success
                                    index == currentStep -> PurpleCore
                                    else -> if (isDark) DarkBgElevated else Color(0xFFE9EEF3)
                                }
                            )
                            .then(
                                if (index > currentStep) {
                                    Modifier.border(1.dp, PurpleCore.copy(alpha = 0.3f), CircleShape)
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index < currentStep) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                fontFamily = DMSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (index == currentStep) Color.White else TextMuted
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontFamily = DMSans,
                        fontWeight = if (index == currentStep) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 10.sp,
                        color = if (index <= currentStep) PurpleCore else TextMuted
                    )
                }
                if (index < 2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .padding(horizontal = 8.dp)
                            .background(
                                if (index < currentStep) Success
                                else PurpleCore.copy(alpha = 0.15f),
                                RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ══════ SCROLLABLE FORM ══════
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            // ── Error message ──
            if (formState.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Danger.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        .border(1.dp, Danger.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = formState.errorMessage!!,
                        fontFamily = DMSans,
                        fontSize = 13.sp,
                        color = Danger
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ══════ PERSONAL DETAILS ══════
            DevoraCard(
                isDark = isDark,
                showTopAccent = true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SectionHeader(title = "Personal Details")
                    Spacer(modifier = Modifier.height(16.dp))

                    FormField(
                        label = "Full Name",
                        value = formState.fullName,
                        onValueChange = viewModel::updateFullName,
                        placeholder = "Enter your full name",
                        icon = Icons.Filled.Person,
                        inputBg = inputBg,
                        textColor = textColor,
                        isDark = isDark
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    FormField(
                        label = "Employee ID",
                        value = formState.employeeId,
                        onValueChange = viewModel::updateEmployeeId,
                        placeholder = "e.g. EMP-1024",
                        icon = Icons.Filled.Badge,
                        inputBg = inputBg,
                        textColor = textColor,
                        isDark = isDark
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    FormField(
                        label = "Work Email",
                        value = formState.email,
                        onValueChange = viewModel::updateEmail,
                        placeholder = "name@company.com",
                        icon = Icons.Filled.Email,
                        inputBg = inputBg,
                        textColor = textColor,
                        isDark = isDark
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    DropdownField(
                        label = "Department",
                        value = formState.department,
                        options = viewModel.departments,
                        onSelect = viewModel::updateDepartment,
                        icon = Icons.Filled.Business,
                        inputBg = inputBg,
                        textColor = textColor,
                        isDark = isDark
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    FormField(
                        label = "Designation",
                        value = formState.designation,
                        onValueChange = viewModel::updateDesignation,
                        placeholder = "e.g. Software Engineer",
                        icon = Icons.Filled.Work,
                        inputBg = inputBg,
                        textColor = textColor,
                        isDark = isDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ══════ DEVICE REQUEST ══════
            DevoraCard(
                isDark = isDark,
                showTopAccent = true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SectionHeader(title = "Device Request")
                    Spacer(modifier = Modifier.height(16.dp))

                    DropdownField(
                        label = "Device Type",
                        value = formState.deviceType,
                        options = viewModel.deviceTypes,
                        onSelect = viewModel::updateDeviceType,
                        icon = Icons.Filled.DevicesOther,
                        inputBg = inputBg,
                        textColor = textColor,
                        isDark = isDark
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── Purpose / Reason (multiline) ──
                    Text(
                        text = "Purpose / Reason",
                        fontFamily = DMSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = PurpleCore,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(inputBg, InputShape)
                            .padding(12.dp)
                    ) {
                        if (formState.reason.isEmpty()) {
                            Text(
                                text = "Describe why you need this device...",
                                fontFamily = DMSans,
                                fontSize = 14.sp,
                                color = TextMuted
                            )
                        }
                        BasicTextField(
                            value = formState.reason,
                            onValueChange = viewModel::updateReason,
                            textStyle = TextStyle(
                                fontFamily = DMSans,
                                fontSize = 14.sp,
                                color = textColor
                            ),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formState.reason.length} / 200",
                        fontFamily = DMSans,
                        fontSize = 11.sp,
                        color = if (formState.reason.length > 180) Warning else TextMuted,
                        modifier = Modifier.align(Alignment.End)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    DropdownField(
                        label = "Priority",
                        value = formState.priority,
                        options = viewModel.priorities,
                        onSelect = viewModel::updatePriority,
                        icon = Icons.Filled.PriorityHigh,
                        inputBg = inputBg,
                        textColor = textColor,
                        isDark = isDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ══════ SUBMIT BUTTON ══════
            DevoraButton(
                text = if (formState.isSuccess) "Registration Submitted ✓" else "Submit Registration",
                onClick = { viewModel.submitRegistration(onRegistrationSuccess) },
                variant = ButtonVariant.PRIMARY,
                isLoading = formState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Already registered link ──
            Text(
                text = "Already registered? Sign In",
                fontFamily = DMSans,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = PurpleBright,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onBack() }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ══════════════════════════════════════
// REUSABLE FORM FIELD
// ══════════════════════════════════════

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    inputBg: Color,
    textColor: Color,
    isDark: Boolean
) {
    Text(
        text = label,
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = PurpleCore,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(6.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(inputBg, InputShape)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PurpleCore,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
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

// ══════════════════════════════════════
// REUSABLE DROPDOWN FIELD
// ══════════════════════════════════════

@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    icon: ImageVector,
    inputBg: Color,
    textColor: Color,
    isDark: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Text(
        text = label,
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = PurpleCore,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(6.dp))
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(inputBg, InputShape)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PurpleCore,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = value.ifEmpty { "Select $label" },
                    fontFamily = DMSans,
                    fontSize = 14.sp,
                    color = if (value.isEmpty()) TextMuted else textColor,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = PurpleCore,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            fontFamily = DMSans,
                            fontSize = 14.sp,
                            color = textColor
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

package com.devora.devicemanager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devora.devicemanager.ui.theme.BadgeShape
import com.devora.devicemanager.ui.theme.BgElevated
import com.devora.devicemanager.ui.theme.BgSurface
import com.devora.devicemanager.ui.theme.ButtonShape
import com.devora.devicemanager.ui.theme.CardShape
import com.devora.devicemanager.ui.theme.DMSans
import com.devora.devicemanager.ui.theme.Danger
import com.devora.devicemanager.ui.theme.DarkBgSurface
import com.devora.devicemanager.ui.theme.DarkTextMuted
import com.devora.devicemanager.ui.theme.JetBrainsMono
import com.devora.devicemanager.ui.theme.PlusJakartaSans
import com.devora.devicemanager.ui.theme.PurpleBorder
import com.devora.devicemanager.ui.theme.PurpleBright
import com.devora.devicemanager.ui.theme.PurpleCore
import com.devora.devicemanager.ui.theme.Success
import com.devora.devicemanager.ui.theme.TextMuted
import com.devora.devicemanager.ui.theme.Warning

// ══════════════════════════════════════
// BUTTON VARIANT ENUM
// ══════════════════════════════════════

enum class ButtonVariant {
    PRIMARY, OUTLINE, DANGER
}

// ══════════════════════════════════════
// DEVORA CARD
// ══════════════════════════════════════

@Composable
fun DevoraCard(
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    showTopAccent: Boolean = false,
    isDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val bgColor = if (isDark) DarkBgSurface else BgSurface
    val borderColor = PurpleBorder

    Box(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = bgColor),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isDark) 0.dp else 2.dp
            )
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Left accent strip
                if (accentColor != null) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(accentColor)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Top accent gradient bar
                    if (showTopAccent) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(PurpleCore, Color.Transparent)
                                    )
                                )
                        )
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        content()
                    }
                }
            }
        }

        // Border overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = CardShape
                )
        )
    }
}

// ══════════════════════════════════════
// DEVORA BUTTON
// ══════════════════════════════════════

@Composable
fun DevoraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    isLoading: Boolean = false,
    isDark: Boolean = false
) {
    val backgroundColor = when (variant) {
        ButtonVariant.PRIMARY -> PurpleCore
        ButtonVariant.OUTLINE -> Color.Transparent
        ButtonVariant.DANGER -> Color.Transparent
    }

    val textColor = when (variant) {
        ButtonVariant.PRIMARY -> Color.White
        ButtonVariant.OUTLINE -> PurpleCore
        ButtonVariant.DANGER -> Danger
    }

    val borderColor = when (variant) {
        ButtonVariant.PRIMARY -> Color.Transparent
        ButtonVariant.OUTLINE -> PurpleCore
        ButtonVariant.DANGER -> Danger
    }

    val borderWidth = when (variant) {
        ButtonVariant.PRIMARY -> 0.dp
        ButtonVariant.OUTLINE -> 1.5.dp
        ButtonVariant.DANGER -> 1.5.dp
    }

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(ButtonShape)
            .background(backgroundColor)
            .then(
                if (variant != ButtonVariant.PRIMARY) {
                    Modifier.border(borderWidth, borderColor, ButtonShape)
                } else {
                    Modifier
                }
            )
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = textColor,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = textColor
            )
        }
    }
}

// ══════════════════════════════════════
// STATUS BADGE
// ══════════════════════════════════════

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val config = when (status.uppercase()) {
        "ONLINE" -> StatusConfig(
            bgColor = Success.copy(alpha = 0.12f),
            borderColor = Success.copy(alpha = 0.30f),
            textColor = Success
        )
        "OFFLINE" -> StatusConfig(
            bgColor = TextMuted.copy(alpha = 0.12f),
            borderColor = TextMuted.copy(alpha = 0.30f),
            textColor = TextMuted
        )
        "FLAGGED" -> StatusConfig(
            bgColor = Danger.copy(alpha = 0.12f),
            borderColor = Danger.copy(alpha = 0.30f),
            textColor = Danger
        )
        "PENDING" -> StatusConfig(
            bgColor = Warning.copy(alpha = 0.12f),
            borderColor = Warning.copy(alpha = 0.30f),
            textColor = Warning
        )
        else -> StatusConfig(
            bgColor = TextMuted.copy(alpha = 0.12f),
            borderColor = TextMuted.copy(alpha = 0.30f),
            textColor = TextMuted
        )
    }

    Row(
        modifier = modifier
            .background(config.bgColor, BadgeShape)
            .border(1.dp, config.borderColor, BadgeShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Prefix dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(config.textColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = status.uppercase(),
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            color = config.textColor
        )
    }
}

private data class StatusConfig(
    val bgColor: Color,
    val borderColor: Color,
    val textColor: Color
)

// ══════════════════════════════════════
// SECTION HEADER
// ══════════════════════════════════════

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    isDark: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Vertical bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(22.dp)
                    .background(PurpleCore, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
                color = if (isDark) Color(0xFFF0F2F5) else Color(0xFF1D1D21)
            )
        }

        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                fontFamily = DMSans,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = PurpleCore,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onActionClick
                )
            )
        }
    }
}

// ══════════════════════════════════════
// DEVORA BOTTOM NAV
// ══════════════════════════════════════

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("Dashboard", Icons.Filled.Dashboard, "dashboard"),
    BottomNavItem("Devices", Icons.Filled.PhoneAndroid, "device_list"),
    BottomNavItem("Enroll", Icons.Filled.QrCodeScanner, "admin_generate_enrollment"),
    BottomNavItem("Settings", Icons.Filled.Settings, "settings")
)

@Composable
fun DevoraBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    isDark: Boolean = false
) {
    val bgColor = if (isDark) DarkBgSurface else BgSurface
    val topBorderColor = if (isDark) {
        PurpleCore.copy(alpha = 0.15f)
    } else {
        PurpleCore.copy(alpha = 0.12f)
    }
    val unselectedColor = TextMuted

    Column {
        // Top border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(topBorderColor)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentRoute == item.route
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) PurpleCore else unselectedColor,
                    label = "navIconColor"
                )
                val labelColor by animateColorAsState(
                    targetValue = if (isSelected) PurpleCore else unselectedColor,
                    label = "navLabelColor"
                )

                Column(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onNavigate(item.route) }
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon with optional purple circle background
                    Box(contentAlignment = Alignment.Center) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        PurpleCore.copy(alpha = 0.12f),
                                        CircleShape
                                    )
                            )
                        }
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Label
                    Text(
                        text = item.label,
                        fontFamily = PlusJakartaSans,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 10.sp,
                        color = labelColor
                    )

                    // Purple dot indicator for selected
                    if (isSelected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .background(PurpleCore, CircleShape)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(5.dp))
                    }
                }
            }
        }
    }
}

package com.devora.devicemanager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ══════════════════════════════════════
// SHAPE TOKENS
// ══════════════════════════════════════
val CardShape = RoundedCornerShape(16.dp)
val ButtonShape = RoundedCornerShape(12.dp)
val InputShape = RoundedCornerShape(10.dp)
val ChipShape = RoundedCornerShape(8.dp)
val BadgeShape = RoundedCornerShape(100.dp)

// ══════════════════════════════════════
// MATERIAL 3 SHAPES
// ══════════════════════════════════════
val DevoraShapes = Shapes(
    extraSmall = ChipShape,
    small = InputShape,
    medium = ButtonShape,
    large = CardShape,
    extraLarge = BadgeShape
)

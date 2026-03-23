package com.devora.devicemanager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.devora.devicemanager.R

// ══════════════════════════════════════
// GOOGLE FONTS PROVIDER
// ══════════════════════════════════════
val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// ══════════════════════════════════════
// FONT FAMILIES
// ══════════════════════════════════════

// Plus Jakarta Sans → ALL headings, titles, numbers, app name
val PlusJakartaSans = FontFamily(
    Font(
        googleFont = GoogleFont("Plus Jakarta Sans"),
        fontProvider = googleFontProvider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = GoogleFont("Plus Jakarta Sans"),
        fontProvider = googleFontProvider,
        weight = FontWeight.Medium
    ),
    Font(
        googleFont = GoogleFont("Plus Jakarta Sans"),
        fontProvider = googleFontProvider,
        weight = FontWeight.SemiBold
    ),
    Font(
        googleFont = GoogleFont("Plus Jakarta Sans"),
        fontProvider = googleFontProvider,
        weight = FontWeight.Bold
    ),
    Font(
        googleFont = GoogleFont("Plus Jakarta Sans"),
        fontProvider = googleFontProvider,
        weight = FontWeight.ExtraBold
    )
)

// DM Sans → ALL body text, labels, descriptions
val DMSans = FontFamily(
    Font(
        googleFont = GoogleFont("DM Sans"),
        fontProvider = googleFontProvider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = GoogleFont("DM Sans"),
        fontProvider = googleFontProvider,
        weight = FontWeight.Medium
    ),
    Font(
        googleFont = GoogleFont("DM Sans"),
        fontProvider = googleFontProvider,
        weight = FontWeight.SemiBold
    ),
    Font(
        googleFont = GoogleFont("DM Sans"),
        fontProvider = googleFontProvider,
        weight = FontWeight.Bold
    )
)

// JetBrains Mono → ALL data values, IDs, codes, timestamps
val JetBrainsMono = FontFamily(
    Font(
        googleFont = GoogleFont("JetBrains Mono"),
        fontProvider = googleFontProvider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = GoogleFont("JetBrains Mono"),
        fontProvider = googleFontProvider,
        weight = FontWeight.Medium
    ),
    Font(
        googleFont = GoogleFont("JetBrains Mono"),
        fontProvider = googleFontProvider,
        weight = FontWeight.SemiBold
    ),
    Font(
        googleFont = GoogleFont("JetBrains Mono"),
        fontProvider = googleFontProvider,
        weight = FontWeight.Bold
    )
)

// ══════════════════════════════════════
// MATERIAL 3 TYPOGRAPHY
// ══════════════════════════════════════
val DevoraTypography = Typography(
    // Display
    displayLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 52.sp,
        lineHeight = 58.sp
    ),
    displayMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    displaySmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),

    // Headline
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),

    // Title
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),

    // Body (DM Sans)
    bodyLarge = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),

    // Label (JetBrains Mono for data)
    labelLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)

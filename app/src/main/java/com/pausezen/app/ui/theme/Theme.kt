package com.pausezen.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object PauseZenPalette {
    val Canvas = Color(0xFF111110)          // Warm obsidian ink
    val Surface = Color(0xFF191817)         // Warm elevated charcoal
    val SurfaceControl = Color(0xFF23211F)  // Tactile input / pill surface
    val Hairline = Color(0xFF2B2825)        // 1dp crisp divider

    val TextPrimary = Color(0xFFF3EFEA)     // Warm ivory
    val TextSecondary = Color(0xFFA39E96)   // Warm stone
    val TextMuted = Color(0xFF6E6A64)       // Quiet metadata

    val Accent = Color(0xFFE8764B)          // Living terracotta ember
    val AccentSoft = Color(0xFF2C1B14)      // Tinted accent container
    val OnAccent = Color(0xFF1A0D07)

    val ActiveGreen = Color(0xFF6FBF9C)     // Quiet semantic dot only
    val WarningAmber = Color(0xFFE6A957)
    val Danger = Color(0xFFE06C65)
}

private val WarmObsidianColorScheme = darkColorScheme(
    primary = PauseZenPalette.Accent,
    onPrimary = PauseZenPalette.OnAccent,
    primaryContainer = PauseZenPalette.AccentSoft,
    onPrimaryContainer = Color(0xFFFFD8C8),
    secondary = PauseZenPalette.TextSecondary,
    onSecondary = PauseZenPalette.Canvas,
    background = PauseZenPalette.Canvas,
    onBackground = PauseZenPalette.TextPrimary,
    surface = PauseZenPalette.Surface,
    onSurface = PauseZenPalette.TextPrimary,
    surfaceVariant = PauseZenPalette.SurfaceControl,
    onSurfaceVariant = PauseZenPalette.TextSecondary,
    outline = PauseZenPalette.Hairline,
    outlineVariant = PauseZenPalette.Hairline,
    error = PauseZenPalette.Danger
)

private val PauseZenTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        fontFeatureSettings = "tnum"
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 29.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontFeatureSettings = "tnum"
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum"
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontFeatureSettings = "tnum"
    )
)

@Composable
fun PauseZenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WarmObsidianColorScheme,
        typography = PauseZenTypography,
        content = content
    )
}

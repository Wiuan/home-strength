package com.homestrength.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Soft dopamine stage (refs: fitness archives + wellness cards):
 * neutral off-white canvas, white cards, purple CTA — color pops on accents only.
 */
object Palette {
    /** App canvas — cool off-white, almost neutral (not mint/lilac wash). */
    val Paper = Color(0xFFF5F6FA)
    val PaperDeep = Color(0xFFEEEFF4)
    /** Cards — pure white, like the reference mockups. */
    val Card = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1C1C1E)
    val InkSoft = Color(0xFF6B7280)
    /** Accents — soft purple CTA (legacy Gold* names kept for call sites). */
    val Gold = Color(0xFFA78BFA)
    val GoldDeep = Color(0xFF6C5CE7)
    val GoldSoft = Color(0xFFEDE9FE)
    val Line = Color(0xFFE5E7EB)
    val Caution = Color(0xFFFFB020)
    val Fatigue = Color(0xFFFF5A7A)

    val NightBg = Color(0xFF121216)
    val NightCard = Color(0xFF1C1C22)
    val NightInk = Color(0xFFF4F4F5)
    val NightInkSoft = Color(0xFFA1A1AA)
    val NightGold = Color(0xFFA78BFA)
}

private val LightColors = lightColorScheme(
    primary = Palette.GoldDeep,
    onPrimary = Color.White,
    secondary = Palette.Gold,
    tertiary = Palette.Fatigue,
    background = Palette.Paper,
    surface = Palette.Card,
    onBackground = Palette.Ink,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.PaperDeep,
    onSurfaceVariant = Palette.InkSoft,
    outline = Palette.Line,
    outlineVariant = Palette.Line
)

private val DarkColors = darkColorScheme(
    primary = Palette.NightGold,
    onPrimary = Color(0xFF1A1230),
    secondary = Palette.NightGold,
    tertiary = Color(0xFFFF8AD8),
    background = Palette.NightBg,
    surface = Palette.NightCard,
    onBackground = Palette.NightInk,
    onSurface = Palette.NightInk,
    surfaceVariant = Color(0xFF2A2A32),
    onSurfaceVariant = Palette.NightInkSoft,
    outline = Color(0xFF3F3F46),
    outlineVariant = Color(0xFF3F3F46)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.2.sp
    )
)

@Composable
fun HomeStrengthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}

object StatusColors {
    val complete = Palette.GoldDeep
    val caution = Palette.Caution
    val fatigue = Palette.Fatigue
    val completeDark = Palette.NightGold
}

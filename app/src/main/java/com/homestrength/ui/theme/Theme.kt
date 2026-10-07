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

/** Cream / navy / gold — closer to a gentle “trainee file” look than gym green. */
object Palette {
    val Paper = Color(0xFFF6F1E8)
    val PaperDeep = Color(0xFFEDE4D4)
    val Card = Color(0xFFFFFBF6)
    val Ink = Color(0xFF1B2A41)
    val InkSoft = Color(0xFF5C6B7A)
    val Gold = Color(0xFFC4A35A)
    val GoldDeep = Color(0xFFA4843A)
    val GoldSoft = Color(0xFFF3E6C8)
    val Line = Color(0xFFE4D8C4)
    val Caution = Color(0xFFC9A227)
    val Fatigue = Color(0xFFB85C4A)

    val NightBg = Color(0xFF16130F)
    val NightCard = Color(0xFF221E18)
    val NightInk = Color(0xFFF3EDE3)
    val NightInkSoft = Color(0xFFC4B8A6)
    val NightGold = Color(0xFFD4B56A)
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
    onPrimary = Color(0xFF2A210C),
    secondary = Palette.NightGold,
    tertiary = Color(0xFFE0A090),
    background = Palette.NightBg,
    surface = Palette.NightCard,
    onBackground = Palette.NightInk,
    onSurface = Palette.NightInk,
    surfaceVariant = Color(0xFF2C2720),
    onSurfaceVariant = Palette.NightInkSoft,
    outline = Color(0xFF3F392F),
    outlineVariant = Color(0xFF3F392F)
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
    val completeDark = Palette.Gold
}

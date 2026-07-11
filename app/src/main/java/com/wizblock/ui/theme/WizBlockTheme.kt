package com.wizblock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E6BFF),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF3A86FF),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF0F3DB8),
    background = Color(0xFFF4F8FF),
    onBackground = Color(0xFF1A2233),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D2432),
    surfaceVariant = Color(0xFFE3ECFF),
    onSurfaceVariant = Color(0xFF5D6678),
    outline = Color(0xFFB2BDCF),
    error = Color(0xFFFF3B30),
    onError = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4E92FF),
    onPrimary = Color(0xFF011B46),
    secondary = Color(0xFF5FA2FF),
    onSecondary = Color(0xFF082C62),
    tertiary = Color(0xFF8FB7FF),
    background = Color(0xFF09142D),
    onBackground = Color(0xFFE9EEFA),
    surface = Color(0xFF111F3E),
    onSurface = Color(0xFFE9EEFA),
    surfaceVariant = Color(0xFF1B2E57),
    onSurfaceVariant = Color(0xFFB8C5DB),
    outline = Color(0xFF64748F),
    error = Color(0xFFFF6C63),
    onError = Color(0xFF300301)
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
)

@Composable
fun WizBlockTheme(
    content: @Composable () -> Unit
) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

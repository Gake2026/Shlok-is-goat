package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SolanaDarkColorScheme = darkColorScheme(
    primary = SolanaPurple,
    secondary = SolanaGreen,
    tertiary = SolanaMagenta,
    background = SolanaDarkBg,
    surface = SolanaSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SolanaSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = PriceDown,
    onError = Color.White
)

private val SolanaLightColorScheme = lightColorScheme(
    primary = SolanaPurple,
    secondary = SolanaGreen,
    tertiary = SolanaMagenta,
    background = Color(0xFFF4F5F8),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFF101218),
    onSurface = Color(0xFF101218),
    surfaceVariant = Color(0xFFE8EAF0),
    onSurfaceVariant = Color(0xFF5A6072),
    error = PriceDown,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SolanaDarkColorScheme,
        typography = Typography,
        content = content
    )
}


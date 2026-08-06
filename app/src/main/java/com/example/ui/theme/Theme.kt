package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = HotelNavy,
    onPrimary = Color.White,
    primaryContainer = HotelNavyLight,
    onPrimaryContainer = Color.White,
    secondary = HotelGold,
    onSecondary = Color.White,
    secondaryContainer = HotelGoldLight,
    onSecondaryContainer = HotelNavy,
    background = SurfaceLight,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondary
)

private val DarkColorScheme = darkColorScheme(
    primary = HotelGold,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF334155),
    onPrimaryContainer = Color.White,
    secondary = HotelGoldLight,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color.White,
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

@Composable
fun HotelRiveraTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}



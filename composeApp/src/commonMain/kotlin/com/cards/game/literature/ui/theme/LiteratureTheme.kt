package com.cards.game.literature.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkGreen = Color(0xFF1B5E20)
val FeltGreen = Color(0xFF2E7D32)
val LightGreen = Color(0xFF4CAF50)
val CardRed = Color(0xFFD32F2F)
val GoldAccent = Color(0xFFFFD54F)
val DarkGold = Color(0xFFF9A825)
val SurfaceDark = Color(0xFF1A1A2E)
val SurfaceVariant = Color(0xFF16213E)
val OnSurfaceLight = Color(0xFFE0E0E0)

private val DarkColorScheme = darkColorScheme(
    primary = LightGreen,
    onPrimary = Color.White,
    primaryContainer = DarkGreen,
    onPrimaryContainer = Color.White,
    secondary = GoldAccent,
    onSecondary = Color.Black,
    secondaryContainer = DarkGold,
    onSecondaryContainer = Color.Black,
    tertiary = CardRed,
    onTertiary = Color.White,
    background = SurfaceDark,
    onBackground = OnSurfaceLight,
    surface = SurfaceVariant,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFF1E3A2F),
    onSurfaceVariant = Color(0xFFB0BEC5),
    error = Color(0xFFEF5350),
    onError = Color.White,
    outline = Color(0xFF4A6741)
)

@Composable
fun LiteratureTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

package com.mercedes.w205.obd.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AmgRed,
    onPrimary = AmgSilver,
    secondary = AmgCyan,
    background = AmgBlack,
    surface = AmgCardBg,
    onBackground = AmgSilver,
    onSurface = AmgSilver
)

@Composable
fun MercedesW205Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

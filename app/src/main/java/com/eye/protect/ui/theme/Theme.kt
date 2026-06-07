package com.eye.protect.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Green500,
    onPrimary = White,
    primaryContainer = Green200,
    secondary = Teal200,
    background = LightGray,
    surface = White,
    onBackground = DarkGray,
    onSurface = DarkGray
)

@Composable
fun EyeProtectTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

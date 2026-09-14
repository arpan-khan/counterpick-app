package com.counterpick.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF191308),
    secondary = Blue,
    background = Bg,
    onBackground = TextMain,
    surface = Surface,
    onSurface = TextMain,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextDim,
    outline = Border,
    error = Red
)

private val LightColors = lightColorScheme(
    primary = Gold,
    secondary = Blue,
    error = Red
)

@Composable
fun CounterPickTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = CounterPickTypography,
        content = content
    )
}

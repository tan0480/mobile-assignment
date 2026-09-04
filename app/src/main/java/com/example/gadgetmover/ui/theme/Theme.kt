package com.example.gadgetmover.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    secondary = BrandOrange,
    onSecondary = Color.White,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = CardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFEDEFF3),
    onSurfaceVariant = TextSecondaryLight,
    error = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7DA6E8),
    onPrimary = BrandBlueDark,
    secondary = Color(0xFFFFB066),
    onSecondary = BrandOrangeDark,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = CardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF363940),
    onSurfaceVariant = TextSecondaryDark,
    error = Color(0xFFEF9A9A)
)

@Composable
fun GadgetMoverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = GadgetMoverTypography,
        content = content
    )
}

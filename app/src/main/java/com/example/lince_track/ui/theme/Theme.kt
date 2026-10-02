package com.example.lince_track.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TealDark,
    onPrimary = NavyBlueDark,
    primaryContainer = NavyBlue,
    onPrimaryContainer = TextDark,
    secondary = TealDark,
    onSecondary = NavyBlueDark,
    background = NavyBlueDark,
    onBackground = TextDark,
    surface = NavyBlue,
    onSurface = TextDark,
    surfaceVariant = NavyBlue,
    onSurfaceVariant = TextDark
)

private val LightColorScheme = lightColorScheme(
    primary = NavyBlue,
    onPrimary = OnNavyBlue,
    primaryContainer = NavyBlueLight,
    onPrimaryContainer = NavyBlueDark,
    secondary = SubtitleTeal,
    onSecondary = OnTeal,
    secondaryContainer = TealLight,
    onSecondaryContainer = NavyBlueDark,
    background = BackgroundColor,
    onBackground = TextLight,
    surface = OnNavyBlue,
    onSurface = TextLight,
    surfaceVariant = NavyBlueLight,
    onSurfaceVariant = NavyBlueDark
)

@Composable
fun Lince_TrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

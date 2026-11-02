package com.example.lince_track.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

private val LightColorScheme = lightColorScheme(
    primary = NavyBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = NavyContainer,
    onPrimaryContainer = OnNavyContainer,
    secondary = TealGreen,
    onSecondary = SurfaceWhite,
    secondaryContainer = TealContainer,
    onSecondaryContainer = OnTealContainer,
    tertiary = GoldenOrange,
    onTertiary = SurfaceWhite,
    tertiaryContainer = OrangeContainer,
    onTertiaryContainer = OnOrangeContainer,
    error = ErrorRed,
    onError = SurfaceWhite,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = BorderLine,
    onSurfaceVariant = TextSecondary,
    outline = BorderLine,
    outlineVariant = BorderLine,
    inverseSurface = NavyBlue,
    inverseOnSurface = SurfaceWhite,
    inversePrimary = TealGreen
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Colores de la barra superior de las pantallas y del fondo de la barra de estado. En modo oscuro
 * `primary` es un azul claro pensado para botones, por eso la barra tiene colores propios.
 */
@Immutable
private data class ColoresBarras(val fondo: Color, val contenido: Color, val estado: Color)

private val LocalColoresBarras = staticCompositionLocalOf {
    ColoresBarras(fondo = NavyBlue, contenido = SurfaceWhite, estado = NavyBlue)
}

object BarraSuperior {
    val fondo: Color
        @Composable get() = LocalColoresBarras.current.fondo
    val contenido: Color
        @Composable get() = LocalColoresBarras.current.contenido
    val estado: Color
        @Composable get() = LocalColoresBarras.current.estado
}

@Composable
fun Lince_TrackTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val coloresBarras = if (darkTheme) {
        // Se funde con el fondo de la pantalla, sin cambio de color.
        ColoresBarras(fondo = colorScheme.background, contenido = colorScheme.onBackground, estado = colorScheme.background)
    } else {
        // La barra de estado sigue a la superior: una sola franja azul, con íconos blancos legibles.
        ColoresBarras(fondo = colorScheme.primary, contenido = colorScheme.onPrimary, estado = colorScheme.primary)
    }

    CompositionLocalProvider(LocalColoresBarras provides coloresBarras) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}

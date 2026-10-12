package com.parswipe.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Paleta "Modern Black AMOLED": negro puro, alto contraste, minimalista. */
object ParColors {
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF0A0A0A)
    val SurfaceVariant = Color(0xFF151515)
    val Outline = Color(0xFF282828)
    val TextPrimary = Color(0xFFF4F4F4)
    val TextSecondary = Color(0xFF7E7E7E)
    val Keep = Color(0xFF2FD07A)
    val Delete = Color(0xFFFF4A3D)
    val Accent = Color(0xFFFFFFFF)
}

private val ParColorScheme = darkColorScheme(
    primary = ParColors.Accent,
    onPrimary = ParColors.Black,
    primaryContainer = ParColors.SurfaceVariant,
    onPrimaryContainer = ParColors.TextPrimary,
    secondary = ParColors.TextSecondary,
    onSecondary = ParColors.Black,
    background = ParColors.Black,
    onBackground = ParColors.TextPrimary,
    surface = ParColors.Black,
    onSurface = ParColors.TextPrimary,
    surfaceVariant = ParColors.SurfaceVariant,
    onSurfaceVariant = ParColors.TextSecondary,
    outline = ParColors.Outline,
    outlineVariant = ParColors.Outline,
    error = ParColors.Delete,
    onError = ParColors.Black,
)

@Composable
fun ParSwipeTheme(content: @Composable () -> Unit) {
    // La app es siempre AMOLED: no se usa el tema del sistema.
    @Suppress("UNUSED_EXPRESSION")
    isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = ParColorScheme,
        typography = Typography(),
        content = content,
    )
}

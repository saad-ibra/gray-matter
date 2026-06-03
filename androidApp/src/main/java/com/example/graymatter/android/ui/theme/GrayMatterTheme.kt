package com.example.graymatter.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Dark color scheme matching the Gray Matter design.
 */
private val DarkColorScheme = darkColorScheme(
    primary = GrayMatterColors.Primary,
    onPrimary = GrayMatterColors.OnPrimary,
    primaryContainer = GrayMatterColors.SurfaceDark,
    onPrimaryContainer = GrayMatterColors.TextPrimary,
    
    secondary = GrayMatterColors.Neutral400,
    onSecondary = GrayMatterColors.OnPrimary,
    secondaryContainer = GrayMatterColors.Neutral800,
    onSecondaryContainer = GrayMatterColors.TextPrimary,
    
    tertiary = GrayMatterColors.Neutral500,
    onTertiary = GrayMatterColors.OnPrimary,
    
    background = GrayMatterColors.BackgroundDark,
    onBackground = GrayMatterColors.TextPrimary,
    
    surface = GrayMatterColors.SurfaceDark,
    onSurface = GrayMatterColors.TextPrimary,
    surfaceVariant = GrayMatterColors.SurfaceInput,
    onSurfaceVariant = GrayMatterColors.TextSecondary,
    
    outline = GrayMatterColors.SurfaceBorder,
    outlineVariant = GrayMatterColors.Neutral800,
    
    error = GrayMatterColors.Error,
    onError = GrayMatterColors.TextPrimary,
    
    inverseSurface = GrayMatterColors.Neutral100,
    inverseOnSurface = GrayMatterColors.Neutral900,
    inversePrimary = GrayMatterColors.Neutral900
)

/**
 * Light color scheme (optional, app defaults to dark).
 */
private val LightColorScheme = lightColorScheme(
    primary = GrayMatterColors.OnPrimary,
    onPrimary = GrayMatterColors.Primary,
    primaryContainer = GrayMatterColors.Neutral100,
    onPrimaryContainer = GrayMatterColors.Neutral900,
    
    secondary = GrayMatterColors.Neutral600,
    onSecondary = GrayMatterColors.Primary,
    secondaryContainer = GrayMatterColors.Neutral200,
    onSecondaryContainer = GrayMatterColors.Neutral900,
    
    background = GrayMatterColors.BackgroundLight,
    onBackground = GrayMatterColors.Neutral900,
    
    surface = GrayMatterColors.Primary,
    onSurface = GrayMatterColors.Neutral900,
    surfaceVariant = GrayMatterColors.Neutral100,
    onSurfaceVariant = GrayMatterColors.Neutral600,
    
    outline = GrayMatterColors.Neutral300,
    outlineVariant = GrayMatterColors.Neutral200,
    
    error = GrayMatterColors.Error,
    onError = GrayMatterColors.Primary
)

object GrayMatterTheme {
    val colors: GrayMatterThemeColors
        @Composable
        get() = LocalGrayMatterThemeColors.current
}

/**
 * Gray Matter app theme.
 */
@Composable
fun GrayMatterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val gmColors = if (darkTheme) darkGrayMatterColors() else lightGrayMatterColors()
    
    CompositionLocalProvider(
        LocalGrayMatterThemeColors provides gmColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GrayMatterTypography,
            content = content
        )
    }
}

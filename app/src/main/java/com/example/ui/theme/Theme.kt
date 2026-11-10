package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LopPrimaryDark,
    onPrimary = LopOnPrimaryDark,
    primaryContainer = LopPrimaryContainerDark,
    onPrimaryContainer = LopOnPrimaryContainerDark,
    secondary = LopSecondaryDark,
    onSecondary = LopOnSecondaryDark,
    secondaryContainer = LopSecondaryContainerDark,
    onSecondaryContainer = LopOnSecondaryContainerDark,
    tertiary = LopTertiaryDark,
    onTertiary = LopOnTertiaryDark,
    tertiaryContainer = LopTertiaryContainerDark,
    onTertiaryContainer = LopOnTertiaryContainerDark,
    background = LopBackgroundDark,
    onBackground = LopOnBackgroundDark,
    surface = LopSurfaceDark,
    onSurface = LopOnSurfaceDark,
    surfaceVariant = LopSurfaceVariantDark,
    onSurfaceVariant = LopOnSurfaceVariantDark,
    outline = LopOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = LopPrimary,
    onPrimary = LopOnPrimary,
    primaryContainer = LopPrimaryContainer,
    onPrimaryContainer = LopOnPrimaryContainer,
    secondary = LopSecondary,
    onSecondary = LopOnSecondary,
    secondaryContainer = LopSecondaryContainer,
    onSecondaryContainer = LopOnSecondaryContainer,
    tertiary = LopTertiary,
    onTertiary = LopOnTertiary,
    tertiaryContainer = LopTertiaryContainer,
    onTertiaryContainer = LopOnTertiaryContainer,
    background = LopBackground,
    onBackground = LopOnBackground,
    surface = LopSurface,
    onSurface = LopOnSurface,
    surfaceVariant = LopSurfaceVariant,
    onSurfaceVariant = LopOnSurfaceVariant,
    outline = LopOutline
)

@Composable
fun LopBrowserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep branded emerald/cyan palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

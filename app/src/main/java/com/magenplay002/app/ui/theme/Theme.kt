package com.magenplay002.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AnimeBlue,
    onPrimary = AnimeDarkBg,
    primaryContainer = AnimeBlueDark,
    onPrimaryContainer = AnimeBlueLight,
    secondary = AnimePink,
    onSecondary = AnimeDarkBg,
    secondaryContainer = AnimePinkDark,
    onSecondaryContainer = AnimePinkLight,
    tertiary = AnimePurple,
    onTertiary = AnimeDarkBg,
    tertiaryContainer = AnimePurpleDark,
    onTertiaryContainer = AnimePurpleLight,
    background = AnimeDarkBg,
    onBackground = AnimeTextPrimary,
    surface = AnimeDarkSurface,
    onSurface = AnimeTextPrimary,
    surfaceVariant = AnimeDarkCard,
    onSurfaceVariant = AnimeTextSecondary,
    outline = AnimeTextTertiary,
    error = AnimeError,
    onError = AnimeTextPrimary,
    inversePrimary = AnimeOrange,
    inverseSurface = AnimeLightSurface,
    inverseOnSurface = AnimeDarkBg,
)

private val LightColorScheme = lightColorScheme(
    primary = AnimeBlueDark,
    onPrimary = AnimeTextPrimary,
    primaryContainer = AnimeBlueLight,
    onPrimaryContainer = AnimeBlueDark,
    secondary = AnimePinkDark,
    onSecondary = AnimeTextPrimary,
    secondaryContainer = AnimePinkLight,
    onSecondaryContainer = AnimePinkDark,
    tertiary = AnimePurpleDark,
    onTertiary = AnimeTextPrimary,
    tertiaryContainer = AnimePurpleLight,
    onTertiaryContainer = AnimePurpleDark,
    background = AnimeLightBg,
    onBackground = AnimeDarkBg,
    surface = AnimeLightSurface,
    onSurface = AnimeDarkBg,
    surfaceVariant = AnimeLightCard,
    onSurfaceVariant = AnimeTextSecondary,
    outline = AnimeTextTertiary,
    error = AnimeError,
    onError = AnimeTextPrimary,
)

@Composable
fun MagenPlay002Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                androidx.compose.material3.dynamicDarkColorScheme(context)
            } else {
                androidx.compose.material3.dynamicLightColorScheme(context)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MagenPlayTypography,
        content = content
    )
}

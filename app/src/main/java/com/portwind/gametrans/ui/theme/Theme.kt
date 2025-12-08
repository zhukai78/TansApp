package com.portwind.gametrans.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AnimePrimaryDark,
    secondary = AnimeSecondaryDark,
    tertiary = AnimeTertiaryDark,
    background = AnimeBackgroundDark,
    surface = AnimeSurfaceDark,
    onPrimary = AnimeOnPrimaryDark,
    onSecondary = AnimeOnSecondaryDark,
    onTertiary = AnimeOnPrimaryDark,
    onBackground = AnimeOnBackgroundDark,
    onSurface = AnimeOnSurfaceDark,
)

private val LightColorScheme = lightColorScheme(
    primary = AnimePrimary,
    secondary = AnimeSecondary,
    tertiary = AnimeTertiary,
    background = AnimeBackground,
    surface = AnimeSurface,
    onPrimary = AnimeOnPrimary,
    onSecondary = AnimeOnSecondary,
    onTertiary = AnimeOnPrimary,
    onBackground = AnimeOnBackground,
    onSurface = AnimeOnSurface,
)

@Composable
fun GameTransTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic color to enforce Anime theme
    dynamicColor: Boolean = false,
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
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
    primary = TranslatePrimary80,
    secondary = TranslateSecondary80,
    tertiary = TranslateTertiary80,
    background = TranslateSurfaceDark,
    surface = TranslateSurfaceDark,
    surfaceVariant = TranslateSurfaceVariantDark,
    error = TranslateError80,
    onPrimary = Color(0xFF0C4A6E),
    onSecondary = Color(0xFF0E7490),
    onTertiary = Color(0xFF065F46),
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFFCBD5E1),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = TranslatePrimary40,
    secondary = TranslateSecondary40,
    tertiary = TranslateTertiary40,
    background = TranslateSurface,
    surface = TranslateSurface,
    surfaceVariant = TranslateSurfaceVariant,
    error = TranslateError40,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF475569),
    onError = Color.White
)

@Composable
fun GameTransTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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
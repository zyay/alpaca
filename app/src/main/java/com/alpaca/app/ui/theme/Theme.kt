package com.alpaca.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Core neon palette stays fixed; Material You only nudges surface tones on S+.
private val LightColors = lightColorScheme(
    primary = PacoGreen,
    onPrimary = PaperWhite,
    primaryContainer = PacoGreenLight,
    secondary = SunYellow,
    tertiary = SkyBlue,
    error = DangerRed,
    background = MistGray,
    surface = PaperWhite,
    surfaceVariant = CloudGray,
    onBackground = InkDark,
    onSurface = InkDark,
    onSurfaceVariant = InkMid
)

private val DarkColors = darkColorScheme(
    primary = PacoGreen,
    onPrimary = PaperWhite,
    primaryContainer = PacoGreenDark,
    secondary = SunYellow,
    tertiary = SkyBlue,
    error = DangerRed
)

@Composable
fun AlpacaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AlpacaTypography,
        shapes = AlpacaShapes,
        content = content
    )
}

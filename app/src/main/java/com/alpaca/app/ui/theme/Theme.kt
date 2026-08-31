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

// Brand palette is the default so the app reads like its inspiration;
// Material You is an opt-in toggle that nudges surface tones on S+.
private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = PaperWhite,
    primaryContainer = BrandGreenPale,
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
    primary = BrandGreen,
    onPrimary = PaperWhite,
    primaryContainer = BrandGreenDeep,
    secondary = SunYellow,
    tertiary = SkyBlue,
    error = DangerRed
)

@Composable
fun AlpacaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

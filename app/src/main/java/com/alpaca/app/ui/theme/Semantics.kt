package com.alpaca.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable

// Semantic aliases over the Material scheme so screens adapt to dark mode
// automatically: LightColors maps them to the original hardcoded palette.

/** Card / sheet background (PaperWhite in light, NightSurface in dark). */
@Composable
fun alpacaCard(): Color = MaterialTheme.colorScheme.surface

/** Card outline and progress-bar tracks (CloudGray in light, NightSurfaceHigh in dark). */
@Composable
fun alpacaCardBorder(): Color = MaterialTheme.colorScheme.surfaceVariant

/** Secondary text (InkMid in light, NightTextMid in dark). */
@Composable
fun alpacaSecondaryText(): Color = MaterialTheme.colorScheme.onSurfaceVariant

/** De-emphasized text (InkFaint in light, muted NightTextMid in dark). */
@Composable
fun alpacaFaintText(): Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

/** Green highlight tint behind banner/you-row/badges (pale in light, translucent in dark). */
@Composable
fun alpacaGreenTint(): Color = MaterialTheme.colorScheme.primaryContainer

package com.alpaca.app.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import com.alpaca.app.ui.theme.BrandGreen
import com.alpaca.app.ui.theme.CtaGradientTop
import kotlin.math.floor

private val greetings = listOf("¡Hola!", "Bonjour!", "Hallo!")

/** Duolingo-style unlock easing: back-out overshoot. */
val UnlockEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/**
 * Rotating language greetings with a green-gradient fill and a spring pop on
 * each swap — the mascot-free app hero.
 */
@Composable
fun GreetingWordmark(
    modifier: Modifier = Modifier,
    style: TextStyle,
    cycleMillis: Int = 2400
) {
    val transition = rememberInfiniteTransition(label = "greeting")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = greetings.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cycleMillis * greetings.size, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "greeting-cycle"
    )
    val index = floor(t).toInt().coerceIn(greetings.indices)
    val frac = t - floor(t)

    val popT = (frac / 0.3f).coerceAtMost(1f)
    val scale = 0.92f + 0.08f * UnlockEasing.transform(popT)
    val alpha = when {
        frac < 0.08f -> frac / 0.08f
        frac > 0.9f -> (1f - frac) / 0.1f
        else -> 1f
    }

    Text(
        text = greetings[index],
        style = style.copy(
            brush = Brush.horizontalGradient(listOf(CtaGradientTop, BrandGreen))
        ),
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    )
}

package com.alpaca.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/** Text whose number counts up to [target] on first show and eases to new values after. */
@Composable
fun CountUpText(
    target: Int,
    modifier: Modifier = Modifier,
    prefix: String = "",
    suffix: String = "",
    startFromZero: Boolean = false,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = Color.Unspecified
) {
    val anim = remember { Animatable(if (startFromZero) 0f else target.toFloat()) }
    LaunchedEffect(target) {
        if (target == anim.value.roundToInt()) return@LaunchedEffect
        anim.animateTo(target.toFloat(), tween(700, easing = FastOutSlowInEasing))
    }
    Text(
        text = prefix + anim.value.roundToInt().toString() + suffix,
        style = style,
        color = color,
        modifier = modifier
    )
}

/** Fades + slides [content] in on first composition, staggered by [index] (45 ms per step). */
@Composable
fun Entrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(8) * 45L)
        anim.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = anim.value
            translationY = (1f - anim.value) * 40f
        }
    ) { content() }
}

/** Spring-pops [content] whenever [value] changes (skips the initial composition). */
@Composable
fun PopOnChange(
    value: Any?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scale = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(value) {
        if (first) {
            first = false
            return@LaunchedEffect
        }
        scale.snapTo(1.22f)
        scale.animateTo(
            1f,
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }
    Box(modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }) { content() }
}

/** Slow diagonal gloss sweep; apply to fully-drawn content like unlocked badge cards. */
@Composable
fun Modifier.badgeShine(): Modifier {
    val transition = rememberInfiniteTransition(label = "shine")
    val x by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "shine-x"
    )
    return drawWithContent {
        drawContent()
        val w = size.width
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.35f),
                    Color.Transparent
                ),
                start = Offset(w * (x - 0.45f), 0f),
                end = Offset(w * (x + 0.45f), size.height)
            )
        )
    }
}

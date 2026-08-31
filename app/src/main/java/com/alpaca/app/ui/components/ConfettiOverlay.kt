package com.alpaca.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.alpaca.app.ui.theme.HeartPink
import com.alpaca.app.ui.theme.BrandGreen
import com.alpaca.app.ui.theme.SkyBlue
import com.alpaca.app.ui.theme.SunYellow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val ConfettiPalette = listOf(BrandGreen, SunYellow, SkyBlue, HeartPink, Color(0xFFFF8A3D))

private class Particle(
    val x0: Float,
    val y0: Float,
    val vx: Float, // width-fractions per second
    val vy: Float, // height-fractions per second (negative = upward)
    val spin: Float,
    val phase: Float,
    val color: Color,
    val size: Float
)

/** Full-screen confetti burst while [active] is true. Deterministic physics from t. */
@Composable
fun ConfettiOverlay(active: Boolean, modifier: Modifier = Modifier) {
    val particles = remember {
        var seed = 42L
        fun rnd(): Float {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            return seed / 0x7fffffff.toFloat()
        }
        List(80) {
            Particle(
                x0 = 0.15f + rnd() * 0.7f,
                y0 = 0.25f + rnd() * 0.15f,
                vx = (rnd() - 0.5f) * 0.55f,
                vy = -(0.35f + rnd() * 0.5f),
                spin = (rnd() - 0.5f) * 14f,
                phase = rnd() * 2f * PI.toFloat(),
                color = ConfettiPalette[(rnd() * ConfettiPalette.size).toInt().coerceAtMost(ConfettiPalette.size - 1)],
                size = 0.012f + rnd() * 0.012f
            )
        }
    }
    if (!active) return
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        t.snapTo(0f)
        t.animateTo(1f, animationSpec = tween(2400, easing = LinearEasing))
    }
    Canvas(modifier = modifier) {
        val time = t.value
        if (time >= 1f) return@Canvas
        val fade = if (time > 0.75f) (1f - time) / 0.25f else 1f
        particles.forEach { p ->
            val x = (p.x0 + p.vx * time) * size.width
            val gravity = 2.2f
            val y = (p.y0 + p.vy * time + 0.5f * gravity * time * time) * size.height
            if (y > size.height * 1.05f) return@forEach
            drawConfetto(x, y, p, time, fade)
        }
    }
}

private fun DrawScope.drawConfetto(x: Float, y: Float, p: Particle, time: Float, fade: Float) {
    val s = p.size * size.width
    val flutter = cos(p.phase + time * 18f) * 0.5f + 0.5f // simulates tumbling
    drawContext.canvas.save()
    drawContext.canvas.translate(x, y)
    drawContext.canvas.rotate(p.spin * time * 60f + sin(p.phase) * 40f)
    drawRect(
        color = p.color.copy(alpha = fade),
        topLeft = Offset(-s / 2f, -s * (0.25f + flutter * 0.75f) / 2f),
        size = Size(s, s * (0.25f + flutter * 0.75f))
    )
    drawContext.canvas.restore()
}

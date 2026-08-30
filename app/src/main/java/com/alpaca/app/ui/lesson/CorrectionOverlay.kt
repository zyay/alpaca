package com.alpaca.app.ui.lesson

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alpaca.app.ui.components.PacoCharacter
import com.alpaca.app.ui.components.PacoState
import com.alpaca.app.ui.components.PillButton
import com.alpaca.app.ui.theme.InkMid
import com.alpaca.app.ui.theme.PacoGreen
import com.alpaca.app.ui.theme.PacoGreenLight
import com.alpaca.app.ui.theme.SkyBlue

/**
 * The "spit-take": Paco spits a droplet of water at the screen; the splash
 * clears to reveal the correct answer with a one-line explanation.
 */
@Composable
fun CorrectionOverlay(
    correction: LessonViewModel.Correction,
    onContinue: () -> Unit
) {
    val dropletT = remember { Animatable(0f) }
    var showCard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        dropletT.animateTo(1f, animationSpec = tween(750, easing = LinearEasing))
        showCard = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Box(contentAlignment = Alignment.Center) {
                PacoCharacter(
                    state = PacoState.SPIT_TAKE,
                    modifier = Modifier.size(190.dp)
                )
                WaterDroplets(
                    progress = dropletT.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(
                visible = showCard,
                enter = slideInVertically(initialOffsetY = { it / 3 }) + fadeIn(tween(250))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Correct answer",
                        style = MaterialTheme.typography.labelLarge,
                        color = InkMid
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PacoGreenLight)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = correction.correctText,
                            style = MaterialTheme.typography.headlineMedium,
                            color = PacoGreen,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    correction.explanation?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    PillButton(
                        text = "Continue",
                        onClick = onContinue,
                        color = SkyBlue
                    )
                }
            }
        }
    }
}

/** Water droplets flying out of Paco's mouth toward the viewer. */
@Composable
private fun WaterDroplets(progress: Float, modifier: Modifier = Modifier) {
    val drops = remember {
        var seed = 7L
        fun rnd(): Float {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            return seed / 0x7fffffff.toFloat()
        }
        List(14) {
            floatArrayOf(
                0.55f + rnd() * 0.08f,   // origin x fraction
                0.28f + rnd() * 0.08f,   // origin y fraction
                0.25f + rnd() * 0.45f,   // vx
                -0.18f + rnd() * 0.22f,  // vy initial (up-ish)
                0.5f + rnd() * 0.5f      // size scale
            )
        }
    }
    Canvas(modifier = modifier) {
        if (progress <= 0f) return@Canvas
        val alpha = (1f - progress).coerceAtLeast(0f)
        drops.forEach { d ->
            val t = progress
            val x = (d[0] + d[2] * t) * size.width
            val y = (d[1] + d[3] * t + 0.9f * t * t) * size.height
            val r = (4f + d[4] * 7f) * (1f + t * 0.6f)
            drawCircle(SkyBlue.copy(alpha = alpha * 0.9f), r, center = Offset(x, y))
            drawCircle(
                Color.White.copy(alpha = alpha * 0.7f),
                r * 0.35f,
                center = Offset(x - r * 0.3f, y - r * 0.35f)
            )
        }
    }
}

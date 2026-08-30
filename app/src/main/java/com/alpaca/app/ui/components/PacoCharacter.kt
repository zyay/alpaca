package com.alpaca.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.alpaca.app.ui.theme.InkDark
import com.alpaca.app.ui.theme.PacoGreen
import com.alpaca.app.ui.theme.PacoGreenDark
import com.alpaca.app.ui.theme.PacoGreenLight
import com.alpaca.app.ui.theme.SunYellow
import com.alpaca.app.ui.theme.SunYellowDark

enum class PacoState { IDLE, HAPPY, SPIT_TAKE, SAD }

/**
 * Paco the alpaca, drawn entirely in Canvas — no image assets. Neon-green fluff,
 * tiny backward cap, four moods.
 */
@Composable
fun PacoCharacter(
    state: PacoState,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "paco")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "bob"
    )

    val hop = remember { Animatable(0f) }
    LaunchedEffect(state) {
        if (state == PacoState.HAPPY) {
            hop.snapTo(0f)
            hop.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 750
                    0f at 0
                    1f at 280
                    0.15f at 560
                    0.45f at 650
                    0f at 750
                }
            )
        } else {
            hop.snapTo(0f)
        }
    }

    val headTilt by animateFloatAsState(
        when (state) {
            PacoState.IDLE -> 0f
            PacoState.HAPPY -> -6f
            PacoState.SPIT_TAKE -> -22f
            PacoState.SAD -> 14f
        },
        animationSpec = spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "tilt"
    )
    val mouthOpen by animateFloatAsState(
        if (state == PacoState.SPIT_TAKE) 1f else 0f,
        animationSpec = tween(140),
        label = "mouth"
    )
    val droop by animateFloatAsState(
        if (state == PacoState.SAD) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "droop"
    )
    val eyeOpen by animateFloatAsState(
        if (state == PacoState.SAD) 0.5f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "eyes"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bounce = when (state) {
            PacoState.IDLE -> -bob * h * 0.02f
            PacoState.HAPPY -> -hop.value * h * 0.16f
            else -> 0f
        }
        val stretch = when (state) {
            PacoState.HAPPY -> hop.value * 0.06f
            else -> 0f
        }
        drawPaco(w, h, bounce, stretch, headTilt, mouthOpen, droop, eyeOpen)
    }
}

@Suppress("LongParameterList")
private fun DrawScope.drawPaco(
    w: Float,
    h: Float,
    bounce: Float,
    stretch: Float,
    headTilt: Float,
    mouthOpen: Float,
    droop: Float,
    eyeOpen: Float
) {
    val groundY = h * 0.94f

    drawContext.canvas.save()
    // Squash & stretch about the ground pivot.
    drawContext.canvas.translate(w / 2f, groundY)
    drawContext.canvas.scale(1f - stretch * 0.6f, 1f + stretch)
    drawContext.canvas.translate(-w / 2f, -groundY + bounce)

    // Legs (slightly darker for depth).
    val legW = w * 0.052f
    val legTop = h * 0.76f
    for (fx in listOf(0.34f, 0.44f, 0.56f, 0.66f)) {
        drawRoundRect(
            color = PacoGreenDark,
            topLeft = Offset(w * fx - legW / 2f, legTop),
            size = Size(legW, groundY - legTop),
            cornerRadius = CornerRadius(legW / 2f)
        )
    }

    // Tail tuft.
    drawCircle(PacoGreenLight, w * 0.05f, center = Offset(w * 0.255f, h * 0.66f))

    // Body: main blob + fluff bumps along the top.
    val bodyCenter = Offset(w * 0.48f, h * 0.70f)
    drawOval(
        PacoGreen,
        topLeft = Offset(bodyCenter.x - w * 0.245f, bodyCenter.y - h * 0.145f),
        size = Size(w * 0.49f, h * 0.29f)
    )
    for ((fx, fy, fr) in listOf(
        Triple(0.33f, 0.60f, 0.052f),
        Triple(0.42f, 0.575f, 0.058f),
        Triple(0.52f, 0.57f, 0.06f),
        Triple(0.61f, 0.59f, 0.054f)
    )) {
        drawCircle(PacoGreen, w * fr, center = Offset(w * fx, h * fy))
    }
    // Belly highlight.
    drawOval(
        PacoGreenLight.copy(alpha = 0.55f),
        topLeft = Offset(bodyCenter.x - w * 0.15f, bodyCenter.y - h * 0.02f),
        size = Size(w * 0.3f, h * 0.15f)
    )

    // Neck.
    drawRoundRect(
        color = PacoGreen,
        topLeft = Offset(w * 0.60f, h * 0.28f),
        size = Size(w * 0.125f, h * 0.42f),
        cornerRadius = CornerRadius(w * 0.06f)
    )

    // ---- Head group (rotates around the top of the neck) ----
    val pivot = Offset(w * 0.665f, h * 0.32f)
    drawContext.canvas.save()
    drawContext.canvas.translate(pivot.x, pivot.y)
    drawContext.canvas.rotate(headTilt)
    drawContext.canvas.translate(-pivot.x, -pivot.y)

    // Ears (drawn before the cap so the cap overlaps their bases).
    val earDroopDeg = droop * 38f
    for ((earFx, dir) in listOf(0.622f to -1f, 0.706f to 1f)) {
        drawContext.canvas.save()
        drawContext.canvas.translate(w * earFx, h * 0.175f)
        drawContext.canvas.rotate(dir * (-14f + earDroopDeg))
        drawOval(
            PacoGreen,
            topLeft = Offset(-w * 0.023f, -h * 0.095f),
            size = Size(w * 0.046f, h * 0.1f)
        )
        drawOval(
            PacoGreenDark.copy(alpha = 0.7f),
            topLeft = Offset(-w * 0.011f, -h * 0.078f),
            size = Size(w * 0.022f, h * 0.06f)
        )
        drawContext.canvas.restore()
    }

    // Head.
    drawOval(
        PacoGreen,
        topLeft = Offset(w * 0.665f - w * 0.105f, h * 0.195f - h * 0.085f),
        size = Size(w * 0.21f, h * 0.17f)
    )
    // Muzzle.
    drawOval(
        PacoGreenLight,
        topLeft = Offset(w * 0.745f - w * 0.05f, h * 0.235f - h * 0.042f),
        size = Size(w * 0.1f, h * 0.084f)
    )

    // Backward cap: dome over the skull, brim pointing back (left).
    drawOval(
        SunYellow,
        topLeft = Offset(w * 0.665f - w * 0.088f, h * 0.128f - h * 0.042f),
        size = Size(w * 0.176f, h * 0.105f)
    )
    drawRoundRect(
        color = SunYellowDark,
        topLeft = Offset(w * 0.545f, h * 0.115f),
        size = Size(w * 0.085f, h * 0.028f),
        cornerRadius = CornerRadius(h * 0.014f)
    )
    // Cap button.
    drawCircle(SunYellowDark, w * 0.014f, center = Offset(w * 0.66f, h * 0.096f))

    // Eyes.
    val eyeR = w * 0.016f
    for (ex in listOf(0.645f, 0.708f)) {
        val eyeCenter = Offset(w * ex, h * 0.185f)
        drawOval(
            InkDark,
            topLeft = Offset(eyeCenter.x - eyeR, eyeCenter.y - eyeR * eyeOpen),
            size = Size(eyeR * 2f, eyeR * 2f * eyeOpen)
        )
        if (eyeOpen > 0.7f) {
            drawCircle(
                Color.White,
                eyeR * 0.35f,
                center = eyeCenter + Offset(-eyeR * 0.3f, -eyeR * 0.35f)
            )
        }
    }
    // Blush.
    drawCircle(Color(0xFFFF9EB5).copy(alpha = 0.6f), w * 0.02f, center = Offset(w * 0.63f, h * 0.215f))

    // Nose + mouth.
    drawOval(
        PacoGreenDark,
        topLeft = Offset(w * 0.755f - w * 0.012f, h * 0.222f - h * 0.008f),
        size = Size(w * 0.024f, h * 0.016f)
    )
    if (mouthOpen > 0.05f) {
        drawOval(
            InkDark,
            topLeft = Offset(w * 0.752f - w * 0.02f, h * 0.242f),
            size = Size(w * 0.04f, h * 0.034f * mouthOpen)
        )
    } else {
        drawOval(
            PacoGreenDark,
            topLeft = Offset(w * 0.748f, h * 0.248f),
            size = Size(w * 0.026f, h * 0.008f)
        )
    }

    drawContext.canvas.restore() // head group
    drawContext.canvas.restore() // squash & stretch
}

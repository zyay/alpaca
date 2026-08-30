package com.alpaca.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.alpaca.app.ui.theme.CloudGray
import com.alpaca.app.ui.theme.HeartPink

/**
 * Row of fleece tufts (lives). A lost tuft shrinks to a gray puff with a spring pop.
 */
@Composable
fun EnergyHearts(
    energy: Int,
    modifier: Modifier = Modifier,
    max: Int = 5
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(max) { index ->
            val filled = index < energy
            val scale by animateFloatAsState(
                if (filled) 1f else 0.82f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioHighBouncy
                ),
                label = "fleece-$index"
            )
            val tuftColor = if (filled) HeartPink else CloudGray
            Canvas(modifier = Modifier.size(26.dp)) {
                val s = size.minDimension * scale
                val cx = size.width / 2f
                val cy = size.height / 2f
                drawFleeceTuft(Offset(cx, cy), s / 2f, tuftColor)
            }
        }
    }
}

private fun DrawScope.drawFleeceTuft(center: Offset, radius: Float, color: Color) {
    // Fluffy cloud: overlapping circles.
    drawCircle(color, radius * 0.72f, center = center + Offset(-radius * 0.45f, radius * 0.1f))
    drawCircle(color, radius * 0.78f, center = center + Offset(0f, -radius * 0.25f))
    drawCircle(color, radius * 0.72f, center = center + Offset(radius * 0.45f, radius * 0.1f))
    drawCircle(color, radius * 0.85f, center = center + Offset(0f, radius * 0.18f))
}

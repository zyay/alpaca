package com.alpaca.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alpaca.app.ui.theme.CloudGray
import com.alpaca.app.ui.theme.SkyBlue

@Composable
fun LessonProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = SkyBlue
) {
    val animated by animateFloatAsState(
        fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 320),
        label = "progress"
    )
    val shape = RoundedCornerShape(100.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(shape)
            .background(CloudGray)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(shape)
                .background(color)
        )
        if (animated > 0.08f) {
            // Glossy highlight stripe, Duolingo-style.
            Box(
                modifier = Modifier
                    .padding(top = 2.dp, start = 4.dp)
                    .fillMaxHeight(0.3f)
                    .fillMaxWidth(animated * 0.96f)
                    .clip(shape)
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    }
}

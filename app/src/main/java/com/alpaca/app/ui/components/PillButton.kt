package com.alpaca.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alpaca.app.ui.theme.CloudGray
import com.alpaca.app.ui.theme.InkFaint
import com.alpaca.app.ui.theme.PacoGreen
import com.alpaca.app.ui.theme.PaperWhite

/**
 * Duolingo-style pill button: bold label on a solid face with a hard-colored 3D
 * bottom edge that the face physically presses into.
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = PacoGreen,
    textColor: Color = PaperWhite,
    fillWidth: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(16.dp)

    val face = if (enabled) color else CloudGray
    val edge = if (enabled) color.darkened() else Color(0xFFD0D0D0)
    val pressOffset by animateDpAsState(
        if (pressed && enabled) 4.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pill-press"
    )

    Box(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Hard 3D edge: the face sinks down onto this when pressed.
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .clip(shape)
                .background(edge)
        )
        Box(
            modifier = Modifier
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .offset(y = pressOffset)
                .clip(shape)
                .background(face)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) textColor else InkFaint,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun Color.darkened(factor: Float = 0.76f): Color =
    Color(red * factor, green * factor, blue * factor, alpha)

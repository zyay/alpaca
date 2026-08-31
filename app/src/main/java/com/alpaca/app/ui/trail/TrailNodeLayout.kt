package com.alpaca.app.ui.trail

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alpaca.app.data.db.entities.LessonStatus
import com.alpaca.app.ui.theme.CloudGray
import com.alpaca.app.ui.theme.InkMid
import com.alpaca.app.ui.theme.BrandGreen
import com.alpaca.app.ui.theme.BrandGreenDeep
import com.alpaca.app.ui.theme.PaperWhite
import kotlin.math.roundToInt
import kotlin.math.sin

private val NodeSize = 78.dp
private val Spacing = 118.dp
private val TopPad = 100.dp

/** Fraction of trail width for node [i], following a lazy sine-wave wander. */
private fun xFraction(i: Int) = 0.5f + sin(i * 0.95 + 0.7).toFloat() * 0.28f

/**
 * The trail map: a winding path of lesson nodes; the current one pulses with
 * a floating "You" flag chip above it.
 */
@Composable
fun TrailMap(
    nodes: List<TrailViewModel.NodeUi>,
    flagEmoji: String,
    onNodeClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val width = maxWidth
        val height = TopPad + NodeSize + Spacing * (nodes.size - 1) + 64.dp

        fun centerX(i: Int): Dp = width * xFraction(i)
        fun centerY(i: Int): Dp = TopPad + NodeSize / 2 + Spacing * i

        Box(modifier = Modifier.fillMaxWidth().height(height)) {
            // Connector path behind the nodes.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pts = nodes.indices.map { i ->
                    Offset(centerX(i).toPx(), centerY(i).toPx())
                }
                if (pts.size < 2) return@Canvas
                val firstIncomplete = nodes.indexOfFirst { it.status != LessonStatus.COMPLETE }
                    .let { if (it == -1) nodes.size else it }

                fun segment(toIndex: Int): Path = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    for (i in 0 until toIndex) {
                        val midY = (pts[i].y + pts[i + 1].y) / 2f
                        cubicTo(pts[i].x, midY, pts[i + 1].x, midY, pts[i + 1].x, pts[i + 1].y)
                    }
                }

                drawPath(segment(pts.size - 1), CloudGray, style = Stroke(width = 14f))
                if (firstIncomplete > 0) {
                    drawPath(segment(firstIncomplete - 1), BrandGreen, style = Stroke(width = 14f))
                }
            }

            nodes.forEachIndexed { index, node ->
                // Node circle with 3D edge.
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (centerX(index) - NodeSize / 2).roundToPx(),
                                (centerY(index) - NodeSize / 2).roundToPx()
                            )
                        }
                        .size(NodeSize)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNodeClick(index) }
                ) {
                    val isLocked = node.status == LessonStatus.LOCKED
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(y = 6.dp)
                            .clip(CircleShape)
                            .background(if (isLocked) Color(0xFFD0D0D0) else BrandGreenDeep)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CircleShape)
                            .background(if (isLocked) CloudGray else BrandGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        when (node.status) {
                            LessonStatus.COMPLETE -> Icon(
                                Icons.Filled.Check, "Completed",
                                tint = PaperWhite, modifier = Modifier.size(34.dp)
                            )
                            LessonStatus.AVAILABLE -> Icon(
                                Icons.Filled.PlayArrow, "Start lesson",
                                tint = PaperWhite, modifier = Modifier.size(38.dp)
                            )
                            LessonStatus.LOCKED -> Icon(
                                Icons.Filled.Lock, "Locked",
                                tint = InkMid, modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Lesson title under the node.
                Text(
                    text = node.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (node.status == LessonStatus.LOCKED) InkMid
                    else MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (centerX(index) - 80.dp).roundToPx(),
                                (centerY(index) + NodeSize / 2 + 10.dp).roundToPx()
                            )
                        }
                        .width(160.dp)
                )
            }

            // Current-node marker: pulsing halo + floating "You" flag chip.
            val current = nodes.indexOfFirst { it.status == LessonStatus.AVAILABLE }
            if (current >= 0) {
                val transition = rememberInfiniteTransition(label = "current-node")
                val pulse by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1600, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "node-pulse"
                )
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (centerX(current) - NodeSize).roundToPx(),
                                (centerY(current) - NodeSize).roundToPx()
                            )
                        }
                        .size(NodeSize * 2)
                        .graphicsLayer {
                            scaleX = 1f + 0.12f * pulse
                            scaleY = 1f + 0.12f * pulse
                            alpha = 0.55f * (1f - pulse)
                        }
                        .clip(CircleShape)
                        .background(BrandGreen)
                )
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (centerX(current) - 44.dp).roundToPx(),
                                (centerY(current) - NodeSize / 2 - 64.dp).roundToPx()
                            )
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(PaperWhite)
                        .border(2.dp, BrandGreen, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(flagEmoji, fontSize = 14.sp)
                        Text(
                            "You",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandGreen,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

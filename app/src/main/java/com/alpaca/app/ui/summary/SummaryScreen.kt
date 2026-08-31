package com.alpaca.app.ui.summary

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import com.alpaca.app.AlpacaApp
import com.alpaca.app.data.repository.LessonResult
import com.alpaca.app.ui.components.ConfettiOverlay
import com.alpaca.app.ui.components.GreetingWordmark
import com.alpaca.app.ui.components.PillButton
import com.alpaca.app.ui.theme.InkMid
import com.alpaca.app.ui.theme.BrandGreen
import com.alpaca.app.ui.theme.BrandGreenPale
import com.alpaca.app.ui.theme.SunYellow
import com.alpaca.app.util.HapticPlayer
import kotlin.math.roundToInt

@Composable
fun SummaryScreen(
    app: AlpacaApp,
    haptics: HapticPlayer?,
    onContinue: () -> Unit
) {
    val result = remember { app.lastLessonResult }
    if (result == null) {
        LaunchedEffect(Unit) { onContinue() }
        return
    }

    val container = com.alpaca.app.di.LocalAppContainer.current
    val soundEnabled by androidx.compose.runtime.remember(container) {
        container.prefs.prefs.map { it.soundEnabled }
    }.collectAsStateWithLifecycle(initialValue = true)

    LaunchedEffect(Unit) {
        if (!result.outOfEnergy) {
            haptics?.celebrate()
            if (soundEnabled) container.soundPlayer.finish()
        } else {
            haptics?.wrongBuzz()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            GreetingWordmark(
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (result.outOfEnergy) "Out of fleece!" else lessonHeadline(result),
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (result.outOfEnergy) {
                    "Fleece energy regrows every 30 minutes — come back soon!"
                } else {
                    result.lessonTitle
                },
                style = MaterialTheme.typography.bodyLarge,
                color = InkMid,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            if (!result.outOfEnergy) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        icon = {
                            Icon(
                                Icons.Filled.Stars, null,
                                tint = SunYellow, modifier = Modifier.size(22.dp)
                            )
                        },
                        label = "Total XP",
                        value = "+${result.xpGained}",
                        background = Color(0xFFFFF7DD)
                    )
                    StatCard(
                        icon = {
                            Icon(
                                Icons.Filled.CheckCircle, null,
                                tint = BrandGreen, modifier = Modifier.size(22.dp)
                            )
                        },
                        label = "Accuracy",
                        value =
                        "${(result.correctCount * 100f / result.totalCount.coerceAtLeast(1)).roundToInt()}%",
                        background = BrandGreenPale
                    )
                    StatCard(
                        icon = {
                            Icon(
                                Icons.Filled.LocalFireDepartment, null,
                                tint = Color(0xFFFF9600), modifier = Modifier.size(22.dp)
                            )
                        },
                        label = "Streak",
                        value = "${result.newStreak}${if (result.streakIncreased) " ↑" else ""}",
                        background = Color(0xFFFFEBD6)
                    )
                }
                Spacer(Modifier.height(8.dp))
                CoinsEarnedRow(result.coinsGained)
            }

            Spacer(Modifier.height(16.dp))

            if (result.mistaken.isNotEmpty()) {
                ReviewCard(
                    title = "Needs practice",
                    items = result.mistaken,
                    icon = Icons.Filled.Refresh,
                    iconTint = Color(0xFFFF9600)
                )
                Spacer(Modifier.height(12.dp))
            }

            PillButton(text = "Continue", onClick = onContinue)
            Spacer(Modifier.height(24.dp))
        }

        ConfettiOverlay(
            active = !result.outOfEnergy,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun lessonHeadline(result: LessonResult): String = when {
    result.perfect -> "¡Perfecto!"
    result.correctCount == result.totalCount -> "Lesson complete!"
    else -> "Good work!"
}

@Composable
private fun RowScope.StatCard(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    background: Color
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InkMid)
    }
}

@Composable
private fun CoinsEarnedRow(coins: Int) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animated.animateTo(1f, animationSpec = tween(900))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEFF8FF))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Stars, null, tint = SunYellow)
        Spacer(Modifier.size(8.dp))
        Text(
            "+${(coins * animated.value).roundToInt()} Coins",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun ReviewCard(
    title: String,
    items: List<String>,
    icon: ImageVector,
    iconTint: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        items.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(item, style = MaterialTheme.typography.bodyMedium, color = InkMid)
            }
        }
    }
}

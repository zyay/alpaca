package com.alpaca.app.ui.trail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpaca.app.data.db.entities.LessonStatus
import com.alpaca.app.ui.components.EnergyHearts
import com.alpaca.app.ui.components.PillButton
import com.alpaca.app.ui.theme.SkyBlue
import com.alpaca.app.ui.theme.SunYellow
import com.alpaca.app.util.HapticPlayer

@Composable
fun TrailScreen(
    viewModel: TrailViewModel,
    onOpenLesson: (String) -> Unit,
    onOpenVoice: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenSettings: () -> Unit,
    haptics: HapticPlayer?
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val unit = state.unit
    val user = state.user

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        // Top bar: region + entry points.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = unit?.region ?: "The Andes Trail",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = unit?.title ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onOpenLeaderboard) {
                Icon(Icons.Filled.EmojiEvents, "Leaderboards", tint = SunYellow)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Filled.Settings, "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Stats chips: streak, coins, fleece energy.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatChip(
                icon = {
                    Icon(
                        Icons.Filled.LocalFireDepartment, null,
                        tint = Color(0xFFFF9600), modifier = Modifier.size(20.dp)
                    )
                },
                value = "${user?.streakDays ?: 0}"
            )
            StatChip(
                icon = {
                    Icon(
                        Icons.Filled.Stars, null,
                        tint = SunYellow, modifier = Modifier.size(20.dp)
                    )
                },
                value = "${user?.coins ?: 0}"
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                EnergyHearts(energy = user?.fleeceEnergy ?: 5)
            }
        }

        Spacer(Modifier.height(24.dp))

        TrailMap(
            nodes = state.nodes,
            onNodeClick = { index ->
                val node = state.nodes.getOrNull(index) ?: return@TrailMap
                if (node.status == LessonStatus.LOCKED) {
                    haptics?.wrongBuzz()
                } else {
                    haptics?.light()
                    onOpenLesson(node.lessonId)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        PillButton(
            text = "Practice speaking with Paco",
            onClick = onOpenVoice,
            color = SkyBlue
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun StatChip(icon: @Composable () -> Unit, value: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
    }
}

package com.alpaca.app.ui.trail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
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
import com.alpaca.app.data.content.ContentRepository
import com.alpaca.app.data.db.entities.LessonStatus
import com.alpaca.app.ui.components.EnergyHearts
import com.alpaca.app.ui.components.PillButton
import com.alpaca.app.ui.theme.CloudGray
import com.alpaca.app.ui.theme.DangerRed
import com.alpaca.app.ui.theme.InkMid
import com.alpaca.app.ui.theme.SkyBlue
import com.alpaca.app.ui.theme.SunYellow
import com.alpaca.app.util.HapticPlayer

@Composable
fun TrailScreen(
    viewModel: TrailViewModel,
    onOpenLesson: (String) -> Unit,
    onOpenVoice: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenSettings: () -> Unit,
    haptics: HapticPlayer?
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user = state.user
    val selected = state.selectedUnit

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = selected?.region ?: "The Andes Trail",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = selected?.let { "${it.title} · ${it.completed}/${it.total}" } ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton3(onClick = onOpenAchievements, glyph = "🏅", label = "Achievements")
            IconButton3(onClick = onOpenLeaderboard, glyph = "🏆", label = "Leaderboards")
            IconButton3(onClick = onOpenSettings, glyph = "⚙️", label = "Settings")
        }

        Spacer(Modifier.height(8.dp))

        // Unit tabs.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.units.forEach { tab ->
                UnitChip(
                    tab = tab,
                    selected = tab.unitId == state.selectedUnitId,
                    onClick = {
                        if (tab.unlocked) {
                            haptics?.light()
                            viewModel.selectUnit(tab.unitId)
                        } else {
                            haptics?.wrongBuzz()
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

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
            StatChip(
                icon = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward, null,
                        tint = SkyBlue, modifier = Modifier.size(20.dp)
                    )
                },
                value = "${user?.xp ?: 0} XP"
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

        Spacer(Modifier.height(20.dp))

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

        Spacer(Modifier.height(16.dp))

        if (state.mistakeCount > 0) {
            PillButton(
                text = "Repaso de errores (${state.mistakeCount})",
                onClick = { onOpenLesson(ContentRepository.REVIEW_LESSON_ID) },
                color = Color(0xFFFF8A3D)
            )
            Spacer(Modifier.height(10.dp))
        }

        PillButton(
            text = "Practice speaking with Paco",
            onClick = onOpenVoice,
            color = SkyBlue
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun UnitChip(tab: TrailViewModel.UnitTab, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(100.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) com.alpaca.app.ui.theme.PacoGreen else Color.White)
            .clickable(enabled = tab.unlocked) { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!tab.unlocked) {
            Icon(Icons.Filled.Lock, "Locked", tint = InkMid, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = tab.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (selected) Color.White else if (tab.unlocked) InkMid else InkMid.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun IconButton3(onClick: () -> Unit, glyph: String, label: String) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(Color.White)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(glyph, style = MaterialTheme.typography.titleLarge)
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

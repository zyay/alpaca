package com.alpaca.app.ui.trail

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpaca.app.data.content.ContentRepository
import com.alpaca.app.data.db.entities.LessonStatus
import com.alpaca.app.data.db.entities.UserEntity
import com.alpaca.app.ui.components.EnergyHearts
import com.alpaca.app.ui.components.PillButton
import com.alpaca.app.ui.theme.BrandGreen
import com.alpaca.app.ui.theme.CloudGray
import com.alpaca.app.ui.theme.GemPurple
import com.alpaca.app.ui.theme.HeartPink
import com.alpaca.app.ui.theme.InkFaint
import com.alpaca.app.ui.theme.InkMid
import com.alpaca.app.ui.theme.SkyBlue
import com.alpaca.app.ui.theme.StreakOrange
import com.alpaca.app.ui.theme.SunYellow
import com.alpaca.app.util.HapticPlayer
import java.time.LocalDate

private val UnitColors = listOf(BrandGreen, SkyBlue, GemPurple, StreakOrange, HeartPink)

@Composable
fun TrailScreen(
    viewModel: TrailViewModel,
    onOpenLesson: (String) -> Unit,
    onOpenVoice: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCourses: () -> Unit,
    onOpenQuests: () -> Unit,
    haptics: HapticPlayer?
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user = state.user
    val selected = state.selectedUnit

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
            }

            Spacer(Modifier.height(10.dp))

            // Current course switcher.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenCourses)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.language.flagEmoji} ${state.language.nativeName}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Change course",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

            // Stats chips: streak, coins, XP, fleece energy.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatChip(
                    icon = {
                        AtRiskFlame(
                            atRisk = isStreakAtRisk(user),
                            modifier = Modifier.size(20.dp)
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
                            Icons.Filled.Diamond, null,
                            tint = GemPurple, modifier = Modifier.size(20.dp)
                        )
                    },
                    value = "${user?.gems ?: 0}",
                    onClick = {
                        haptics?.light()
                        onOpenQuests()
                    }
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
                if ((user?.streakFreezes ?: 0) > 0) {
                    StatChip(
                        icon = {
                            Icon(
                                Icons.Filled.AcUnit, null,
                                tint = SkyBlue, modifier = Modifier.size(20.dp)
                            )
                        },
                        value = "${user?.streakFreezes ?: 0}",
                        onClick = {
                            haptics?.light()
                            onOpenQuests()
                        }
                    )
                }
            }

            // Daily quests banner.
            if (state.quests.isNotEmpty()) {
                QuestBanner(
                    claimed = state.quests.count { it.claimed },
                    claimable = state.claimableQuests,
                    total = state.quests.size,
                    onClick = {
                        haptics?.light()
                        onOpenQuests()
                    },
                    modifier = Modifier.padding(top = 14.dp)
                )
            }

            // Weekly streak dots.
            StreakWeekDots(user = user, modifier = Modifier.padding(top = 10.dp))

            Spacer(Modifier.height(16.dp))

            // Unit section header card.
            if (selected != null) {
                val sectionColor = UnitColors[
                    state.units.indexOfFirst { it.unitId == selected.unitId }
                        .coerceAtLeast(0) % UnitColors.size
                ]
                UnitSectionHeader(
                    title = selected.title,
                    subtitle = "${selected.completed} of ${selected.total} lessons complete",
                    color = sectionColor
                )
                Spacer(Modifier.height(16.dp))
            }

            TrailMap(
                nodes = state.nodes,
                flagEmoji = state.language.flagEmoji,
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
                    color = StreakOrange
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(96.dp))
        }

        TrailBottomBar(
            mistakeCount = state.mistakeCount,
            onLearn = { haptics?.light() },
            onReview = {
                haptics?.light()
                onOpenLesson(ContentRepository.REVIEW_LESSON_ID)
            },
            onSpeak = {
                haptics?.light()
                onOpenVoice()
            },
            onLeagues = {
                haptics?.light()
                onOpenLeaderboard()
            },
            onProfile = {
                haptics?.light()
                onOpenSettings()
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun TrailBottomBar(
    mistakeCount: Int,
    onLearn: () -> Unit,
    onReview: () -> Unit,
    onSpeak: () -> Unit,
    onLeagues: () -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(2.dp, CloudGray, RoundedCornerShape(24.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomNavItem(
            icon = Icons.Filled.School,
            label = "Learn",
            active = true,
            onClick = onLearn
        )
        BottomNavItem(
            icon = Icons.Filled.Refresh,
            label = if (mistakeCount > 0) "Review · $mistakeCount" else "Review",
            active = false,
            onClick = onReview
        )
        BottomNavItem(
            icon = Icons.Filled.Mic,
            label = "Speak",
            active = false,
            onClick = onSpeak
        )
        BottomNavItem(
            icon = Icons.Filled.EmojiEvents,
            label = "Leagues",
            active = false,
            onClick = onLeagues
        )
        BottomNavItem(
            icon = Icons.Filled.Person,
            label = "Profile",
            active = false,
            onClick = onProfile
        )
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val tint = if (active) BrandGreen else InkMid
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun isStreakAtRisk(user: UserEntity?): Boolean {
    user ?: return false
    if (user.streakDays <= 0) return false
    return user.lastPracticeEpochDay < LocalDate.now().toEpochDay()
}

/** At-risk streaks burn faster (700ms) than safe ones (1600ms). */
@Composable
private fun AtRiskFlame(atRisk: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "flame")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (atRisk) 700 else 1600,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame-pulse"
    )
    Icon(
        Icons.Filled.LocalFireDepartment, null,
        tint = StreakOrange.copy(alpha = if (atRisk) 1f else 0.85f),
        modifier = modifier
            .scale(if (atRisk) scale else 1f)
    )
}

@Composable
private fun StreakWeekDots(user: UserEntity?, modifier: Modifier = Modifier) {
    val today = LocalDate.now().toEpochDay()
    val lastPractice = user?.lastPracticeEpochDay ?: -1L
    val streak = user?.streakDays ?: 0
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    // Column index of Monday in the ISO week containing today.
    val todayIndex = LocalDate.now().dayOfWeek.value - 1
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
    ) {
        days.forEachIndexed { i, label ->
            val day = today - (todayIndex - i)
            val lit = lastPractice >= 0 && day <= lastPractice &&
                (lastPractice - day) < streak.coerceAtLeast(1)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .alpha(if (lit) 1f else 0.25f)
                        .clip(CircleShape)
                        .background(if (lit) StreakOrange else CloudGray)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = if (lit) StreakOrange else InkFaint,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun UnitSectionHeader(
    title: String,
    subtitle: String,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun UnitChip(tab: TrailViewModel.UnitTab, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(100.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) com.alpaca.app.ui.theme.BrandGreen else Color.White)
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
private fun StatChip(icon: @Composable () -> Unit, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(Color.White)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun QuestBanner(
    claimed: Int,
    claimable: Int,
    total: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, if (claimable > 0) BrandGreen else CloudGray, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Flag, null, tint = StreakOrange, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Daily quests",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = if (claimable > 0) {
                    "$claimable reward${if (claimable > 1) "s" else ""} ready to claim!"
                } else {
                    "Every day brings new gem rewards"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (claimable > 0) BrandGreen else InkMid
            )
        }
        Text(
            text = "$claimed/$total",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = InkFaint
        )
    }
}

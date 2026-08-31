package com.alpaca.app.ui.achievements

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpaca.app.ui.components.Entrance
import com.alpaca.app.ui.components.badgeShine
import com.alpaca.app.ui.theme.CloudGray
import com.alpaca.app.ui.theme.InkMid
import com.alpaca.app.ui.theme.BrandGreenPale

@Composable
fun AchievementsScreen(
    viewModel: AchievementsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text("Logros de ${state.displayName}", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(state.badges) { index, badge ->
                Entrance(index = index) {
                    BadgeCard(badge)
                }
            }
        }
    }
}

@Composable
private fun BadgeCard(badge: Badge) {
    // Progress strings look like "3/5"; unparseable ones skip the bar.
    val parts = badge.progress.split("/").mapNotNull { it.trim().toIntOrNull() }
    val progressFraction = if (!badge.unlocked && parts.size == 2 && parts[1] > 0) {
        (parts[0].toFloat() / parts[1]).coerceIn(0f, 1f)
    } else {
        null
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction ?: 0f,
        animationSpec = tween(600),
        label = "badge-progress"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(18.dp))
            .background(if (badge.unlocked) BrandGreenPale else CloudGray.copy(alpha = 0.4f))
            .alpha(if (badge.unlocked) 1f else 0.7f)
            .badgeShineIf(badge.unlocked)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = badge.emoji,
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = badge.title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = badge.description,
            style = MaterialTheme.typography.bodyMedium,
            color = InkMid,
            textAlign = TextAlign.Center
        )
        if (progressFraction != null) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(com.alpaca.app.ui.theme.BrandGreen)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (badge.unlocked) "¡Desbloqueado!" else badge.progress,
            style = MaterialTheme.typography.labelLarge,
            color = if (badge.unlocked) com.alpaca.app.ui.theme.BrandGreen else InkMid,
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(Color.White)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun Modifier.badgeShineIf(enabled: Boolean): Modifier =
    if (enabled) this.badgeShine() else this

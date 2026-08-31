package com.alpaca.app.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.TrendingUp
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
import com.alpaca.app.ui.components.CountUpText
import com.alpaca.app.ui.components.EmptyStateCard
import com.alpaca.app.ui.components.Entrance
import com.alpaca.app.ui.components.LoadingView
import com.alpaca.app.ui.components.PillButton
import com.alpaca.app.ui.theme.alpacaFaintText
import com.alpaca.app.ui.theme.BrandGreen
import com.alpaca.app.ui.theme.SkyBlue
import com.alpaca.app.ui.theme.SunYellow
import com.alpaca.app.ui.theme.alpacaCard
import com.alpaca.app.ui.theme.alpacaCardBorder
import com.alpaca.app.ui.theme.alpacaGreenTint
import com.alpaca.app.ui.theme.alpacaSecondaryText

private const val PROMOTION_ZONE = 5

@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text(
                "Weekly League",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // League banner.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(alpacaGreenTint())
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.EmojiEvents, null,
                tint = SunYellow, modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Emerald Herd", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (state.online) {
                        "Top $PROMOTION_ZONE advance · resets ${resetLabel(state.resetsInMs)}"
                    } else {
                        "Local preview · offline herd"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = alpacaSecondaryText()
                )
            }
        }

        if (!state.online && state.errorMessage != null) {
            Text(
                text = "Online league unavailable (${state.errorMessage}). Playing the practice herd instead.",
                style = MaterialTheme.typography.bodySmall,
                color = alpacaFaintText()
            )
            Spacer(Modifier.height(8.dp))
        }

        if (state.loading) {
            LoadingView("Loading the herd…")
            return@Column
        }

        if (state.entries.isEmpty()) {
            EmptyStateCard(
                emoji = "🏁",
                title = "The herd is empty",
                blurb = "Earn XP in any lesson to join this week's race.",
                modifier = Modifier.weight(1f)
            )
            return@Column
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(state.entries) { index, row ->
                val rank = index + 1
                val inPromotion = state.online && rank <= PROMOTION_ZONE
                Entrance(index = index) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                        .background(
                            when {
                                row.isYou -> alpacaGreenTint()
                                inPromotion -> BrandGreen.copy(alpha = 0.06f)
                                else -> alpacaCard()
                            }
                        )
                        .border(
                            2.dp,
                            when {
                                row.isYou -> BrandGreen
                                inPromotion -> BrandGreen.copy(alpha = 0.35f)
                                else -> alpacaCardBorder()
                            },
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.TrendingUp, null,
                        tint = if (inPromotion) BrandGreen else Color.Transparent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$rank",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (rank <= 3) SunYellow else alpacaSecondaryText(),
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.width(30.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (row.isYou) BrandGreen else alpacaCardBorder()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = row.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (row.isYou) Color.White else alpacaSecondaryText()
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (row.isYou) "${row.name} (you)" else row.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    CountUpText(
                        target = row.xp,
                        suffix = " XP",
                        style = MaterialTheme.typography.titleMedium,
                        color = alpacaSecondaryText()
                    )
                }
                }
            }
            item {
                Text(
                    text = if (state.online) {
                        "Every lesson's XP lands here instantly. Same herd all week, worldwide."
                    } else {
                        "Set VERCEL_BASE_URL + Redis on the backend to race real learners."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = alpacaSecondaryText(),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }

        if (state.online) {
            PillButton(
                text = "Refresh standings",
                onClick = { viewModel.refresh() },
                color = SkyBlue
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun resetLabel(resetsInMs: Long): String {
    if (resetsInMs <= 0) return "soon"
    val hours = resetsInMs / 3_600_000
    return when {
        hours >= 24 -> "${hours / 24}d ${hours % 24}h"
        hours >= 1 -> "${hours}h ${(resetsInMs % 3_600_000) / 60_000}m"
        else -> "${resetsInMs / 60_000}m"
    }
}

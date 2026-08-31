package com.alpaca.app.ui.quests

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpaca.app.data.db.entities.QuestEntity
import com.alpaca.app.data.db.entities.UserEntity
import com.alpaca.app.data.repository.QuestRepository
import com.alpaca.app.ui.components.PillButton
import com.alpaca.app.ui.theme.BrandGreen
import com.alpaca.app.ui.theme.CloudGray
import com.alpaca.app.ui.theme.GemPurple
import com.alpaca.app.ui.theme.InkFaint
import com.alpaca.app.ui.theme.InkMid
import com.alpaca.app.ui.theme.SkyBlue
import com.alpaca.app.ui.theme.StreakOrange
import com.alpaca.app.ui.theme.SunYellow
import com.alpaca.app.util.HapticPlayer

@Composable
fun QuestsScreen(
    viewModel: QuestsViewModel,
    onBack: () -> Unit,
    haptics: HapticPlayer? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user = state.user

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Quests",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            GemsChip(gems = user?.gems ?: 0)
        }

        Spacer(Modifier.height(4.dp))

        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = BrandGreen,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Flag, null, tint = StreakOrange, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Daily quests",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Text(
            text = "New quests every day. Gems in the pot for everyone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        if (state.loading) {
            Text("Loading…", color = InkFaint, style = MaterialTheme.typography.bodyLarge)
        }
        state.quests.forEach { quest ->
            QuestCard(
                quest = quest,
                title = QuestRepository.specs.firstOrNull { it.questId == quest.questId }?.title
                    ?: quest.type,
                onClaim = {
                    haptics?.light()
                    viewModel.claim(quest.questId)
                }
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Storefront, null, tint = SkyBlue, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Gem shop",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(Modifier.height(12.dp))

        ShopCard(
            icon = { Icon(Icons.Filled.AcUnit, null, tint = SkyBlue, modifier = Modifier.size(32.dp)) },
            title = "Streak Freeze",
            subtitle = "Protects your streak for one missed day. Equips automatically.",
            price = UserEntity.FREEZE_PRICE_GEMS,
            owned = "Owned: ${user?.streakFreezes ?: 0}/${UserEntity.MAX_FREEZES}",
            enabled = (user?.streakFreezes ?: 0) < UserEntity.MAX_FREEZES &&
                (user?.gems ?: 0) >= UserEntity.FREEZE_PRICE_GEMS,
            onBuy = {
                haptics?.light()
                viewModel.buyStreakFreeze()
            }
        )
        Spacer(Modifier.height(10.dp))
        ShopCard(
            icon = { Icon(Icons.Filled.Bolt, null, tint = SunYellow, modifier = Modifier.size(32.dp)) },
            title = "Fleece Refill",
            subtitle = "Instantly refills all 5 fleece hearts. No waiting.",
            price = UserEntity.REFILL_PRICE_GEMS,
            owned = "Fleece: ${user?.fleeceEnergy ?: 0}/${UserEntity.MAX_ENERGY}",
            enabled = (user?.fleeceEnergy ?: 0) < UserEntity.MAX_ENERGY &&
                (user?.gems ?: 0) >= UserEntity.REFILL_PRICE_GEMS,
            onBuy = {
                haptics?.light()
                viewModel.buyEnergyRefill()
            }
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun GemsChip(gems: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(Color.White)
            .border(2.dp, CloudGray, RoundedCornerShape(100.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(GemPurple)
        )
        Spacer(Modifier.width(6.dp))
        Text("$gems", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun QuestCard(quest: QuestEntity, title: String, onClaim: () -> Unit) {
    val progressFraction = (quest.progress.toFloat() / quest.target).coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(320),
        label = "quest-progress"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, CloudGray, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (quest.claimed) "Claimed" else
                        "${quest.progress.coerceAtMost(quest.target)}/${quest.target}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (quest.claimed) BrandGreen else InkMid,
                    fontWeight = if (quest.claimed) FontWeight.Bold else FontWeight.Normal
                )
            }
            RewardPill(gems = quest.rewardGems, claimed = quest.claimed)
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(CloudGray)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .height(14.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (quest.isComplete) BrandGreen else StreakOrange)
            )
        }

        if (quest.isComplete && !quest.claimed) {
            Spacer(Modifier.height(12.dp))
            PillButton(text = "Claim +${quest.rewardGems} gems", onClick = onClaim)
        }
    }
}

@Composable
private fun RewardPill(gems: Int, claimed: Boolean) {
    Row(
        modifier = Modifier
            .alpha(if (claimed) 0.45f else 1f)
            .clip(RoundedCornerShape(100.dp))
            .background(GemPurple.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(GemPurple)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "$gems",
            style = MaterialTheme.typography.titleSmall,
            color = InkMid,
            fontWeight = FontWeight.ExtraBold
        )
        if (claimed) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.Check, null, tint = BrandGreen, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun ShopCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    price: Int,
    owned: String,
    enabled: Boolean,
    onBuy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, CloudGray, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) { icon() }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = owned,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMid
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(GemPurple)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$price",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        PillButton(
            text = "Buy",
            onClick = onBuy,
            enabled = enabled
        )
    }
}

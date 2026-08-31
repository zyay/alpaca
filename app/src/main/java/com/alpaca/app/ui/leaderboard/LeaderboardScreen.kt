package com.alpaca.app.ui.leaderboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpaca.app.ui.theme.BronzeFleece
import com.alpaca.app.ui.theme.InkMid
import com.alpaca.app.ui.theme.BrandGreenPale
import com.alpaca.app.ui.theme.SunYellow

data class LeaderRow(val name: String, val xp: Int)

private val FakeHerd = listOf(
    LeaderRow("Lucía", 812),
    LeaderRow("Mateo", 704),
    LeaderRow("Valentina", 655),
    LeaderRow("Diego", 598),
    LeaderRow("Sofía", 540),
    LeaderRow("Sebastián", 481),
    LeaderRow("Camila", 402),
    LeaderRow("Nicolás", 350),
    LeaderRow("Isabella", 287),
    LeaderRow("Tomás", 233)
)

@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel,
    onBack: () -> Unit
) {
    val userXp by viewModel.userXp.collectAsStateWithLifecycle()
    val displayName by viewModel.displayName.collectAsStateWithLifecycle()

    val rows = (FakeHerd + LeaderRow(displayName, userXp)).sortedByDescending { it.xp }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text("Herd Leaderboard", style = MaterialTheme.typography.headlineMedium)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(BronzeFleece.copy(alpha = 0.18f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🦙", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Bronze Fleece League", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Top 10 advance to Silver Fleece · resets Monday",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMid
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(rows) { index, row ->
                val isUser = row.name == displayName
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isUser) BrandGreenPale else Color.White)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (index < 3) SunYellow else InkMid,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.width(30.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isUser) Color(0xFF58CC02) else Color(0xFFEDEDED)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = row.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isUser) Color.White else InkMid
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (isUser) "$displayName (you)" else row.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${row.xp} XP", style = MaterialTheme.typography.titleMedium, color = InkMid)
                }
            }
            item {
                Text(
                    text = "Online leagues are coming soon — this is a local preview.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMid,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

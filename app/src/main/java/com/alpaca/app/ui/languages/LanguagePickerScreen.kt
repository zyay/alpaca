package com.alpaca.app.ui.languages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
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
import com.alpaca.app.ui.theme.BrandGreen
import com.alpaca.app.ui.theme.CloudGray
import com.alpaca.app.ui.theme.InkFaint
import com.alpaca.app.ui.theme.InkMid

@Composable
fun LanguagePickerScreen(
    viewModel: LanguagePickerViewModel,
    onBack: () -> Unit
) {
    val currentId by viewModel.currentLanguageId.collectAsStateWithLifecycle()

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
            Text("Courses", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Switch course any time. Your streak and XP are shared across languages.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMid,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(viewModel.available.size) { index ->
                val language = viewModel.available[index]
                LanguageCard(
                    flag = language.flagEmoji,
                    nativeName = language.nativeName,
                    displayName = language.displayName,
                    selected = language.id == currentId,
                    onClick = {
                        viewModel.select(language.id)
                        onBack()
                    }
                )
            }
            items(viewModel.comingSoon.size) { index ->
                val language = viewModel.comingSoon[index]
                LanguageCard(
                    flag = language.flagEmoji,
                    nativeName = language.nativeName,
                    displayName = language.displayName,
                    selected = false,
                    locked = true,
                    onClick = { }
                )
            }
        }
    }
}

@Composable
private fun LanguageCard(
    flag: String,
    nativeName: String,
    displayName: String,
    selected: Boolean,
    locked: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .then(
                if (selected) Modifier.border(
                    BorderStroke(3.dp, BrandGreen),
                    RoundedCornerShape(16.dp)
                ) else Modifier
            )
            .clickable(enabled = !locked, onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CloudGray),
            contentAlignment = Alignment.Center
        ) {
            Text(flag, style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                nativeName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
            Text(displayName, style = MaterialTheme.typography.bodyMedium, color = InkMid)
        }
        if (locked) {
            Icon(Icons.Filled.Lock, "Locked", tint = InkFaint)
        } else if (selected) {
            Text(
                "LEARNING",
                style = MaterialTheme.typography.labelLarge,
                color = BrandGreen
            )
        }
    }
}

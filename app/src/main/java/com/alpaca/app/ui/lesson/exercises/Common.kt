package com.alpaca.app.ui.lesson.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alpaca.app.ui.components.PillButton
import com.alpaca.app.ui.theme.alpacaCard
import com.alpaca.app.ui.theme.alpacaCardBorder
import com.alpaca.app.ui.theme.SkyBlue

/** Exercise layout: scrollable content on top, CHECK pill pinned at the bottom. */
@Composable
fun ExerciseScaffold(
    checkEnabled: Boolean,
    onCheck: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
        PillButton(
            text = "Check",
            onClick = onCheck,
            enabled = checkEnabled,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Composable
fun PromptText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        modifier = modifier.padding(bottom = 4.dp)
    )
}

/** Selectable answer card used by multiple-choice and listening exercises. */
@Composable
fun OptionCard(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val borderColor = if (selected) SkyBlue else alpacaCardBorder()
    val background = if (selected) SkyBlue.copy(alpha = 0.16f) else alpacaCard()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(2.dp, borderColor, shape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) SkyBlue else MaterialTheme.colorScheme.onSurface
        )
    }
}

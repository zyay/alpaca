package com.alpaca.app.ui.lesson.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alpaca.app.data.content.FillBlankExercise
import com.alpaca.app.ui.theme.alpacaCardBorder
import com.alpaca.app.ui.theme.alpacaCard
import com.alpaca.app.ui.theme.alpacaFaintText
import com.alpaca.app.ui.theme.SkyBlue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FillBlankUi(
    exercise: FillBlankExercise,
    onCorrect: () -> Unit,
    onWrong: (correctText: String, explanation: String?, mistakeLabel: String) -> Unit,
    onCheckHaptic: (correct: Boolean) -> Unit
) {
    val bank = remember { exercise.wordBank.shuffled() }
    var picked by rememberSaveable { mutableStateOf<String?>(null) }

    val parts = exercise.sentence.split("___")
    val fullCorrect = exercise.sentence.replace("___", exercise.answer)

    ExerciseScaffold(
        checkEnabled = picked != null,
        onCheck = {
            if (picked == exercise.answer) {
                onCheckHaptic(true)
                onCorrect()
            } else {
                onCheckHaptic(false)
                onWrong(fullCorrect, exercise.explanation, exercise.sentence)
            }
        }
    ) {
        PromptText("Fill in the blank")

        // Sentence with an inline blank slot.
        val sentenceStyle = MaterialTheme.typography.displaySmall
        Text(
            text = parts.firstOrNull().orEmpty(),
            style = sentenceStyle,
            fontWeight = FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (picked != null) SkyBlue.copy(alpha = 0.16f) else alpacaCard())
                .border(
                    2.dp,
                    if (picked != null) SkyBlue else alpacaCardBorder(),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 28.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = picked ?: "•••",
                style = sentenceStyle,
                fontWeight = FontWeight.Bold,
                color = if (picked != null) SkyBlue else alpacaFaintText()
            )
        }
        Text(
            text = parts.getOrNull(1).orEmpty(),
            style = sentenceStyle,
            fontWeight = FontWeight.Medium
        )

        // Word bank chips.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            bank.forEach { word ->
                val isPicked = picked == word
                Text(
                    text = word,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isPicked) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isPicked) SkyBlue else alpacaCard())
                        .border(
                            2.dp,
                            if (isPicked) SkyBlue else alpacaCardBorder(),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { picked = if (isPicked) null else word }
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                )
            }
        }
    }
}

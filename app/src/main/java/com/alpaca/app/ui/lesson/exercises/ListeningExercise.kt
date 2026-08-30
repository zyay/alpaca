package com.alpaca.app.ui.lesson.exercises

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.alpaca.app.audio.TtsSpeaker
import com.alpaca.app.data.content.ListeningExercise
import com.alpaca.app.ui.theme.PacoGreen
import com.alpaca.app.ui.theme.PacoGreenDark
import com.alpaca.app.ui.theme.PaperWhite
import kotlinx.coroutines.launch

@Composable
fun ListeningUi(
    exercise: ListeningExercise,
    tts: TtsSpeaker,
    onCorrect: () -> Unit,
    onWrong: (correctText: String, explanation: String?, mistakeLabel: String) -> Unit,
    onCheckHaptic: (correct: Boolean) -> Unit
) {
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var speaking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val transition = rememberInfiniteTransition(label = "tts")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (speaking) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "pulse"
    )

    fun play(rate: Float) {
        scope.launch {
            speaking = true
            tts.speak(exercise.text, rate)
            speaking = false
        }
    }

    ExerciseScaffold(
        checkEnabled = selected >= 0,
        onCheck = {
            if (selected == exercise.correctIndex) {
                onCheckHaptic(true)
                onCorrect()
            } else {
                onCheckHaptic(false)
                onWrong(
                    "${exercise.text} — ${exercise.options[exercise.correctIndex]}",
                    exercise.explanation,
                    exercise.text
                )
            }
        }
    ) {
        PromptText("What do you hear?")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(PacoGreen)
                    .clickable { play(0.95f) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.VolumeUp,
                    contentDescription = "Play audio",
                    tint = PaperWhite,
                    modifier = Modifier.size(54.dp)
                )
            }
            Box(
                modifier = Modifier
                    .padding(start = 150.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(PacoGreenDark)
                    .clickable { play(0.6f) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "slow",
                    style = MaterialTheme.typography.labelLarge,
                    color = PaperWhite
                )
            }
        }

        exercise.options.forEachIndexed { index, option ->
            OptionCard(
                text = option,
                selected = selected == index,
                enabled = true,
                onClick = { selected = index },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

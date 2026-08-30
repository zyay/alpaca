package com.alpaca.app.ui.lesson.exercises

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.alpaca.app.data.content.MultipleChoiceExercise

@Composable
fun MultipleChoiceUi(
    exercise: MultipleChoiceExercise,
    onCorrect: () -> Unit,
    onWrong: (correctText: String, explanation: String?, mistakeLabel: String) -> Unit,
    onCheckHaptic: (correct: Boolean) -> Unit
) {
    var selected by rememberSaveable { mutableIntStateOf(-1) }

    ExerciseScaffold(
        checkEnabled = selected >= 0,
        onCheck = {
            if (selected == exercise.correctIndex) {
                onCheckHaptic(true)
                onCorrect()
            } else {
                onCheckHaptic(false)
                onWrong(
                    exercise.options[exercise.correctIndex],
                    exercise.explanation,
                    exercise.prompt
                )
            }
        }
    ) {
        PromptText(exercise.prompt)
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

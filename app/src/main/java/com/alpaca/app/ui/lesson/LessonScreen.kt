package com.alpaca.app.ui.lesson

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpaca.app.AlpacaApp
import com.alpaca.app.data.content.FillBlankExercise
import com.alpaca.app.data.content.ListeningExercise
import com.alpaca.app.data.content.MatchPairsExercise
import com.alpaca.app.data.content.MultipleChoiceExercise
import com.alpaca.app.data.content.PronunciationExercise
import com.alpaca.app.di.LocalAppContainer
import com.alpaca.app.ui.components.EnergyHearts
import com.alpaca.app.ui.components.LessonProgressBar
import com.alpaca.app.ui.components.PacoState
import com.alpaca.app.ui.lesson.exercises.FillBlankUi
import com.alpaca.app.ui.lesson.exercises.ListeningUi
import com.alpaca.app.ui.lesson.exercises.MatchPairsUi
import com.alpaca.app.ui.lesson.exercises.MultipleChoiceUi
import com.alpaca.app.ui.lesson.exercises.PronunciationUi
import com.alpaca.app.ui.theme.PacoGreen
import com.alpaca.app.ui.theme.PaperWhite
import com.alpaca.app.util.HapticPlayer

@Composable
fun LessonScreen(
    lessonId: String,
    viewModel: LessonViewModel,
    haptics: HapticPlayer?,
    onFinished: () -> Unit,
    onQuit: () -> Unit
) {
    val container = LocalAppContainer.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val app = LocalContext.current.applicationContext as AlpacaApp

    LaunchedEffect(Unit) {
        viewModel.resultSink = { app.lastLessonResult = it }
        viewModel.start(lessonId)
    }

    LaunchedEffect(state.finished) {
        if (state.finished) onFinished()
    }

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PacoGreen)
        }
        return
    }

    val progress =
        (state.index + (if (state.justCorrect) 1 else 0)).toFloat() / state.total.coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onQuit) {
                Icon(
                    Icons.Filled.Close, "Quit lesson",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LessonProgressBar(
                fraction = progress,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            )
            EnergyHearts(energy = state.energy)
        }

        Spacer(Modifier.height(20.dp))

        Box(modifier = Modifier.weight(1f)) {
            val exercise = state.exercise
            if (exercise != null) {
                androidx.compose.runtime.key(state.index, state.attempt) {
                    when (exercise) {
                        is MultipleChoiceExercise -> MultipleChoiceUi(
                            exercise,
                            onCorrect = { haptics?.correctThud(); viewModel.onCorrect() },
                            onWrong = viewModel::onWrong,
                            onCheckHaptic = { correct ->
                                if (correct) haptics?.correctThud() else haptics?.wrongBuzz()
                            }
                        )
                        is MatchPairsExercise -> MatchPairsUi(
                            exercise,
                            haptics = haptics,
                            onComplete = { haptics?.correctThud(); viewModel.onCorrect() }
                        )
                        is FillBlankExercise -> FillBlankUi(
                            exercise,
                            onCorrect = { haptics?.correctThud(); viewModel.onCorrect() },
                            onWrong = viewModel::onWrong,
                            onCheckHaptic = { correct ->
                                if (correct) haptics?.correctThud() else haptics?.wrongBuzz()
                            }
                        )
                        is ListeningExercise -> ListeningUi(
                            exercise,
                            tts = container.ttsSpeaker,
                            onCorrect = { haptics?.correctThud(); viewModel.onCorrect() },
                            onWrong = viewModel::onWrong,
                            onCheckHaptic = { correct ->
                                if (correct) haptics?.correctThud() else haptics?.wrongBuzz()
                            }
                        )
                        is PronunciationExercise -> PronunciationUi(
                            exercise,
                            grader = container.pronunciationGrader,
                            onCorrect = { haptics?.correctThud(); viewModel.onCorrect() }
                        )
                    }
                }
            }
        }
    }

    // Success banner.
    AnimatedVisibility(
        visible = state.justCorrect,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PacoGreen)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "¡Muy bien!",
                style = MaterialTheme.typography.headlineMedium,
                color = PaperWhite
            )
        }
    }

    // Spit-take correction overlay.
    state.correction?.let { correction ->
        LaunchedEffect(correction) { haptics?.wrongBuzz() }
        CorrectionOverlay(correction = correction, onContinue = viewModel::dismissCorrection)
    }
}

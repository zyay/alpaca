package com.alpaca.app.ui.lesson.exercises

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alpaca.app.audio.PronunciationGrader
import com.alpaca.app.data.content.PronunciationExercise
import com.alpaca.app.ui.components.PillButton
import com.alpaca.app.ui.theme.DangerRed
import com.alpaca.app.ui.theme.InkMid
import com.alpaca.app.ui.theme.BrandGreen
import com.alpaca.app.ui.theme.PaperWhite
import com.alpaca.app.ui.theme.SkyBlue
import kotlinx.coroutines.launch

private sealed class MicPhase {
    data object Idle : MicPhase()
    data object Listening : MicPhase()
    data class Result(val recognized: String?, val score: Float) : MicPhase()
    data object Unavailable : MicPhase()
}

@Composable
fun PronunciationUi(
    exercise: PronunciationExercise,
    grader: PronunciationGrader,
    languageTag: String,
    languageName: String,
    onCorrect: () -> Unit
) {
    var phase by remember { mutableStateOf<MicPhase>(MicPhase.Idle) }
    val scope = rememberCoroutineScope()

    val transition = rememberInfiniteTransition(label = "mic")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (phase is MicPhase.Listening) 1.15f else 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "mic-pulse"
    )

    fun listen() {
        scope.launch {
            phase = MicPhase.Listening
            val result = grader.listenAndGrade(exercise.expected, languageTag)
            phase = if (result == null) {
                MicPhase.Unavailable
            } else {
                MicPhase.Result(result.recognized, result.score)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PromptText("Say it in $languageName")
        Text(
            text = exercise.expected,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Text(
            text = exercise.translation,
            style = MaterialTheme.typography.bodyMedium,
            color = InkMid
        )
        Spacer(Modifier.height(24.dp))

        when (val p = phase) {
            MicPhase.Idle -> {
                MicButton(pulse, listening = false) { listen() }
                Text(
                    text = "Tap the mic and say the phrase",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMid,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            MicPhase.Listening -> {
                MicButton(pulse, listening = true) { }
                Text(
                    text = "Listening…",
                    style = MaterialTheme.typography.titleMedium,
                    color = DangerRed,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            is MicPhase.Result -> {
                val passed = p.score >= exercise.tolerance
                Text(
                    text = if (passed) "¡Muy bien!" else "Almost…",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (passed) BrandGreen else DangerRed
                )
                Text(
                    text = "You said: \"${p.recognized ?: "…"}\"  ·  match ${(p.score * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMid,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(Modifier.height(20.dp))
                if (passed) {
                    PillButton(text = "Continue", onClick = onCorrect)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PillButton(
                            text = "Try again",
                            onClick = {
                                phase = MicPhase.Idle
                                listen()
                            },
                            fillWidth = false,
                            color = SkyBlue
                        )
                        PillButton(
                            text = "Skip",
                            onClick = onCorrect, // skip advances without penalty
                            fillWidth = false,
                            color = Color(0xFFB0B0B0)
                        )
                    }
                }
            }

            MicPhase.Unavailable -> {
                Text(
                    text = "Speech recognition isn't available on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMid,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                PillButton(text = "Skip ahead", onClick = onCorrect, color = SkyBlue)
            }
        }
    }
}

@Composable
private fun MicButton(pulse: Float, listening: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .scale(pulse)
            .clip(CircleShape)
            .background(if (listening) DangerRed else BrandGreen)
            .clickable(enabled = !listening) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Mic,
            contentDescription = "Record pronunciation",
            tint = PaperWhite,
            modifier = Modifier.size(44.dp)
        )
    }
}

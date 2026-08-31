package com.alpaca.app.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpaca.app.data.coach.CoachClient
import com.alpaca.app.gemini.VoiceSessionState
import com.alpaca.app.ui.components.PillButton
import com.alpaca.app.ui.theme.alpacaCardBorder
import com.alpaca.app.ui.theme.alpacaCard
import com.alpaca.app.ui.theme.alpacaSecondaryText
import com.alpaca.app.ui.theme.DangerRed
import com.alpaca.app.ui.theme.BrandGreen
import com.alpaca.app.ui.theme.BrandGreenPale
import com.alpaca.app.ui.theme.PaperWhite
import com.alpaca.app.ui.theme.SkyBlue

@Composable
fun VoiceCallScreen(
    viewModel: VoiceCallViewModel,
    onBack: () -> Unit
) {
    var activeScenario by remember { mutableStateOf<VoiceCallViewModel.Scenario?>(null) }
    var retryScenario by remember { mutableStateOf<VoiceCallViewModel.Scenario?>(null) }
    val coachState by viewModel.coachState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val scenario = activeScenario
        if (granted && scenario != null) {
            viewModel.start(scenario)
        } else if (!granted) {
            activeScenario = null
        }
    }

    fun launch(scenario: VoiceCallViewModel.Scenario) {
        activeScenario = scenario
        val hasMic = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasMic) {
            viewModel.start(scenario)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val scenario = activeScenario
    when {
        scenario != null -> CallScreen(
            scenario = scenario,
            viewModel = viewModel,
            onEnd = {
                retryScenario = scenario
                viewModel.finishAndCoach()
                activeScenario = null
            },
            onBack = {
                viewModel.end()
                viewModel.resetCoach()
                onBack()
            }
        )
        coachState != VoiceCallViewModel.CoachUiState.Idle -> PostCallFeedback(
            viewModel = viewModel,
            onPracticeAgain = {
                val retry = retryScenario
                viewModel.resetCoach()
                if (retry != null) launch(retry)
            },
            onDone = {
                viewModel.resetCoach()
            }
        )
        else -> ScenarioPicker(
            scenarios = viewModel.scenarios,
            settings = viewModel.settings.collectAsStateWithLifecycle().value,
            onPickLevel = viewModel::setLevel,
            onPickVoice = viewModel::setVoice,
            onPick = ::launch,
            onBack = {
                viewModel.resetCoach()
                onBack()
            }
        )
    }
}

@Composable
private fun ScenarioPicker(
    scenarios: List<VoiceCallViewModel.Scenario>,
    settings: com.alpaca.app.data.datastore.UserPrefs,
    onPickLevel: (VoiceCallViewModel.Level) -> Unit,
    onPickVoice: (String) -> Unit,
    onPick: (VoiceCallViewModel.Scenario) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text("Real-World Simulator", style = MaterialTheme.typography.headlineMedium)
        }
        Text(
            text = "Call a character and practice speaking. Play everyone — interrupt any time!",
            style = MaterialTheme.typography.bodyMedium,
            color = alpacaSecondaryText()
        )

        Spacer(Modifier.height(16.dp))
        Text("Your level", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoiceCallViewModel.Level.entries.forEach { level ->
                val selected = level.id == settings.voiceLevel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) BrandGreen else alpacaCard())
                        .border(
                            2.dp,
                            if (selected) BrandGreen else alpacaCardBorder(),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onPickLevel(level) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = level.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) PaperWhite else alpacaSecondaryText()
                    )
                }
            }
        }
        Text(
            text = VoiceCallViewModel.Level.fromId(settings.voiceLevel).blurb,
            style = MaterialTheme.typography.bodySmall,
            color = alpacaSecondaryText(),
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(Modifier.height(14.dp))
        Text("Tutor voice", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.alpaca.app.gemini.GeminiLiveClient.availableVoices.forEach { voice ->
                val selected = voice == settings.voiceName
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) SkyBlue else alpacaCard())
                        .border(
                            2.dp,
                            if (selected) SkyBlue else alpacaCardBorder(),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onPickVoice(voice) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = voice,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) PaperWhite else alpacaSecondaryText()
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        scenarios.forEach { scenario ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(alpacaCard())
                    .border(2.dp, alpacaCardBorder(), RoundedCornerShape(18.dp))
                    .clickable { onPick(scenario) }
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(scenario.emoji, style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(scenario.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            scenario.blurb,
                            style = MaterialTheme.typography.bodyMedium,
                            color = alpacaSecondaryText()
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Calls run through the Gemini Live API with short-lived backend tokens. " +
                "After each call your AI coach reviews the transcript.",
            style = MaterialTheme.typography.bodyMedium,
            color = alpacaSecondaryText(),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CallScreen(
    scenario: VoiceCallViewModel.Scenario,
    viewModel: VoiceCallViewModel,
    onEnd: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.sessionState.collectAsStateWithLifecycle()
    val level by viewModel.playbackLevel.collectAsStateWithLifecycle()
    val noCredentials by viewModel.noCredentials.collectAsStateWithLifecycle()
    val transcript by viewModel.transcript.collectAsStateWithLifecycle()
    val sessionEnding by viewModel.sessionEnding.collectAsStateWithLifecycle()
    var muted by remember { mutableStateOf(false) }

    val transition = rememberInfiniteTransition(label = "ring")
    val ringPulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (state is VoiceSessionState.Speaking) 1.08f else 1.03f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "ring"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF10241A))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    "End and go back",
                    tint = PaperWhite
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = scenario.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = PaperWhite
                )
                Text(
                    text = statusLabel(state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandGreenPale
                )
            }
        }

        if (sessionEnding && state !is VoiceSessionState.Error) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Reconnecting soon — wrap up your thought",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFE28A)
            )
        }

        Spacer(Modifier.weight(0.5f))

        // Gradient orb with the scenario emoji and a breathing ring.
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size((220f * ringPulse + level * 90f).dp)
                    .clip(CircleShape)
                    .background(BrandGreen.copy(alpha = 0.16f))
            )
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF2E8B3F), Color(0xFF1B3A29))
                        )
                    )
                    .border(3.dp, Color(0xFF3FA955), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = scenario.emoji,
                    fontSize = 84.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Waveform(level = if (state is VoiceSessionState.Speaking) level else 0f)

        Spacer(Modifier.height(12.dp))

        // Live captions: tutor line above, learner line below.
        val tutorLine = transcript.lastOrNull { it.isTutor }?.text
        val learnerLine = transcript.lastOrNull { !it.isTutor }?.text
        if (tutorLine != null) {
            Text(
                text = tutorLine,
                style = MaterialTheme.typography.titleMedium,
                color = PaperWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (learnerLine != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "You: $learnerLine",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandGreenPale,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(8.dp))

        when (val s = state) {
            is VoiceSessionState.Error -> {
                Text(
                    text = s.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFB4B4),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.height(12.dp))
                PillButton(
                    text = "Try again",
                    onClick = { viewModel.start(scenario) },
                    color = SkyBlue,
                    fillWidth = false
                )
            }
            else -> Unit
        }

        if (noCredentials) {
            Text(
                text = "No live credentials available. Either the Alpaca token backend is unreachable " +
                    "or no GEMINI_API_KEY is set locally for development.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFE28A),
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.weight(1f))

        // Call controls.
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (muted) Color(0xFF3A3A3A) else Color(0xFF2E5940))
                    .clickable { muted = viewModel.toggleMute() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    if (muted) "Unmute" else "Mute",
                    tint = PaperWhite,
                    modifier = Modifier.size(30.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(DangerRed)
                    .clickable { onEnd() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.CallEnd,
                    "End call",
                    tint = PaperWhite,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.size(64.dp)) // balance the row
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PostCallFeedback(
    viewModel: VoiceCallViewModel,
    onPracticeAgain: () -> Unit,
    onDone: () -> Unit
) {
    val coachState by viewModel.coachState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Session debrief",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Your AI coach reviewed the conversation.",
            style = MaterialTheme.typography.bodyMedium,
            color = alpacaSecondaryText()
        )
        Spacer(Modifier.height(20.dp))

        when (val s = coachState) {
            VoiceCallViewModel.CoachUiState.Idle -> Unit
            VoiceCallViewModel.CoachUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandGreen)
                }
                Text(
                    text = "Listening back to your call…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = alpacaSecondaryText(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
            }
            is VoiceCallViewModel.CoachUiState.Ready -> {
                if (s.feedback.strengths.isNotEmpty()) {
                    Text("What went well", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    s.feedback.strengths.forEach { strength ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = strength,
                                style = MaterialTheme.typography.bodyMedium,
                                color = alpacaSecondaryText()
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
                if (s.feedback.improvements.isNotEmpty()) {
                    Text("Level up next call", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    s.feedback.improvements.forEachIndexed { index, tip ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(BrandGreen.copy(alpha = 0.08f))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "${index + 1}. ${tip.title}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = tip.tip,
                                style = MaterialTheme.typography.bodyMedium,
                                color = alpacaSecondaryText()
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
                if (s.feedback.vocab.isNotEmpty()) {
                    Text("Words to remember", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    s.feedback.vocab.forEach { item ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = item.term,
                                style = MaterialTheme.typography.titleSmall,
                                color = BrandGreen
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = item.translation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = alpacaSecondaryText()
                            )
                        }
                    }
                }
            }
            VoiceCallViewModel.CoachUiState.Unavailable -> {
                Text(
                    text = "Coaching is unavailable right now — the debrief service needs the " +
                        "Alpaca backend with a Gemini key, and the call needs a few spoken lines.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = alpacaSecondaryText()
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(20.dp))
        PillButton(text = "Practice again", onClick = onPracticeAgain)
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(100.dp))
                .background(alpacaCardBorder().copy(alpha = 0.35f))
                .clickable { onDone() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Back to scenarios",
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun statusLabel(state: VoiceSessionState): String = when (state) {
    VoiceSessionState.Idle -> "Idle"
    VoiceSessionState.Connecting -> "Connecting…"
    VoiceSessionState.Listening -> "Listening… speak up!"
    VoiceSessionState.Speaking -> "The tutor is talking… interrupt!"
    is VoiceSessionState.Error -> "Something went wrong"
}

@Composable
private fun Waveform(level: Float) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(44.dp)
    ) {
        val weights = listOf(0.5f, 0.8f, 1f, 0.7f, 1f, 0.85f, 0.5f)
        weights.forEachIndexed { index, w ->
            val h = (8f + level * 90f * w * if (index % 2 == 0) 1f else 0.75f).dp
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(h)
                    .clip(RoundedCornerShape(100.dp))
                    .background(BrandGreen.copy(alpha = 0.55f + level * 0.45f))
            )
        }
    }
}

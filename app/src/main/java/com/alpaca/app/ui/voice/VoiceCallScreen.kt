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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpaca.app.BuildConfig
import com.alpaca.app.gemini.VoiceSessionState
import com.alpaca.app.ui.components.PacoCharacter
import com.alpaca.app.ui.components.PacoState
import com.alpaca.app.ui.components.PillButton
import com.alpaca.app.ui.theme.CloudGray
import com.alpaca.app.ui.theme.DangerRed
import com.alpaca.app.ui.theme.InkMid
import com.alpaca.app.ui.theme.PacoGreen
import com.alpaca.app.ui.theme.PacoGreenLight
import com.alpaca.app.ui.theme.PaperWhite
import com.alpaca.app.ui.theme.SkyBlue

@Composable
fun VoiceCallScreen(
    viewModel: VoiceCallViewModel,
    onBack: () -> Unit
) {
    var activeScenario by remember { mutableStateOf<VoiceCallViewModel.Scenario?>(null) }
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
    if (scenario == null) {
        ScenarioPicker(
            scenarios = viewModel.scenarios,
            onPick = ::launch,
            onBack = onBack
        )
    } else {
        CallScreen(
            scenario = scenario,
            viewModel = viewModel,
            onEnd = {
                viewModel.end()
                activeScenario = null
            },
            onBack = {
                viewModel.end()
                onBack()
            }
        )
    }
}

@Composable
private fun ScenarioPicker(
    scenarios: List<VoiceCallViewModel.Scenario>,
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
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Call a character and practice speaking. Paco plays everyone — interrupt him any time!",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMid
        )
        Spacer(Modifier.height(20.dp))
        scenarios.forEach { scenario ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(2.dp, CloudGray, RoundedCornerShape(18.dp))
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
                            color = InkMid
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Voice calls run through the Gemini Live API with short-lived backend tokens.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMid,
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
                    color = PacoGreenLight
                )
            }
        }

        Spacer(Modifier.weight(0.5f))

        // Avatar with breathing ring.
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size((220f * ringPulse + level * 90f).dp)
                    .clip(CircleShape)
                    .background(PacoGreen.copy(alpha = 0.16f))
            )
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B3A29)),
                contentAlignment = Alignment.Center
            ) {
                PacoCharacter(
                    state = when (state) {
                        is VoiceSessionState.Speaking -> PacoState.HAPPY
                        is VoiceSessionState.Error -> PacoState.SAD
                        else -> PacoState.IDLE
                    },
                    modifier = Modifier.size(150.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Waveform(level = if (state is VoiceSessionState.Speaking) level else 0f)

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

private fun statusLabel(state: VoiceSessionState): String = when (state) {
    VoiceSessionState.Idle -> "Idle"
    VoiceSessionState.Connecting -> "Connecting…"
    VoiceSessionState.Listening -> "¡Te escucha! Speak in Spanish…"
    VoiceSessionState.Speaking -> "Paco is talking… interrupt him!"
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
                    .background(PacoGreen.copy(alpha = 0.55f + level * 0.45f))
            )
        }
    }
}

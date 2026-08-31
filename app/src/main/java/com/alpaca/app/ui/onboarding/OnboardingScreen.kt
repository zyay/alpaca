package com.alpaca.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alpaca.app.di.AppContainer
import com.alpaca.app.ui.components.PacoCharacter
import com.alpaca.app.ui.components.PacoState
import com.alpaca.app.ui.components.PillButton
import com.alpaca.app.ui.theme.CloudGray
import com.alpaca.app.ui.theme.InkMid
import com.alpaca.app.ui.theme.PacoGreen
import com.alpaca.app.ui.theme.SunYellow
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    container: AppContainer,
    onDone: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun saveAndContinue() {
        scope.launch {
            if (name.isNotBlank()) container.prefs.setDisplayName(name)
            container.prefs.setOnboarded()
        }
        onDone()
    }

    val pacoSays = if (name.isBlank()) {
        "¡Hola! I'm Paco. Your fleece grows back fast, my hat stays on."
    } else {
        "¡Mucho gusto, $name! Let's get you talking."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(36.dp))
        PacoCharacter(state = PacoState.HAPPY, modifier = Modifier.size(230.dp))
        Spacer(Modifier.height(12.dp))
        Text("¡Hola! I'm Paco", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(6.dp))
        Text(
            text = pacoSays,
            style = MaterialTheme.typography.bodyLarge,
            color = InkMid,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SunYellow.copy(alpha = 0.12f))
                .padding(14.dp)
        ) {
            Text(
                text = "Bite-sized lessons. Real conversations. Zero fear of mistakes — " +
                    "if you slip, I just spit a little. That's my job.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMid,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(24.dp))
        TextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("What should I call you?") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { saveAndContinue() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = PacoGreen,
                unfocusedIndicatorColor = CloudGray
            ),
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        PillButton(
            text = "¡Empezar!",
            onClick = { saveAndContinue() }
        )
    }
}

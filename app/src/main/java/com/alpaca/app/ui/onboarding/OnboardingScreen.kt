package com.alpaca.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alpaca.app.di.AppContainer
import com.alpaca.app.ui.components.GreetingWordmark
import com.alpaca.app.ui.components.PillButton
import com.alpaca.app.ui.theme.alpacaCardBorder
import com.alpaca.app.ui.theme.alpacaSecondaryText
import com.alpaca.app.ui.theme.BrandGreen
import com.alpaca.app.ui.theme.SunYellow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

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

    val hello = if (name.isBlank()) {
        "Bite-sized lessons in seven languages. Real conversations, zero fear of mistakes."
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
        FloatingFlagChips()
        Spacer(Modifier.height(16.dp))
        GreetingWordmark(
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = hello,
            style = MaterialTheme.typography.bodyLarge,
            color = alpacaSecondaryText(),
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
                text = "Slip up and a splash shows you exactly why — so the fix sticks on the spot.",
                style = MaterialTheme.typography.bodyMedium,
                color = alpacaSecondaryText(),
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
                focusedIndicatorColor = BrandGreen,
                unfocusedIndicatorColor = alpacaCardBorder()
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

@Composable
private fun FloatingFlagChips() {
    val flags = listOf("🇪🇸", "🇫🇷", "🇩🇪", "🇮🇹", "🇵🇹", "🇺🇸", "🇷🇺")
    val transition = rememberInfiniteTransition(label = "flags")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flag-bob"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        flags.forEachIndexed { i, flag ->
            val dy = sin(bob + i * (2f * PI.toFloat() / flags.size)) * 7f
            Text(
                text = flag,
                fontSize = 32.sp,
                modifier = Modifier.offset { IntOffset(0, dy.roundToInt()) }
            )
        }
    }
}

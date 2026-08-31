package com.alpaca.app.ui.lesson.exercises

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alpaca.app.data.content.MatchPairsExercise
import com.alpaca.app.ui.theme.alpacaCardBorder
import com.alpaca.app.ui.theme.DangerRed
import com.alpaca.app.ui.theme.BrandGreen
import com.alpaca.app.ui.theme.BrandGreenPale
import com.alpaca.app.ui.theme.SkyBlue
import com.alpaca.app.util.HapticPlayer
import kotlinx.coroutines.delay

@Composable
fun MatchPairsUi(
    exercise: MatchPairsExercise,
    haptics: HapticPlayer?,
    onComplete: () -> Unit
) {
    val left = remember { exercise.pairs.map { it[0] }.shuffled() }
    val right = remember { exercise.pairs.map { it[1] }.shuffled() }
    val pairMap = remember { exercise.pairs.associate { it[0] to it[1] } }

    var selectedLeft by remember { mutableIntStateOf(-1) }
    var selectedRight by remember { mutableIntStateOf(-1) }
    val matchedLeft = remember { mutableSetOf<String>() }
    val matchedRight = remember { mutableSetOf<String>() }
    var wrongFlash by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    LaunchedEffect(wrongFlash) {
        if (wrongFlash != null) {
            delay(450)
            wrongFlash = null
        }
    }

    fun trySelect() {
        val li = selectedLeft
        val ri = selectedRight
        if (li < 0 || ri < 0) return
        val leftWord = left[li]
        val rightWord = right[ri]
        if (pairMap[leftWord] == rightWord) {
            matchedLeft.add(leftWord)
            matchedRight.add(rightWord)
            haptics?.light()
            selectedLeft = -1
            selectedRight = -1
            if (matchedLeft.size == exercise.pairs.size) {
                onComplete()
            }
        } else {
            haptics?.wrongBuzz()
            wrongFlash = li to ri
            selectedLeft = -1
            selectedRight = -1
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PromptText("Tap the matching pairs")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                left.forEachIndexed { index, word ->
                    PairTile(
                        text = word,
                        selected = selectedLeft == index,
                        matched = matchedLeft.contains(word),
                        wrong = wrongFlash?.first == index,
                        onClick = {
                            if (matchedLeft.contains(word)) return@PairTile
                            selectedLeft = if (selectedLeft == index) -1 else index
                            trySelect()
                        }
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                right.forEachIndexed { index, word ->
                    PairTile(
                        text = word,
                        selected = selectedRight == index,
                        matched = matchedRight.contains(word),
                        wrong = wrongFlash?.second == index,
                        onClick = {
                            if (matchedRight.contains(word)) return@PairTile
                            selectedRight = if (selectedRight == index) -1 else index
                            trySelect()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PairTile(
    text: String,
    selected: Boolean,
    matched: Boolean,
    wrong: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val border by animateColorAsState(
        when {
            wrong -> DangerRed
            matched -> BrandGreen
            selected -> SkyBlue
            else -> alpacaCardBorder()
        },
        label = "tile-border"
    )
    val background by animateColorAsState(
        when {
            matched -> BrandGreenPale
            selected -> Color(0xFFDDF1FF)
            else -> Color.White
        },
        label = "tile-bg"
    )
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        color = if (matched) BrandGreen else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(2.dp, border, shape)
            .clickable(
                enabled = !matched,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 8.dp, vertical = 14.dp)
    )
}

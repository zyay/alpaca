package com.alpaca.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpaca.app.data.content.CourseLanguage
import com.alpaca.app.ui.theme.InkMid
import com.alpaca.app.ui.theme.BrandGreen
import com.alpaca.app.ui.theme.SunYellow

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenCourses: () -> Unit,
    onOpenAchievements: () -> Unit,
    onBack: () -> Unit
) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(16.dp))

        SettingsCard {
            SettingRow("Sound effects", prefs.soundEnabled, viewModel::setSound)
            SettingRow("Haptic feedback", prefs.hapticsEnabled, viewModel::setHaptics)
            SettingRow(
                "Material You colors",
                prefs.dynamicColor,
                viewModel::setDynamicColor
            )
        }

        Spacer(Modifier.height(14.dp))

        SettingsCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAchievements)
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = "Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "🏅",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open achievements",
                    tint = InkMid
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenCourses)
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = "Course",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = CourseLanguage.byId(prefs.currentLanguage).flagEmoji,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Change course",
                    tint = InkMid
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Alpaca Max", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Unlimited fleece, unlimited voice chats, no ads. (Preview toggle — " +
                            "billing ships with the Play Store release.)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMid
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = prefs.alpacaMax,
                    onCheckedChange = viewModel::setMax,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SunYellow
                    )
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Alpaca v0.3.0 · Learn loud. Travel far.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMid
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp),
        content = { content() }
    )
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BrandGreen
            )
        )
    }
}

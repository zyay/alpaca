package com.alpaca.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.alpaca.app.BuildConfig
import com.alpaca.app.data.content.CourseLanguage
import com.alpaca.app.ui.components.PillButton
import com.alpaca.app.ui.theme.CloudGray
import com.alpaca.app.ui.theme.InkFaint
import com.alpaca.app.ui.theme.InkMid
import com.alpaca.app.ui.theme.BrandGreen
import com.alpaca.app.ui.theme.SunYellow
import android.app.Activity

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenCourses: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenAccount: () -> Unit,
    onBack: () -> Unit
) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val billing by viewModel.billing.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

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
            if (prefs.signedIn) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BrandGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (prefs.authName.ifBlank { "L" }).take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = prefs.authName.ifBlank { "Learner" },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = prefs.authEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMid
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                PillButton(
                    text = "Log out",
                    onClick = viewModel::signOut,
                    color = CloudGray,
                    textColor = InkMid
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAccount)
                        .padding(vertical = 6.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Account", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Sign in or create one — keeps your league identity",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMid
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open account",
                        tint = InkMid
                    )
                }
            }
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
                        text = if (prefs.alpacaMax) {
                            "Unlimited fleece energy is active. Happy trails!"
                        } else {
                            "Unlimited fleece energy — never wait for hearts again."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMid
                    )
                }
                if (BuildConfig.DEBUG) {
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
            if (!prefs.alpacaMax) {
                Spacer(Modifier.height(12.dp))
                PillButton(
                    text = when {
                        billing.connected && billing.priceText != null ->
                            "Upgrade · ${billing.priceText}/month"
                        billing.connected -> "Upgrade to Max"
                        else -> "Available on Google Play"
                    },
                    enabled = billing.connected,
                    color = SunYellow,
                    textColor = Color(0xFF3B3000),
                    onClick = {
                        val activity = context as? Activity
                        if (activity == null || !viewModel.buyMax(activity)) {
                            viewModel.restorePurchases()
                        }
                    }
                )
            }
            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Debug build: the toggle previews Max without billing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Alpaca v0.6.0 · Learn loud. Travel far.",
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

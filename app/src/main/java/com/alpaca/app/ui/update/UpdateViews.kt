package com.alpaca.app.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpaca.app.ui.components.PillButton
import com.alpaca.app.ui.theme.alpacaCardBorder
import com.alpaca.app.ui.theme.alpacaSecondaryText
import com.alpaca.app.ui.theme.BrandGreen
import com.alpaca.app.ui.theme.SkyBlue

@Composable
fun UpdateBanner(viewModel: UpdateViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    when (val s = state) {
        is UpdateViewModel.UpdateState.Available -> CompactBanner(
            icon = { Icon(Icons.Filled.CloudDownload, null, tint = BrandGreen, modifier = Modifier.size(22.dp)) },
            title = "Alpaca ${s.release.tagName.removePrefix("v")} is out",
            subtitle = "Tap to download the new version"
        ) {
            viewModel.download(context)
        }
        is UpdateViewModel.UpdateState.Downloading -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BrandGreen.copy(alpha = 0.08f))
                .padding(14.dp)
        ) {
            Text(
                text = "Downloading update… ${s.percent}%",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { s.percent / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = BrandGreen
            )
        }
        is UpdateViewModel.UpdateState.ReadyToInstall -> CompactBanner(
            icon = { Icon(Icons.Filled.InstallMobile, null, tint = SkyBlue, modifier = Modifier.size(22.dp)) },
            title = "Ready to install ${s.release.tagName.removePrefix("v")}",
            subtitle = "Android will ask to confirm the install"
        ) {
            viewModel.install(context)
        }
        else -> Unit
    }
}

@Composable
private fun CompactBanner(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    action: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BrandGreen.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = alpacaSecondaryText(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        PillButton(
            text = if (title.startsWith("Ready")) "Install" else "Update",
            onClick = action,
            color = BrandGreen,
            fillWidth = false
        )
    }
}

/** Full detail block for the Settings screen. */
@Composable
fun UpdateSettingsSection(viewModel: UpdateViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("App version", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text(
                text = "v${com.alpaca.app.BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = alpacaSecondaryText()
            )
        }
        Spacer(Modifier.height(10.dp))

        when (val s = state) {
            UpdateViewModel.UpdateState.Idle, UpdateViewModel.UpdateState.Checking -> Text(
                text = "Checking for updates…",
                style = MaterialTheme.typography.bodySmall,
                color = alpacaSecondaryText()
            )
            UpdateViewModel.UpdateState.UpToDate -> Text(
                text = "You're on the latest version.",
                style = MaterialTheme.typography.bodySmall,
                color = alpacaSecondaryText()
            )
            is UpdateViewModel.UpdateState.Available -> {
                Text(
                    text = "Update available: ${s.release.tagName.removePrefix("v")}",
                    style = MaterialTheme.typography.bodyMedium
                )
                s.release.body?.take(300)?.let { notes ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = notes.lineFirst(),
                        style = MaterialTheme.typography.bodySmall,
                        color = alpacaSecondaryText(),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(10.dp))
                PillButton(
                    text = "Download update",
                    onClick = { viewModel.download(context) },
                    color = BrandGreen
                )
            }
            is UpdateViewModel.UpdateState.Downloading -> Column {
                Text(
                    text = "Downloading… ${s.percent}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = alpacaSecondaryText()
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { s.percent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = BrandGreen
                )
            }
            is UpdateViewModel.UpdateState.ReadyToInstall -> {
                Text(
                    text = "Alpaca ${s.release.tagName.removePrefix("v")} downloaded.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
                PillButton(
                    text = "Install now",
                    onClick = { viewModel.install(context) },
                    color = SkyBlue
                )
            }
            is UpdateViewModel.UpdateState.Failed -> {
                Text(
                    text = s.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(
                        text = "Check again",
                        onClick = { viewModel.check(manual = true) },
                        color = alpacaCardBorder(),
                        fillWidth = false
                    )
                    if (s.release != null) {
                        PillButton(
                            text = "Retry download",
                            onClick = { viewModel.download(context) },
                            color = BrandGreen,
                            fillWidth = false
                        )
                    }
                }
            }
        }
    }
}

private fun String.lineFirst(): String =
    lines().firstOrNull { it.isNotBlank() }?.removePrefix("#")?.trim() ?: ""

package com.alpaca.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpaca.app.data.datastore.UserPrefs
import com.alpaca.app.di.AlpacaViewModelFactory
import com.alpaca.app.di.LocalAppContainer
import com.alpaca.app.di.LocalViewModelFactory
import com.alpaca.app.navigation.AppNavHost
import com.alpaca.app.ui.components.PacoCharacter
import com.alpaca.app.ui.components.PacoState
import com.alpaca.app.ui.theme.AlpacaTheme
import com.alpaca.app.util.HapticPlayer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as AlpacaApp
        setContent {
            AlpacaTheme {
                val view = LocalView.current
                val haptics = remember { HapticPlayer(view) }
                val factory = remember { AlpacaViewModelFactory(app.container) }
                val prefs: UserPrefs? by app.container.prefs.prefs
                    .collectAsStateWithLifecycle(initialValue = null)

                val loaded = prefs
                if (loaded == null) {
                    LoadingSplash()
                } else {
                    CompositionLocalProvider(
                        LocalAppContainer provides app.container,
                        LocalViewModelFactory provides factory
                    ) {
                        AppNavHost(app = app, haptics = haptics, onboarded = loaded.onboarded)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingSplash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        PacoCharacter(state = PacoState.IDLE, modifier = Modifier.fillMaxSize(0.45f))
    }
}

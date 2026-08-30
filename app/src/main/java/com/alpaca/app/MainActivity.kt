package com.alpaca.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.alpaca.app.di.AlpacaViewModelFactory
import com.alpaca.app.di.LocalAppContainer
import com.alpaca.app.di.LocalViewModelFactory
import com.alpaca.app.navigation.AppNavHost
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
                CompositionLocalProvider(
                    LocalAppContainer provides app.container,
                    LocalViewModelFactory provides factory
                ) {
                    AppNavHost(app = app, haptics = haptics)
                }
            }
        }
    }
}

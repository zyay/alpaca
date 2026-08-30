package com.alpaca.app.di

import androidx.compose.runtime.compositionLocalOf

val LocalAppContainer = compositionLocalOf<AppContainer> { error("AppContainer not provided") }
val LocalViewModelFactory = compositionLocalOf<AlpacaViewModelFactory> { error("ViewModelFactory not provided") }

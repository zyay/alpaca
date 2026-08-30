package com.alpaca.app.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.alpaca.app.audio.PlatformAudioEngine
import com.alpaca.app.audio.PronunciationGrader
import com.alpaca.app.audio.TtsSpeaker
import com.alpaca.app.data.content.ContentRepository
import com.alpaca.app.data.datastore.UserPreferencesStore
import com.alpaca.app.data.db.AlpacaDatabase
import com.alpaca.app.data.repository.GamificationRepository
import com.alpaca.app.data.repository.ProgressRepository
import com.alpaca.app.gemini.GeminiLiveClient

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database = AlpacaDatabase.create(appContext)
    val prefs = UserPreferencesStore(appContext)
    val contentRepository = ContentRepository(appContext)
    val progressRepository = ProgressRepository(database, contentRepository)
    val gamificationRepository = GamificationRepository(database)

    val ttsSpeaker = TtsSpeaker(appContext)
    val audioEngine = PlatformAudioEngine()
    val pronunciationGrader = PronunciationGrader(appContext)
    val geminiClient = GeminiLiveClient(audioEngine)

    suspend fun init() {
        progressRepository.seedIfNeeded()
        gamificationRepository.currentUser() // ensures the user row exists
    }
}

class AlpacaViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val ctor = modelClass.constructors.firstOrNull { it.parameterCount == 1 }
            ?: throw IllegalArgumentException("No single-arg constructor for ${modelClass.name}")
        return ctor.newInstance(container) as T
    }
}

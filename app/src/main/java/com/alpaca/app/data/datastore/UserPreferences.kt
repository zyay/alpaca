package com.alpaca.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alpaca_prefs")

data class UserPrefs(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val displayName: String = "Explorer",
    val alpacaMax: Boolean = false
)

class UserPreferencesStore(private val context: Context) {
    private object Keys {
        val SOUND = booleanPreferencesKey("sound_enabled")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val NAME = stringPreferencesKey("display_name")
        val MAX = booleanPreferencesKey("alpaca_max")
    }

    val prefs: Flow<UserPrefs> = context.dataStore.data.map { p ->
        UserPrefs(
            soundEnabled = p[Keys.SOUND] ?: true,
            hapticsEnabled = p[Keys.HAPTICS] ?: true,
            displayName = p[Keys.NAME] ?: "Explorer",
            alpacaMax = p[Keys.MAX] ?: false
        )
    }

    suspend fun setSound(enabled: Boolean) = context.dataStore.edit { it[Keys.SOUND] = enabled }
    suspend fun setHaptics(enabled: Boolean) = context.dataStore.edit { it[Keys.HAPTICS] = enabled }
    suspend fun setDisplayName(name: String) = context.dataStore.edit { it[Keys.NAME] = name.trim() }
    suspend fun setAlpacaMax(enabled: Boolean) = context.dataStore.edit { it[Keys.MAX] = enabled }
}

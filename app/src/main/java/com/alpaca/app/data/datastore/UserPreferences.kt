package com.alpaca.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alpaca_prefs")

data class UserPrefs(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val displayName: String = "Explorer",
    val alpacaMax: Boolean = false,
    val onboarded: Boolean = false,
    val currentUnitId: String = "es_u1",
    val callsMade: Int = 0,
    val dynamicColor: Boolean = false,
    val currentLanguage: String = "es"
)

class UserPreferencesStore(private val context: Context) {
    private object Keys {
        val SOUND = booleanPreferencesKey("sound_enabled")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val NAME = stringPreferencesKey("display_name")
        val MAX = booleanPreferencesKey("alpaca_max")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val UNIT = stringPreferencesKey("current_unit")
        val CALLS = intPreferencesKey("calls_made")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LANGUAGE = stringPreferencesKey("current_language")
    }

    val prefs: Flow<UserPrefs> = context.dataStore.data.map { p ->
        UserPrefs(
            soundEnabled = p[Keys.SOUND] ?: true,
            hapticsEnabled = p[Keys.HAPTICS] ?: true,
            displayName = p[Keys.NAME] ?: "Explorer",
            alpacaMax = p[Keys.MAX] ?: false,
            onboarded = p[Keys.ONBOARDED] ?: false,
            currentUnitId = p[Keys.UNIT] ?: "es_u1",
            callsMade = p[Keys.CALLS] ?: 0,
            dynamicColor = p[Keys.DYNAMIC_COLOR] ?: false,
            currentLanguage = p[Keys.LANGUAGE] ?: "es"
        )
    }

    suspend fun setSound(enabled: Boolean) = context.dataStore.edit { it[Keys.SOUND] = enabled }
    suspend fun setHaptics(enabled: Boolean) = context.dataStore.edit { it[Keys.HAPTICS] = enabled }
    suspend fun setDisplayName(name: String) = context.dataStore.edit { it[Keys.NAME] = name.trim() }
    suspend fun setAlpacaMax(enabled: Boolean) = context.dataStore.edit { it[Keys.MAX] = enabled }
    suspend fun setOnboarded() = context.dataStore.edit { it[Keys.ONBOARDED] = true }
    suspend fun setCurrentUnit(unitId: String) = context.dataStore.edit { it[Keys.UNIT] = unitId }
    suspend fun incrementCalls() = context.dataStore.edit { it[Keys.CALLS] = (it[Keys.CALLS] ?: 0) + 1 }
    suspend fun setDynamicColor(enabled: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    suspend fun setCurrentLanguage(languageId: String) {
        context.dataStore.edit {
            it[Keys.LANGUAGE] = languageId
            it[Keys.UNIT] = "${languageId}_u1"
        }
    }
}

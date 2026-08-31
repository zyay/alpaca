package com.alpaca.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

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
    val currentLanguage: String = "es",
    val deviceId: String = "",
    val authToken: String = "",
    val authUserId: String = "",
    val authEmail: String = "",
    val authName: String = "",
    val voiceLevel: String = "beginner",
    val voiceName: String = "Kore",
    val lastUpdateCheck: Long = 0
) {
    val signedIn: Boolean get() = authToken.isNotEmpty()
}

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
        val DEVICE_ID = stringPreferencesKey("device_id")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val AUTH_USER_ID = stringPreferencesKey("auth_user_id")
        val AUTH_EMAIL = stringPreferencesKey("auth_email")
        val AUTH_NAME = stringPreferencesKey("auth_name")
        val VOICE_LEVEL = stringPreferencesKey("voice_level")
        val VOICE_NAME = stringPreferencesKey("voice_name")
        val LAST_UPDATE_CHECK = longPreferencesKey("last_update_check")
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
            currentLanguage = p[Keys.LANGUAGE] ?: "es",
            deviceId = p[Keys.DEVICE_ID] ?: "",
            authToken = p[Keys.AUTH_TOKEN] ?: "",
            authUserId = p[Keys.AUTH_USER_ID] ?: "",
            authEmail = p[Keys.AUTH_EMAIL] ?: "",
            authName = p[Keys.AUTH_NAME] ?: "",
            voiceLevel = p[Keys.VOICE_LEVEL] ?: "beginner",
            voiceName = p[Keys.VOICE_NAME] ?: "Kore",
            lastUpdateCheck = p[Keys.LAST_UPDATE_CHECK] ?: 0L
        )
    }

    suspend fun setSound(enabled: Boolean) = context.dataStore.edit { it[Keys.SOUND] = enabled }
    suspend fun setHaptics(enabled: Boolean) = context.dataStore.edit { it[Keys.HAPTICS] = enabled }
    suspend fun setDisplayName(name: String) = context.dataStore.edit { it[Keys.NAME] = name.trim() }
    suspend fun setAlpacaMax(enabled: Boolean) = context.dataStore.edit { it[Keys.MAX] = enabled }
    suspend fun setOnboarded() = context.dataStore.edit { it[Keys.ONBOARDED] = true }
    suspend fun setCurrentUnit(unitId: String) = context.dataStore.edit { it[Keys.UNIT] = unitId }
    suspend fun incrementCalls() = context.dataStore.edit { it[Keys.CALLS] = (it[Keys.CALLS] ?: 0) + 1 }
    suspend fun setVoiceLevel(level: String) = context.dataStore.edit { it[Keys.VOICE_LEVEL] = level }
    suspend fun setVoiceName(voice: String) = context.dataStore.edit { it[Keys.VOICE_NAME] = voice }
    suspend fun setLastUpdateCheck(atMs: Long) = context.dataStore.edit { it[Keys.LAST_UPDATE_CHECK] = atMs }
    suspend fun setDynamicColor(enabled: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    suspend fun setCurrentLanguage(languageId: String) {
        context.dataStore.edit {
            it[Keys.LANGUAGE] = languageId
            it[Keys.UNIT] = "${languageId}_u1"
        }
    }

    /** Stable anonymous id for league play; minted on first use. */
    suspend fun ensureDeviceId(): String {
        val existing = context.dataStore.data.first()[Keys.DEVICE_ID]
        if (!existing.isNullOrEmpty()) return existing
        val fresh = UUID.randomUUID().toString()
        context.dataStore.edit { prefs ->
            prefs[Keys.DEVICE_ID] = prefs[Keys.DEVICE_ID] ?: fresh
        }
        return fresh
    }

    suspend fun setSession(token: String, userId: String, email: String, name: String) {
        context.dataStore.edit {
            it[Keys.AUTH_TOKEN] = token
            it[Keys.AUTH_USER_ID] = userId
            it[Keys.AUTH_EMAIL] = email
            it[Keys.AUTH_NAME] = name
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(Keys.AUTH_TOKEN)
            it.remove(Keys.AUTH_USER_ID)
            it.remove(Keys.AUTH_EMAIL)
            it.remove(Keys.AUTH_NAME)
        }
    }
}

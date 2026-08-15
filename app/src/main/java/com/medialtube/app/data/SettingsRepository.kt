package com.medialtube.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val SERVER_URL_KEY = stringPreferencesKey("server_url")
        val THEME_KEY = stringPreferencesKey("app_theme")
        val DEFAULT_TYPE_KEY = stringPreferencesKey("default_type")
        val DEFAULT_QUALITY_KEY = stringPreferencesKey("default_quality")
        val DEFAULT_FORMAT_KEY = stringPreferencesKey("default_format")
        val DEFAULT_CODEC_KEY = stringPreferencesKey("default_codec")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SERVER_URL_KEY] ?: "http://192.168.1.100:8081"
    }

    val selectedTheme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_KEY] ?: "SYSTEM" // Доступные: SYSTEM, LIGHT, DARK, RETRO_98, RETRO_XP
    }

    val defaultType: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_TYPE_KEY] ?: "Video"
    }

    val defaultQuality: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_QUALITY_KEY] ?: "best"
    }

    val defaultFormat: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_FORMAT_KEY] ?: "any"
    }

    val defaultCodec: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_CODEC_KEY] ?: "auto"
    }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { prefs -> prefs[SERVER_URL_KEY] = url }
    }

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { prefs -> prefs[THEME_KEY] = theme }
    }

    suspend fun saveDefaults(type: String, quality: String, format: String, codec: String) {
        context.dataStore.edit { prefs ->
            prefs[DEFAULT_TYPE_KEY] = type
            prefs[DEFAULT_QUALITY_KEY] = quality
            prefs[DEFAULT_FORMAT_KEY] = format
            prefs[DEFAULT_CODEC_KEY] = codec
        }
    }
}

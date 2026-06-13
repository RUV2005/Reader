package com.danmo.reader.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "reader_settings")

object SettingsKeys {
    val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
    val SPEECH_RATE = floatPreferencesKey("speech_rate")
    val FONT_SIZE = intPreferencesKey("font_size")
    val AUTO_SCROLL = booleanPreferencesKey("auto_scroll")
    val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
    val LANGUAGE = stringPreferencesKey("language")
    val THEME = stringPreferencesKey("theme")
}

class SettingsDataStore(private val context: Context) {

    val ttsEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { it[SettingsKeys.TTS_ENABLED] ?: true }

    val speechRate: Flow<Float> = context.settingsDataStore.data
        .map { it[SettingsKeys.SPEECH_RATE] ?: 1.0f }

    val fontSize: Flow<Int> = context.settingsDataStore.data
        .map { it[SettingsKeys.FONT_SIZE] ?: 18 }

    val autoScroll: Flow<Boolean> = context.settingsDataStore.data
        .map { it[SettingsKeys.AUTO_SCROLL] ?: true }

    val highContrast: Flow<Boolean> = context.settingsDataStore.data
        .map { it[SettingsKeys.HIGH_CONTRAST] ?: false }

    val language: Flow<String> = context.settingsDataStore.data
        .map { it[SettingsKeys.LANGUAGE] ?: "zh" }

    val theme: Flow<String> = context.settingsDataStore.data
        .map { it[SettingsKeys.THEME] ?: "system" }

    suspend fun setTtsEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.TTS_ENABLED] = value }
    }

    suspend fun setSpeechRate(value: Float) {
        context.settingsDataStore.edit { it[SettingsKeys.SPEECH_RATE] = value.coerceIn(0.5f, 5.0f) }
    }

    suspend fun setFontSize(value: Int) {
        context.settingsDataStore.edit { it[SettingsKeys.FONT_SIZE] = value.coerceIn(12, 40) }
    }

    suspend fun setAutoScroll(value: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.AUTO_SCROLL] = value }
    }

    suspend fun setHighContrast(value: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.HIGH_CONTRAST] = value }
    }

    suspend fun setLanguage(value: String) {
        context.settingsDataStore.edit { it[SettingsKeys.LANGUAGE] = value }
    }

    suspend fun setTheme(value: String) {
        context.settingsDataStore.edit { it[SettingsKeys.THEME] = value }
    }
}
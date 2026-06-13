package com.danmo.reader.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_TTS_ENABLED   = booleanPreferencesKey("tts_enabled")
        private val KEY_SPEECH_RATE   = floatPreferencesKey("speech_rate")
        private val KEY_FONT_SIZE     = intPreferencesKey("font_size")
        private val KEY_AUTO_SCROLL   = booleanPreferencesKey("auto_scroll")
        private val KEY_HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        private val KEY_LANGUAGE      = stringPreferencesKey("language")
        private val KEY_THEME         = stringPreferencesKey("theme")

        // 默认值
        const val DEFAULT_TTS_ENABLED   = true
        const val DEFAULT_SPEECH_RATE   = 1.0f
        const val DEFAULT_FONT_SIZE     = 18
        const val DEFAULT_AUTO_SCROLL   = true
        const val DEFAULT_HIGH_CONTRAST = false
        const val DEFAULT_LANGUAGE      = "zh"
        const val DEFAULT_THEME         = "system"
    }

    // ── 读取 ──────────────────────────────────────────────

    val ttsEnabled: Flow<Boolean> = context.dataStore.data
        .catchIo()
        .map { it[KEY_TTS_ENABLED] ?: DEFAULT_TTS_ENABLED }

    val speechRate: Flow<Float> = context.dataStore.data
        .catchIo()
        .map { it[KEY_SPEECH_RATE] ?: DEFAULT_SPEECH_RATE }

    val fontSize: Flow<Int> = context.dataStore.data
        .catchIo()
        .map { it[KEY_FONT_SIZE] ?: DEFAULT_FONT_SIZE }

    val autoScroll: Flow<Boolean> = context.dataStore.data
        .catchIo()
        .map { it[KEY_AUTO_SCROLL] ?: DEFAULT_AUTO_SCROLL }

    val highContrast: Flow<Boolean> = context.dataStore.data
        .catchIo()
        .map { it[KEY_HIGH_CONTRAST] ?: DEFAULT_HIGH_CONTRAST }

    val language: Flow<String> = context.dataStore.data
        .catchIo()
        .map { it[KEY_LANGUAGE] ?: DEFAULT_LANGUAGE }

    val theme: Flow<String> = context.dataStore.data
        .catchIo()
        .map { it[KEY_THEME] ?: DEFAULT_THEME }

    // ── 写入 ──────────────────────────────────────────────

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_TTS_ENABLED] = enabled }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[KEY_SPEECH_RATE] = rate.coerceIn(0.5f, 5.0f) }
    }

    suspend fun setFontSize(size: Int) {
        context.dataStore.edit { it[KEY_FONT_SIZE] = size.coerceIn(10, 40) }
    }

    suspend fun setAutoScroll(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_SCROLL] = enabled }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HIGH_CONTRAST] = enabled }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = lang }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    // ── 工具 ──────────────────────────────────────────────

    /** DataStore 文件损坏时静默降级，返回空 Preferences（使用默认值）。 */
    private fun Flow<Preferences>.catchIo(): Flow<Preferences> =
        catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }
}
package com.danmo.reader.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.danmo.reader.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SettingsUiState(
    val ttsEnabled: Boolean = true,
    val speechRate: Float = 1.0f,
    val fontSize: Int = 18,
    val autoScroll: Boolean = true,
    val highContrast: Boolean = false,
    val language: String = "zh",
    val theme: String = "system",
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.ttsEnabled,
                repository.speechRate,
                repository.fontSize,
                repository.autoScroll,
                repository.highContrast,
                repository.language,
                repository.theme,
            ) { values ->
                SettingsUiState(
                    ttsEnabled = values[0] as Boolean,
                    speechRate = values[1] as Float,
                    fontSize = values[2] as Int,
                    autoScroll = values[3] as Boolean,
                    highContrast = values[4] as Boolean,
                    language = values[5] as String,
                    theme = values[6] as String,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleTts() = viewModelScope.launch {
        repository.setTtsEnabled(!_uiState.value.ttsEnabled)
    }

    fun setSpeechRate(rate: Float) = viewModelScope.launch {
        repository.setSpeechRate(rate)
    }

    fun setFontSize(size: Int) = viewModelScope.launch {
        repository.setFontSize(size)
    }

    fun toggleAutoScroll() = viewModelScope.launch {
        repository.setAutoScroll(!_uiState.value.autoScroll)
    }

    fun toggleHighContrast() = viewModelScope.launch {
        repository.setHighContrast(!_uiState.value.highContrast)
    }

    fun setLanguage(lang: String) = viewModelScope.launch {
        repository.setLanguage(lang)
    }

    fun setTheme(theme: String) = viewModelScope.launch {
        repository.setTheme(theme)
    }
}
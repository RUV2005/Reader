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
import java.io.File
import java.util.Locale

data class SettingsUiState(
    val ttsEnabled: Boolean = true,
    val speechRate: Float = 1.0f,
    val fontSize: Int = 18,
    val autoScroll: Boolean = true,
    val highContrast: Boolean = false,
    val language: String = "zh",
    val theme: String = "system",
    val cacheSize: String = "0.00 MB",
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        updateCacheSize()
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
                    cacheSize = _uiState.value.cacheSize // 保持当前缓存大小状态
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

    fun resetToDefaults() = viewModelScope.launch {
        repository.resetToDefaults()
        updateCacheSize() // 同步刷新
    }

    /**
     * 计算并更新当前缓存大小
     */
    fun updateCacheSize() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val cacheDir = getApplication<Application>().cacheDir
            val size = getFolderSize(cacheDir)
            _uiState.value = _uiState.value.copy(cacheSize = formatSize(size))
        }
    }

    /**
     * 清理应用缓存
     */
    fun clearCache() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val cacheDir = getApplication<Application>().cacheDir
            cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            updateCacheSize()
        }
    }

    private fun getFolderSize(folder: File): Long {
        var size: Long = 0
        val files = folder.listFiles()
        if (files != null) {
            for (file in files) {
                size += if (file.isFile) file.length() else getFolderSize(file)
            }
        }
        return size
    }

    private fun formatSize(size: Long): String {
        val mb = size.toDouble() / (1024 * 1024)
        return String.format(Locale.CHINA, "%.2f MB", mb)
    }
}

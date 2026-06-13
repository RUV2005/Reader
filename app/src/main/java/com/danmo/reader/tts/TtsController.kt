package com.danmo.reader.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.danmo.reader.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * TTS 朗读状态
 */
sealed class TtsState {
    data object Idle : TtsState()
    data object Ready : TtsState()
    data object Speaking : TtsState()
    data object Paused : TtsState()
    data class Error(val message: String) : TtsState()
}

/**
 * TTS 控制回调接口
 */
interface TtsCallbacks {
    /** 检查是否还有更多内容可朗读 */
    fun onUtteranceDone(): Boolean
    /** 获取当前位置的朗读文本 */
    fun getCurrentText(): String
    /** 获取当前朗读段的唯一标识 */
    fun getCurrentUtteranceId(): String
    /** 移动到下一段/下一个位置 */
    fun moveToNext()
    /** 移动到上一段/上一个位置 */
    fun moveToPrevious()
}

/**
 * TTS 控制器
 */
class TtsController(
    private val context: Context,
    private val callbacks: TtsCallbacks
) {
    private var tts: TextToSpeech? = null
    private val settingsRepository = SettingsRepository(context)

    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    // 标记当前是否为一次性朗读模式（不触发回调循环）
    private var isOneShotMode = false

    init {
        initializeTts()
        // 从设置读取默认语速
        CoroutineScope(Dispatchers.Main).launch {
            settingsRepository.speechRate.collect { rate ->
                _speechRate.value = rate
                tts?.setSpeechRate(rate)
            }
        }
    }

    private fun initializeTts() {
        tts = TextToSpeech(context) { status ->
            when (status) {
                TextToSpeech.SUCCESS -> {
                    tts?.language = Locale.CHINESE
                    tts?.setSpeechRate(_speechRate.value)
                    setupUtteranceListener()
                    _isReady.value = true
                    _state.value = TtsState.Ready
                }
                else -> {
                    _state.value = TtsState.Error("TTS 初始化失败，状态码: $status")
                }
            }
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _state.value = TtsState.Speaking
            }

            override fun onDone(utteranceId: String?) {
                if (isOneShotMode) {
                    // 一次性朗读完成，恢复状态，不触发回调循环
                    isOneShotMode = false
                    _state.value = TtsState.Ready
                    return
                }

                val shouldContinue = callbacks.onUtteranceDone()
                if (shouldContinue) {
                    // 修复：先移动到下一段，再朗读
                    callbacks.moveToNext()
                    Handler(Looper.getMainLooper()).postDelayed({
                        speakCurrent()
                    }, 300)
                } else {
                    _state.value = TtsState.Ready
                }
            }

            override fun onError(utteranceId: String?) {
                isOneShotMode = false
                _state.value = TtsState.Error("朗读出错")
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                isOneShotMode = false
                if (interrupted) {
                    _state.value = TtsState.Paused
                }
            }
        })
    }

    /**
     * 朗读当前位置的内容（通过回调获取文本，支持自动下一段）
     */
    fun speakCurrent() {
        if (!_isReady.value) return

        val text = callbacks.getCurrentText()
        if (text.isBlank()) {
            callbacks.moveToNext()
            speakCurrent()
            return
        }

        isOneShotMode = false
        tts?.stop()
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            callbacks.getCurrentUtteranceId()
        )
    }

    /**
     * 一次性朗读指定文本，不触发回调的自动下一段逻辑
     */
    fun speak(text: String) {
        if (!_isReady.value) return
        if (text.isBlank()) return

        isOneShotMode = true
        tts?.stop()
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "one_shot_${System.currentTimeMillis()}"
        )
    }

    fun togglePlayPause() {
        when (_state.value) {
            is TtsState.Speaking -> pause()
            is TtsState.Ready, is TtsState.Paused, is TtsState.Error -> {
                speakCurrent()
            }
            else -> {}
        }
    }

    fun pause() {
        tts?.stop()
        _state.value = TtsState.Paused
    }

    fun stop() {
        isOneShotMode = false
        tts?.stop()
        _state.value = TtsState.Ready
    }

    fun speakPrevious() {
        callbacks.moveToPrevious()
        speakCurrent()
    }

    fun speakNext() {
        callbacks.moveToNext()
        speakCurrent()
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(_speechRate.value)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
        _state.value = TtsState.Idle
    }
}

@Composable
fun rememberTtsController(
    callbacks: TtsCallbacks
): TtsController {
    val context = LocalContext.current
    val controller = remember { TtsController(context, callbacks) }

    DisposableEffect(Unit) {
        onDispose {
            controller.shutdown()
        }
    }

    return controller
}
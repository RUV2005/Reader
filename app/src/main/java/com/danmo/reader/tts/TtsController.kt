package com.danmo.reader.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * 由各个 ReaderScreen 实现，处理朗读完成后的业务逻辑（如自动下一项）
 */
interface TtsCallbacks {
    /** 朗读完成时调用，返回是否继续朗读下一项 */
    fun onUtteranceDone(): Boolean
    /** 获取当前项的朗读文本 */
    fun getCurrentText(): String
    /** 获取当前项的唯一标识 */
    fun getCurrentUtteranceId(): String
    /** 移动到下一项 */
    fun moveToNext()
    /** 移动到上一项 */
    fun moveToPrevious()
}

/**
 * TTS 控制器
 * 封装 TTS 初始化、状态管理、语速控制、播放/暂停/跳转等通用逻辑
 */
class TtsController(
    private val context: Context,
    private val callbacks: TtsCallbacks
) {
    private var tts: TextToSpeech? = null

    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        initializeTts()
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
                val shouldContinue = callbacks.onUtteranceDone()
                if (shouldContinue) {
                    // 延迟一小段时间再播放下一个，避免连读太快
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        speakCurrent()
                    }, 300)
                } else {
                    _state.value = TtsState.Ready
                }
            }

            override fun onError(utteranceId: String?) {
                _state.value = TtsState.Error("朗读出错")
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                if (interrupted) {
                    _state.value = TtsState.Paused
                }
            }
        })
    }

    /**
     * 朗读当前项
     */
    fun speakCurrent() {
        if (!_isReady.value) return

        val text = callbacks.getCurrentText()
        if (text.isBlank()) {
            // 空内容自动跳过
            callbacks.moveToNext()
            speakCurrent()
            return
        }

        tts?.stop()
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            callbacks.getCurrentUtteranceId()
        )
    }

    /**
     * 播放/暂停切换
     */
    fun togglePlayPause() {
        when (_state.value) {
            is TtsState.Speaking -> pause()
            is TtsState.Ready, is TtsState.Paused, is TtsState.Error -> {
                speakCurrent()
            }
            else -> {}
        }
    }

    /**
     * 暂停朗读
     */
    fun pause() {
        tts?.stop()
        _state.value = TtsState.Paused
    }

    /**
     * 停止朗读并重置状态
     */
    fun stop() {
        tts?.stop()
        _state.value = TtsState.Ready
    }

    /**
     * 朗读上一项
     */
    fun speakPrevious() {
        callbacks.moveToPrevious()
        speakCurrent()
    }

    /**
     * 朗读下一项
     */
    fun speakNext() {
        callbacks.moveToNext()
        speakCurrent()
    }

    /**
     * 设置语速
     */
    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(_speechRate.value)
    }

    /**
     * 释放资源
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
        _state.value = TtsState.Idle
    }
}

/**
 * Compose remember 版本的 TTS 控制器
 * 自动处理生命周期（初始化 + 销毁）
 */
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
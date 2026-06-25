package com.danmo.reader.tts

import android.content.Context
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
 * TTS 朗读状态枚举
 */
sealed class TtsState {
    data object Idle : TtsState()      // 闲置状态
    data object Ready : TtsState()     // 引擎已就绪
    data object Speaking : TtsState()  // 正在朗读
    data object Paused : TtsState()    // 已暂停
    data class Error(val message: String) : TtsState() // 出错
}

/**
 * TTS 控制回调接口
 * 用于解耦控制器与具体的页面逻辑（如 Word, PDF 解析进度）
 */
interface TtsCallbacks {
    /** 检查是否还有更多内容可朗读（用于判断是否自动翻段） */
    fun onUtteranceDone(): Boolean
    /** 获取当前光标位置的待朗读文本 */
    fun getCurrentText(): String
    /** 获取当前朗读段的唯一 ID（用于 TTS 引擎标识进度） */
    fun getCurrentUtteranceId(): String
    /** 逻辑移动：跳转到下一段 */
    fun moveToNext()
    /** 逻辑移动：返回上一段 */
    fun moveToPrevious()
}

/**
 * TTS 控制器核心类
 * 封装了系统 TextToSpeech 引擎的初始化、播放控制、语速管理和持久化逻辑
 */
class TtsController(
    private val context: Context,
    private val callbacks: TtsCallbacks
) {
    private var tts: TextToSpeech? = null
    private val settingsRepository = SettingsRepository(context)

    // 内部状态流，外部通过 state.collect 观察 UI 变化
    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    val state: StateFlow<TtsState> = _state.asStateFlow()

    // 语速状态，由 SettingsRepository 提供初始值并实时同步
    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    // 标记引擎是否准备好，防止初始化完成前调用导致崩溃
    private val _isReady = MutableStateFlow(value = false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    // 标记当前是否为“单次朗读”模式（例如设置页面的效果预览），此模式下不会触发自动下一段的回调
    private var isOneShotMode = false

    init {
        initializeTts()
        // 从持久化存储（DataStore）订阅语速变化，实时更新引擎
        CoroutineScope(Dispatchers.Main).launch {
            settingsRepository.speechRate.collect { rate ->
                _speechRate.value = rate
                tts?.setSpeechRate(rate)
            }
        }
    }

    /**
     * 初始化 TTS 引擎
     */
    private fun initializeTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.apply {
                    language = Locale.CHINESE
                    setSpeechRate(_speechRate.value)
                    setupUtteranceListener() // 配置进度监听器
                }
                _isReady.value = true
                _state.value = TtsState.Ready
            } else {
                _state.value = TtsState.Error("TTS 引擎启动失败，状态码: $status")
            }
        }
    }

    /**
     * 设置朗读进度监听器
     * 处理朗读开始、完成、错误和停止的回调逻辑
     */
    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _state.value = TtsState.Speaking
            }

            override fun onDone(utteranceId: String?) {
                if (isOneShotMode) {
                    // 如果是一次性预览模式，读完就停，不触发连续朗读
                    isOneShotMode = false
                    _state.value = TtsState.Ready
                    return
                }

                // 在主线程处理连续朗读逻辑，确保与 UI 状态同步
                CoroutineScope(Dispatchers.Main).launch {
                    if (callbacks.onUtteranceDone()) {
                        callbacks.moveToNext()     // 逻辑上移动到下一段
                        kotlinx.coroutines.delay(timeMillis = 100) // 极短延迟避免引擎切换过于突兀
                        speakCurrent()            // 开始朗读新的内容
                    } else {
                        _state.value = TtsState.Ready
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isOneShotMode = false
                _state.value = TtsState.Error("语音播放出错")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                isOneShotMode = false
                _state.value = TtsState.Error("朗读错误 (代码: $errorCode)")
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                // 当调用 tts.stop() 或 speak(QUEUE_FLUSH) 时触发
                if (!isOneShotMode) {
                    _state.value = TtsState.Paused
                }
            }
        })
    }

    /**
     * 朗读当前位置的内容
     * 包含自动跳过空行的智能逻辑
     */
    fun speakCurrent() {
        if (!_isReady.value) return

        var text = callbacks.getCurrentText()
        
        // 智能跳过空内容：如果当前段落是空的，自动尝试寻找下一段
        while (text.isBlank()) {
            if (callbacks.onUtteranceDone()) {
                callbacks.moveToNext()
                text = callbacks.getCurrentText()
            } else {
                _state.value = TtsState.Ready
                return
            }
        }

        isOneShotMode = false
        // 使用 QUEUE_FLUSH 会立即中断当前正在读的内容
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            callbacks.getCurrentUtteranceId()
        )
    }

    /**
     * 一次性朗读指定文本
     * 用于设置预览、操作提示等，不影响阅读器的当前进度
     */
    fun speak(text: String) {
        if (!_isReady.value || text.isBlank()) return

        isOneShotMode = true
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "one_shot_${System.currentTimeMillis()}"
        )
    }

    /**
     * 播放/暂停切换逻辑
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

    /**
     * 设置并持久化保存语速
     */
    fun setSpeechRate(rate: Float) {
        val clampedRate = rate.coerceIn(0.5f, 2.0f)
        _speechRate.value = clampedRate
        tts?.setSpeechRate(clampedRate)
        
        // 使用协程异步保存到 DataStore，防止阻塞 UI
        CoroutineScope(Dispatchers.IO).launch {
            settingsRepository.setSpeechRate(clampedRate)
        }
    }

    /**
     * 彻底关闭引擎，释放硬件资源
     */
    fun shutdown() {
        tts?.apply {
            stop()
            shutdown()
        }
        tts = null
        _isReady.value = false
        _state.value = TtsState.Idle
    }
}

/**
 * Compose 记住 TTS 控制器的 Helper 方法
 * 自动绑定 Composable 的生命周期，在页面销毁时自动 shutdown
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

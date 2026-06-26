package com.danmo.reader.settings

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danmo.reader.R
import java.util.*

data class SettingItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val iconRes: Int,
    val iconColor: Color,
    val type: SettingType = SettingType.NAVIGATE,
)

enum class SettingType {
    NAVIGATE,
    TOGGLE,
    SELECT,
    VALUE,
}

data class SettingGroup(
    val title: String,
    val items: List<SettingItem>,
)

// ==================== 设置主屏幕 ====================

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showSpeechRateDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog    by remember { mutableStateOf(false) }
    var showLanguageDialog    by remember { mutableStateOf(false) }
    var showThemeDialog       by remember { mutableStateOf(false) }
    var showAboutDialog       by remember { mutableStateOf(false) }
    var showStorageDialog     by remember { mutableStateOf(false) }
    var showGestureDialog     by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog    by remember { mutableStateOf(false) }
    var showPrivacyDialog     by remember { mutableStateOf(false) }

    val settingGroups = listOf(
        SettingGroup(
            title = stringResource(id = R.string.group_reading),
            items = listOf(
                SettingItem(
                    id = "tts",
                    title = stringResource(id = R.string.setting_tts),
                    subtitle = if (uiState.ttsEnabled) "已开启" else "已关闭",
                    iconRes = R.drawable.ic_tts,
                    iconColor = Color(0xFF4A6FA5),
                    type = SettingType.TOGGLE,
                ),
                SettingItem(
                    id = "speech_rate",
                    title = stringResource(id = R.string.setting_speech_rate),
                    subtitle = "${uiState.speechRate}x",
                    iconRes = R.drawable.ic_speed,
                    iconColor = Color(0xFF6B8CBB),
                    type = SettingType.VALUE,
                ),
                SettingItem(
                    id = "font_size",
                    title = stringResource(id = R.string.setting_font_size),
                    subtitle = "${uiState.fontSize}sp",
                    iconRes = R.drawable.ic_text_size,
                    iconColor = Color(0xFF8B9DC3),
                    type = SettingType.VALUE,
                ),
                SettingItem(
                    id = "auto_scroll",
                    title = stringResource(id = R.string.setting_auto_scroll),
                    subtitle = if (uiState.autoScroll) "已开启" else "已关闭",
                    iconRes = R.drawable.ic_scroll,
                    iconColor = Color(0xFF4A6FA5),
                    type = SettingType.TOGGLE,
                ),
            ),
        ),
        SettingGroup(
            title = stringResource(id = R.string.group_accessibility),
            items = listOf(
                SettingItem(
                    id = "high_contrast",
                    title = stringResource(id = R.string.setting_high_contrast),
                    subtitle = if (uiState.highContrast) "已开启" else "已关闭",
                    iconRes = R.drawable.ic_contrast,
                    iconColor = Color(0xFF217346),
                    type = SettingType.TOGGLE,
                ),
                SettingItem(
                    id = "gesture",
                    title = stringResource(id = R.string.setting_gesture),
                    subtitle = "查看操作指南",
                    iconRes = R.drawable.ic_gesture,
                    iconColor = Color(0xFFD24726),
                    type = SettingType.NAVIGATE,
                ),
                SettingItem(
                    id = "accessibility",
                    title = stringResource(id = R.string.setting_talkback),
                    subtitle = "读屏体验优化",
                    iconRes = R.drawable.ic_accessibility,
                    iconColor = Color(0xFF4A6FA5),
                    type = SettingType.NAVIGATE,
                ),
            ),
        ),
        SettingGroup(
            title = stringResource(id = R.string.group_general),
            items = listOf(
                SettingItem(
                    id = "storage",
                    title = stringResource(id = R.string.setting_storage),
                    subtitle = "当前缓存: ${uiState.cacheSize}",
                    iconRes = R.drawable.ic_storage,
                    iconColor = Color(0xFF6B8CBB),
                    type = SettingType.NAVIGATE,
                ),
                SettingItem(
                    id = "language",
                    title = stringResource(id = R.string.setting_language),
                    subtitle = when (uiState.language) {
                        "zh" -> "简体中文"
                        "en" -> "English"
                        else -> "简体中文"
                    },
                    iconRes = R.drawable.ic_language,
                    iconColor = Color(0xFF8B9DC3),
                    type = SettingType.SELECT,
                ),
                SettingItem(
                    id = "theme",
                    title = stringResource(id = R.string.setting_theme),
                    subtitle = when (uiState.theme) {
                        "system" -> "跟随系统"
                        "light"  -> "浅色"
                        "dark"   -> "深色"
                        else     -> "跟随系统"
                    },
                    iconRes = R.drawable.ic_theme,
                    iconColor = Color(0xFF4A6FA5),
                    type = SettingType.SELECT,
                ),
            ),
        ),
        SettingGroup(
            title = stringResource(id = R.string.group_about),
            items = listOf(
                SettingItem(
                    id = "about",
                    title = "关于应用",
                    subtitle = "版本 1.0.0 (Stable)",
                    iconRes = R.drawable.ic_info,
                    iconColor = Color(0xFF999999),
                    type = SettingType.NAVIGATE,
                ),
                SettingItem(
                    id = "feedback",
                    title = "意见反馈",
                    subtitle = "将您的建议告诉我们",
                    iconRes = R.drawable.ic_feedback,
                    iconColor = Color(0xFF999999),
                    type = SettingType.NAVIGATE,
                ),
                SettingItem(
                    id = "privacy",
                    title = "隐私政策",
                    subtitle = "数据安全说明",
                    iconRes = R.drawable.ic_privacy,
                    iconColor = Color(0xFF999999),
                    type = SettingType.NAVIGATE,
                ),
            ),
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
    ) {
        SettingsPreviewSection(
            fontSize    = uiState.fontSize,
            highContrast = uiState.highContrast,
            speechRate  = uiState.speechRate,
            ttsEnabled  = uiState.ttsEnabled,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .shadow(elevation = 4.dp)
                .background(Color(0xFFE0E0E0)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            settingGroups.forEach { group ->
                SettingGroupSection(
                    group = group,
                    onItemClick = { item ->
                        when (item.id) {
                            "tts"           -> viewModel.toggleTts()
                            "auto_scroll"   -> viewModel.toggleAutoScroll()
                            "high_contrast" -> viewModel.toggleHighContrast()
                            "speech_rate"   -> showSpeechRateDialog = true
                            "font_size"     -> showFontSizeDialog = true
                            "language"      -> showLanguageDialog = true
                            "theme"         -> showThemeDialog = true
                            "storage"       -> showStorageDialog = true
                            "gesture"       -> showGestureDialog = true
                            "accessibility" -> showAccessibilityDialog = true
                            "about"         -> showAboutDialog = true
                            "feedback"      -> showFeedbackDialog = true
                            "privacy"       -> showPrivacyDialog = true
                        }
                    },
                    toggleStates = mapOf(
                        "tts"           to uiState.ttsEnabled,
                        "auto_scroll"   to uiState.autoScroll,
                        "high_contrast" to uiState.highContrast,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "版本 1.0.0 (Build 20240611)",
                fontSize = 12.sp,
                color = Color(0xFFBBBBBB),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                textAlign = TextAlign.Center,
            )
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Dialogs
    if (showSpeechRateDialog) {
        SpeechRateDialog(
            currentRate = uiState.speechRate,
            onRateSelected = { viewModel.setSpeechRate(it) },
            onDismiss = { showSpeechRateDialog = false },
        )
    }
    if (showFontSizeDialog) {
        FontSizeDialog(
            currentSize = uiState.fontSize,
            onSizeSelected = { viewModel.setFontSize(it) },
            onDismiss = { showFontSizeDialog = false },
        )
    }
    if (showLanguageDialog) {
        LanguageDialog(
            currentLanguage = uiState.language,
            onLanguageSelected = { viewModel.setLanguage(it) },
            onDismiss = { showLanguageDialog = false },
        )
    }
    if (showThemeDialog) {
        ThemeDialog(
            currentTheme = uiState.theme,
            onThemeSelected = { viewModel.setTheme(it) },
            onDismiss = { showThemeDialog = false },
        )
    }
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
    if (showStorageDialog) {
        StorageDialog(
            cacheSize = uiState.cacheSize,
            onClearCache = { viewModel.clearCache() },
            onDismiss = { showStorageDialog = false }
        )
    }
    if (showGestureDialog) {
        GestureDialog(onDismiss = { showGestureDialog = false })
    }
    if (showAccessibilityDialog) {
        AccessibilityDialog(onDismiss = { showAccessibilityDialog = false })
    }
    if (showFeedbackDialog) {
        FeedbackDialog(onDismiss = { showFeedbackDialog = false })
    }
    if (showPrivacyDialog) {
        PrivacyDialog(onDismiss = { showPrivacyDialog = false })
    }
}

// ==================== 存储管理对话框 ====================

@Composable
private fun StorageDialog(
    cacheSize: String,
    onClearCache: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("存储管理", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("当前应用占用的缓存空间为：", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = cacheSize,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A6FA5)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "缓存主要包含文档预览图和 OCR 临时文件。清理缓存不会删除您的本地文档或阅读历史。",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onClearCache(); onDismiss() },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Text("清理缓存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

// ==================== 手势控制说明 ====================

@Composable
private fun GestureDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手势控制指南", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                GestureItem("左右滑动", "在阅读器中切换上一个/下一个段落。")
                GestureItem("双击屏幕", "在阅读器中立即开始朗读下一段。")
                GestureItem("长按卡片", "在首页可触发文档的更多管理操作（即将上线）。")
                GestureItem("摇一摇", "在任何页面快速唤起语音助手（开发中）。")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "提示：所有手势均已适配系统 TalkBack 读屏操作，建议开启 TalkBack 获取最佳无障碍体验。",
                    fontSize = 12.sp,
                    color = Color(0xFF217346)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        }
    )
}

@Composable
private fun GestureItem(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF4A6FA5))
        Text(text = desc, fontSize = 13.sp, color = Color.DarkGray)
    }
}

// ==================== 无障碍优化说明 ====================

@Composable
private fun AccessibilityDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("TalkBack 无障碍优化", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "我们为读屏用户深度定制了以下体验：",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                BulletPoint("语义化标签：为所有图标和操作按钮添加了精准的中文描述。")
                BulletPoint("焦点流优化：确保读屏器焦点按照文档逻辑顺序移动。")
                BulletPoint("高对比度：支持黑底黄字模式，辅助弱视用户识别。")
                BulletPoint("实时状态播报：朗读进度、解析成功等状态实时通过语音反馈。")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("确定") }
        }
    )
}

@Composable
private fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        Text("• ", fontWeight = FontWeight.Bold)
        Text(text = text, fontSize = 13.sp)
    }
}

// ==================== 意见反馈 ====================

@Composable
private fun FeedbackDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("意见反馈", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("感谢您使用智能阅读器！如果您在使用过程中遇到任何问题或有改进建议，请通过以下方式联系我们：", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("客服邮箱：support@danmo-reader.com", fontWeight = FontWeight.Bold, color = Color(0xFF4A6FA5))
                Text("反馈群组：微信号 ReaderHelper", fontWeight = FontWeight.Bold, color = Color(0xFF4A6FA5))
                Spacer(modifier = Modifier.height(16.dp))
                Text("您的每一个反馈都能帮助我们做得更好。", fontSize = 12.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("确定") }
        }
    )
}

// ==================== 隐私政策 ====================

@Composable
private fun PrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("隐私政策摘要", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("智能阅读器高度重视您的个人隐私：", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "1. 数据不出本地：所有的文档解析和 OCR 识别均在您的设备本地完成，不会上传到任何服务器。\n\n" +
                    "2. 权限最小化：应用仅申请相机（用于 OCR）和存储（用于读文档）所必须的权限。\n\n" +
                    "3. 历史记录管理：您的阅读历史仅保存在手机 DataStore 中，您可以随时通过清理应用数据进行彻底清除。\n\n" +
                    "4. 无第三方共享：我们绝不会将您的任何数据共享给第三方广告商或分析平台。",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("同意并接受") }
        }
    )
}

// ==================== 关于对话框 ====================

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于智能阅读器", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "版本：1.0.0 (Stable)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A6FA5)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "智能阅读器是一款专为视障和低视力用户打造的全能文档辅助工具。支持 Word, Excel, PPT, PDF 以及 OCR 拍照识字，旨在用技术消除阅读障碍。",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "© 2024 智能阅读器团队",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

// ==================== 吸顶预览区 ====================

private const val PREVIEW_TEXT =
    "春江潮水连海平，海上明月共潮生。滟滟随波千万里，何处春江无月明。"

@Composable
private fun SettingsPreviewSection(
    fontSize: Int,
    highContrast: Boolean,
    speechRate: Float,
    ttsEnabled: Boolean,
) {
    val context = LocalContext.current

    var isTtsReady by remember { mutableStateOf(false) }
    var isPlaying  by remember { mutableStateOf(false) }
    var tts        by remember { mutableStateOf<TextToSpeech?>(null) }

    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            isTtsReady = (status == TextToSpeech.SUCCESS)
            if (isTtsReady) tts?.language = Locale.CHINESE
        }
    }

    LaunchedEffect(speechRate) {
        tts?.setSpeechRate(speechRate)
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    fun speakPreview() {
        if (!isTtsReady) return
        isPlaying = true 
        tts?.setSpeechRate(speechRate)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?)  { }
            override fun onDone(utteranceId: String?)   { isPlaying = false }
            override fun onError(utteranceId: String?)  { isPlaying = false }
            override fun onStop(utteranceId: String?, interrupted: Boolean) { isPlaying = false }
        })
        val result = tts?.speak(PREVIEW_TEXT, TextToSpeech.QUEUE_FLUSH, null, "settings_preview")
        if (result == TextToSpeech.ERROR) {
            isPlaying = false
        }
    }

    fun stopPreview() {
        tts?.stop()
        isPlaying = false
    }

    val bgColor by animateColorAsState(
        targetValue = if (highContrast) Color(0xFF000000) else Color(0xFF1E2A3A),
        animationSpec = tween(300), label = "previewBg",
    )
    val bodyTextColor by animateColorAsState(
        targetValue = if (highContrast) Color(0xFFFFFFFF) else Color(0xFFDDDDDD),
        animationSpec = tween(300), label = "previewBody",
    )
    val accentColor by animateColorAsState(
        targetValue = if (highContrast) Color(0xFF00FF00) else Color(0xFF6B8CBB),
        animationSpec = tween(300), label = "previewAccent",
    )
    val highlightColor by animateColorAsState(
        targetValue = if (highContrast) Color(0xFFFFFF00) else Color(0xFFFFD966),
        animationSpec = tween(300), label = "previewHighlight",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "效果预览",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                letterSpacing = 1.sp,
                modifier = Modifier.semantics {
                    contentDescription = "效果预览区域，显示当前字体和对比度效果"
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PreviewTag(
                    text = "字体 ${fontSize}sp",
                    color = accentColor,
                    description = "当前字体大小 ${fontSize}sp",
                )
                PreviewTag(
                    text = "语速 ${(speechRate * 100).toInt()}%",
                    color = accentColor,
                    description = "当前语速 ${(speechRate * 100).toInt()}%",
                )
                if (highContrast) {
                    PreviewTag(
                        text = "高对比",
                        color = Color(0xFF00FF00),
                        description = "高对比度模式已开启",
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(highlightColor.copy(alpha = if (highContrast) 0.25f else 0.15f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics {
                    contentDescription = "当前朗读段落示例：$PREVIEW_TEXT"
                },
        ) {
            Text(
                text = PREVIEW_TEXT,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                color = highlightColor,
                lineHeight = (fontSize + 10).sp,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "江天一色无纤尘，皎皎空中孤月轮。",
            fontSize = fontSize.sp,
            color = bodyTextColor,
            lineHeight = (fontSize + 10).sp,
            modifier = Modifier.semantics {
                contentDescription = "普通正文预览，字体大小 ${fontSize}sp"
            },
        )

        if (ttsEnabled) {
            Spacer(modifier = Modifier.height(10.dp))
            TtsPreviewButton(
                isPlaying    = isPlaying,
                isReady      = isTtsReady,
                accentColor  = accentColor,
                highlightColor = highlightColor,
                speechRate   = speechRate,
                onPlay       = { speakPreview() },
                onStop       = { stopPreview() },
            )
        }

        if (highContrast) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "⚡ 高对比度：黑底 + 高亮黄色，提升弱视用户辨识度",
                fontSize = 11.sp,
                color = Color(0xFF00FF00),
                modifier = Modifier.semantics {
                    contentDescription = "高对比度模式已启用，使用黑色背景配合高亮黄色文字"
                },
            )
        }
    }
}

@Composable
private fun TtsPreviewButton(
    isPlaying: Boolean,
    isReady: Boolean,
    accentColor: Color,
    highlightColor: Color,
    speechRate: Float,
    onPlay: () -> Unit,
    onStop: () -> Unit,
) {
    val buttonColor by animateColorAsState(
        targetValue = if (isPlaying) highlightColor else accentColor,
        animationSpec = tween(200), label = "ttsBtn",
    )

    val label = when {
        !isReady  -> "语音引擎初始化中…"
        isPlaying -> "■ 停止试听"
        else      -> "▶ 试听朗读效果（语速 ${(speechRate * 100).toInt()}%）"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(buttonColor.copy(alpha = 0.18f))
            .then(
                if (isReady) Modifier.clickable(onClick = if (isPlaying) onStop else onPlay)
                else Modifier,
            )
            .padding(vertical = 10.dp)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (isPlaying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(13.dp),
                    color = highlightColor,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = buttonColor,
            )
        }
    }
}

@Composable
private fun PreviewTag(
    text: String,
    color: Color,
    description: String = text,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .semantics { contentDescription = description },
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SettingGroupSection(
    group: SettingGroup,
    onItemClick: (SettingItem) -> Unit,
    toggleStates: Map<String, Boolean>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = group.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF666666),
            modifier = Modifier
                .padding(start = 8.dp, bottom = 8.dp, top = 8.dp)
                .semantics { contentDescription = "${group.title}，设置分组" },
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column {
                group.items.forEachIndexed { index, item ->
                    SettingItemRow(
                        item = item,
                        isChecked = toggleStates[item.id] ?: false,
                        onClick = { onItemClick(item) },
                        isLast = index == group.items.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingItemRow(
    item: SettingItem,
    isChecked: Boolean,
    onClick: () -> Unit,
    isLast: Boolean,
) {
    val semanticsDescription = when (item.type) {
        SettingType.TOGGLE  -> {
            val state = if (isChecked) "已开启" else "已关闭"
            "${item.title}，开关，$state，${item.subtitle ?: ""}，双击切换"
        }
        SettingType.VALUE   -> "${item.title}，${item.subtitle ?: ""}，双击修改"
        SettingType.SELECT  -> "${item.title}，当前选中：${item.subtitle ?: ""}，双击修改"
        SettingType.NAVIGATE -> "${item.title}，${item.subtitle ?: ""}，双击进入"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { contentDescription = semanticsDescription },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(item.iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = item.iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = item.iconColor,
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333),
            )
            item.subtitle?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                )
            }
        }

        when (item.type) {
            SettingType.TOGGLE -> {
                Switch(
                    checked = isChecked,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor   = Color.White,
                        checkedTrackColor   = item.iconColor,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCCCCCC),
                    ),
                )
            }
            SettingType.NAVIGATE, SettingType.SELECT, SettingType.VALUE -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.type == SettingType.VALUE) {
                        Text(
                            text = when (item.id) {
                                "speech_rate" -> item.subtitle?.substringAfter("当前: ") ?: ""
                                "font_size"   -> item.subtitle?.substringAfter("当前: ") ?: ""
                                else          -> ""
                            },
                            fontSize = 14.sp,
                            color = Color(0xFF999999),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_right),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFCCCCCC),
                    )
                }
            }
        }
    }

    if (!isLast) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 66.dp),
            color = Color(0xFFEEEEEE),
            thickness = 0.5.dp,
        )
    }
}

@Composable
private fun SpeechRateDialog(
    currentRate: Float,
    onRateSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val rates = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f, 3.0f, 4.0f, 5.0f)
    val rateLabels = mapOf(
        0.5f  to "50% — 非常慢",
        0.75f to "75% — 慢速",
        1.0f  to "100% — 正常",
        1.25f to "125% — 较快",
        1.5f  to "150% — 快速",
        2.0f  to "200% — 非常快",
        2.5f  to "250% — 超快",
        3.0f  to "300% — 极速",
        4.0f  to "400% — 极限",
        5.0f  to "500% — 最高",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择语速", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                rates.forEach { rate ->
                    val label = rateLabels[rate] ?: "${(rate * 100).toInt()}%"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRateSelected(rate); onDismiss() }
                            .padding(vertical = 12.dp)
                            .semantics {
                                contentDescription =
                                    "$label${if (rate == currentRate) "，当前选中" else ""}"
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = rate == currentRate, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            fontWeight = if (rate == currentRate) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun FontSizeDialog(
    currentSize: Int,
    onSizeSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    data class SizeOption(val size: Int, val label: String, val sample: String)

    val options = listOf(
        SizeOption(10, "10sp · 极小",     "永"),
        SizeOption(11, "11sp · 很小",     "永"),
        SizeOption(12, "12sp · 小",       "永字"),
        SizeOption(13, "13sp · 偏小",     "永字"),
        SizeOption(14, "14sp · 较小",     "永字八"),
        SizeOption(15, "15sp · 正常偏小", "永字八法"),
        SizeOption(16, "16sp · 正常",     "永字八法"),
        SizeOption(17, "17sp · 正常+",    "永字八法"),
        SizeOption(18, "18sp · 推荐",     "永字八法"),
        SizeOption(20, "20sp · 偏大",     "永字八"),
        SizeOption(22, "22sp · 较大",     "永字八"),
        SizeOption(24, "24sp · 大",       "永字"),
        SizeOption(26, "26sp · 很大",     "永字"),
        SizeOption(28, "28sp · 超大",     "永字"),
        SizeOption(30, "30sp · 极大",     "永"),
        SizeOption(32, "32sp · 低视力",   "永"),
        SizeOption(36, "36sp · 弱视",     "永"),
        SizeOption(40, "40sp · 严重弱视", "永"),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择字体大小", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                options.forEach { opt ->
                    val isSelected = opt.size == currentSize
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSizeSelected(opt.size); onDismiss() }
                            .background(
                                if (isSelected) Color(0xFF4A6FA5).copy(alpha = 0.08f)
                                else Color.Transparent
                            )
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .semantics {
                                contentDescription =
                                    "${opt.label}${if (isSelected) "，当前选中" else ""}"
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = opt.label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF4A6FA5) else Color(0xFF333333),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = opt.sample,
                            fontSize = opt.size.sp,
                            color = if (isSelected) Color(0xFF4A6FA5) else Color(0xFF999999),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.width(56.dp),
                            textAlign = TextAlign.End,
                            maxLines = 1,
                        )
                    }
                    if (opt != options.last()) {
                        HorizontalDivider(
                            color = Color(0xFFF0F0F0),
                            thickness = 0.5.dp,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun LanguageDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val languages = listOf("zh" to "简体中文", "en" to "English")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择语言", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                languages.forEach { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(code); onDismiss() }
                            .padding(vertical = 12.dp)
                            .semantics {
                                contentDescription =
                                    "$name${if (code == currentLanguage) "，当前选中" else ""}"
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = code == currentLanguage, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            fontWeight = if (code == currentLanguage) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ThemeDialog(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val themes = listOf(
        "system" to "跟随系统",
        "light"  to "浅色",
        "dark"   to "深色",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主题", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                themes.forEach { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(code); onDismiss() }
                            .padding(vertical = 12.dp)
                            .semantics {
                                contentDescription =
                                    "$name${if (code == currentTheme) "，当前选中" else ""}"
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = code == currentTheme, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            fontWeight = if (code == currentTheme) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

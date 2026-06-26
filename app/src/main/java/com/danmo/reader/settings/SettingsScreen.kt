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
                    subtitle = if (uiState.ttsEnabled) stringResource(id = R.string.setting_tts_on) else stringResource(id = R.string.setting_tts_off),
                    iconRes = R.drawable.ic_tts,
                    iconColor = Color(0xFF4A6FA5),
                    type = SettingType.TOGGLE,
                ),
                SettingItem(
                    id = "speech_rate",
                    title = stringResource(id = R.string.setting_speech_rate),
                    subtitle = stringResource(id = R.string.setting_speech_rate_subtitle, uiState.speechRate),
                    iconRes = R.drawable.ic_speed,
                    iconColor = Color(0xFF6B8CBB),
                    type = SettingType.VALUE,
                ),
                SettingItem(
                    id = "font_size",
                    title = stringResource(id = R.string.setting_font_size),
                    subtitle = stringResource(id = R.string.setting_font_size_subtitle, uiState.fontSize),
                    iconRes = R.drawable.ic_text_size,
                    iconColor = Color(0xFF8B9DC3),
                    type = SettingType.VALUE,
                ),
                SettingItem(
                    id = "auto_scroll",
                    title = stringResource(id = R.string.setting_auto_scroll),
                    subtitle = if (uiState.autoScroll) stringResource(id = R.string.setting_auto_scroll_desc_on) else stringResource(id = R.string.setting_auto_scroll_desc_off),
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
                    subtitle = if (uiState.highContrast) stringResource(id = R.string.setting_tts_on) else stringResource(id = R.string.setting_high_contrast_desc),
                    iconRes = R.drawable.ic_contrast,
                    iconColor = Color(0xFF217346),
                    type = SettingType.TOGGLE,
                ),
                SettingItem(
                    id = "gesture",
                    title = stringResource(id = R.string.setting_gesture),
                    subtitle = stringResource(id = R.string.setting_gesture_subtitle),
                    iconRes = R.drawable.ic_gesture,
                    iconColor = Color(0xFFD24726),
                    type = SettingType.NAVIGATE,
                ),
                SettingItem(
                    id = "accessibility",
                    title = stringResource(id = R.string.setting_talkback),
                    subtitle = stringResource(id = R.string.setting_talkback_subtitle),
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
                    subtitle = stringResource(id = R.string.setting_storage_subtitle, uiState.cacheSize),
                    iconRes = R.drawable.ic_storage,
                    iconColor = Color(0xFF6B8CBB),
                    type = SettingType.NAVIGATE,
                ),
                SettingItem(
                    id = "language",
                    title = stringResource(id = R.string.setting_language),
                    subtitle = when (uiState.language) {
                        "zh" -> stringResource(id = R.string.lang_zh)
                        "en" -> stringResource(id = R.string.lang_en)
                        else -> stringResource(id = R.string.lang_zh)
                    },
                    iconRes = R.drawable.ic_language,
                    iconColor = Color(0xFF8B9DC3),
                    type = SettingType.SELECT,
                ),
                SettingItem(
                    id = "theme",
                    title = stringResource(id = R.string.setting_theme),
                    subtitle = when (uiState.theme) {
                        "system" -> stringResource(id = R.string.theme_system)
                        "light"  -> stringResource(id = R.string.theme_light)
                        "dark"   -> stringResource(id = R.string.theme_dark)
                        else     -> stringResource(id = R.string.theme_system)
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
                    title = stringResource(id = R.string.about_title),
                    subtitle = stringResource(id = R.string.about_version),
                    iconRes = R.drawable.ic_info,
                    iconColor = Color(0xFF999999),
                    type = SettingType.NAVIGATE,
                ),
                SettingItem(
                    id = "feedback",
                    title = stringResource(id = R.string.feedback_title),
                    subtitle = stringResource(id = R.string.feedback_subtitle),
                    iconRes = R.drawable.ic_feedback,
                    iconColor = Color(0xFF999999),
                    type = SettingType.NAVIGATE,
                ),
                SettingItem(
                    id = "privacy",
                    title = stringResource(id = R.string.privacy_title),
                    subtitle = stringResource(id = R.string.privacy_subtitle),
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
                text = stringResource(id = R.string.about_build),
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
        title = { Text(stringResource(id = R.string.setting_storage), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(stringResource(id = R.string.storage_desc), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = cacheSize,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A6FA5)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(id = R.string.storage_tip),
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
                Text(stringResource(id = R.string.storage_clear))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dialog_close))
            }
        }
    )
}

// ==================== 手势控制说明 ====================

@Composable
private fun GestureDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.gesture_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                GestureItem(stringResource(id = R.string.gesture_swipe_title), stringResource(id = R.string.gesture_swipe_desc))
                GestureItem(stringResource(id = R.string.gesture_double_tap_title), stringResource(id = R.string.gesture_double_tap_desc))
                GestureItem(stringResource(id = R.string.gesture_long_press_title), stringResource(id = R.string.gesture_long_press_desc))
                GestureItem(stringResource(id = R.string.gesture_shake_title), stringResource(id = R.string.gesture_shake_desc))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(id = R.string.gesture_tip),
                    fontSize = 12.sp,
                    color = Color(0xFF217346)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.dialog_know)) }
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
        title = { Text(stringResource(id = R.string.acc_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(id = R.string.acc_intro),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                BulletPoint(stringResource(id = R.string.acc_item_1))
                BulletPoint(stringResource(id = R.string.acc_item_2))
                BulletPoint(stringResource(id = R.string.acc_item_3))
                BulletPoint(stringResource(id = R.string.acc_item_4))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.dialog_confirm)) }
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
        title = { Text(stringResource(id = R.string.feedback_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(stringResource(id = R.string.feedback_intro), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(id = R.string.feedback_email), fontWeight = FontWeight.Bold, color = Color(0xFF4A6FA5))
                Text(stringResource(id = R.string.feedback_wechat), fontWeight = FontWeight.Bold, color = Color(0xFF4A6FA5))
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(id = R.string.feedback_footer), fontSize = 12.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.dialog_confirm)) }
        }
    )
}

// ==================== 隐私政策 ====================

@Composable
private fun PrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.privacy_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(id = R.string.privacy_intro), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(id = R.string.privacy_item_1) + "\n\n" +
                    stringResource(id = R.string.privacy_item_2) + "\n\n" +
                    stringResource(id = R.string.privacy_item_3) + "\n\n" +
                    stringResource(id = R.string.privacy_item_4),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.privacy_confirm)) }
        }
    )
}

// ==================== 关于对话框 ====================

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.about_main_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.about_version),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A6FA5)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(id = R.string.about_desc),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.about_copyright),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dialog_confirm))
            }
        }
    )
}

// ==================== 吸顶预览区 ====================

@Composable
private fun SettingsPreviewSection(
    fontSize: Int,
    highContrast: Boolean,
    speechRate: Float,
    ttsEnabled: Boolean,
) {
    val context = LocalContext.current
    val previewHighlight = stringResource(id = R.string.preview_text_highlight)
    val previewBody = stringResource(id = R.string.preview_text_body)

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
        tts?.speak(previewHighlight, TextToSpeech.QUEUE_FLUSH, null, "settings_preview")
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
                text = stringResource(id = R.string.preview_title),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                letterSpacing = 1.sp,
                modifier = Modifier.semantics {
                    contentDescription = context.getString(R.string.preview_desc)
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PreviewTag(
                    text = stringResource(id = R.string.preview_tag_font, fontSize),
                    color = accentColor,
                )
                PreviewTag(
                    text = stringResource(id = R.string.preview_tag_speed, (speechRate * 100).toInt()),
                    color = accentColor,
                )
                if (highContrast) {
                    PreviewTag(
                        text = stringResource(id = R.string.preview_tag_contrast),
                        color = Color(0xFF00FF00),
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
                    contentDescription = previewHighlight
                },
        ) {
            Text(
                text = previewHighlight,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                color = highlightColor,
                lineHeight = (fontSize + 10).sp,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = previewBody,
            fontSize = fontSize.sp,
            color = bodyTextColor,
            lineHeight = (fontSize + 10).sp,
            modifier = Modifier.semantics {
                contentDescription = previewBody
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
                text = stringResource(id = R.string.preview_contrast_tip),
                fontSize = 11.sp,
                color = Color(0xFF00FF00),
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
        !isReady  -> stringResource(id = R.string.preview_tts_ready)
        isPlaying -> stringResource(id = R.string.preview_tts_stop)
        else      -> stringResource(id = R.string.preview_tts_play, (speechRate * 100).toInt())
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
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
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
                .padding(start = 8.dp, bottom = 8.dp, top = 8.dp),
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
    val context = LocalContext.current
    val semanticsDescription = when (item.type) {
        SettingType.TOGGLE  -> {
            val state = if (isChecked) context.getString(R.string.setting_tts_on) else context.getString(R.string.setting_tts_off)
            "${item.title}, $state, ${item.subtitle ?: ""}"
        }
        else -> "${item.title}, ${item.subtitle ?: ""}"
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
                                "speech_rate" -> item.subtitle?.substringAfter(": ") ?: ""
                                "font_size"   -> item.subtitle?.substringAfter(": ") ?: ""
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
    val context = LocalContext.current
    val rates = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f, 3.0f, 4.0f, 5.0f)
    val rateLabels = mapOf(
        0.5f  to stringResource(id = R.string.speed_0_5),
        0.75f to stringResource(id = R.string.speed_0_75),
        1.0f  to stringResource(id = R.string.speed_1_0),
        1.25f to stringResource(id = R.string.speed_1_25),
        1.5f  to stringResource(id = R.string.speed_1_5),
        2.0f  to stringResource(id = R.string.speed_2_0),
        2.5f  to stringResource(id = R.string.speed_2_5),
        3.0f  to stringResource(id = R.string.speed_3_0),
        4.0f  to stringResource(id = R.string.speed_4_0),
        5.0f  to stringResource(id = R.string.speed_5_0),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.dialog_speed_title), fontWeight = FontWeight.Bold) },
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
                                    "$label${if (rate == currentRate) ", " + context.getString(R.string.dialog_selected) else ""}"
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
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.dialog_cancel)) } },
    )
}

@Composable
private fun FontSizeDialog(
    currentSize: Int,
    onSizeSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    data class SizeOption(val size: Int, val label: String, val sample: String)

    val options = listOf(
        SizeOption(10, "10sp", "永"),
        SizeOption(12, "12sp", "永字"),
        SizeOption(14, "14sp", "永字八"),
        SizeOption(16, "16sp", "永字八法"),
        SizeOption(18, "18sp", "永字八法"),
        SizeOption(20, "20sp", "永字八"),
        SizeOption(24, "24sp", "永字"),
        SizeOption(28, "28sp", "永字"),
        SizeOption(32, "32sp", "永"),
        SizeOption(36, "36sp", "永"),
        SizeOption(40, "40sp", "永"),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.dialog_font_title), fontWeight = FontWeight.Bold) },
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
                                    "${opt.label}${if (isSelected) ", " + context.getString(R.string.dialog_selected) else ""}"
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
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.dialog_cancel)) } },
    )
}

@Composable
private fun LanguageDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val languages = listOf("zh" to stringResource(id = R.string.lang_zh), "en" to stringResource(id = R.string.lang_en))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.dialog_lang_title), fontWeight = FontWeight.Bold) },
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
                                    "$name${if (code == currentLanguage) ", " + context.getString(R.string.dialog_selected) else ""}"
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
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.dialog_cancel)) } },
    )
}

@Composable
private fun ThemeDialog(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val themes = listOf(
        "system" to stringResource(id = R.string.theme_system),
        "light"  to stringResource(id = R.string.theme_light),
        "dark"   to stringResource(id = R.string.theme_dark),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.dialog_theme_title), fontWeight = FontWeight.Bold) },
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
                                    "$name${if (code == currentTheme) ", " + context.getString(R.string.dialog_selected) else ""}"
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
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.dialog_cancel)) } },
    )
}

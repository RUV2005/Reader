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

// ==================== 主屏幕 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToAccessibility: () -> Unit = {},
    onNavigateToGesture: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onNavigateToFeedback: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showSpeechRateDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog    by remember { mutableStateOf(false) }
    var showLanguageDialog    by remember { mutableStateOf(false) }
    var showThemeDialog       by remember { mutableStateOf(false) }

    val settingGroups = remember(uiState) {
        listOf(
            SettingGroup(
                title = "阅读设置",
                items = listOf(
                    SettingItem(
                        id = "tts",
                        title = "语音朗读",
                        subtitle = if (uiState.ttsEnabled) "已开启，自动朗读文档内容" else "已关闭",
                        iconRes = R.drawable.ic_tts,
                        iconColor = Color(0xFF4A6FA5),
                        type = SettingType.TOGGLE,
                    ),
                    SettingItem(
                        id = "speech_rate",
                        title = "默认语速",
                        subtitle = "当前: ${uiState.speechRate}x",
                        iconRes = R.drawable.ic_speed,
                        iconColor = Color(0xFF6B8CBB),
                        type = SettingType.VALUE,
                    ),
                    SettingItem(
                        id = "font_size",
                        title = "字体大小",
                        subtitle = "当前: ${uiState.fontSize}sp",
                        iconRes = R.drawable.ic_text_size,
                        iconColor = Color(0xFF8B9DC3),
                        type = SettingType.VALUE,
                    ),
                    SettingItem(
                        id = "auto_scroll",
                        title = "自动滚动",
                        subtitle = if (uiState.autoScroll) "朗读时自动滚动到当前位置" else "自动滚动已关闭",
                        iconRes = R.drawable.ic_scroll,
                        iconColor = Color(0xFF4A6FA5),
                        type = SettingType.TOGGLE,
                    ),
                ),
            ),
            SettingGroup(
                title = "无障碍",
                items = listOf(
                    SettingItem(
                        id = "high_contrast",
                        title = "高对比度模式",
                        subtitle = if (uiState.highContrast) "已开启，增强文字与背景对比度" else "增强文字与背景对比度",
                        iconRes = R.drawable.ic_contrast,
                        iconColor = Color(0xFF217346),
                        type = SettingType.TOGGLE,
                    ),
                    SettingItem(
                        id = "gesture",
                        title = "手势控制",
                        subtitle = "配置滑动和点击手势",
                        iconRes = R.drawable.ic_gesture,
                        iconColor = Color(0xFFD24726),
                        type = SettingType.NAVIGATE,
                    ),
                    SettingItem(
                        id = "accessibility",
                        title = "TalkBack 优化",
                        subtitle = "优化屏幕阅读器体验",
                        iconRes = R.drawable.ic_accessibility,
                        iconColor = Color(0xFF4A6FA5),
                        type = SettingType.NAVIGATE,
                    ),
                ),
            ),
            SettingGroup(
                title = "通用",
                items = listOf(
                    SettingItem(
                        id = "storage",
                        title = "存储管理",
                        subtitle = "清理缓存、管理下载文件",
                        iconRes = R.drawable.ic_storage,
                        iconColor = Color(0xFF6B8CBB),
                        type = SettingType.NAVIGATE,
                    ),
                    SettingItem(
                        id = "language",
                        title = "语言",
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
                        title = "主题",
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
                title = "关于",
                items = listOf(
                    SettingItem(
                        id = "about",
                        title = "关于应用",
                        subtitle = "版本 1.0.0",
                        iconRes = R.drawable.ic_info,
                        iconColor = Color(0xFF999999),
                        type = SettingType.NAVIGATE,
                    ),
                    SettingItem(
                        id = "feedback",
                        title = "反馈建议",
                        subtitle = "帮助我们改进产品",
                        iconRes = R.drawable.ic_feedback,
                        iconColor = Color(0xFF999999),
                        type = SettingType.NAVIGATE,
                    ),
                    SettingItem(
                        id = "privacy",
                        title = "隐私政策",
                        iconRes = R.drawable.ic_privacy,
                        iconColor = Color(0xFF999999),
                        type = SettingType.NAVIGATE,
                    ),
                ),
            ),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "设置",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.semantics { contentDescription = "返回" },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4A6FA5),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { paddingValues ->
        // 外层 Column：顶部固定预览，下方列表独立滚动
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5)),
        ) {
            // ── 吸顶预览区 ────────────────────────────────
            SettingsPreviewSection(
                fontSize    = uiState.fontSize,
                highContrast = uiState.highContrast,
                speechRate  = uiState.speechRate,
                ttsEnabled  = uiState.ttsEnabled,
            )

            // 分隔阴影线，视觉上区隔预览与列表
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .shadow(elevation = 4.dp)
                    .background(Color(0xFFE0E0E0)),
            )

            // ── 可滚动设置列表 ────────────────────────────
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
                                "about"         -> onNavigateToAbout()
                                "accessibility" -> onNavigateToAccessibility()
                                "gesture"       -> onNavigateToGesture()
                                "storage"       -> onNavigateToStorage()
                                "feedback"      -> onNavigateToFeedback()
                                "privacy"       -> onNavigateToPrivacy()
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
            }
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
        tts?.setSpeechRate(speechRate)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?)  { isPlaying = true }
            override fun onDone(utteranceId: String?)   { isPlaying = false }
            override fun onError(utteranceId: String?)  { isPlaying = false }
            override fun onStop(utteranceId: String?, interrupted: Boolean) { isPlaying = false }
        })
        tts?.speak(PREVIEW_TEXT, TextToSpeech.QUEUE_FLUSH, null, "settings_preview")
    }

    fun stopPreview() {
        tts?.stop()
        isPlaying = false
    }

    // 颜色随高对比度平滑过渡
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
        // 标题行：说明这是预览区
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
            // 状态标签
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

        // 当前高亮段落（模拟朗读中的高亮）
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

        // 普通正文
        Text(
            text = "江天一色无纤尘，皎皎空中孤月轮。",
            fontSize = fontSize.sp,
            color = bodyTextColor,
            lineHeight = (fontSize + 10).sp,
            modifier = Modifier.semantics {
                contentDescription = "普通正文预览，字体大小 ${fontSize}sp"
            },
        )

        // TTS 试听按钮
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

        // 高对比度开启时的补充说明
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

// ==================== TTS 试听按钮 ====================

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

// ==================== 预览标签 ====================

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

// ==================== 设置分组和行 ====================

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

// ==================== 对话框 ====================

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
    // 每个 size 对应：显示标签、示例文字
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
                        // 标签字号固定，不随选项变化
                        Text(
                            text = opt.label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF4A6FA5) else Color(0xFF333333),
                            modifier = Modifier.weight(1f),
                        )
                        // 右侧用实际字号渲染单个汉字作为直观预览
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
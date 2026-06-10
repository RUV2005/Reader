package com.danmo.reader.word

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danmo.reader.R
import com.danmo.reader.tts.TtsCallbacks
import com.danmo.reader.tts.TtsController
import com.danmo.reader.tts.TtsState
import com.danmo.reader.tts.rememberTtsController
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

// ==================== 数据模型 ====================

data class WordDocument(
    val filePath: String,
    val fileName: String,
    val paragraphs: List<String>,
    val lastReadIndex: Int = 0
)

// ==================== 模拟数据 ====================

val sampleWordDoc = WordDocument(
    filePath = "/storage/documents/项目计划书.docx",
    fileName = "项目计划书.docx",
    paragraphs = listOf(
        "第一章 项目概述",
        "本项目旨在开发一款面向视障用户的文档阅读辅助应用，帮助视障人士高效阅读Word、Excel、PPT和PDF等办公文档。",
        "1.1 项目背景",
        "随着信息无障碍建设的推进，视障群体对数字化办公文档的阅读需求日益增长。然而，现有的通用阅读软件在文档格式支持、语音朗读体验和无障碍交互方面仍存在不足。",
        "1.2 项目目标",
        "我们的目标是打造一款专为视障用户设计的文档阅读工具，具备以下核心能力：支持多种办公文档格式解析、智能语音朗读、手势快捷操作、阅读进度记忆等功能。",
        "第二章 技术方案",
        "2.1 文档解析",
        "采用Apache POI库解析Word文档，提取纯文本内容并按段落拆分，保留文档的层级结构信息。",
        "2.2 语音合成",
        "集成系统TTS引擎，支持语速、音调调节。针对长文本进行智能分段，确保朗读流畅自然。",
        "第三章 实施计划",
        "第一阶段：完成基础阅读功能，包括文档打开、文本提取、语音朗读和进度记忆。",
        "第二阶段：优化无障碍交互，支持TalkBack、高对比度模式、手势控制和语音搜索。"
    ),
    lastReadIndex = 0
)

// ==================== Word阅读页面 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordReaderScreen(
    document: WordDocument = sampleWordDoc,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 当前段落索引
    var currentParagraphIndex by remember { mutableIntStateOf(document.lastReadIndex) }
    var isSpeaking by remember { mutableStateOf(false) }

    // TTS 回调实现
    val ttsCallbacks = remember(document) {
        object : TtsCallbacks {
            override fun onUtteranceDone(): Boolean {
                return currentParagraphIndex < document.paragraphs.size - 1
            }

            override fun getCurrentText(): String {
                return document.paragraphs.getOrNull(currentParagraphIndex) ?: ""
            }

            override fun getCurrentUtteranceId(): String {
                return "word_para_$currentParagraphIndex"
            }

            override fun moveToNext() {
                if (currentParagraphIndex < document.paragraphs.size - 1) {
                    currentParagraphIndex++
                }
            }

            override fun moveToPrevious() {
                if (currentParagraphIndex > 0) {
                    currentParagraphIndex--
                }
            }
        }
    }

    // TTS 控制器
    val ttsController = rememberTtsController(callbacks = ttsCallbacks)

    // 监听 TTS 状态
    LaunchedEffect(ttsController) {
        ttsController.state.collectLatest { state ->
            isSpeaking = state is TtsState.Speaking
        }
    }

    // 自动滚动到当前段落
    LaunchedEffect(currentParagraphIndex) {
        // 保存进度（实际应用应写入数据库）
        // saveProgress(document.filePath, currentParagraphIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = document.fileName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            ttsController.stop()
                            onBackClick()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "返回，当前朗读将暂停"
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.semantics {
                            contentDescription = "阅读设置"
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2B579A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            ReaderControlBar(
                isSpeaking = isSpeaking,
                currentIndex = currentParagraphIndex,
                totalCount = document.paragraphs.size,
                speechRate = ttsController.speechRate.collectAsState().value,
                onPrevious = { ttsController.speakPrevious() },
                onPlayPause = { ttsController.togglePlayPause() },
                onNext = { ttsController.speakNext() },
                onRateChange = { ttsController.setSpeechRate(it) }
            )
        }
    ) { paddingValues ->
        // 内容区域 - 支持双击/三击手势 + 滑动手势
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF1A1A1A))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { ttsController.speakNext() },
                        onTap = { /* 单击可显示当前段落信息 */ }
                    )
                }
                .pointerInput(Unit) {
                    var swipeDirection = -1
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val (x, y) = dragAmount
                            if (kotlin.math.abs(x) > kotlin.math.abs(y)) {
                                when {
                                    x > 0 -> swipeDirection = 0 // 右滑
                                    x < 0 -> swipeDirection = 1 // 左滑
                                }
                            } else {
                                when {
                                    y > 0 -> swipeDirection = 2 // 下滑
                                    y < 0 -> swipeDirection = 3 // 上滑
                                }
                            }
                        },
                        onDragEnd = {
                            when (swipeDirection) {
                                0 -> ttsController.speakPrevious() // 右滑 → 上一段
                                1 -> ttsController.speakNext()     // 左滑 → 下一段
                                2 -> { /* 下滑 */ }
                                3 -> { /* 上滑 */ }
                            }
                            swipeDirection = -1
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                document.paragraphs.forEachIndexed { index, paragraph ->
                    val isCurrent = index == currentParagraphIndex
                    val isHeading = paragraph.startsWith("第") && paragraph.contains("章") ||
                            paragraph.matches(Regex("""^\\d+\\.\\d+.*"""))

                    ParagraphItem(
                        text = paragraph,
                        isCurrent = isCurrent,
                        isHeading = isHeading,
                        index = index,
                        onClick = {
                            currentParagraphIndex = index
                            ttsController.speakCurrent()
                        }
                    )

                    if (index < document.paragraphs.size - 1) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 底部留白，避免被控制栏遮挡
                Spacer(modifier = Modifier.height(80.dp))
            }

            // 当前段落指示器（悬浮在右侧）
            CurrentParagraphIndicator(
                currentIndex = currentParagraphIndex,
                totalCount = document.paragraphs.size,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

// ==================== 段落项 ====================

@Composable
fun ParagraphItem(
    text: String,
    isCurrent: Boolean,
    isHeading: Boolean,
    index: Int,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isCurrent -> Color(0xFF2B579A).copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    val textColor = when {
        isCurrent -> Color(0xFFFFFF00) // 高亮黄色
        isHeading -> Color(0xFF6B8CBB) // 标题蓝色
        else -> Color.White
    }

    val fontSize = when {
        isHeading -> 22.sp
        else -> 20.sp
    }

    val fontWeight = when {
        isHeading -> FontWeight.Bold
        isCurrent -> FontWeight.Bold
        else -> FontWeight.Normal
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics {
                contentDescription = if (isHeading) "标题：$text" else "第${index + 1}段，$text"
            }
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor,
            lineHeight = 32.sp,
            textAlign = TextAlign.Start
        )
    }
}

// ==================== 当前段落指示器 ====================

@Composable
fun CurrentParagraphIndicator(
    currentIndex: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2B579A).copy(alpha = 0.8f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${currentIndex + 1}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "/",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = "$totalCount",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

// ==================== 阅读控制栏 ====================

@Composable
fun ReaderControlBar(
    isSpeaking: Boolean,
    currentIndex: Int,
    totalCount: Int,
    speechRate: Float,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRateChange: (Float) -> Unit
) {
    var showRateMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF2B2B2B),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // 进度条
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / totalCount.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF4A6FA5),
                trackColor = Color(0xFF444444)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 控制按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 语速调节
                Box {
                    ControlButton(
                        iconRes = R.drawable.ic_speed,
                        label = "${(speechRate * 100).toInt()}%",
                        onClick = { showRateMenu = !showRateMenu },
                        buttonDescription = "当前语速${(speechRate * 100).toInt()}%，点击调节"
                    )

                    // 语速菜单
                    DropdownMenu(
                        expanded = showRateMenu,
                        onDismissRequest = { showRateMenu = false },
                        modifier = Modifier.background(Color(0xFF333333))
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { rate ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${(rate * 100).toInt()}%",
                                        color = if (rate == speechRate) Color(0xFF4A6FA5) else Color.White,
                                        fontWeight = if (rate == speechRate) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onRateChange(rate)
                                    showRateMenu = false
                                }
                            )
                        }
                    }
                }

                // 上一段
                ControlButton(
                    iconRes = R.drawable.ic_previous,
                    label = "上段",
                    onClick = onPrevious,
                    buttonDescription = "朗读上一段"
                )

                // 播放/暂停
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4A6FA5))
                        .clickable(onClick = onPlayPause)
                        .semantics {
                            contentDescription = if (isSpeaking) "暂停朗读" else "开始朗读"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isSpeaking) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // 下一段
                ControlButton(
                    iconRes = R.drawable.ic_next,
                    label = "下段",
                    onClick = onNext,
                    buttonDescription = "朗读下一段"
                )

                // 段落跳转
                ControlButton(
                    iconRes = R.drawable.ic_chapters,
                    label = "${currentIndex + 1}/$totalCount",
                    onClick = { /* TODO: 显示段落列表 */ },
                    buttonDescription = "当前第${currentIndex + 1}段，共$totalCount 段，点击跳转"
                )
            }
        }
    }
}

// ==================== 控制按钮 ====================

@Composable
fun ControlButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    buttonDescription: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .semantics { contentDescription = buttonDescription }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color(0xFFCCCCCC),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFFCCCCCC)
        )
    }
}

// ==================== 预览 ====================

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun WordReaderScreenPreview() {
    MaterialTheme {
        WordReaderScreen()
    }
}
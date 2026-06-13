package com.danmo.reader.word

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
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
import com.danmo.reader.common.ReaderControlBar
import com.danmo.reader.data.repository.SettingsRepository
import com.danmo.reader.tts.TtsCallbacks
import com.danmo.reader.tts.TtsState
import com.danmo.reader.tts.rememberTtsController
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

// ==================== 数据模型 ====================

data class WordDocument(
    val filePath: String,
    val fileName: String,
    val paragraphs: List<String>,
    val lastReadIndex: Int = 0,
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
        "第二阶段：优化无障碍交互，支持TalkBack、高对比度模式、手势控制和语音搜索。",
    ),
    lastReadIndex = 0,
)

// ==================== Word阅读页面 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordReaderScreen(
    document: WordDocument = sampleWordDoc,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val settingsRepository = remember(context) { SettingsRepository(context) }

    var fontSize by remember { mutableIntStateOf(18) }
    LaunchedEffect(Unit) {
        settingsRepository.fontSize.collect { size ->
            fontSize = size
        }
    }

    var currentParagraphIndex by remember { mutableIntStateOf(document.lastReadIndex) }
    var isSpeaking by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    var viewportHeight by remember { mutableIntStateOf(0) }
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }

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

    val ttsController = rememberTtsController(callbacks = ttsCallbacks)

    LaunchedEffect(ttsController) {
        ttsController.state.collectLatest { state ->
            isSpeaking = state is TtsState.Speaking
        }
    }

    LaunchedEffect(currentParagraphIndex) {
        kotlinx.coroutines.delay(50)
        val itemHeight = itemHeights[currentParagraphIndex] ?: 0
        val viewportCenter = viewportHeight / 2
        val scrollOffset = if (itemHeight > 0) {
            -viewportCenter + itemHeight / 2
        } else {
            -viewportCenter + 40
        }
        lazyListState.animateScrollToItem(
            index = currentParagraphIndex,
            scrollOffset = scrollOffset
        )
    }

    // 横屏布局：Row(内容 | 控制栏)
    // 竖屏布局：Scaffold(topBar + bottomBar)
    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A1A)),
        ) {
            // 左侧内容区
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                // 顶部栏
                TopAppBar(
                    title = {
                        Text(
                            text = document.fileName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
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
                            },
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.semantics {
                                contentDescription = "阅读设置"
                            },
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_settings),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF2B579A),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    ),
                )

                // 内容区域
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            viewportHeight = coordinates.size.height
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { ttsController.speakNext() },
                                onTap = { },
                            )
                        }
                        .pointerInput(Unit) {
                            var swipeDirection = -1
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val (x, y) = dragAmount
                                    if (abs(x) > abs(y)) {
                                        when {
                                            x > 0 -> swipeDirection = 0
                                            x < 0 -> swipeDirection = 1
                                        }
                                    } else {
                                        when {
                                            y > 0 -> swipeDirection = 2
                                            y < 0 -> swipeDirection = 3
                                        }
                                    }
                                },
                                onDragEnd = {
                                    when (swipeDirection) {
                                        0 -> ttsController.speakPrevious()
                                        1 -> ttsController.speakNext()
                                        2 -> { }
                                        3 -> { }
                                    }
                                    swipeDirection = -1
                                },
                            )
                        },
                ) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        itemsIndexed(
                            items = document.paragraphs,
                            key = { index, _ -> "word_para_$index" }
                        ) { index, paragraph ->
                            val isCurrent = index == currentParagraphIndex
                            val isHeading = paragraph.startsWith("第") && paragraph.contains("章") ||
                                    paragraph.matches(Regex("""^\d+\.\d+.*"""))

                            Box(
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    itemHeights[index] = coordinates.size.height
                                }
                            ) {
                                ParagraphItem(
                                    text = paragraph,
                                    isCurrent = isCurrent,
                                    isHeading = isHeading,
                                    index = index,
                                    fontSize = fontSize,
                                    onClick = {
                                        currentParagraphIndex = index
                                        ttsController.speakCurrent()
                                    },
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }

                    CurrentParagraphIndicator(
                        currentIndex = currentParagraphIndex,
                        totalCount = document.paragraphs.size,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }

            // 右侧控制栏
            ReaderControlBar(
                isSpeaking = isSpeaking,
                currentIndex = currentParagraphIndex,
                totalCount = document.paragraphs.size,
                speechRate = ttsController.speechRate.collectAsState().value,
                accentColor = Color(0xFF4A6FA5),
                progressColor = Color(0xFF4A6FA5),
                previousLabel = "上段",
                nextLabel = "下段",
                positionText = "${currentParagraphIndex + 1}/${document.paragraphs.size}",
                onPrevious = { ttsController.speakPrevious() },
                onPlayPause = { ttsController.togglePlayPause() },
                onNext = { ttsController.speakNext() },
                onRateChange = { ttsController.setSpeechRate(it) },
            )
        }
    } else {
        // 竖屏：保持原有 Scaffold 布局
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = document.fileName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
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
                            },
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.semantics {
                                contentDescription = "阅读设置"
                            },
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_settings),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF2B579A),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    ),
                )
            },
            bottomBar = {
                ReaderControlBar(
                    isSpeaking = isSpeaking,
                    currentIndex = currentParagraphIndex,
                    totalCount = document.paragraphs.size,
                    speechRate = ttsController.speechRate.collectAsState().value,
                    accentColor = Color(0xFF4A6FA5),
                    progressColor = Color(0xFF4A6FA5),
                    previousLabel = "上段",
                    nextLabel = "下段",
                    positionText = "${currentParagraphIndex + 1}/${document.paragraphs.size}",
                    onPrevious = { ttsController.speakPrevious() },
                    onPlayPause = { ttsController.togglePlayPause() },
                    onNext = { ttsController.speakNext() },
                    onRateChange = { ttsController.setSpeechRate(it) },
                )
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFF1A1A1A))
                    .onGloballyPositioned { coordinates ->
                        viewportHeight = coordinates.size.height
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { ttsController.speakNext() },
                            onTap = { },
                        )
                    }
                    .pointerInput(Unit) {
                        var swipeDirection = -1
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val (x, y) = dragAmount
                                if (abs(x) > abs(y)) {
                                    when {
                                        x > 0 -> swipeDirection = 0
                                        x < 0 -> swipeDirection = 1
                                    }
                                } else {
                                    when {
                                        y > 0 -> swipeDirection = 2
                                        y < 0 -> swipeDirection = 3
                                    }
                                }
                            },
                            onDragEnd = {
                                when (swipeDirection) {
                                    0 -> ttsController.speakPrevious()
                                    1 -> ttsController.speakNext()
                                    2 -> { }
                                    3 -> { }
                                }
                                swipeDirection = -1
                            },
                        )
                    },
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    itemsIndexed(
                        items = document.paragraphs,
                        key = { index, _ -> "word_para_$index" }
                    ) { index, paragraph ->
                        val isCurrent = index == currentParagraphIndex
                        val isHeading = paragraph.startsWith("第") && paragraph.contains("章") ||
                                paragraph.matches(Regex("""^\d+\.\d+.*"""))

                        Box(
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                itemHeights[index] = coordinates.size.height
                            }
                        ) {
                            ParagraphItem(
                                text = paragraph,
                                isCurrent = isCurrent,
                                isHeading = isHeading,
                                index = index,
                                fontSize = fontSize,
                                onClick = {
                                    currentParagraphIndex = index
                                    ttsController.speakCurrent()
                                },
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }

                CurrentParagraphIndicator(
                    currentIndex = currentParagraphIndex,
                    totalCount = document.paragraphs.size,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
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
    fontSize: Int,
    onClick: () -> Unit,
) {
    val backgroundColor = when {
        isCurrent -> Color(0xFF2B579A).copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    val textColor = when {
        isCurrent -> Color(0xFFFFFF00)
        isHeading -> Color(0xFF6B8CBB)
        else -> Color.White
    }

    val fontSizeSp = when {
        isHeading -> (fontSize + 4).sp
        else -> fontSize.sp
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
            },
    ) {
        Text(
            text = text,
            fontSize = fontSizeSp,
            fontWeight = fontWeight,
            color = textColor,
            lineHeight = (fontSize + 12).sp,
            textAlign = TextAlign.Start,
        )
    }
}

// ==================== 当前段落指示器 ====================

@Composable
fun CurrentParagraphIndicator(
    currentIndex: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2B579A).copy(alpha = 0.8f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${currentIndex + 1}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = "/",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
        )
        Text(
            text = "$totalCount",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
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
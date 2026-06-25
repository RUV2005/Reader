package com.danmo.reader.word

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import coil.compose.AsyncImage
import com.danmo.reader.R
import com.danmo.reader.common.ReaderControlBar
import com.danmo.reader.data.repository.SettingsRepository
import com.danmo.reader.tts.TtsCallbacks
import com.danmo.reader.tts.TtsState
import com.danmo.reader.tts.rememberTtsController
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

// ==================== 数据模型 ====================

/**
 * Word 文档内容的抽象定义
 */
sealed class WordContent {
    // 普通文本或标题
    data class Text(val text: String, val isHeading: Boolean = false, val index: Int = 0) : WordContent()
    // 文档内嵌图片
    data class Image(val imagePath: String, val description: String? = null, val index: Int = 0) : WordContent()
    // 结构化表格
    data class Table(val rows: List<List<String>>, val index: Int = 0) : WordContent()
}

/**
 * Word 文档完整对象
 */
data class WordDocument(
    val filePath: String,       // 文件 URI 字符串
    val fileName: String,       // 文件显示名
    val contents: List<WordContent>, // 内容项列表（按文档原始顺序排列）
    val lastReadIndex: Int = 0, // 上次阅读/朗读到的索引位置
)

// ==================== Word 阅读主屏幕 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordReaderScreen(
    document: WordDocument,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val settingsRepository = remember(context) { SettingsRepository(context) }

    // 1. 订阅字体大小设置
    var fontSize by remember { mutableIntStateOf(18) }
    LaunchedEffect(Unit) {
        settingsRepository.fontSize.collect { size ->
            fontSize = size
        }
    }

    // 2. 阅读状态管理
    var currentParagraphIndex by remember { mutableIntStateOf(document.lastReadIndex) }
    var isSpeaking by remember { mutableStateOf(false) }

    // 3. 滚动位置同步
    val lazyListState = rememberLazyListState()
    var viewportHeight by remember { mutableIntStateOf(0) }
    val itemHeights = remember { mutableStateMapOf<Int, Int>() } // 存储每个条目的动态高度

    // 4. TTS 核心回调逻辑定义
    val ttsCallbacks = remember(document) {
        object : TtsCallbacks {
            override fun onUtteranceDone(): Boolean {
                // 判断是否已经读到了最后一段
                return currentParagraphIndex < document.contents.size - 1
            }

            override fun getCurrentText(): String {
                // 根据当前条目类型返回不同的朗读文本
                return when (val content = document.contents.getOrNull(currentParagraphIndex)) {
                    is WordContent.Text -> content.text
                    is WordContent.Image -> content.description ?: "插图"
                    is WordContent.Table -> {
                        val rowCount = content.rows.size
                        val colCount = content.rows.firstOrNull()?.size ?: 0
                        val tableSummary = "表格，共${rowCount}行${colCount}列。"
                        val firstRow = content.rows.firstOrNull()?.joinToString(separator = "，") ?: ""
                        if (firstRow.isNotEmpty()) {
                            "$tableSummary。第一行内容为：$firstRow"
                        } else {
                            tableSummary
                        }
                    }
                    null -> ""
                }
            }

            override fun getCurrentUtteranceId(): String {
                return "word_content_$currentParagraphIndex"
            }

            override fun moveToNext() {
                if (currentParagraphIndex < document.contents.size - 1) {
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

    // 初始化控制器
    val ttsController = rememberTtsController(callbacks = ttsCallbacks)

    // 监听 TTS 状态以同步播放按钮 UI
    LaunchedEffect(ttsController) {
        ttsController.state.collectLatest { state ->
            isSpeaking = state is TtsState.Speaking
        }
    }

    // 当 currentParagraphIndex 改变时（无论是点击还是自动切换），自动滚动到视图中心
    LaunchedEffect(currentParagraphIndex) {
        kotlinx.coroutines.delay(timeMillis = 50)
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

    // 5. 渲染布局（区分横竖屏）
    if (isLandscape) {
        // ── 横屏布局 ────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A1A)),
        ) {
            // 左侧主要内容区
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                // 顶部标题栏
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
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = "返回",
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_settings),
                                contentDescription = "设置",
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

                // 内容列表区
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { viewportHeight = it.size.height }
                ) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        itemsIndexed(
                            items = document.contents,
                            key = { index, _ -> "word_content_$index" }
                        ) { index, content ->
                            val isCurrent = index == currentParagraphIndex

                            Box(
                                modifier = Modifier.onGloballyPositioned { 
                                    itemHeights[index] = it.size.height 
                                }
                            ) {
                                when (content) {
                                    is WordContent.Text -> {
                                        ParagraphItem(
                                            text = content.text,
                                            isCurrent = isCurrent,
                                            isHeading = content.isHeading,
                                            index = index,
                                            fontSize = fontSize,
                                            onClick = {
                                                currentParagraphIndex = index
                                                ttsController.speakCurrent()
                                            },
                                        )
                                    }
                                    is WordContent.Image -> {
                                        ImageItem(
                                            imagePath = content.imagePath,
                                            description = content.description,
                                            isCurrent = isCurrent,
                                            onClick = {
                                                currentParagraphIndex = index
                                                ttsController.speakCurrent()
                                            }
                                        )
                                    }
                                    is WordContent.Table -> {
                                        TableItem(
                                            rows = content.rows,
                                            isCurrent = isCurrent,
                                            onClick = {
                                                currentParagraphIndex = index
                                                ttsController.speakCurrent()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        // 底部占位，防止最后一段被控制栏遮挡
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }

                    // 右侧位置指示器
                    CurrentParagraphIndicator(
                        currentIndex = currentParagraphIndex,
                        totalCount = document.contents.size,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }

            // 右侧固定控制栏
            ReaderControlBar(
                isSpeaking = isSpeaking,
                currentIndex = currentParagraphIndex,
                totalCount = document.contents.size,
                speechRate = ttsController.speechRate.collectAsState().value,
                accentColor = Color(0xFF4A6FA5),
                progressColor = Color(0xFF4A6FA5),
                previousLabel = "上段",
                nextLabel = "下段",
                positionText = "${currentParagraphIndex + 1}/${document.contents.size}",
                onPrevious = { ttsController.speakPrevious() },
                onPlayPause = { ttsController.togglePlayPause() },
                onNext = { ttsController.speakNext() },
                onRateChange = { ttsController.setSpeechRate(it) },
            )
        }
    } else {
        // ── 竖屏布局 ────────────────────────────────
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
                        IconButton(onClick = { ttsController.stop(); onBackClick() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = "返回",
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_settings),
                                contentDescription = "设置",
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
                    totalCount = document.contents.size,
                    speechRate = ttsController.speechRate.collectAsState().value,
                    accentColor = Color(0xFF4A6FA5),
                    progressColor = Color(0xFF4A6FA5),
                    previousLabel = "上段",
                    nextLabel = "下段",
                    positionText = "${currentParagraphIndex + 1}/${document.contents.size}",
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
                    .onGloballyPositioned { viewportHeight = it.size.height }
                    .pointerInput(Unit) {
                        // 支持手势：双击朗读下一段
                        detectTapGestures(onDoubleTap = { ttsController.speakNext() })
                    }
                    .pointerInput(Unit) {
                        // 支持手势：滑动翻段
                        var swipeDirection = -1
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val (x, y) = dragAmount
                                if (abs(x) > abs(y)) {
                                    if (x > 0) swipeDirection = 0 // 右滑
                                    if (x < 0) swipeDirection = 1 // 左滑
                                }
                            },
                            onDragEnd = {
                                when (swipeDirection) {
                                    0 -> ttsController.speakPrevious()
                                    1 -> ttsController.speakNext()
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
                        items = document.contents,
                        key = { index, _ -> "word_content_$index" }
                    ) { index, content ->
                        val isCurrent = index == currentParagraphIndex
                        Box(modifier = Modifier.onGloballyPositioned { itemHeights[index] = it.size.height }) {
                            when (content) {
                                is WordContent.Text -> ParagraphItem(
                                    text = content.text,
                                    isCurrent = isCurrent,
                                    isHeading = content.isHeading,
                                    index = index,
                                    fontSize = fontSize,
                                    onClick = { currentParagraphIndex = index; ttsController.speakCurrent() }
                                )
                                is WordContent.Image -> ImageItem(
                                    imagePath = content.imagePath,
                                    description = content.description,
                                    isCurrent = isCurrent,
                                    onClick = { currentParagraphIndex = index; ttsController.speakCurrent() }
                                )
                                is WordContent.Table -> TableItem(
                                    rows = content.rows,
                                    isCurrent = isCurrent,
                                    onClick = { currentParagraphIndex = index; ttsController.speakCurrent() }
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }

                CurrentParagraphIndicator(
                    currentIndex = currentParagraphIndex,
                    totalCount = document.contents.size,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }
}

// ==================== 子组件：文字段落 ====================

@Composable
fun ParagraphItem(
    text: String,
    isCurrent: Boolean,
    isHeading: Boolean,
    index: Int,
    fontSize: Int,
    onClick: () -> Unit,
) {
    // 动态配色逻辑：当前朗读段落使用高对比度黄色，标题使用淡蓝色
    val backgroundColor = if (isCurrent) Color(0xFF2B579A).copy(alpha = 0.3f) else Color.Transparent
    val textColor = when {
        isCurrent -> Color(0xFFFFFF00) // 朗读高亮色
        isHeading -> Color(0xFF6B8CBB) // 标题色
        else -> Color.White            // 普通正文
    }
    val fontSizeSp = if (isHeading) (fontSize + 4).sp else fontSize.sp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics {
                // 语义化标签：方便无障碍引擎（TalkBack）播报
                contentDescription = if (isHeading) "标题：$text" else "第${index + 1}段，$text"
            },
    ) {
        Text(
            text = text,
            fontSize = fontSizeSp,
            fontWeight = if (isHeading || isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            lineHeight = (fontSize + 12).sp,
        )
    }
}

// ==================== 子组件：当前进度指示器 ====================

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
        Text(text = (currentIndex + 1).toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = "/", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
        Text(text = "$totalCount", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

// ==================== 子组件：图片项 ====================

@Composable
fun ImageItem(
    imagePath: String,
    description: String?,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isCurrent) Color(0xFFFFFF00) else Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCurrent) Color(0xFF2B579A).copy(alpha = 0.3f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = imagePath,
            contentDescription = description ?: "图片",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .clip(RoundedCornerShape(4.dp))
                .then(if (isCurrent) Modifier.background(borderColor).padding(2.dp) else Modifier),
        )
        if (!description.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, fontSize = 12.sp, color = if (isCurrent) Color(0xFFFFFF00) else Color.LightGray)
        }
    }
}

// ==================== 子组件：表格项 ====================

@Composable
fun TableItem(
    rows: List<List<String>>,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCurrent) Color(0xFF2B579A).copy(alpha = 0.3f) else Color(0xFF2A2A2A))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .semantics { contentDescription = "表格，共${rows.size}行" },
    ) {
        rows.take(10).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState).padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { cell ->
                    Text(
                        text = cell,
                        fontSize = 14.sp,
                        color = if (isCurrent) Color(0xFFFFFF00) else Color.White,
                        modifier = Modifier.width(120.dp).background(Color.Black.copy(alpha = 0.2f)).padding(6.dp),
                        maxLines = 3,
                    )
                }
            }
        }
        if (rows.size > 10) {
            Text(text = "更多数据请进入表格详情查看", fontSize = 12.sp, color = Color.LightGray, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

// ==================== 界面预览 ====================

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun WordReaderScreenPreview() {
    MaterialTheme {
        WordReaderScreen(
            document = WordDocument(
                filePath = "",
                fileName = "预览文档.docx",
                contents = listOf(WordContent.Text("正在预览文档内容..."))
            )
        )
    }
}

package com.danmo.reader.ppt

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danmo.reader.R
import com.danmo.reader.common.ReaderControlBar
import com.danmo.reader.tts.TtsCallbacks
import com.danmo.reader.tts.TtsState
import com.danmo.reader.tts.rememberTtsController
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

// ==================== 数据模型 ====================

data class PptSlide(
    val slideNumber: Int,
    val title: String,
    val content: List<String>,
    val notes: String = "",
)

data class PptDocument(
    val filePath: String,
    val fileName: String,
    val totalSlides: Int,
    val slides: List<PptSlide>,
    val lastReadSlide: Int = 0,
)

// ==================== 模拟数据 ====================

val samplePptDoc = PptDocument(
    filePath = "/storage/documents/产品发布会.pptx",
    fileName = "产品发布会.pptx",
    totalSlides = 5,
    slides = listOf(
        PptSlide(
            slideNumber = 1,
            title = "封面",
            content = listOf("2024年度产品发布会", "创新科技，引领未来"),
            notes = "这是封面页，包含会议主题和标语",
        ),
        PptSlide(
            slideNumber = 2,
            title = "目录",
            content = listOf("1. 公司介绍", "2. 产品亮点", "3. 市场分析", "4. 未来规划"),
            notes = "目录页，概述本次发布会的主要内容",
        ),
        PptSlide(
            slideNumber = 3,
            title = "公司介绍",
            content = listOf(
                "成立于2018年，专注于人工智能技术研发",
                "核心团队来自顶尖高校和知名企业",
                "已获得三轮融资，估值超过10亿美元",
                "服务客户超过5000家，遍布全球30个国家",
            ),
            notes = "介绍公司背景、团队实力和融资情况",
        ),
        PptSlide(
            slideNumber = 4,
            title = "产品亮点",
            content = listOf(
                "智能语音助手：支持多轮对话和上下文理解",
                "自适应学习系统：根据用户习惯优化推荐",
                "多模态交互：语音、文字、图像全面支持",
                "隐私保护：端侧计算，数据不上云",
            ),
            notes = "重点介绍产品的四大核心亮点",
        ),
        PptSlide(
            slideNumber = 5,
            title = "谢谢观看",
            content = listOf("联系方式：<contact@company.com>", "官方网站：[www.company.com](http://www.company.com)"),
            notes = "结束页，提供联系方式",
        ),
    ),
    lastReadSlide = 0,
)

// ==================== PPT阅读页面 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PptReaderScreen(
    document: PptDocument = samplePptDoc,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var currentSlideIndex by remember { mutableIntStateOf(document.lastReadSlide) }
    var isSpeaking by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    var viewportHeight by remember { mutableIntStateOf(0) }
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }

    val ttsCallbacks = remember(document, showNotes) {
        object : TtsCallbacks {
            override fun onUtteranceDone(): Boolean {
                return currentSlideIndex < document.slides.size - 1
            }

            override fun getCurrentText(): String {
                val slide = document.slides.getOrNull(currentSlideIndex) ?: return ""
                return buildString {
                    append("第${slide.slideNumber}页，${slide.title}。")
                    slide.content.forEach { item ->
                        append("$item。")
                    }
                    if (showNotes && slide.notes.isNotEmpty()) {
                        append("备注：${slide.notes}。")
                    }
                }
            }

            override fun getCurrentUtteranceId(): String {
                return "ppt_slide_$currentSlideIndex"
            }

            override fun moveToNext() {
                if (currentSlideIndex < document.slides.size - 1) {
                    currentSlideIndex++
                }
            }

            override fun moveToPrevious() {
                if (currentSlideIndex > 0) {
                    currentSlideIndex--
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

    LaunchedEffect(showNotes) {
        if (isSpeaking) {
            ttsController.stop()
            kotlinx.coroutines.delay(200)
            ttsController.speakCurrent()
        }
    }

    LaunchedEffect(currentSlideIndex) {
        kotlinx.coroutines.delay(50)
        val itemHeight = itemHeights[currentSlideIndex] ?: 0
        val viewportCenter = viewportHeight / 2
        val scrollOffset = if (itemHeight > 0) {
            -viewportCenter + itemHeight / 2
        } else {
            -viewportCenter + 40
        }
        lazyListState.animateScrollToItem(
            index = currentSlideIndex,
            scrollOffset = scrollOffset
        )
    }

    val topBar: @Composable () -> Unit = {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = document.fileName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text = "共 ${document.totalSlides} 页",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
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
                    onClick = { showNotes = !showNotes },
                    modifier = Modifier.semantics {
                        contentDescription = if (showNotes) "隐藏备注" else "显示备注"
                    },
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (showNotes) R.drawable.ic_notes_visible else R.drawable.ic_notes_hidden,
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
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
                containerColor = Color(0xFFD24726),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White,
            ),
        )
    }

    val controlBar: @Composable () -> Unit = {
        ReaderControlBar(
            isSpeaking = isSpeaking,
            currentIndex = currentSlideIndex,
            totalCount = document.slides.size,
            speechRate = ttsController.speechRate.collectAsState().value,
            accentColor = Color(0xFFD24726),
            progressColor = Color(0xFFD24726),
            previousLabel = "上页",
            nextLabel = "下页",
            positionText = "${currentSlideIndex + 1}/${document.slides.size}",
            onPrevious = { ttsController.speakPrevious() },
            onPlayPause = { ttsController.togglePlayPause() },
            onNext = { ttsController.speakNext() },
            onRateChange = { ttsController.setSpeechRate(it) },
        )
    }

    val content: @Composable (Modifier) -> Unit = { modifier ->
        Box(
            modifier = modifier
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
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(
                    items = document.slides,
                    key = { index, _ -> "ppt_slide_$index" }
                ) { index, slide ->
                    val isCurrent = index == currentSlideIndex

                    Box(
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            itemHeights[index] = coordinates.size.height
                        }
                    ) {
                        SlideCard(
                            slide = slide,
                            isCurrent = isCurrent,
                            showNotes = showNotes,
                            onClick = {
                                currentSlideIndex = index
                                ttsController.speakCurrent()
                            },
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            CurrentSlideIndicator(
                currentIndex = currentSlideIndex,
                totalCount = document.slides.size,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A1A)),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                topBar()
                content(Modifier.fillMaxSize())
            }
            controlBar()
        }
    } else {
        Scaffold(
            topBar = { topBar() },
            bottomBar = { controlBar() },
        ) { paddingValues ->
            content(Modifier.fillMaxSize().padding(paddingValues))
        }
    }
}

// ==================== 幻灯片卡片 ====================

@Composable
fun SlideCard(
    slide: PptSlide,
    isCurrent: Boolean,
    showNotes: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = when {
        isCurrent -> Color(0xFFD24726).copy(alpha = 0.2f)
        else -> Color(0xFF2A2A2A)
    }

    val borderColor = when {
        isCurrent -> Color(0xFFD24726)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append("第${slide.slideNumber}页，${slide.title}。")
                    slide.content.forEach { append("$it。") }
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 1.dp),
        border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, borderColor) else null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isCurrent) Color(0xFFD24726) else Color(0xFF444444)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${slide.slideNumber}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = slide.title,
                    fontSize = if (isCurrent) 20.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) Color(0xFFFFA07A) else Color(0xFFCCCCCC),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            slide.content.forEach { content ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isCurrent) Color(0xFFFFA07A) else Color(0xFF666666))
                            .padding(top = 8.dp),
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = content,
                        fontSize = 15.sp,
                        color = if (isCurrent) Color(0xFFFFFF00) else Color.White,
                        fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                        lineHeight = 24.sp,
                    )
                }
            }

            if (showNotes && slide.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF333333))
                        .padding(12.dp),
                ) {
                    Column {
                        Text(
                            text = "备注",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFAAAAAA),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = slide.notes,
                            fontSize = 14.sp,
                            color = Color(0xFFCCCCCC),
                            lineHeight = 22.sp,
                        )
                    }
                }
            }
        }
    }
}

// ==================== 当前页指示器 ====================

@Composable
fun CurrentSlideIndicator(
    currentIndex: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFD24726).copy(alpha = 0.8f))
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
fun PptReaderScreenPreview() {
    MaterialTheme {
        PptReaderScreen()
    }
}
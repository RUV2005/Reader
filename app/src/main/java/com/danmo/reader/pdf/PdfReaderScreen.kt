package com.danmo.reader.pdf

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

data class PdfPage(
    val pageNumber: Int,
    val paragraphs: List<String>
)

data class PdfDocument(
    val filePath: String,
    val fileName: String,
    val totalPages: Int,
    val pages: List<PdfPage>,
    val lastReadPage: Int = 0,
    val lastReadParagraph: Int = 0
)

// ==================== 模拟数据 ====================

val samplePdfDoc = PdfDocument(
    filePath = "/storage/documents/合同协议.pdf",
    fileName = "合同协议.pdf",
    totalPages = 3,
    pages = listOf(
        PdfPage(
            pageNumber = 1,
            paragraphs = listOf(
                "合同编号：HT-2024-001",
                "甲方（委托方）：科技有限公司",
                "乙方（受托方）：软件开发工作室",
                "签订日期：2024年1月15日",
                "签订地点：北京市海淀区",
                "",
                "鉴于甲方需要开发一套面向视障用户的文档阅读辅助软件，乙方具备相应的技术能力和开发经验，双方经友好协商，达成如下协议。"
            )
        ),
        PdfPage(
            pageNumber = 2,
            paragraphs = listOf(
                "第一条 项目内容",
                "1.1 乙方负责为甲方开发一款支持Word、Excel、PPT、PDF格式的文档阅读应用。",
                "1.2 应用需具备语音朗读、手势控制、高对比度显示等无障碍功能。",
                "1.3 支持Android 8.0及以上系统版本。",
                "",
                "第二条 开发周期",
                "2.1 项目总工期为90个工作日，自合同签订之日起计算。",
                "2.2 乙方应按以下里程碑提交成果：需求分析（15日）、原型设计（20日）、开发实施（40日）、测试验收（15日）。"
            )
        ),
        PdfPage(
            pageNumber = 3,
            paragraphs = listOf(
                "第三条 验收标准",
                "3.1 应用需通过甲方组织的无障碍功能测试，包括TalkBack兼容性、语音朗读准确性、手势操作响应速度等。",
                "3.2 乙方需提供完整的技术文档和用户手册。",
                "",
                "第四条 费用及支付",
                "4.1 项目总费用为人民币伍拾万元整。",
                "4.2 付款方式：合同签订后支付30%，原型确认后支付30%，验收合格后支付40%。",
                "",
                "第五条 保密条款",
                "5.1 双方应对本合同内容及项目相关信息严格保密，未经对方书面同意不得向第三方披露。",
                "",
                "本合同一式两份，甲乙双方各执一份，具有同等法律效力。"
            )
        )
    ),
    lastReadPage = 0,
    lastReadParagraph = 0
)

// ==================== PDF阅读页面 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    document: PdfDocument = samplePdfDoc,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 全局段落索引
    var globalParagraphIndex by remember {
        mutableIntStateOf(
            document.pages.take(document.lastReadPage).sumOf { it.paragraphs.size } + document.lastReadParagraph
        )
    }
    var isSpeaking by remember { mutableStateOf(false) }

    // 计算当前页和段落索引
    fun calculatePageAndParagraph(globalIndex: Int): Pair<Int, Int> {
        var remaining = globalIndex
        for ((pageIdx, page) in document.pages.withIndex()) {
            if (remaining < page.paragraphs.size) {
                return pageIdx to remaining
            }
            remaining -= page.paragraphs.size
        }
        return document.pages.size - 1 to 0
    }

    // 所有段落的扁平列表
    val allParagraphs = remember(document.pages) {
        document.pages.flatMap { it.paragraphs }
    }

    val currentPageIndex = remember(globalParagraphIndex) {
        calculatePageAndParagraph(globalParagraphIndex).first
    }
    val currentParagraphIndex = remember(globalParagraphIndex) {
        calculatePageAndParagraph(globalParagraphIndex).second
    }

    // TTS 回调实现
    val ttsCallbacks = remember(document) {
        object : TtsCallbacks {
            override fun onUtteranceDone(): Boolean {
                return globalParagraphIndex < allParagraphs.size - 1
            }

            override fun getCurrentText(): String {
                val text = allParagraphs.getOrNull(globalParagraphIndex) ?: ""
                // 空段落自动跳过
                if (text.isBlank() && globalParagraphIndex < allParagraphs.size - 1) {
                    globalParagraphIndex++
                    return getCurrentText()
                }
                return text
            }

            override fun getCurrentUtteranceId(): String {
                return "pdf_para_$globalParagraphIndex"
            }

            override fun moveToNext() {
                if (globalParagraphIndex < allParagraphs.size - 1) {
                    globalParagraphIndex++
                }
            }

            override fun moveToPrevious() {
                if (globalParagraphIndex > 0) {
                    globalParagraphIndex--
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = document.fileName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "第 ${currentPageIndex + 1} / ${document.totalPages} 页",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
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
                    containerColor = Color(0xFFB91C1C),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            PdfControlBar(
                isSpeaking = isSpeaking,
                currentPage = currentPageIndex + 1,
                totalPages = document.totalPages,
                currentParagraph = globalParagraphIndex + 1,
                totalParagraphs = allParagraphs.size,
                speechRate = ttsController.speechRate.collectAsState().value,
                onPrevious = { ttsController.speakPrevious() },
                onPlayPause = { ttsController.togglePlayPause() },
                onNext = { ttsController.speakNext() },
                onRateChange = { ttsController.setSpeechRate(it) }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF1A1A1A))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { ttsController.speakNext() },
                        onTap = { /* 单击显示当前段落信息 */ }
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
                document.pages.forEachIndexed { pageIdx, page ->
                    // 页码分隔
                    if (pageIdx > 0) {
                        PageDivider(pageNumber = page.pageNumber)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 页面内容
                    page.paragraphs.forEachIndexed { paraIdx, paragraph ->
                        val isCurrent = pageIdx == currentPageIndex && paraIdx == currentParagraphIndex
                        val isEmpty = paragraph.isBlank()

                        if (!isEmpty) {
                            val globalIdx = document.pages.take(pageIdx).sumOf { it.paragraphs.size } + paraIdx
                            PdfParagraphItem(
                                text = paragraph,
                                isCurrent = isCurrent,
                                pageNumber = page.pageNumber,
                                paragraphNumber = paraIdx + 1,
                                onClick = {
                                    globalParagraphIndex = globalIdx
                                    ttsController.speakCurrent()
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        } else {
                            // 空段落（间距）
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            // 当前位置指示器
            CurrentPositionIndicator(
                currentPage = currentPageIndex + 1,
                totalPages = document.totalPages,
                currentParagraph = globalParagraphIndex + 1,
                totalParagraphs = allParagraphs.size,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

// ==================== 页码分隔线 ====================

@Composable
fun PageDivider(pageNumber: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFF444444))
        )

        Text(
            text = "— 第 $pageNumber 页 —",
            fontSize = 12.sp,
            color = Color(0xFF666666),
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFF444444))
        )
    }
}

// ==================== PDF段落项 ====================

@Composable
fun PdfParagraphItem(
    text: String,
    isCurrent: Boolean,
    pageNumber: Int,
    paragraphNumber: Int,
    onClick: () -> Unit
) {
    val isHeading = text.matches(Regex("""^第[一二三四五六七八九十]+条.*""")) ||
            text.startsWith("合同编号") ||
            text.startsWith("鉴于") ||
            text.startsWith("本合同")

    val backgroundColor = when {
        isCurrent -> Color(0xFFB91C1C).copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    val textColor = when {
        isCurrent -> Color(0xFFFFFF00)
        isHeading -> Color(0xFFFF6B6B)
        else -> Color.White
    }

    val fontSize = when {
        isHeading -> 18.sp
        else -> 16.sp
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
                contentDescription = "第${pageNumber}页第${paragraphNumber}段，$text"
            }
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor,
            lineHeight = 28.sp,
            textAlign = TextAlign.Start
        )
    }
}

// ==================== 当前位置指示器 ====================

@Composable
fun CurrentPositionIndicator(
    currentPage: Int,
    totalPages: Int,
    currentParagraph: Int,
    totalParagraphs: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFB91C1C).copy(alpha = 0.8f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 页码
        Text(
            text = "$currentPage",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "/$totalPages",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 段落进度
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(20.dp)
                .background(Color.White.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((currentParagraph.toFloat() / totalParagraphs.toFloat() * 20).dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.White)
            )
        }
    }
}

// ==================== PDF控制栏 ====================

@Composable
fun PdfControlBar(
    isSpeaking: Boolean,
    currentPage: Int,
    totalPages: Int,
    currentParagraph: Int,
    totalParagraphs: Int,
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
            // 双进度条（页进度 + 段进度）
            Column {
                // 页进度
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "页",
                        fontSize = 10.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.width(20.dp)
                    )
                    LinearProgressIndicator(
                        progress = { currentPage.toFloat() / totalPages.toFloat() },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFFB91C1C),
                        trackColor = Color(0xFF444444)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 段进度
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "段",
                        fontSize = 10.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.width(20.dp)
                    )
                    LinearProgressIndicator(
                        progress = { currentParagraph.toFloat() / totalParagraphs.toFloat() },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFFFF6B6B),
                        trackColor = Color(0xFF444444)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 控制按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 语速调节
                Box {
                    PdfControlButton(
                        iconRes = R.drawable.ic_speed,
                        label = "${(speechRate * 100).toInt()}%",
                        onClick = { showRateMenu = !showRateMenu },
                        buttonDescription = "当前语速${(speechRate * 100).toInt()}%，点击调节"
                    )

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
                                        color = if (rate == speechRate) Color(0xFFB91C1C) else Color.White,
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
                PdfControlButton(
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
                        .background(Color(0xFFB91C1C))
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
                PdfControlButton(
                    iconRes = R.drawable.ic_next,
                    label = "下段",
                    onClick = onNext,
                    buttonDescription = "朗读下一段"
                )

                // 位置显示
                PdfControlButton(
                    iconRes = R.drawable.ic_pages,
                    label = "$currentPage/$totalPages",
                    onClick = { /* TODO: 显示页面列表 */ },
                    buttonDescription = "当前第${currentPage}页共${totalPages}页，第${currentParagraph}段共${totalParagraphs}段，点击跳转"
                )
            }
        }
    }
}

// ==================== PDF控制按钮 ====================

@Composable
fun PdfControlButton(
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
fun PdfReaderScreenPreview() {
    MaterialTheme {
        PdfReaderScreen()
    }
}
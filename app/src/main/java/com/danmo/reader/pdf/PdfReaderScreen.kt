package com.danmo.reader.pdf

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
import com.danmo.reader.tts.TtsCallbacks
import com.danmo.reader.tts.TtsState
import com.danmo.reader.tts.rememberTtsController
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

// ==================== 数据模型 ====================

data class PdfPage(
    val pageNumber: Int,
    val paragraphs: List<String>,
)

data class PdfDocument(
    val filePath: String,
    val fileName: String,
    val totalPages: Int,
    val pages: List<PdfPage>,
    val lastReadPage: Int = 0,
    val lastReadParagraph: Int = 0,
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
                "鉴于甲方需要开发一套面向视障用户的文档阅读辅助软件，乙方具备相应的技术能力和开发经验，双方经友好协商，达成如下协议。",
            ),
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
                "2.2 乙方应按以下里程碑提交成果：需求分析（15日）、原型设计（20日）、开发实施（40日）、测试验收（15日）。",
            ),
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
                "本合同一式两份，甲乙双方各执一份，具有同等法律效力。",
            ),
        ),
    ),
    lastReadPage = 0,
    lastReadParagraph = 0,
)

// ==================== PDF阅读页面 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    document: PdfDocument = samplePdfDoc,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val context = LocalContext.current

    // 全局段落索引
    var globalParagraphIndex by remember {
        val initialValue = document.pages.take(document.lastReadPage.coerceAtLeast(0).coerceAtMost(document.pages.size))
            .sumOf { it.paragraphs.size } + document.lastReadParagraph.coerceAtLeast(0)
        mutableIntStateOf(initialValue.coerceIn(0, (document.pages.flatMap { it.paragraphs }.size - 1).coerceAtLeast(0)))
    }
    var isSpeaking by remember { mutableStateOf(false) }

    // 计算当前页和段落索引
    fun calculatePageAndParagraph(globalIndex: Int): Pair<Int, Int> {
        var remaining = globalIndex.coerceAtLeast(0)
        for ((pageIdx, page) in document.pages.withIndex()) {
            if (remaining < page.paragraphs.size) {
                return pageIdx to remaining
            }
            remaining -= page.paragraphs.size
        }
        return document.pages.size - 1 to 0
    }

    val allParagraphs = remember(document) {
        document.pages.flatMap { it.paragraphs }
    }

    val currentPageIndex = remember(globalParagraphIndex, allParagraphs) {
        calculatePageAndParagraph(globalParagraphIndex).first
    }
    val currentParagraphIndex = remember(globalParagraphIndex, allParagraphs) {
        calculatePageAndParagraph(globalParagraphIndex).second
    }

    // 辅助函数：跳过空段落
    fun skipEmptyParagraphs(startIndex: Int, direction: Int): Int {
        var index = startIndex
        while (index in allParagraphs.indices) {
            if (allParagraphs[index].isNotBlank()) {
                return index
            }
            index += direction
        }
        return startIndex.coerceIn(0, allParagraphs.size - 1)
    }

    // 扁平化的段落列表（带页码信息），用于 LazyColumn
    data class FlatParagraph(val globalIndex: Int, val pageIndex: Int, val paraIndex: Int, val text: String, val pageNumber: Int)

    val flatParagraphs = remember(document) {
        val list = mutableListOf<FlatParagraph>()
        var globalIdx = 0
        document.pages.forEachIndexed { pageIdx, page ->
            page.paragraphs.forEachIndexed { paraIdx, text ->
                list.add(FlatParagraph(globalIdx, pageIdx, paraIdx, text, page.pageNumber))
                globalIdx++
            }
        }
        list
    }

    // LazyListState 用于精确控制滚动位置
    val lazyListState = rememberLazyListState()
    var viewportHeight by remember { mutableIntStateOf(0) }
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }

    // TTS 回调实现
    val ttsCallbacks = remember(document) {
        object : TtsCallbacks {
            override fun onUtteranceDone(): Boolean {
                return globalParagraphIndex < allParagraphs.size - 1
            }

            override fun getCurrentText(): String {
                val text = allParagraphs.getOrNull(globalParagraphIndex) ?: ""
                return if (text.isBlank()) {
                    "空段落"
                } else {
                    text
                }
            }

            override fun getCurrentUtteranceId(): String {
                return "pdf_para_$globalParagraphIndex"
            }

            override fun moveToNext() {
                if (globalParagraphIndex < allParagraphs.size - 1) {
                    globalParagraphIndex++
                    // 跳过连续的空段落
                    while (globalParagraphIndex < allParagraphs.size - 1 &&
                        allParagraphs.getOrNull(globalParagraphIndex)?.isBlank() == true) {
                        globalParagraphIndex++
                    }
                }
            }

            override fun moveToPrevious() {
                if (globalParagraphIndex > 0) {
                    globalParagraphIndex--
                    // 跳过连续的空段落
                    while (globalParagraphIndex > 0 &&
                        allParagraphs.getOrNull(globalParagraphIndex)?.isBlank() == true) {
                        globalParagraphIndex--
                    }
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

    // 核心：当前段落变化时，滚动到屏幕中央
    LaunchedEffect(globalParagraphIndex) {
        kotlinx.coroutines.delay(50)

        val itemHeight = itemHeights[globalParagraphIndex] ?: 0
        val viewportCenter = viewportHeight / 2
        val scrollOffset = if (itemHeight > 0) {
            -viewportCenter + itemHeight / 2
        } else {
            -viewportCenter + 40
        }

        lazyListState.animateScrollToItem(
            index = globalParagraphIndex,
            scrollOffset = scrollOffset
        )
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
                            maxLines = 1,
                        )
                        Text(
                            text = "第 ${currentPageIndex + 1} / ${document.totalPages} 页",
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
                    containerColor = Color(0xFFB91C1C),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            val pdfAccent = Color(0xFFB91C1C)
            ReaderControlBar(
                isSpeaking = isSpeaking,
                currentIndex = currentPageIndex,
                totalCount = document.totalPages,
                speechRate = ttsController.speechRate.collectAsState().value,
                accentColor = pdfAccent,
                progressColor = pdfAccent,
                previousLabel = "上段",
                nextLabel = "下段",
                positionText = "${currentPageIndex + 1}/${document.totalPages}",
                onPrevious = { ttsController.speakPrevious() },
                onPlayPause = { ttsController.togglePlayPause() },
                onNext = { ttsController.speakNext() },
                onRateChange = { ttsController.setSpeechRate(it) },
                progressBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "页",
                                fontSize = 10.sp,
                                color = Color(0xFF888888),
                                modifier = Modifier.width(20.dp),
                            )
                            LinearProgressIndicator(
                                progress = { (currentPageIndex + 1).toFloat() / document.totalPages.toFloat() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = pdfAccent,
                                trackColor = Color(0xFF444444),
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "段",
                                fontSize = 10.sp,
                                color = Color(0xFF888888),
                                modifier = Modifier.width(20.dp),
                            )
                            LinearProgressIndicator(
                                progress = { (globalParagraphIndex + 1).toFloat() / allParagraphs.size.toFloat() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFFFF6B6B),
                                trackColor = Color(0xFF444444),
                            )
                        }
                    }
                },
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(
                    items = flatParagraphs,
                    key = { _, item -> "pdf_para_${item.globalIndex}" }
                ) { _, item ->
                    val isCurrent = item.globalIndex == globalParagraphIndex
                    val isEmpty = item.text.isBlank()

                    if (!isEmpty) {
                        // 页码分隔（每页第一段前显示）
                        if (item.paraIndex == 0 && item.pageIndex > 0) {
                            PageDivider(pageNumber = item.pageNumber)
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Box(
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                itemHeights[item.globalIndex] = coordinates.size.height
                            }
                        ) {
                            PdfParagraphItem(
                                text = item.text,
                                isCurrent = isCurrent,
                                pageNumber = item.pageNumber,
                                paragraphNumber = item.paraIndex + 1,
                                onClick = {
                                    globalParagraphIndex = item.globalIndex
                                    ttsController.speakCurrent()
                                },
                            )
                        }
                    } else {
                        // 空段落作为间距
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            CurrentPositionIndicator(
                currentPage = currentPageIndex + 1,
                totalPages = document.totalPages,
                currentParagraph = globalParagraphIndex + 1,
                totalParagraphs = allParagraphs.size,
                modifier = Modifier.align(Alignment.CenterEnd),
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
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFF444444)),
        )
        Text(
            text = "— 第 $pageNumber 页 —",
            fontSize = 12.sp,
            color = Color(0xFF666666),
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFF444444)),
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
    onClick: () -> Unit,
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
            },
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor,
            lineHeight = 28.sp,
            textAlign = TextAlign.Start,
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFB91C1C).copy(alpha = 0.8f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "$currentPage",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = "/$totalPages",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(20.dp)
                .background(Color.White.copy(alpha = 0.3f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((currentParagraph.toFloat() / totalParagraphs.toFloat() * 20).dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.White),
            )
        }
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
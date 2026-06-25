package com.danmo.reader.pdf

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.danmo.reader.tts.TtsCallbacks
import com.danmo.reader.tts.TtsState
import com.danmo.reader.tts.rememberTtsController
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

// ==================== 数据模型 ====================

data class PdfPage(
    val pageNumber: Int,
    val paragraphs: List<String>,
    val images: List<String> = emptyList(),
)

data class PdfDocument(
    val filePath: String,
    val fileName: String,
    val totalPages: Int,
    val pages: List<PdfPage>,
    val lastReadPage: Int = 0,
    val lastReadParagraph: Int = 0,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    document: PdfDocument,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var globalParagraphIndex by remember {
        val initialValue = document.pages.asSequence().take(document.lastReadPage.coerceAtLeast(0).coerceAtMost(document.pages.size))
            .sumOf { it.paragraphs.size } + document.lastReadParagraph.coerceAtLeast(0)
        mutableIntStateOf(initialValue.coerceIn(0, (document.pages.flatMap { it.paragraphs }.size - 1).coerceAtLeast(0)))
    }
    var isSpeaking by remember { mutableStateOf(value = false) }

    fun calculatePageAndParagraph(globalIndex: Int): Pair<Int, Int> {
        var remaining = globalIndex.coerceAtLeast(0)
        for ((pageIdx, page) in document.pages.withIndex()) {
            if (remaining < page.paragraphs.size) {
                return (pageIdx to remaining)
            }
            remaining -= page.paragraphs.size
        }
        return (document.pages.size - 1 to 0)
    }

    val allParagraphs = remember(document) {
        document.pages.flatMap { it.paragraphs }
    }

    val currentPageIndex = remember(globalParagraphIndex, allParagraphs) {
        calculatePageAndParagraph(globalParagraphIndex).first
    }

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

    val lazyListState = rememberLazyListState()
    var viewportHeight by remember { mutableIntStateOf(0) }
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }

    val ttsCallbacks = remember(document) {
        object : TtsCallbacks {
            override fun onUtteranceDone(): Boolean {
                return globalParagraphIndex < allParagraphs.size - 1
            }

            override fun getCurrentText(): String {
                val text = allParagraphs.getOrNull(globalParagraphIndex) ?: ""
                return text.ifBlank {
                    "空段落"
                }
            }

            override fun getCurrentUtteranceId(): String {
                return "pdf_para_$globalParagraphIndex"
            }

            override fun moveToNext() {
                if (globalParagraphIndex < allParagraphs.size - 1) {
                    globalParagraphIndex++
                    while (globalParagraphIndex < allParagraphs.size - 1 &&
                        allParagraphs.getOrNull(globalParagraphIndex)?.isBlank() == true) {
                        globalParagraphIndex++
                    }
                }
            }

            override fun moveToPrevious() {
                if (globalParagraphIndex > 0) {
                    globalParagraphIndex--
                    while (globalParagraphIndex > 0 &&
                        allParagraphs.getOrNull(globalParagraphIndex)?.isBlank() == true) {
                        globalParagraphIndex--
                    }
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

    LaunchedEffect(globalParagraphIndex) {
        kotlinx.coroutines.delay(timeMillis = 50)
        val itemHeight = itemHeights[globalParagraphIndex] ?: 0
        val viewportCenter = viewportHeight / 2
        val scrollOffset = if (itemHeight > 0) {
            -viewportCenter + itemHeight / 2
        } else {
            -viewportCenter + 40
        }
        lazyListState.animateScrollToItem(
            index = globalParagraphIndex,
            scrollOffset = scrollOffset,
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
    }

    val controlBar: @Composable () -> Unit = {
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
                        // 修复：在 itemsIndexed 的 content 中直接渲染 PageDivider，不用 item()
                        if (item.paraIndex == 0) {
                            if (item.pageIndex > 0) {
                                PageDivider(pageNumber = item.pageNumber)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            // 显示本页图片
                            val page = document.pages.getOrNull(item.pageIndex)
                            if (page != null && page.images.isNotEmpty()) {
                                page.images.forEach { imagePath ->
                                    AsyncImage(
                                        model = imagePath,
                                        contentDescription = "第 ${item.pageNumber} 页插图",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black),
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
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
    
    val isTable = text.startsWith("表格数据：")

    val backgroundColor = when {
        isCurrent -> Color(0xFFB91C1C).copy(alpha = 0.2f)
        isTable -> Color.White.copy(alpha = 0.05f)
        else -> Color.Transparent
    }

    val textColor = when {
        isCurrent -> Color(0xFFFFFF00)
        isHeading -> Color(0xFFFF6B6B)
        isTable -> Color(0xFF6BFF9E)
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
            .then(if (isTable) Modifier.border(0.5.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics {
                contentDescription = "第${pageNumber}页第${paragraphNumber}段，$text"
            },
    ) {
        Text(
            text = if (isTable) text.substringAfter("表格数据：") else text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor,
            lineHeight = 28.sp,
            textAlign = TextAlign.Start,
            modifier = if (isTable) Modifier.horizontalScroll(rememberScrollState()) else Modifier
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
            text = currentPage.toString(),
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
        PdfReaderScreen(
            document = PdfDocument(
                filePath = "",
                fileName = "预览文档.pdf",
                totalPages = 1,
                pages = listOf(PdfPage(1, listOf("内容")))
            )
        )
    }
}

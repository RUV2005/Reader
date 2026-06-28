package com.danmo.reader.ppt

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

data class PptSlide(
    val slideNumber: Int,
    val title: String,
    val content: List<String>,
    val notes: String = "",
    val images: List<String> = emptyList(),
    val tables: List<List<List<String>>> = emptyList(),
)

data class PptDocument(
    val filePath: String,
    val fileName: String,
    val totalSlides: Int,
    val slides: List<PptSlide>,
    val lastReadSlide: Int = 0,
)

// ==================== PPT阅读页面 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PptReaderScreen(
    document: PptDocument,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var currentSlideIndex by remember { mutableIntStateOf(document.lastReadSlide) }
    var isSpeaking by remember { mutableStateOf(value = false) }
    var showNotes by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    var viewportHeight by remember { mutableIntStateOf(0) }
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }

    // 资源获取
    val slideDesc = stringResource(id = R.string.reader_page_format)
    val imageCountDesc = stringResource(id = R.string.desc_image_count)
    val tableCountDesc = stringResource(id = R.string.desc_table_count)
    val tableSummaryDesc = stringResource(id = R.string.desc_table_summary)
    val tableFirstRowDesc = stringResource(id = R.string.desc_table_first_row)
    val notesDesc = stringResource(id = R.string.desc_notes)

    val ttsCallbacks = remember(document, showNotes, currentSlideIndex) {
        object : TtsCallbacks {
            override fun onUtteranceDone(): Boolean {
                return (currentSlideIndex < (document.slides.size - 1))
            }

            override fun getCurrentText(): String {
                val slide = document.slides.getOrNull(currentSlideIndex) ?: return ""
                return buildString {
                    append(String.format(java.util.Locale.getDefault(), slideDesc, slide.slideNumber, document.totalSlides))
                    append("，${slide.title}。")
                    slide.content.forEach { item ->
                        append("$item。")
                    }
                    if (slide.images.isNotEmpty()) {
                        append(String.format(java.util.Locale.getDefault(), imageCountDesc, slide.images.size))
                    }
                    if (slide.tables.isNotEmpty()) {
                        append(String.format(java.util.Locale.getDefault(), tableCountDesc, slide.tables.size))
                        slide.tables.forEachIndexed { index, table ->
                            val rowCount = table.size
                            val colCount = table.firstOrNull()?.size ?: 0
                            append("Table ${index + 1} ")
                            append(String.format(java.util.Locale.getDefault(), tableSummaryDesc, rowCount, colCount))
                            val firstRow = table.firstOrNull()?.joinToString(separator = "，") ?: ""
                            if (firstRow.isNotEmpty()) {
                                append(String.format(java.util.Locale.getDefault(), tableFirstRowDesc, firstRow))
                            }
                        }
                    }
                    if (showNotes && slide.notes.isNotEmpty()) {
                        append(String.format(java.util.Locale.getDefault(), notesDesc, slide.notes))
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
            kotlinx.coroutines.delay(timeMillis = 200)
            ttsController.speakCurrent()
        }
    }

    LaunchedEffect(currentSlideIndex) {
        kotlinx.coroutines.delay(timeMillis = 50)
        val itemHeight = itemHeights[currentSlideIndex] ?: 0
        val viewportCenter = viewportHeight / 2
        val scrollOffset = if (itemHeight > 0) {
            -viewportCenter + itemHeight / 2
        } else {
            -viewportCenter + 40
        }
        lazyListState.animateScrollToItem(
            index = currentSlideIndex,
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
                        text = stringResource(id = R.string.reader_page_format, currentSlideIndex + 1, document.totalSlides),
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
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = stringResource(id = R.string.dialog_close),
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
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White,
            ),
        )
    }

    val controlBar: @Composable () -> Unit = {
        val accentColor = MaterialTheme.colorScheme.primary
        ReaderControlBar(
            isSpeaking = isSpeaking,
            currentIndex = currentSlideIndex,
            totalCount = document.slides.size,
            speechRate = ttsController.speechRate.collectAsState().value,
            accentColor = accentColor,
            progressColor = accentColor,
            previousLabel = stringResource(id = R.string.reader_prev_page),
            nextLabel = stringResource(id = R.string.reader_next_page),
            positionText = stringResource(id = R.string.reader_page_format, currentSlideIndex + 1, document.slides.size),
            onPrevious = { ttsController.speakPrevious() },
            onPlayPause = { ttsController.togglePlayPause() },
            onNext = { ttsController.speakNext() },
            onRateChange = { ttsController.setSpeechRate(it) },
        )
    }

    val content: @Composable (Modifier) -> Unit = { modifier ->
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.background)
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
                                if (x > 0) swipeDirection = 0
                                if (x < 0) swipeDirection = 1
                            }
                        },
                        onDragEnd = {
                            if (swipeDirection == 0) ttsController.speakPrevious()
                            if (swipeDirection == 1) ttsController.speakNext()
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
                .background(MaterialTheme.colorScheme.background),
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
            containerColor = MaterialTheme.colorScheme.background
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
    val highlightColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val backgroundColor = when {
        isCurrent -> highlightColor.copy(alpha = 0.15f)
        else -> surfaceColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Slide ${slide.slideNumber}"
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 1.dp),
        border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, highlightColor) else null,
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
                        .background(if (isCurrent) highlightColor else onSurfaceVariantColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = slide.slideNumber.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) Color.White else onSurfaceColor,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = slide.title,
                    fontSize = if (isCurrent) 20.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) highlightColor else onSurfaceColor,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (slide.images.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(slide.images) { _, imagePath ->
                        AsyncImage(
                            model = imagePath,
                            contentDescription = stringResource(id = R.string.desc_image_general),
                            modifier = Modifier
                                .height(120.dp)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

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
                            .background(if (isCurrent) highlightColor else onSurfaceVariantColor)
                            .padding(top = 8.dp),
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = content,
                        fontSize = 15.sp,
                        color = if (isCurrent) highlightColor else onSurfaceColor,
                        fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                        lineHeight = 24.sp,
                    )
                }
            }

            if (slide.tables.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                slide.tables.forEach { table ->
                    PptTableItem(rows = table, isCurrent = isCurrent)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (showNotes && slide.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                ) {
                    Column {
                        Text(
                            text = stringResource(id = R.string.desc_notes, "").replace(": ", ""),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceVariantColor,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = slide.notes,
                            fontSize = 14.sp,
                            color = onSurfaceColor,
                            lineHeight = 22.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PptTableItem(
    rows: List<List<String>>,
    isCurrent: Boolean,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { cell ->
                    Text(
                        text = cell,
                        fontSize = 12.sp,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .width(100.dp)
                            .padding(2.dp),
                        maxLines = 3,
                        textAlign = TextAlign.Start
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = (currentIndex + 1).toString(),
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
            text = totalCount.toString(),
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
        PptReaderScreen(
            document = PptDocument(
                filePath = "",
                fileName = "预览幻灯片.pptx",
                totalSlides = 1,
                slides = listOf(PptSlide(1, "标题", listOf("内容")))
            )
        )
    }
}

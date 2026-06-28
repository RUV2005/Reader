package com.danmo.reader.word

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

sealed class WordContent {
    data class Text(val text: String, val isHeading: Boolean, val index: Int) : WordContent()
    data class Image(val imagePath: String, val description: String, val index: Int) : WordContent()
    data class Table(val rows: List<List<String>>, val index: Int) : WordContent()
}

data class WordDocument(
    val filePath: String,
    val fileName: String,
    val contents: List<WordContent>,
    val lastReadIndex: Int = 0,
)

// ==================== Word 阅读器屏幕 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordReaderScreen(
    document: WordDocument,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var currentIndex by remember { mutableIntStateOf(document.lastReadIndex) }
    var isSpeaking by remember { mutableStateOf(value = false) }

    val lazyListState = rememberLazyListState()
    var viewportHeight by remember { mutableIntStateOf(0) }
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }

    // 资源获取
    val tableSummaryFormat = stringResource(id = R.string.desc_table_summary)
    val tableFirstRowFormat = stringResource(id = R.string.desc_table_first_row)
    val posFormat = stringResource(id = R.string.reader_pos_format)
    val paraTipFormat = stringResource(id = R.string.reader_para_tip)
    val titleTipFormat = stringResource(id = R.string.reader_title_tip)

    val ttsCallbacks = remember(document, currentIndex) {
        object : TtsCallbacks {
            override fun onUtteranceDone(): Boolean {
                return (currentIndex < (document.contents.size - 1))
            }

            override fun getCurrentText(): String {
                return when (val content = document.contents.getOrNull(currentIndex)) {
                    is WordContent.Text -> content.text
                    is WordContent.Image -> content.description
                    is WordContent.Table -> {
                        val rowCount = content.rows.size
                        val colCount = content.rows.firstOrNull()?.size ?: 0
                        val summary = String.format(java.util.Locale.getDefault(), tableSummaryFormat, rowCount, colCount)
                        val firstRow = content.rows.firstOrNull()?.joinToString(separator = "，") ?: ""
                        summary + if (firstRow.isNotEmpty()) String.format(java.util.Locale.getDefault(), tableFirstRowFormat, firstRow) else ""
                    }
                    null -> ""
                }
            }

            override fun getCurrentUtteranceId(): String {
                return "word_content_$currentIndex"
            }

            override fun moveToNext() {
                if (currentIndex < document.contents.size - 1) {
                    currentIndex++
                }
            }

            override fun moveToPrevious() {
                if (currentIndex > 0) {
                    currentIndex--
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

    LaunchedEffect(currentIndex) {
        kotlinx.coroutines.delay(timeMillis = 50)
        val itemHeight = itemHeights[currentIndex] ?: 0
        val viewportCenter = viewportHeight / 2
        val scrollOffset = if (itemHeight > 0) {
            -viewportCenter + itemHeight / 2
        } else {
            -viewportCenter + 40
        }
        lazyListState.animateScrollToItem(
            index = currentIndex,
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
                        text = String.format(java.util.Locale.getDefault(), posFormat, currentIndex + 1, document.contents.size),
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
                        contentDescription = stringResource(id = R.string.reader_back_tip),
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = onSettingsClick
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = stringResource(id = R.string.tab_settings),
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
        val wordAccent = MaterialTheme.colorScheme.primary
        ReaderControlBar(
            isSpeaking = isSpeaking,
            currentIndex = currentIndex,
            totalCount = document.contents.size,
            speechRate = ttsController.speechRate.collectAsState().value,
            accentColor = wordAccent,
            progressColor = wordAccent,
            previousLabel = stringResource(id = R.string.reader_prev_para),
            nextLabel = stringResource(id = R.string.reader_next_para),
            positionText = String.format(java.util.Locale.getDefault(), posFormat, currentIndex + 1, document.contents.size),
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
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(document.contents) { index, item ->
                    val isCurrent = index == currentIndex
                    Box(
                        modifier = Modifier.onGloballyPositioned { 
                            itemHeights[index] = it.size.height 
                        }
                    ) {
                        when (item) {
                            is WordContent.Text -> WordTextItem(
                                text = item.text,
                                isHeading = item.isHeading,
                                isCurrent = isCurrent,
                                index = index,
                                labelPara = paraTipFormat,
                                labelTitle = titleTipFormat,
                                onClick = {
                                    currentIndex = index
                                    ttsController.speakCurrent()
                                }
                            )
                            is WordContent.Image -> WordImageItem(
                                path = item.imagePath,
                                desc = item.description,
                                isCurrent = isCurrent,
                                onClick = {
                                    currentIndex = index
                                    ttsController.speakCurrent()
                                }
                            )
                            is WordContent.Table -> WordTableItem(
                                rows = item.rows,
                                isCurrent = isCurrent,
                                onClick = {
                                    currentIndex = index
                                    ttsController.speakCurrent()
                                }
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }

            CurrentProgressIndicator(
                currentIndex = currentIndex,
                totalCount = document.contents.size,
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

// ==================== 子组件 ====================

@Composable
fun WordTextItem(
    text: String,
    isHeading: Boolean,
    isCurrent: Boolean,
    index: Int,
    labelPara: String,
    labelTitle: String,
    onClick: () -> Unit,
) {
    val highlightColor = MaterialTheme.colorScheme.primary
    val textColor = if (isCurrent) highlightColor else MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCurrent) highlightColor.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(8.dp)
            .semantics {
                contentDescription = if (isHeading) {
                    String.format(java.util.Locale.getDefault(), labelTitle, text)
                } else {
                    String.format(java.util.Locale.getDefault(), labelPara, index + 1, text)
                }
            },
    ) {
        Text(
            text = text,
            fontSize = if (isHeading) 22.sp else 18.sp,
            fontWeight = if (isHeading) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            lineHeight = 28.sp,
        )
    }
}

@Composable
fun WordImageItem(
    path: String,
    desc: String,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val highlightColor = MaterialTheme.colorScheme.primary
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) highlightColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 图片保护性容器：使用浅灰色背景确保黑色前景（如签名）在深色模式下可见
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE0E0E0)), // 固定的浅灰色背景，保护签名等深色前景图
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = path,
                    contentDescription = desc,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = desc,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun WordTableItem(
    rows: List<List<String>>,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val highlightColor = MaterialTheme.colorScheme.primary
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) highlightColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, highlightColor) else null
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
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
                            fontSize = 14.sp,
                            modifier = Modifier
                                .width(120.dp)
                                .padding(4.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
fun CurrentProgressIndicator(
    currentIndex: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = (currentIndex + 1).toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = "/$totalCount",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}

// ==================== 预览 ====================

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun WordReaderScreenPreview() {
    MaterialTheme {
        WordReaderScreen(
            document = WordDocument(
                filePath = "",
                fileName = "预览文档.docx",
                contents = listOf(WordContent.Text("内容", false, 0))
            )
        )
    }
}

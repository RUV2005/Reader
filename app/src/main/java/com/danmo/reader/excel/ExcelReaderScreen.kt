package com.danmo.reader.excel

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
import com.danmo.reader.common.ReaderControlButton
import com.danmo.reader.tts.TtsCallbacks
import com.danmo.reader.tts.TtsState
import com.danmo.reader.tts.rememberTtsController
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

// ==================== 数据模型 ====================

data class ExcelDocument(
    val filePath: String,
    val fileName: String,
    val sheetName: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val lastReadRow: Int = 0,
)

// ==================== 模拟数据 ====================

val sampleExcelDoc = ExcelDocument(
    filePath = "/storage/documents/销售数据报表.xlsx",
    fileName = "销售数据报表.xlsx",
    sheetName = "2024年Q1",
    headers = listOf("月份", "产品", "销售额", "销量", "增长率"),
    rows = listOf(
        listOf("1月", "智能手机", "1,250,000", "500", "+12%"),
        listOf("1月", "平板电脑", "680,000", "200", "+8%"),
        listOf("2月", "智能手机", "1,380,000", "550", "+10%"),
        listOf("2月", "平板电脑", "720,000", "220", "+6%"),
        listOf("3月", "智能手机", "1,520,000", "600", "+15%"),
        listOf("3月", "平板电脑", "850,000", "260", "+18%"),
        listOf("合计", "-", "5,400,000", "1,780", "+11.5%"),
    ),
    lastReadRow = 0,
)

// ==================== 朗读模式 ====================

enum class ReadMode {
    ROW_BY_ROW,
    COLUMN_BY_COLUMN,
}

// ==================== Excel阅读页面 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelReaderScreen(
    document: ExcelDocument = sampleExcelDoc,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val context = LocalContext.current

    var currentRowIndex by remember { mutableIntStateOf(document.lastReadRow) }
    var isSpeaking by remember { mutableStateOf(false) }
    var readMode by remember { mutableStateOf(ReadMode.ROW_BY_ROW) }
    var currentColIndex by remember { mutableIntStateOf(0) }

    // LazyListState 用于精确控制滚动位置
    val lazyListState = rememberLazyListState()
    var viewportHeight by remember { mutableIntStateOf(0) }
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }

    // TTS 回调实现
    val ttsCallbacks = remember(document, readMode) {
        object : TtsCallbacks {
            override fun onUtteranceDone(): Boolean {
                return when (readMode) {
                    ReadMode.ROW_BY_ROW -> currentRowIndex < document.rows.size - 1
                    ReadMode.COLUMN_BY_COLUMN -> {
                        val totalCells = document.rows.size * document.headers.size
                        val currentCell = currentRowIndex * document.headers.size + currentColIndex
                        currentCell < totalCells - 1
                    }
                }
            }

            override fun getCurrentText(): String {
                return when (readMode) {
                    ReadMode.ROW_BY_ROW -> {
                        val row = document.rows.getOrNull(currentRowIndex) ?: return ""
                        buildString {
                            append("第${currentRowIndex + 1}行。")
                            document.headers.forEachIndexed { colIndex, header ->
                                if (colIndex < row.size) {
                                    append("$header，${row[colIndex]}。")
                                }
                            }
                        }
                    }
                    ReadMode.COLUMN_BY_COLUMN -> {
                        val row = document.rows.getOrNull(currentRowIndex) ?: return ""
                        val header = document.headers.getOrNull(currentColIndex) ?: return ""
                        val cellValue = row.getOrNull(currentColIndex) ?: ""
                        buildString {
                            append("${header}列，第${currentRowIndex + 1}行，$cellValue。")
                        }
                    }
                }
            }

            override fun getCurrentUtteranceId(): String {
                return when (readMode) {
                    ReadMode.ROW_BY_ROW -> "excel_row_$currentRowIndex"
                    ReadMode.COLUMN_BY_COLUMN -> "excel_col_${currentColIndex}_row_$currentRowIndex"
                }
            }

            override fun moveToNext() {
                when (readMode) {
                    ReadMode.ROW_BY_ROW -> {
                        if (currentRowIndex < document.rows.size - 1) {
                            currentRowIndex++
                        }
                    }
                    ReadMode.COLUMN_BY_COLUMN -> {
                        if (currentRowIndex < document.rows.size - 1) {
                            currentRowIndex++
                        } else if (currentColIndex < document.headers.size - 1) {
                            currentRowIndex = 0
                            currentColIndex++
                        }
                    }
                }
            }

            override fun moveToPrevious() {
                when (readMode) {
                    ReadMode.ROW_BY_ROW -> {
                        if (currentRowIndex > 0) {
                            currentRowIndex--
                        }
                    }
                    ReadMode.COLUMN_BY_COLUMN -> {
                        if (currentRowIndex > 0) {
                            currentRowIndex--
                        } else if (currentColIndex > 0) {
                            currentColIndex--
                            currentRowIndex = document.rows.size - 1
                        }
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

    // 核心：当前行变化时，滚动到屏幕中央
    LaunchedEffect(currentRowIndex) {
        kotlinx.coroutines.delay(50)

        val itemHeight = itemHeights[currentRowIndex] ?: 0
        val viewportCenter = viewportHeight / 2
        val scrollOffset = if (itemHeight > 0) {
            -viewportCenter + itemHeight / 2
        } else {
            -viewportCenter + 40
        }

        lazyListState.animateScrollToItem(
            index = currentRowIndex,
            scrollOffset = scrollOffset
        )
    }

    // 朗读表头
    fun speakHeaders() {
        val text = buildString {
            append("表格共有${document.headers.size}列。")
            document.headers.forEachIndexed { index, header ->
                append("第${index + 1}列是$header。")
            }
            append("共有${document.rows.size}行数据。")
        }
        ttsController.stop()
        ttsController.speak(text)
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
                            text = "工作表：${document.sheetName}",
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
                        onClick = { speakHeaders() },
                        modifier = Modifier.semantics {
                            contentDescription = "朗读表头信息"
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_info),
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
                    containerColor = Color(0xFF217346),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            val excelAccent = Color(0xFF217346)
            ReaderControlBar(
                isSpeaking = isSpeaking,
                currentIndex = currentRowIndex,
                totalCount = document.rows.size,
                speechRate = ttsController.speechRate.collectAsState().value,
                accentColor = excelAccent,
                progressColor = excelAccent,
                previousLabel = "上行",
                nextLabel = "下行",
                positionText = "${currentRowIndex + 1}/${document.rows.size}",
                onPrevious = { ttsController.speakPrevious() },
                onPlayPause = { ttsController.togglePlayPause() },
                onNext = { ttsController.speakNext() },
                onRateChange = { ttsController.setSpeechRate(it) },
                leftExtra = {
                    var showModeMenu by remember { mutableStateOf(false) }
                    Box {
                        ReaderControlButton(
                            iconRes = R.drawable.ic_mode,
                            label = if (readMode == ReadMode.ROW_BY_ROW) "按行" else "按列",
                            onClick = { showModeMenu = !showModeMenu },
                            buttonDescription = "当前${if (readMode == ReadMode.ROW_BY_ROW) "按行朗读" else "按列朗读"}，点击切换",
                        )
                        DropdownMenu(
                            expanded = showModeMenu,
                            onDismissRequest = { showModeMenu = false },
                            modifier = Modifier.background(Color(0xFF333333)),
                        ) {
                            ReadMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (mode == ReadMode.ROW_BY_ROW) "按行朗读" else "按列朗读",
                                            color = if (mode == readMode) excelAccent else Color.White,
                                            fontWeight = if (mode == readMode) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                    onClick = {
                                        readMode = mode
                                        currentColIndex = 0
                                        showModeMenu = false
                                    },
                                )
                            }
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
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // 表头行
                item(key = "header") {
                    ExcelHeaderRow(
                        headers = document.headers,
                        onClick = { speakHeaders() },
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                // 数据行
                itemsIndexed(
                    items = document.rows,
                    key = { index, _ -> "excel_row_$index" }
                ) { index, row ->
                    val isCurrent = when (readMode) {
                        ReadMode.ROW_BY_ROW -> index == currentRowIndex
                        ReadMode.COLUMN_BY_COLUMN -> index == currentRowIndex
                    }
                    val isTotalRow = row.any { cell ->
                        cell.contains("合计") || cell.contains("总计") || cell.contains("Total") || cell.contains("Sum")
                    }

                    Box(
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            itemHeights[index] = coordinates.size.height
                        }
                    ) {
                        ExcelDataRow(
                            row = row,
                            headers = document.headers,
                            isCurrent = isCurrent,
                            isTotalRow = isTotalRow,
                            index = index,
                            onClick = {
                                currentRowIndex = index
                                currentColIndex = 0
                                ttsController.speakCurrent()
                            },
                        )
                    }
                }

                // 底部留白
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            CurrentRowIndicator(
                currentIndex = currentRowIndex,
                totalCount = document.rows.size,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

// ==================== 表头行 ====================

@Composable
fun ExcelHeaderRow(
    headers: List<String>,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF217346)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            headers.forEach { header ->
                Text(
                    text = header,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

// ==================== 数据行 ====================

@Composable
fun ExcelDataRow(
    row: List<String>,
    headers: List<String>,
    isCurrent: Boolean,
    isTotalRow: Boolean,
    index: Int,
    onClick: () -> Unit,
) {
    val backgroundColor = when {
        isCurrent -> Color(0xFF217346).copy(alpha = 0.3f)
        isTotalRow -> Color(0xFF333333)
        else -> Color(0xFF2A2A2A)
    }

    val textColor = when {
        isCurrent -> Color(0xFFFFFF00)
        isTotalRow -> Color(0xFF6BFF9E)
        else -> Color.White
    }

    val fontWeight = when {
        isCurrent -> FontWeight.Bold
        isTotalRow -> FontWeight.Bold
        else -> FontWeight.Normal
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append("第${index + 1}行。")
                    headers.forEachIndexed { colIndex, header ->
                        if (colIndex < row.size) {
                            append("$header，${row[colIndex]}。")
                        }
                    }
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            row.forEachIndexed { colIndex, cell ->
                Text(
                    text = cell,
                    fontSize = 14.sp,
                    fontWeight = fontWeight,
                    color = textColor,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

// ==================== 当前行指示器 ====================

@Composable
fun CurrentRowIndicator(
    currentIndex: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF217346).copy(alpha = 0.8f))
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
fun ExcelReaderScreenPreview() {
    MaterialTheme {
        ExcelReaderScreen()
    }
}
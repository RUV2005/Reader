package com.danmo.reader.excel

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    val images: List<String> = emptyList(),
)

// ==================== 朗读模式 ====================

enum class ReadMode {
    ROW_BY_ROW,
    COLUMN_BY_COLUMN,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelReaderScreen(
    document: ExcelDocument,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var currentRowIndex by remember { mutableIntStateOf(document.lastReadRow) }
    var isSpeaking by remember { mutableStateOf(value = false) }
    var readMode by remember { mutableStateOf(ReadMode.ROW_BY_ROW) }
    var currentColIndex by remember { mutableIntStateOf(0) }

    val lazyListState = rememberLazyListState()
    val sharedHorizontalScrollState = rememberScrollState()
    var viewportHeight by remember { mutableIntStateOf(0) }
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }

    // 资源获取
    val modeRowStr = stringResource(id = R.string.excel_mode_row_desc)
    val modeColStr = stringResource(id = R.string.excel_mode_column_desc)
    val rowFormat = stringResource(id = R.string.reader_row_format)
    val colRowFormat = stringResource(id = R.string.excel_col_row_format)
    val totalColsStr = stringResource(id = R.string.excel_total_cols)
    val colNameStr = stringResource(id = R.string.excel_col_name)
    val totalRowsStr = stringResource(id = R.string.excel_total_rows)
    val excelHeaderInfoDesc = stringResource(id = R.string.excel_header_info)
    val settingsDesc = stringResource(id = R.string.tab_settings)

    val ttsCallbacks = remember(document, readMode, currentRowIndex, currentColIndex) {
        object : TtsCallbacks {
            override fun onUtteranceDone(): Boolean {
                return when (readMode) {
                    ReadMode.ROW_BY_ROW -> (currentRowIndex < (document.rows.size - 1))
                    ReadMode.COLUMN_BY_COLUMN -> {
                        val totalCells = document.rows.size * document.headers.size
                        val currentCell = currentRowIndex * document.headers.size + currentColIndex
                        (currentCell < (totalCells - 1))
                    }
                }
            }

            override fun getCurrentText(): String {
                return when (readMode) {
                    ReadMode.ROW_BY_ROW -> {
                        val row = document.rows.getOrNull(currentRowIndex) ?: return ""
                        buildString {
                            append(String.format(java.util.Locale.getDefault(), rowFormat, currentRowIndex + 1, document.rows.size))
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
                        String.format(java.util.Locale.getDefault(), colRowFormat, header, currentRowIndex + 1, cellValue)
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

    LaunchedEffect(currentRowIndex) {
        kotlinx.coroutines.delay(timeMillis = 50)
        val itemHeight = itemHeights[currentRowIndex] ?: 0
        val viewportCenter = viewportHeight / 2
        val scrollOffset = if (itemHeight > 0) {
            (-viewportCenter) + (itemHeight / 2)
        } else {
            (-viewportCenter) + 40
        }
        lazyListState.animateScrollToItem(
            index = currentRowIndex,
            scrollOffset = scrollOffset,
        )
    }

    fun speakHeaders() {
        val text = buildString {
            append(String.format(java.util.Locale.getDefault(), totalColsStr, document.headers.size))
            document.headers.forEachIndexed { index, header ->
                append(String.format(java.util.Locale.getDefault(), colNameStr, index + 1, header))
            }
            append(String.format(java.util.Locale.getDefault(), totalRowsStr, document.rows.size))
        }
        ttsController.stop()
        ttsController.speak(text)
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
                        color = MaterialTheme.colorScheme.onPrimary // 关键修复
                    )
                    Text(
                        text = stringResource(id = R.string.excel_sheet_label, document.sheetName),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
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
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { speakHeaders() },
                    modifier = Modifier.semantics {
                        contentDescription = excelHeaderInfoDesc
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_info),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.semantics {
                        contentDescription = settingsDesc
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
    }

    val controlBar: @Composable () -> Unit = {
        val excelAccent = MaterialTheme.colorScheme.primary
        ReaderControlBar(
            isSpeaking = isSpeaking,
            currentIndex = currentRowIndex,
            totalCount = document.rows.size,
            speechRate = ttsController.speechRate.collectAsState().value,
            accentColor = excelAccent,
            progressColor = excelAccent,
            previousLabel = stringResource(id = R.string.reader_prev_row),
            nextLabel = stringResource(id = R.string.reader_next_row),
            positionText = String.format(java.util.Locale.getDefault(), rowFormat, currentRowIndex + 1, document.rows.size),
            onPrevious = { ttsController.speakPrevious() },
            onPlayPause = { ttsController.togglePlayPause() },
            onNext = { ttsController.speakNext() },
            onRateChange = { ttsController.setSpeechRate(it) },
            leftExtra = {
                var showModeMenu by remember { mutableStateOf(false) }
                val currentModeLabel = if (readMode == ReadMode.ROW_BY_ROW) stringResource(id = R.string.excel_mode_row) else stringResource(id = R.string.excel_mode_column)
                
                Box {
                    ReaderControlButton(
                        iconRes = R.drawable.ic_mode,
                        label = currentModeLabel,
                        onClick = { showModeMenu = !showModeMenu },
                        buttonDescription = stringResource(id = R.string.excel_mode_switch_tip, if (readMode == ReadMode.ROW_BY_ROW) modeRowStr else modeColStr),
                    )
                    DropdownMenu(
                        expanded = showModeMenu,
                        onDismissRequest = { showModeMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                    ) {
                        ReadMode.entries.forEach { mode ->
                            val label = if (mode == ReadMode.ROW_BY_ROW) modeRowStr else modeColStr
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        color = if (mode == readMode) excelAccent else MaterialTheme.colorScheme.onSurface,
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
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (document.images.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(id = R.string.excel_image_section, document.images.size),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(document.images) { imagePath ->
                                // 图片保护性容器：使用浅灰色背景防止深色内容不可见
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE0E0E0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = imagePath,
                                        contentDescription = stringResource(id = R.string.desc_image_general),
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                itemsIndexed(
                    items = document.rows,
                    key = { index, _ -> "excel_row_$index" }
                ) { index, row ->
                    val isCurrent = (index == currentRowIndex)
                    val isTotalRow = row.any { cell ->
                        cell.contains("合计") || cell.contains("总计") || cell.contains("Total") || cell.contains("Sum") || cell.contains("SUM")
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
                            currentColIndex = currentColIndex,
                            readMode = readMode,
                            isTotalRow = isTotalRow,
                            index = index,
                            scrollState = sharedHorizontalScrollState,
                            onClick = { colIdx ->
                                currentRowIndex = index
                                currentColIndex = if (colIdx != -1) colIdx else 0
                                ttsController.speakCurrent()
                            },
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            CurrentRowIndicator(
                currentIndex = currentRowIndex,
                totalCount = document.rows.size,
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

// ==================== 表头行 ====================

@Composable
fun ExcelHeaderRow(
    headers: List<String>,
    scrollState: androidx.compose.foundation.ScrollState,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            headers.forEach { header ->
                Text(
                    text = header,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.width(100.dp),
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
    currentColIndex: Int,
    readMode: ReadMode,
    isTotalRow: Boolean,
    index: Int,
    scrollState: androidx.compose.foundation.ScrollState,
    onClick: (Int) -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    
    val backgroundColor = when {
        isCurrent && readMode == ReadMode.ROW_BY_ROW -> primaryColor.copy(alpha = 0.3f)
        isCurrent && readMode == ReadMode.COLUMN_BY_COLUMN -> primaryColor.copy(alpha = 0.15f)
        isTotalRow -> MaterialTheme.colorScheme.surfaceVariant
        else -> surfaceColor
    }

    val textColor = when {
        isCurrent -> MaterialTheme.colorScheme.primary
        isTotalRow -> MaterialTheme.colorScheme.secondary
        else -> onSurfaceColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(-1) }
            .semantics {
                contentDescription = "Row ${index + 1}"
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            row.forEachIndexed { colIndex, cell ->
                val isCurrentCell = isCurrent && readMode == ReadMode.COLUMN_BY_COLUMN && colIndex == currentColIndex
                
                Surface(
                    modifier = Modifier
                        .width(100.dp)
                        .clickable { onClick(colIndex) }
                        .padding(horizontal = 2.dp),
                    color = if (isCurrentCell) primaryColor.copy(alpha = 0.5f) else Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = cell,
                        fontSize = 14.sp,
                        fontWeight = if (isCurrentCell) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrentCell) Color.White else textColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.padding(4.dp)
                    )
                }
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
fun ExcelReaderScreenPreview() {
    MaterialTheme {
        ExcelReaderScreen(
            document = ExcelDocument(
                filePath = "",
                fileName = "预览表格.xlsx",
                sheetName = "Sheet1",
                headers = listOf("列1"),
                rows = listOf(listOf("数据"))
            )
        )
    }
}

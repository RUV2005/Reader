package com.danmo.reader.excel

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import java.util.*

// ==================== 数据模型 ====================

data class ExcelDocument(
    val filePath: String,
    val fileName: String,
    val sheetName: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val lastReadRow: Int = 0
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
        listOf("合计", "-", "5,400,000", "1,780", "+11.5%")
    ),
    lastReadRow = 0
)

// ==================== Excel阅读页面 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelReaderScreen(
    document: ExcelDocument = sampleExcelDoc,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // TTS 状态
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var currentRowIndex by remember { mutableIntStateOf(document.lastReadRow) }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var readMode by remember { mutableStateOf(ReadMode.ROW_BY_ROW) } // 朗读模式

    // 初始化 TTS
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.CHINESE
                tts?.setSpeechRate(speechRate)
                isTtsReady = true
            }
        }
    }

    // 清理 TTS
    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // 朗读指定行
    val speakRow = remember { { index: Int ->
        if (!isTtsReady || index < 0 || index >= document.rows.size) return@remember

        currentRowIndex = index
        val row = document.rows[index]

        // 构建朗读文本：第X行，月份是X，产品是X，销售额是X...
        val text = buildString {
            append("第${index + 1}行。")
            document.headers.forEachIndexed { colIndex, header ->
                if (colIndex < row.size) {
                    append("$header，${row[colIndex]}。")
                }
            }
        }

        tts?.stop()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "row_$index")
        isSpeaking = true
    } }

    // 朗读表头
    val speakHeaders = remember { {
        val text = buildString {
            append("表格共有${document.headers.size}列。")
            document.headers.forEachIndexed { index, header ->
                append("第${index + 1}列是$header。")
            }
            append("共有${document.rows.size}行数据。")
        }
        tts?.stop()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "headers")
        isSpeaking = true
    } }

    // 暂停朗读
    val pauseSpeaking = remember { {
        tts?.stop()
        isSpeaking = false
    } }

    // 继续朗读
    val resumeSpeaking = remember { {
        speakRow(currentRowIndex)
    } }

    // 下一行
    val nextRow = remember { {
        if (currentRowIndex < document.rows.size - 1) {
            speakRow(currentRowIndex + 1)
        }
    } }

    // 上一行
    val previousRow = remember { {
        if (currentRowIndex > 0) {
            speakRow(currentRowIndex - 1)
        }
    } }

    // 设置语速
    val setSpeechRate = remember { { rate: Float ->
        speechRate = rate
        tts?.setSpeechRate(rate)
    } }

    // TTS 完成监听
    LaunchedEffect(isTtsReady) {
        if (isTtsReady) {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (currentRowIndex < document.rows.size - 1) {
                        speakRow(currentRowIndex + 1)
                    } else {
                        isSpeaking = false
                    }
                }
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                }
            })
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
                            text = "工作表：${document.sheetName}",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            pauseSpeaking()
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
                    // 表头朗读按钮
                    IconButton(
                        onClick = { speakHeaders() },
                        modifier = Modifier.semantics {
                            contentDescription = "朗读表头信息"
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_info),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
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
                    containerColor = Color(0xFF217346),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            ExcelControlBar(
                isSpeaking = isSpeaking,
                currentIndex = currentRowIndex,
                totalCount = document.rows.size,
                speechRate = speechRate,
                readMode = readMode,
                onPrevious = { previousRow() },
                onPlayPause = {
                    if (isSpeaking) pauseSpeaking() else resumeSpeaking()
                },
                onNext = { nextRow() },
                onRateChange = { setSpeechRate(it) },
                onModeChange = { readMode = it }
            )
        }
    ) { paddingValues ->
        // 内容区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF1A1A1A))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { nextRow() },
                        onTap = { /* 单击显示当前行信息 */ }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            ) {
                // 表头行
                ExcelHeaderRow(
                    headers = document.headers,
                    onClick = { speakHeaders() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 数据行
                document.rows.forEachIndexed { index, row ->
                    val isCurrent = index == currentRowIndex
                    val isTotalRow = row.firstOrNull()?.contains("合计") == true ||
                            row.firstOrNull()?.contains("总计") == true

                    ExcelDataRow(
                        row = row,
                        headers = document.headers,
                        isCurrent = isCurrent,
                        isTotalRow = isTotalRow,
                        index = index,
                        onClick = { speakRow(index) }
                    )

                    if (index < document.rows.size - 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // 底部留白
                Spacer(modifier = Modifier.height(80.dp))
            }

            // 当前行指示器
            CurrentRowIndicator(
                currentIndex = currentRowIndex,
                totalCount = document.rows.size,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

// ==================== 表头行 ====================

@Composable
fun ExcelHeaderRow(
    headers: List<String>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF217346)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            headers.forEach { header ->
                Text(
                    text = header,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
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
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isCurrent -> Color(0xFF217346).copy(alpha = 0.3f)
        isTotalRow -> Color(0xFF333333)
        else -> Color(0xFF2A2A2A)
    }

    val textColor = when {
        isCurrent -> Color(0xFFFFFF00) // 高亮黄色
        isTotalRow -> Color(0xFF6BFF9E) // 合计行绿色
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
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            row.forEachIndexed { colIndex, cell ->
                Text(
                    text = cell,
                    fontSize = 14.sp,
                    fontWeight = fontWeight,
                    color = textColor,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF217346).copy(alpha = 0.8f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${currentIndex + 1}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "/",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = "$totalCount",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

// ==================== 朗读模式 ====================

enum class ReadMode {
    ROW_BY_ROW,      // 逐行朗读
    COLUMN_BY_COLUMN // 逐列朗读
}

// ==================== Excel控制栏 ====================

@Composable
fun ExcelControlBar(
    isSpeaking: Boolean,
    currentIndex: Int,
    totalCount: Int,
    speechRate: Float,
    readMode: ReadMode,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRateChange: (Float) -> Unit,
    onModeChange: (ReadMode) -> Unit
) {
    var showRateMenu by remember { mutableStateOf(false) }
    var showModeMenu by remember { mutableStateOf(false) }

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
            // 进度条
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / totalCount.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF217346),
                trackColor = Color(0xFF444444)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 控制按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 语速调节
                Box {
                    ExcelControlButton(
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
                                        color = if (rate == speechRate) Color(0xFF217346) else Color.White,
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

                // 朗读模式
                Box {
                    ExcelControlButton(
                        iconRes = R.drawable.ic_mode,
                        label = if (readMode == ReadMode.ROW_BY_ROW) "按行" else "按列",
                        onClick = { showModeMenu = !showModeMenu },
                        buttonDescription = "当前${if (readMode == ReadMode.ROW_BY_ROW) "按行朗读" else "按列朗读"}，点击切换"
                    )

                    DropdownMenu(
                        expanded = showModeMenu,
                        onDismissRequest = { showModeMenu = false },
                        modifier = Modifier.background(Color(0xFF333333))
                    ) {
                        ReadMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (mode == ReadMode.ROW_BY_ROW) "按行朗读" else "按列朗读",
                                        color = if (mode == readMode) Color(0xFF217346) else Color.White,
                                        fontWeight = if (mode == readMode) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onModeChange(mode)
                                    showModeMenu = false
                                }
                            )
                        }
                    }
                }

                // 上一行
                ExcelControlButton(
                    iconRes = R.drawable.ic_previous,
                    label = "上行",
                    onClick = onPrevious,
                    buttonDescription = "朗读上一行"
                )

                // 播放/暂停
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF217346))
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

                // 下一行
                ExcelControlButton(
                    iconRes = R.drawable.ic_next,
                    label = "下行",
                    onClick = onNext,
                    buttonDescription = "朗读下一行"
                )

                // 行数显示
                ExcelControlButton(
                    iconRes = R.drawable.ic_rows,
                    label = "${currentIndex + 1}/$totalCount",
                    onClick = { /* TODO: 显示行列表 */ },
                    buttonDescription = "当前第${currentIndex + 1}行，共$totalCount 行，点击跳转"
                )
            }
        }
    }
}

// ==================== Excel控制按钮 ====================

@Composable
fun ExcelControlButton(
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
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color(0xFFCCCCCC),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFFCCCCCC)
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
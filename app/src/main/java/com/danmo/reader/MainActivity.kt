package com.danmo.reader

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.danmo.reader.data.repository.RecentFileRepository
import com.danmo.reader.excel.ExcelDocument
import com.danmo.reader.excel.ExcelReaderScreen
import com.danmo.reader.file.FileListScreen
import com.danmo.reader.file.FileType
import com.danmo.reader.file.sampleFiles
import com.danmo.reader.filepicker.DocumentPicker
import com.danmo.reader.parser.DocumentType
import com.danmo.reader.parser.ExcelParser
import com.danmo.reader.parser.ParseResult
import com.danmo.reader.parser.PdfParser
import com.danmo.reader.parser.PptParser
import com.danmo.reader.parser.WordParser
import com.danmo.reader.pdf.PdfDocument
import com.danmo.reader.pdf.PdfReaderScreen
import com.danmo.reader.ppt.PptDocument
import com.danmo.reader.ppt.PptReaderScreen
import com.danmo.reader.settings.SettingsScreen
import com.danmo.reader.ui.theme.ReaderTheme
import com.danmo.reader.word.WordDocument
import com.danmo.reader.word.WordReaderScreen
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

enum class MainTab {
    FILES, HOME, SETTINGS,
}

sealed class Screen {
    data class WordReader(val doc: WordDocument) : Screen()
    data class ExcelReader(val doc: ExcelDocument) : Screen()
    data class PptReader(val doc: PptDocument) : Screen()
    data class PdfReader(val doc: PdfDocument) : Screen()
}

class MainActivity : AppCompatActivity() {

    private lateinit var documentPickerLauncher: ActivityResultLauncher<android.content.Intent>
    private lateinit var recentFileRepository: RecentFileRepository

    private var currentTab by mutableStateOf(MainTab.HOME)
    private var screenStack by mutableStateOf(listOf<Screen>())
    private var parseError by mutableStateOf<String?>(null)
    private var isLoading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        recentFileRepository = RecentFileRepository(this)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        screenStack.isNotEmpty() -> popScreen()
                        currentTab != MainTab.HOME -> currentTab = MainTab.HOME
                        else -> {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            }
        )

        documentPickerLauncher = DocumentPicker.createLauncher(this) { uri: Uri?, docType: DocumentType? ->
            Log.d(TAG, "DocumentPicker回调: uri=$uri, docType=$docType")
            uri?.let { handleSelectedDocument(it, docType) }
        }

        setContent {
            val recentFiles by recentFileRepository.getRecentFiles()
                .collectAsState(initial = emptyList())

            ReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val topScreen = screenStack.lastOrNull()

                    when {
                        topScreen != null -> when (topScreen) {
                            is Screen.WordReader -> WordReaderScreen(
                                document = topScreen.doc,
                                onBackClick = { popScreen() },
                            )
                            is Screen.ExcelReader -> ExcelReaderScreen(
                                document = topScreen.doc,
                                onBackClick = { popScreen() },
                            )
                            is Screen.PptReader -> PptReaderScreen(
                                document = topScreen.doc,
                                onBackClick = { popScreen() },
                            )
                            is Screen.PdfReader -> PdfReaderScreen(
                                document = topScreen.doc,
                                onBackClick = { popScreen() },
                            )
                        }

                        currentTab == MainTab.HOME -> HomeScreen(
                            onNavigateToShelf = { currentTab = MainTab.FILES },
                            onNavigateToProfile = { currentTab = MainTab.SETTINGS },
                            onFunctionCardClick = { title ->
                                when {
                                    title.contains("Word") -> openDocumentPicker(DocumentType.WORD)
                                    title.contains("Excel") -> openDocumentPicker(DocumentType.EXCEL)
                                    title.contains("PPT") -> openDocumentPicker(DocumentType.POWERPOINT)
                                    title.contains("PDF") -> openDocumentPicker(DocumentType.PDF)
                                }
                            },
                            onRecentFileClick = { file ->
                                when (file.type) {
                                    "word" -> pushScreen(
                                        Screen.WordReader(
                                            WordDocument(
                                                filePath = file.filePath,
                                                fileName = file.name,
                                                paragraphs = listOf("重新加载中..."),
                                                lastReadIndex = 0,
                                            )
                                        )
                                    )
                                    "excel" -> pushScreen(
                                        Screen.ExcelReader(createSampleExcelDoc())
                                    )
                                    "ppt" -> pushScreen(
                                        Screen.PptReader(
                                            PptDocument(
                                                filePath = file.filePath,
                                                fileName = file.name,
                                                totalSlides = 1,
                                                slides = emptyList(),
                                                lastReadSlide = 0,
                                            )
                                        )
                                    )
                                    "pdf" -> pushScreen(
                                        Screen.PdfReader(
                                            PdfDocument(
                                                filePath = file.filePath,
                                                fileName = file.name,
                                                totalPages = 1,
                                                pages = emptyList(),
                                                lastReadPage = 0,
                                                lastReadParagraph = 0,
                                            )
                                        )
                                    )
                                }
                            },
                            recentFiles = recentFiles,
                        )

                        currentTab == MainTab.FILES -> FileListScreen(
                            files = sampleFiles,
                            onFileClick = { file ->
                                when (file.type) {
                                    FileType.WORD -> pushScreen(
                                        Screen.WordReader(
                                            WordDocument(
                                                filePath = file.path,
                                                fileName = file.name,
                                                paragraphs = listOf("示例段落1", "示例段落2"),
                                                lastReadIndex = 0,
                                            )
                                        )
                                    )
                                    FileType.EXCEL -> pushScreen(
                                        Screen.ExcelReader(
                                            createSampleExcelDoc().copy(
                                                filePath = file.path,
                                                fileName = file.name,
                                            )
                                        )
                                    )
                                    FileType.PPT -> pushScreen(
                                        Screen.PptReader(
                                            PptDocument(
                                                filePath = file.path,
                                                fileName = file.name,
                                                totalSlides = 3,
                                                slides = listOf(
                                                    com.danmo.reader.ppt.PptSlide(1, "标题", listOf("内容1")),
                                                    com.danmo.reader.ppt.PptSlide(2, "标题2", listOf("内容2")),
                                                ),
                                                lastReadSlide = 0,
                                            )
                                        )
                                    )
                                    FileType.PDF -> pushScreen(
                                        Screen.PdfReader(
                                            PdfDocument(
                                                filePath = file.path,
                                                fileName = file.name,
                                                totalPages = 3,
                                                pages = listOf(
                                                    com.danmo.reader.pdf.PdfPage(1, listOf("PDF段落1")),
                                                    com.danmo.reader.pdf.PdfPage(2, listOf("PDF段落2")),
                                                ),
                                                lastReadPage = 0,
                                                lastReadParagraph = 0,
                                            )
                                        )
                                    )
                                }
                            },
                            onBackClick = { currentTab = MainTab.HOME },
                            onPickFile = { openDocumentPicker() },
                        )

                        currentTab == MainTab.SETTINGS -> SettingsScreen(
                            onBackClick = { currentTab = MainTab.HOME },
                        )
                    }

                    if (isLoading) {
                        LoadingOverlay()
                    }
                    parseError?.let { error ->
                        ErrorDialog(
                            message = error,
                            onDismiss = { parseError = null },
                            onRetry = {
                                parseError = null
                                openDocumentPicker()
                            },
                        )
                    }
                }
            }
        }
    }

    private fun pushScreen(screen: Screen) {
        screenStack = screenStack + screen
    }

    private fun popScreen() {
        if (screenStack.isNotEmpty()) {
            screenStack = screenStack.dropLast(1)
        }
    }

    private fun openDocumentPicker(type: DocumentType? = null) {
        DocumentPicker.openPicker(documentPickerLauncher, type)
    }

    private fun handleSelectedDocument(uri: Uri, docType: DocumentType?) {
        Log.d(TAG, "handleSelectedDocument: uri=$uri")
        Log.d(TAG, "handleSelectedDocument: docType=$docType")

        // 使用 ContentResolver 查询文件名来推断类型
        val inferredType = inferTypeFromUri(uri)
        Log.d(TAG, "handleSelectedDocument: inferredType=$inferredType")

        // 优先使用回调的类型（如果不是 UNKNOWN），否则使用推断的
        val actualType = if (docType != null && docType != DocumentType.UNKNOWN) {
            docType
        } else {
            inferredType
        }
        Log.d(TAG, "handleSelectedDocument: actualType=$actualType")

        if (actualType == DocumentType.UNKNOWN) {
            Log.e(TAG, "handleSelectedDocument: 无法识别文件类型")
            parseError = "不支持的文件格式\n请确保选择 .docx, .xlsx, .pptx 或 .pdf 文件"
            return
        }

        isLoading = true
        parseError = null

        lifecycleScope.launch {
            try {
                when (actualType) {
                    DocumentType.WORD -> {
                        when (val result = WordParser().parse(this@MainActivity, uri)) {
                            is ParseResult.Success -> {
                                recentFileRepository.addRecentFile(
                                    filePath = uri.toString(),
                                    fileName = result.data.fileName,
                                    type = "word",
                                )
                                pushScreen(
                                    Screen.WordReader(
                                        WordDocument(
                                            filePath = uri.toString(),
                                            fileName = result.data.fileName,
                                            paragraphs = result.data.paragraphs.map { it.text },
                                            lastReadIndex = 0,
                                        )
                                    )
                                )
                            }
                            is ParseResult.Error -> parseError = result.message
                        }
                    }
                    DocumentType.EXCEL -> {
                        when (val result = ExcelParser().parse(this@MainActivity, uri)) {
                            is ParseResult.Success -> {
                                recentFileRepository.addRecentFile(
                                    filePath = uri.toString(),
                                    fileName = result.data.fileName,
                                    type = "excel",
                                )
                                val sheet = result.data.sheets.firstOrNull()
                                pushScreen(
                                    Screen.ExcelReader(
                                        if (sheet != null) {
                                            ExcelDocument(
                                                filePath = uri.toString(),
                                                fileName = result.data.fileName,
                                                sheetName = sheet.name,
                                                headers = sheet.headers,
                                                rows = sheet.rows.map { it.cells },
                                                lastReadRow = 0,
                                            )
                                        } else createSampleExcelDoc()
                                    )
                                )
                            }
                            is ParseResult.Error -> parseError = result.message
                        }
                    }
                    DocumentType.POWERPOINT -> {
                        when (val result = PptParser().parse(this@MainActivity, uri)) {
                            is ParseResult.Success -> {
                                recentFileRepository.addRecentFile(
                                    filePath = uri.toString(),
                                    fileName = result.data.fileName,
                                    type = "ppt",
                                )
                                pushScreen(
                                    Screen.PptReader(
                                        PptDocument(
                                            filePath = uri.toString(),
                                            fileName = result.data.fileName,
                                            totalSlides = result.data.totalSlides,
                                            slides = result.data.slides.map {
                                                com.danmo.reader.ppt.PptSlide(
                                                    slideNumber = it.slideNumber,
                                                    title = it.title,
                                                    content = it.content,
                                                    notes = it.notes,
                                                )
                                            },
                                            lastReadSlide = 0,
                                        )
                                    )
                                )
                            }
                            is ParseResult.Error -> parseError = result.message
                        }
                    }
                    DocumentType.PDF -> {
                        when (val result = PdfParser().parse(this@MainActivity, uri)) {
                            is ParseResult.Success -> {
                                recentFileRepository.addRecentFile(
                                    filePath = uri.toString(),
                                    fileName = result.data.fileName,
                                    type = "pdf",
                                )
                                pushScreen(
                                    Screen.PdfReader(
                                        PdfDocument(
                                            filePath = uri.toString(),
                                            fileName = result.data.fileName,
                                            totalPages = result.data.totalPages,
                                            pages = result.data.pages.map {
                                                com.danmo.reader.pdf.PdfPage(
                                                    pageNumber = it.pageNumber,
                                                    paragraphs = it.paragraphs,
                                                )
                                            },
                                            lastReadPage = 0,
                                            lastReadParagraph = 0,
                                        )
                                    )
                                )
                            }
                            is ParseResult.Error -> parseError = result.message
                        }
                    }
                    DocumentType.UNKNOWN -> {
                        parseError = "不支持的文件格式"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "解析异常", e)
                parseError = "解析异常: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * 使用 ContentResolver 查询文件名推断类型
     */
    private fun inferTypeFromUri(uri: Uri): DocumentType {
        // 先尝试从 ContentResolver 查询显示名
        val displayName = try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        val name = cursor.getString(nameIndex)
                        Log.d(TAG, "ContentResolver查询到文件名: $name")
                        name
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "ContentResolver查询失败", e)
            null
        }

        // 使用查询到的文件名，或回退到 URI 字符串
        val fileName = (displayName ?: uri.toString()).lowercase()
        Log.d(TAG, "inferTypeFromUri: 用于匹配的文件名=$fileName")

        return when {
            fileName.endsWith(".docx") || fileName.endsWith(".doc") -> DocumentType.WORD
            fileName.endsWith(".xlsx") || fileName.endsWith(".xls") -> DocumentType.EXCEL
            fileName.endsWith(".pptx") || fileName.endsWith(".ppt") -> DocumentType.POWERPOINT
            fileName.endsWith(".pdf") -> DocumentType.PDF
            else -> DocumentType.UNKNOWN
        }
    }

    private fun createSampleExcelDoc(): ExcelDocument {
        return ExcelDocument(
            filePath = "",
            fileName = "示例表格",
            sheetName = "Sheet1",
            headers = listOf("列1", "列2", "列3"),
            rows = listOf(
                listOf("数据1", "数据2", "数据3"),
                listOf("数据4", "数据5", "数据6"),
            ),
            lastReadRow = 0,
        )
    }
}

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xCC000000)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Text("解析中...", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("打开失败") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text("重试")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
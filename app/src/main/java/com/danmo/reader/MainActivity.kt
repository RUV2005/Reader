package com.danmo.reader

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.danmo.reader.data.local.UriPermissionManager
import com.danmo.reader.data.repository.RecentFileRepository
import com.danmo.reader.excel.ExcelDocument
import com.danmo.reader.excel.ExcelReaderScreen
import com.danmo.reader.file.FileListScreen
import com.danmo.reader.file.FileType
import com.danmo.reader.file.DocumentFile
import com.danmo.reader.filepicker.DocumentPicker
import com.danmo.reader.ocr.OcrManager
import com.danmo.reader.ocr.camera.CameraCaptureScreen
import com.danmo.reader.ocr.ui.OcrResultScreen
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

/**
 * 主底部导航栏的标签枚举
 */
enum class MainTab {
    FILES, HOME, SETTINGS,
}

/**
 * 屏幕导航栈中的屏幕定义（密封类，保证类型安全）
 */
sealed class Screen {
    data class WordReader(val doc: WordDocument) : Screen()
    data class ExcelReader(val doc: ExcelDocument) : Screen()
    data class PptReader(val doc: PptDocument) : Screen()
    data class PdfReader(val doc: PdfDocument) : Screen()
    data class OcrResult(val text: String, val blocks: List<String>) : Screen()
    data object CameraCapture : Screen()
    data object Settings : Screen()
}

/**
 * 应用的主 Activity，负责：
 * 1. 初始化核心仓库和 UI
 * 2. 处理文件选择与解析流程
 * 3. 管理应用内的导航状态（Tab 切换与阅读器栈）
 */
class MainActivity : AppCompatActivity() {

    // 文件选择器的启动器，用于处理 SAF (Storage Access Framework) 回调
    private lateinit var documentPickerLauncher: ActivityResultLauncher<android.content.Intent>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    // 最近文件历史记录的持久化仓库
    private lateinit var recentFileRepository: RecentFileRepository
    private lateinit var ocrManager: OcrManager
    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>

    // 核心 UI 状态，使用 Compose 观察
    private var currentTab by mutableStateOf(MainTab.HOME)         // 当前处于哪个 Tab
    private var screenStack by mutableStateOf(listOf<Screen>())    // 阅读器屏幕栈（后进先出）
    private var parseError by mutableStateOf<String?>(null)        // 解析过程中的错误提示
    private var isLoading by mutableStateOf(value = false)         // 是否显示全屏加载中

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 开启全屏沉浸式体验

        cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                pushScreen(Screen.CameraCapture)
            } else {
                parseError = "需要相机权限才能使用扫描功能"
            }
        }

        recentFileRepository = RecentFileRepository(this)
        ocrManager = OcrManager(this)

        imagePickerLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleOcrImage(it) }
        }

        // 自定义返回键处理逻辑：如果有打开的文档则先关闭文档，否则切换回首页，最后才退出应用
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

        // 初始化文件选择器，处理用户选定文件后的 URI、类型和文件名信息
        documentPickerLauncher = DocumentPicker.createLauncher(this) { uri: Uri?, docType: DocumentType?, fileName: String? ->
            Log.d(TAG, "DocumentPicker回调: uri=$uri, docType=$docType, fileName=$fileName")
            if (uri != null && docType != null && docType != DocumentType.UNKNOWN) {
                // 关键点：持久化外部 URI 的读取权限，否则应用重启后无法再次访问该文件
                UriPermissionManager.persistUriPermission(this, uri)
                // 开始解析并打开文档
                handleSelectedDocument(uri, docType, fileName)
            }
        }

        setContent {
            // 实时订阅最近文件列表的变化
            val recentFiles by recentFileRepository.getRecentFiles()
                .collectAsState(initial = emptyList())

            ReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val topScreen = screenStack.lastOrNull()

                    when {
                        // 1. 如果屏幕栈不为空，渲染最顶层的阅读器页面
                        topScreen != null -> when (topScreen) {
                            is Screen.WordReader -> WordReaderScreen(
                                document = topScreen.doc,
                                onBackClick = { popScreen() }
                            ) {
                                pushScreen(Screen.Settings) // 进入设置
                            }
                            is Screen.ExcelReader -> ExcelReaderScreen(
                                document = topScreen.doc,
                                onBackClick = { popScreen() }
                            ) {
                                pushScreen(Screen.Settings)
                            }
                            is Screen.PptReader -> PptReaderScreen(
                                document = topScreen.doc,
                                onBackClick = { popScreen() }
                            ) {
                                pushScreen(Screen.Settings)
                            }
                            is Screen.PdfReader -> PdfReaderScreen(
                                document = topScreen.doc,
                                onBackClick = { popScreen() }
                            ) {
                                pushScreen(Screen.Settings)
                            }
                            is Screen.OcrResult -> OcrResultScreen(
                                text = topScreen.text,
                                blocks = topScreen.blocks,
                                onBackClick = { popScreen() }
                            )
                            is Screen.CameraCapture -> CameraCaptureScreen(
                                onImageCaptured = { uri ->
                                    popScreen() // 关闭相机
                                    handleOcrImage(uri) // 处理识别
                                },
                                onGalleryClick = {
                                    imagePickerLauncher.launch("image/*")
                                },
                                onBackClick = { popScreen() }
                            )
                            is Screen.Settings -> SettingsScreen(
                                onBackClick = { popScreen() }
                            )
                        }

                        // 2. 首页 Tab：包含快捷入口和最近列表
                        currentTab == MainTab.HOME -> HomeScreen(
                            onNavigateToShelf = { currentTab = MainTab.FILES },
                            onNavigateToProfile = { currentTab = MainTab.SETTINGS },
                            onSettingsClick = { currentTab = MainTab.SETTINGS },
                            onScanClick = {
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            },
                            onFunctionCardClick = { title ->
                                when {
                                    title.contains("Word") -> openDocumentPicker(DocumentType.WORD)
                                    title.contains("Excel") -> openDocumentPicker(DocumentType.EXCEL)
                                    title.contains("PPT") -> openDocumentPicker(DocumentType.POWERPOINT)
                                    title.contains("PDF") -> openDocumentPicker(DocumentType.PDF)
                                }
                            },
                            onRecentFileClick = { file ->
                                Log.d(TAG, "点击最近文件: ${file.name}, type=${file.type}, uri=${file.uri}")
                                val uri = file.uri.toUri()
                                // 检查历史权限是否依然有效
                                if (UriPermissionManager.hasUriPermission(this, uri)) {
                                    val docType = when (file.type) {
                                        "word" -> DocumentType.WORD
                                        "excel" -> DocumentType.EXCEL
                                        "ppt" -> DocumentType.POWERPOINT
                                        "pdf" -> DocumentType.PDF
                                        else -> DocumentType.UNKNOWN
                                    }
                                    handleSelectedDocument(uri, docType, file.name)
                                } else {
                                    parseError = "文件访问权限已失效，请重新选择文件"
                                }
                            },
                            recentFiles = recentFiles,
                        )

                        // 3. 文件列表 Tab：查看所有真实打开过的文件记录
                        currentTab == MainTab.FILES -> FileListScreen(
                            files = recentFiles.map { file ->
                                DocumentFile(
                                    id = file.uri,
                                    name = file.name,
                                    type = when (file.type) {
                                        "word" -> FileType.WORD
                                        "excel" -> FileType.EXCEL
                                        "ppt" -> FileType.PPT
                                        "pdf" -> FileType.PDF
                                        else -> FileType.WORD
                                    },
                                    size = "",
                                    modifiedTime = file.openTimeDisplay,
                                    path = file.uri
                                )
                            },
                            onFileClick = { file ->
                                Log.d(TAG, "从文件列表打开: ${file.name}, uri=${file.id}")
                                val uri = file.id.toUri()
                                if (UriPermissionManager.hasUriPermission(this@MainActivity, uri)) {
                                    val docType = when (file.type) {
                                        FileType.WORD -> DocumentType.WORD
                                        FileType.EXCEL -> DocumentType.EXCEL
                                        FileType.PPT -> DocumentType.POWERPOINT
                                        FileType.PDF -> DocumentType.PDF
                                    }
                                    handleSelectedDocument(uri, docType, file.name)
                                } else {
                                    parseError = "文件访问权限已失效，请重新选择文件"
                                }
                            },
                            onBackClick = { currentTab = MainTab.HOME },
                            onPickFile = { openDocumentPicker() },
                        )

                        // 4. 全局设置 Tab
                        currentTab == MainTab.SETTINGS -> SettingsScreen(
                            onBackClick = { currentTab = MainTab.HOME },
                        )
                    }

                    // 加载中遮罩
                    if (isLoading) {
                        LoadingOverlay()
                    }
                    
                    // 错误提示对话框
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

    /**
     * 进入新的阅读器页面（压栈）
     */
    private fun pushScreen(screen: Screen) {
        screenStack += screen
    }

    /**
     * 退出当前阅读器页面（出栈）
     */
    private fun popScreen() {
        if (screenStack.isNotEmpty()) {
            screenStack = screenStack.dropLast(1)
        }
    }

    /**
     * 打开 SAF 文件选择器
     */
    private fun openDocumentPicker(type: DocumentType? = null) {
        DocumentPicker.openPicker(documentPickerLauncher, type)
    }

    /**
     * 处理 OCR 图片识别
     */
    private fun handleOcrImage(uri: Uri) {
        isLoading = true
        lifecycleScope.launch {
            try {
                val result = ocrManager.recognizeText(uri)
                if (result.blocks.isNotEmpty()) {
                    pushScreen(Screen.OcrResult(result.text, result.blocks))
                } else {
                    parseError = "未能在图片中识别到文字"
                }
            } catch (e: Exception) {
                parseError = "识别出错: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * 核心业务逻辑：处理用户选定的文档
     * 包含：显示加载动画 -> 后台协程解析内容 -> 处理解析结果 -> 进入对应阅读器
     */
    private fun handleSelectedDocument(uri: Uri, docType: DocumentType, fileName: String?) {
        Log.d(TAG, "handleSelectedDocument: uri=$uri, docType=$docType, fileName=$fileName")

        if (docType == DocumentType.UNKNOWN) {
            parseError = "不支持的文件格式"
            return
        }

        isLoading = true
        parseError = null

        // 启动后台协程，避免解析大文件时界面卡死
        lifecycleScope.launch {
            try {
                when (docType) {
                    DocumentType.WORD -> {
                        when (val result = WordParser().parse(this@MainActivity, uri)) {
                            is ParseResult.Success -> {
                                recentFileRepository.addRecentFile(
                                    uri = uri.toString(),
                                    fileName = fileName ?: result.data.fileName,
                                    type = "word",
                                )
                                pushScreen(
                                    Screen.WordReader(
                                        WordDocument(
                                            filePath = uri.toString(),
                                            fileName = fileName ?: result.data.fileName,
                                            contents = result.data.contents.map {
                                                when (it.type) {
                                                    com.danmo.reader.parser.WordContentType.IMAGE -> {
                                                        com.danmo.reader.word.WordContent.Image(
                                                            imagePath = it.imagePath ?: "",
                                                            description = it.description,
                                                            index = it.index
                                                        )
                                                    }
                                                    com.danmo.reader.parser.WordContentType.TABLE -> {
                                                        com.danmo.reader.word.WordContent.Table(
                                                            rows = it.tableRows ?: emptyList(),
                                                            index = it.index
                                                        )
                                                    }
                                                    else -> {
                                                        com.danmo.reader.word.WordContent.Text(
                                                            text = it.text ?: "",
                                                            isHeading = it.isHeading,
                                                            index = it.index
                                                        )
                                                    }
                                                }
                                            },
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
                                    uri = uri.toString(),
                                    fileName = fileName ?: result.data.fileName,
                                    type = "excel",
                                )
                                val sheet = result.data.sheets.firstOrNull()
                                pushScreen(
                                    Screen.ExcelReader(
                                        if (sheet != null) {
                                            ExcelDocument(
                                                filePath = uri.toString(),
                                                fileName = fileName ?: result.data.fileName,
                                                sheetName = sheet.name,
                                                headers = sheet.headers,
                                                rows = sheet.rows.map { it.cells },
                                                lastReadRow = 0,
                                                images = sheet.imagePaths
                                            )
                                        } else {
                                            throw Exception("解析失败，未找到有效的工作表")
                                        }
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
                                    uri = uri.toString(),
                                    fileName = fileName ?: result.data.fileName,
                                    type = "ppt",
                                )
                                pushScreen(
                                    Screen.PptReader(
                                        PptDocument(
                                            filePath = uri.toString(),
                                            fileName = fileName ?: result.data.fileName,
                                            totalSlides = result.data.totalSlides,
                                            slides = result.data.slides.map {
                                                com.danmo.reader.ppt.PptSlide(
                                                    slideNumber = it.slideNumber,
                                                    title = it.title,
                                                    content = it.content,
                                                    notes = it.notes,
                                                    images = it.imagePaths,
                                                    tables = it.tables
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
                                    uri = uri.toString(),
                                    fileName = fileName ?: result.data.fileName,
                                    type = "pdf",
                                )
                                pushScreen(
                                    Screen.PdfReader(
                                        PdfDocument(
                                            filePath = uri.toString(),
                                            fileName = fileName ?: result.data.fileName,
                                            totalPages = result.data.totalPages,
                                            pages = result.data.pages.map {
                                                com.danmo.reader.pdf.PdfPage(
                                                    pageNumber = it.pageNumber,
                                                    paragraphs = it.paragraphs,
                                                    images = it.imagePaths
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
                isLoading = false // 无论成功失败，隐藏加载动画
            }
        }
    }
}

/**
 * 通用的加载中遮罩组件
 */
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

/**
 * 通用的错误对话框组件
 */
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

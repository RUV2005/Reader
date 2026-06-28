package com.danmo.reader

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.danmo.reader.collections.CollectionsScreen
import com.danmo.reader.common.utils.HapticUtils
import com.danmo.reader.common.utils.LocaleUtils
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
    FILES, HOME, NOTES, SETTINGS,
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
 * 底部/侧边导航项的数据结构
 */
data class BottomNavItemData(
    val labelRes: Int,
    val iconRes: Int
)

val bottomNavItems = listOf(
    BottomNavItemData(R.string.tab_files, R.drawable.ic_files),
    BottomNavItemData(R.string.tab_home, R.drawable.ic_home),
    BottomNavItemData(R.string.tab_notes, R.drawable.ic_chapters),
    BottomNavItemData(R.string.tab_settings, R.drawable.ic_settings_nav)
)

/**
 * 应用的主 Activity，负责：
 * 1. 初始化核心仓库和 UI
 * 2. 处理文件选择与解析流程
 * 3. 管理应用内的导航状态（Tab 切换与阅读器栈）
 */
@OptIn(ExperimentalMaterial3Api::class)
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
        installSplashScreen() // 必须在 super.onCreate 之前调用
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 开启全屏沉浸式体验

        cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                pushScreen(Screen.CameraCapture)
            } else {
                parseError = getString(R.string.perm_camera_denied)
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
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            
            // 订阅全局设置：最近文件、语言和主题
            val recentFiles by recentFileRepository.getRecentFiles()
                .collectAsState(initial = emptyList())
            
            val language by recentFileRepository.getSettingsRepository().language
                .collectAsState(initial = "zh")
            
            val theme by recentFileRepository.getSettingsRepository().theme
                .collectAsState(initial = "system")
            
            val highContrast by recentFileRepository.getSettingsRepository().highContrast
                .collectAsState(initial = false)

            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides LocaleUtils.applyLocale(LocalContext.current, language)
            ) {
                ReaderTheme(themeSetting = theme, isHighContrast = highContrast) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        val topScreen = screenStack.lastOrNull()

                        Scaffold(
                            topBar = {
                                if (topScreen == null) {
                                    when (currentTab) {
                                        MainTab.FILES -> {
                                            TopAppBar(
                                                title = { Text(stringResource(id = R.string.files_title), fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                                                navigationIcon = {
                                                    IconButton(onClick = { 
                                                        HapticUtils.triggerTick(this@MainActivity)
                                                        currentTab = MainTab.HOME 
                                                    }) {
                                                        Icon(painterResource(id = R.drawable.ic_back), contentDescription = stringResource(id = R.string.dialog_close))
                                                    }
                                                },
                                                colors = TopAppBarDefaults.topAppBarColors(
                                                    containerColor = Color(0xFF4A6FA5),
                                                    titleContentColor = Color.White,
                                                    navigationIconContentColor = Color.White
                                                )
                                            )
                                        }
                                        MainTab.SETTINGS -> {
                                            TopAppBar(
                                                title = { Text(stringResource(id = R.string.tab_settings), fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                                                navigationIcon = {
                                                    IconButton(onClick = { 
                                                        HapticUtils.triggerTick(this@MainActivity)
                                                        currentTab = MainTab.HOME 
                                                    }) {
                                                        Icon(painterResource(id = R.drawable.ic_back), contentDescription = stringResource(id = R.string.dialog_close))
                                                    }
                                                },
                                                colors = TopAppBarDefaults.topAppBarColors(
                                                    containerColor = Color(0xFF4A6FA5),
                                                    titleContentColor = Color.White,
                                                    navigationIconContentColor = Color.White
                                                )
                                            )
                                        }
                                        MainTab.NOTES -> {
                                            TopAppBar(
                                                title = { Text(stringResource(id = R.string.tab_notes), fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                                                navigationIcon = {
                                                    IconButton(onClick = { 
                                                        HapticUtils.triggerTick(this@MainActivity)
                                                        currentTab = MainTab.HOME 
                                                    }) {
                                                        Icon(painterResource(id = R.drawable.ic_back), contentDescription = stringResource(id = R.string.dialog_close))
                                                    }
                                                },
                                                colors = TopAppBarDefaults.topAppBarColors(
                                                    containerColor = Color(0xFF4A6FA5),
                                                    titleContentColor = Color.White,
                                                    navigationIconContentColor = Color.White
                                                )
                                            )
                                        }
                                        MainTab.HOME -> { /* 首页自带 Header */ }
                                    }
                                } else if (topScreen is Screen.Settings) {
                                    TopAppBar(
                                        title = { Text(stringResource(id = R.string.tab_settings), fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                                        navigationIcon = {
                                            IconButton(onClick = { 
                                                HapticUtils.triggerTick(this@MainActivity)
                                                popScreen()
                                            }) {
                                                Icon(painterResource(id = R.drawable.ic_back), contentDescription = stringResource(id = R.string.dialog_close))
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = Color(0xFF4A6FA5),
                                            titleContentColor = Color.White,
                                            navigationIconContentColor = Color.White
                                        )
                                    )
                                }
                            },
                            bottomBar = {
                                AnimatedVisibility(
                                    visible = !isLandscape && topScreen == null,
                                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                                ) {
                                    BottomNavigationBar(
                                        selectedTab = when (currentTab) {
                                            MainTab.FILES -> 0
                                            MainTab.HOME -> 1
                                            MainTab.NOTES -> 2
                                            MainTab.SETTINGS -> 3
                                        },
                                        onTabSelected = { index: Int ->
                                            HapticUtils.triggerTick(this@MainActivity)
                                            screenStack = emptyList()
                                            currentTab = when (index) {
                                                0 -> MainTab.FILES
                                                1 -> MainTab.HOME
                                                2 -> MainTab.NOTES
                                                3 -> MainTab.SETTINGS
                                                else -> MainTab.HOME
                                            }
                                        }
                                    )
                                }
                            },
                            floatingActionButton = {
                                if (topScreen == null && !isLandscape) {
                                    when (currentTab) {
                                        MainTab.HOME -> {
                                            com.danmo.reader.ScanFloatingButton {
                                                HapticUtils.triggerImpact(this@MainActivity)
                                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                            }
                                        }
                                        MainTab.FILES -> {
                                            ExtendedFloatingActionButton(
                                                onClick = { 
                                                    HapticUtils.triggerTick(this@MainActivity)
                                                    openDocumentPicker() 
                                                },
                                                containerColor = Color(0xFF4A6FA5),
                                                icon = { Icon(painterResource(id = R.drawable.ic_add), contentDescription = null, tint = Color.White) },
                                                text = { Text(stringResource(id = R.string.action_open_new_file), color = Color.White) }
                                            )
                                        }
                                        MainTab.NOTES, MainTab.SETTINGS -> {}
                                    }
                                }
                            },
                            floatingActionButtonPosition = if (currentTab == MainTab.HOME) FabPosition.Center else FabPosition.End
                        ) { paddingValues ->
                            Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                                if (isLandscape && topScreen == null) {
                                    GlobalNavigationRail(
                                        selectedTab = when (currentTab) {
                                            MainTab.FILES -> 0
                                            MainTab.HOME -> 1
                                            MainTab.NOTES -> 2
                                            MainTab.SETTINGS -> 3
                                        },
                                        onTabSelected = { index: Int ->
                                            HapticUtils.triggerTick(this@MainActivity)
                                            screenStack = emptyList()
                                            currentTab = when (index) {
                                                0 -> MainTab.FILES
                                                1 -> MainTab.HOME
                                                2 -> MainTab.NOTES
                                                3 -> MainTab.SETTINGS
                                                else -> MainTab.HOME
                                            }
                                        }
                                    )
                                }

                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    AnimatedContent(
                                        targetState = topScreen ?: currentTab,
                                        transitionSpec = {
                                            val animationDuration = 350
                                            val easing = FastOutSlowInEasing

                                            if (targetState is Screen && initialState !is Screen) {
                                                // 深度入场：从右侧推入并伴随轻微缩放
                                                (slideInHorizontally(
                                                    initialOffsetX = { it },
                                                    animationSpec = tween(animationDuration, easing = easing)
                                                ) + fadeIn()).togetherWith(
                                                    slideOutHorizontally(
                                                        targetOffsetX = { -it / 3 },
                                                        animationSpec = tween(animationDuration, easing = easing)
                                                    ) + fadeOut()
                                                )
                                            } else if (initialState is Screen && targetState !is Screen) {
                                                // 回退退出：向右侧推出
                                                (slideInHorizontally(
                                                    initialOffsetX = { -it / 3 },
                                                    animationSpec = tween(animationDuration, easing = easing)
                                                ) + fadeIn()).togetherWith(
                                                    slideOutHorizontally(
                                                        targetOffsetX = { it },
                                                        animationSpec = tween(animationDuration, easing = easing)
                                                    ) + fadeOut()
                                                )
                                            } else {
                                                // Tab 左右滑动：根据 Tab 索引决定方向
                                                val fromIndex = (initialState as? MainTab)?.ordinal ?: 0
                                                val toIndex = (targetState as? MainTab)?.ordinal ?: 0
                                                
                                                if (toIndex > fromIndex) {
                                                    (slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(250)) + fadeIn()).togetherWith(
                                                        slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(250)) + fadeOut()
                                                    )
                                                } else {
                                                    (slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(250)) + fadeIn()).togetherWith(
                                                        slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(250)) + fadeOut()
                                                    )
                                                }
                                            }
                                        },
                                        label = "ScreenTransition"
                                    ) { state ->
                                        when (state) {
                                            is Screen.WordReader -> WordReaderScreen(
                                                document = state.doc,
                                                onBackClick = { popScreen() }
                                            ) {
                                                HapticUtils.triggerTick(this@MainActivity)
                                                pushScreen(Screen.Settings)
                                            }
                                            is Screen.ExcelReader -> ExcelReaderScreen(
                                                document = state.doc,
                                                onBackClick = { popScreen() }
                                            ) {
                                                HapticUtils.triggerTick(this@MainActivity)
                                                pushScreen(Screen.Settings)
                                            }
                                            is Screen.PptReader -> PptReaderScreen(
                                                document = state.doc,
                                                onBackClick = { popScreen() }
                                            ) {
                                                HapticUtils.triggerTick(this@MainActivity)
                                                pushScreen(Screen.Settings)
                                            }
                                            is Screen.PdfReader -> PdfReaderScreen(
                                                document = state.doc,
                                                onBackClick = { popScreen() }
                                            ) {
                                                HapticUtils.triggerTick(this@MainActivity)
                                                pushScreen(Screen.Settings)
                                            }
                                            is Screen.Settings -> SettingsScreen()
                                            is Screen.OcrResult -> OcrResultScreen(
                                                text = state.text,
                                                blocks = state.blocks,
                                                onBackClick = { popScreen() }
                                            )
                                            is Screen.CameraCapture -> CameraCaptureScreen(
                                                onImageCaptured = { uri ->
                                                    HapticUtils.triggerSuccess(this@MainActivity)
                                                    popScreen()
                                                    handleOcrImage(uri)
                                                },
                                                onGalleryClick = { imagePickerLauncher.launch("image/*") },
                                                onBackClick = { popScreen() }
                                            )

                                            MainTab.HOME -> HomeScreen(
                                                onNavigateToShelf = { 
                                                    HapticUtils.triggerTick(this@MainActivity)
                                                    currentTab = MainTab.FILES 
                                                },
                                                onNavigateToProfile = { 
                                                    HapticUtils.triggerTick(this@MainActivity)
                                                    currentTab = MainTab.SETTINGS 
                                                },
                                                onSettingsClick = { 
                                                    HapticUtils.triggerTick(this@MainActivity)
                                                    currentTab = MainTab.SETTINGS
                                                    screenStack = emptyList()
                                                },
                                                onViewAllClick = { 
                                                    HapticUtils.triggerTick(this@MainActivity)
                                                    currentTab = MainTab.FILES 
                                                },
                                                onScanClick = {
                                                    HapticUtils.triggerImpact(this@MainActivity)
                                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                                },
                                                onFunctionCardClick = { title ->
                                                    HapticUtils.triggerTick(this@MainActivity)
                                                    when {
                                                        title.contains("Word") -> openDocumentPicker(DocumentType.WORD)
                                                        title.contains("Excel") -> openDocumentPicker(DocumentType.EXCEL)
                                                        title.contains("PPT") -> openDocumentPicker(DocumentType.POWERPOINT)
                                                        title.contains("PDF") -> openDocumentPicker(DocumentType.PDF)
                                                    }
                                                },
                                                onRecentFileClick = { file ->
                                                    HapticUtils.triggerTick(this@MainActivity)
                                                    val uri = file.uri.toUri()
                                                    if (UriPermissionManager.hasUriPermission(this@MainActivity, uri)) {
                                                        val docType = when (file.type) {
                                                            "word" -> DocumentType.WORD
                                                            "excel" -> DocumentType.EXCEL
                                                            "ppt" -> DocumentType.POWERPOINT
                                                            "pdf" -> DocumentType.PDF
                                                            else -> DocumentType.UNKNOWN
                                                        }
                                                        handleSelectedDocument(uri, docType, file.name)
                                                    } else {
                                                        parseError = getString(R.string.perm_file_invalid)
                                                    }
                                                },
                                                recentFiles = recentFiles,
                                            )

                                            MainTab.FILES -> FileListScreen(
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
                                                        parseError = getString(R.string.perm_file_invalid)
                                                    }
                                                }
                                            )

                                            MainTab.NOTES -> CollectionsScreen()

                                            MainTab.SETTINGS -> SettingsScreen()
                                        }
                                    }
                                }
                            }
                        }

                        if (isLoading) {
                            LoadingOverlay()
                        }
                        
                        parseError?.let { error ->
                            ErrorDialog(
                                message = error,
                                onDismiss = { parseError = null },
                                onRetry = { parseError = null; openDocumentPicker() },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun pushScreen(screen: Screen) {
        screenStack += screen
    }

    private fun popScreen() {
        if (screenStack.isNotEmpty()) {
            screenStack = screenStack.dropLast(1)
        }
    }

    private fun openDocumentPicker(type: DocumentType? = null) {
        DocumentPicker.openPicker(documentPickerLauncher, type)
    }

    private fun handleOcrImage(uri: Uri) {
        isLoading = true
        lifecycleScope.launch {
            try {
                val result = ocrManager.recognizeText(uri)
                if (result.blocks.isNotEmpty()) {
                    pushScreen(Screen.OcrResult(result.text, result.blocks))
                } else {
                    parseError = getString(R.string.ocr_no_text)
                }
            } catch (e: Exception) {
                parseError = "${getString(R.string.error_open_failed)}: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    private fun handleSelectedDocument(uri: Uri, docType: DocumentType, fileName: String?) {
        if (docType == DocumentType.UNKNOWN) {
            parseError = "Unsupported format"
            return
        }
        isLoading = true
        parseError = null
        lifecycleScope.launch {
            try {
                when (docType) {
                    DocumentType.WORD -> {
                        when (val result = WordParser().parse(this@MainActivity, uri)) {
                            is ParseResult.Success -> {
                                recentFileRepository.addRecentFile(uri.toString(), fileName ?: result.data.fileName, "word")
                                pushScreen(Screen.WordReader(WordDocument(uri.toString(), fileName ?: result.data.fileName, result.data.contents.map {
                                    when (it.type) {
                                        com.danmo.reader.parser.WordContentType.IMAGE -> com.danmo.reader.word.WordContent.Image(it.imagePath ?: "", it.description ?: "", it.index)
                                        com.danmo.reader.parser.WordContentType.TABLE -> com.danmo.reader.word.WordContent.Table(it.tableRows ?: emptyList(), it.index)
                                        else -> com.danmo.reader.word.WordContent.Text(it.text ?: "", it.isHeading, it.index)
                                    }
                                })))
                            }
                            is ParseResult.Error -> parseError = result.message
                        }
                    }
                    DocumentType.EXCEL -> {
                        when (val result = ExcelParser().parse(this@MainActivity, uri)) {
                            is ParseResult.Success -> {
                                recentFileRepository.addRecentFile(uri.toString(), fileName ?: result.data.fileName, "excel")
                                val sheet = result.data.sheets.firstOrNull()
                                if (sheet != null) {
                                    pushScreen(Screen.ExcelReader(ExcelDocument(uri.toString(), fileName ?: result.data.fileName, sheet.name, sheet.headers, sheet.rows.map { it.cells }, 0, sheet.imagePaths)))
                                } else throw Exception("No sheet found")
                            }
                            is ParseResult.Error -> parseError = result.message
                        }
                    }
                    DocumentType.POWERPOINT -> {
                        when (val result = PptParser().parse(this@MainActivity, uri)) {
                            is ParseResult.Success -> {
                                recentFileRepository.addRecentFile(uri.toString(), fileName ?: result.data.fileName, "ppt")
                                pushScreen(Screen.PptReader(PptDocument(uri.toString(), fileName ?: result.data.fileName, result.data.totalSlides, result.data.slides.map {
                                    com.danmo.reader.ppt.PptSlide(it.slideNumber, it.title, it.content, it.notes, it.imagePaths, it.tables)
                                })))
                            }
                            is ParseResult.Error -> parseError = result.message
                        }
                    }
                    DocumentType.PDF -> {
                        when (val result = PdfParser().parse(this@MainActivity, uri)) {
                            is ParseResult.Success -> {
                                recentFileRepository.addRecentFile(uri.toString(), fileName ?: result.data.fileName, "pdf")
                                pushScreen(Screen.PdfReader(PdfDocument(uri.toString(), fileName ?: result.data.fileName, result.data.totalPages, result.data.pages.map {
                                    com.danmo.reader.pdf.PdfPage(it.pageNumber, it.paragraphs, it.imagePaths)
                                })))
                            }
                            is ParseResult.Error -> parseError = result.message
                        }
                    }
                    else -> parseError = "Unsupported"
                }
            } catch (e: Exception) {
                parseError = "${getString(R.string.error_open_failed)}: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    @Composable
    private fun LoadingOverlay() {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(120.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(id = R.string.loading_parsing), color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }

    @Composable
    private fun BottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface, // 动态使用主题表面色
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavItems.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    val activeColor = MaterialTheme.colorScheme.primary
                    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(index) }
                            )
                            .padding(vertical = 4.dp)
                    ) {
                        val label = stringResource(id = item.labelRes)
                        Icon(
                            painter = painterResource(id = item.iconRes),
                            contentDescription = label,
                            modifier = Modifier.size(24.dp),
                            tint = if (isSelected) activeColor else inactiveColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = if (isSelected) activeColor else inactiveColor
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun GlobalNavigationRail(selectedTab: Int, onTabSelected: (Int) -> Unit) {
        NavigationRail(containerColor = Color.White, modifier = Modifier.fillMaxHeight()) {
            Spacer(modifier = Modifier.weight(1f))
            bottomNavItems.forEachIndexed { index, item ->
                val isSelected = selectedTab == index
                NavigationRailItem(
                    selected = isSelected,
                    onClick = { onTabSelected(index) },
                    icon = { Icon(painter = painterResource(id = item.iconRes), contentDescription = stringResource(id = item.labelRes), modifier = Modifier.size(24.dp)) },
                    label = { Text(stringResource(id = item.labelRes), fontSize = 11.sp) },
                    colors = NavigationRailItemDefaults.colors(selectedIconColor = Color(0xFF4A6FA5), selectedTextColor = Color(0xFF4A6FA5), unselectedIconColor = Color(0xFF999999), unselectedTextColor = Color(0xFF999999), indicatorColor = Color(0xFF4A6FA5).copy(alpha = 0.1f))
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }

    @Composable
    private fun ErrorDialog(message: String, onDismiss: () -> Unit, onRetry: () -> Unit) {
        AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(id = R.string.error_open_failed)) }, text = { Text(message) }, confirmButton = { TextButton(onClick = onRetry) { Text(stringResource(id = R.string.error_retry)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.dialog_cancel)) } })
    }
}

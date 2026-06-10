package com.danmo.reader

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.danmo.reader.filepicker.DocumentPicker
import com.danmo.reader.parser.*
import com.danmo.reader.ui.theme.ReaderTheme
import com.danmo.reader.word.WordReaderScreen
import com.danmo.reader.word.WordDocument
import com.danmo.reader.excel.ExcelReaderScreen
import com.danmo.reader.excel.ExcelDocument
import com.danmo.reader.ppt.PptReaderScreen
import com.danmo.reader.ppt.PptDocument
import com.danmo.reader.pdf.PdfReaderScreen
import com.danmo.reader.pdf.PdfDocument
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var documentPickerLauncher: ActivityResultLauncher<android.content.Intent>
    private var currentUri by mutableStateOf<Uri?>(null)
    private var currentDocumentType by mutableStateOf<DocumentType?>(null)

    // 解析结果状态
    private var wordResult by mutableStateOf<WordParseResult?>(null)
    private var excelResult by mutableStateOf<ExcelParseResult?>(null)
    private var pptResult by mutableStateOf<PptParseResult?>(null)
    private var pdfResult by mutableStateOf<PdfParseResult?>(null)
    private var parseError by mutableStateOf<String?>(null)
    private var isLoading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 注册文档选择器
        documentPickerLauncher = DocumentPicker.createLauncher(this) { uri: Uri?, docType: DocumentType? ->
            uri?.let { handleSelectedDocument(it, docType) }
        }

        setContent {
            ReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        isLoading -> LoadingScreen()
                        parseError != null -> ErrorScreen(
                            message = parseError!!,
                            onRetry = { openDocumentPicker() }
                        )
                        wordResult != null -> {
                            val doc = wordResult!!
                            WordReaderScreen(
                                document = WordDocument(
                                    filePath = "",
                                    fileName = doc.fileName,
                                    paragraphs = doc.paragraphs.map { it.text },
                                    lastReadIndex = 0
                                ),
                                onBackClick = { resetState() }
                            )
                        }
                        excelResult != null -> {
                            val doc = excelResult!!
                            val sheet = doc.sheets.firstOrNull()
                            ExcelReaderScreen(
                                document = if (sheet != null) {
                                    ExcelDocument(
                                        filePath = "",
                                        fileName = doc.fileName,
                                        sheetName = sheet.name,
                                        headers = sheet.headers,
                                        rows = sheet.rows.map { it.cells },
                                        lastReadRow = 0
                                    )
                                } else createSampleExcelDoc(),
                                onBackClick = { resetState() }
                            )
                        }
                        pptResult != null -> {
                            val doc = pptResult!!
                            PptReaderScreen(
                                document = PptDocument(
                                    filePath = "",
                                    fileName = doc.fileName,
                                    totalSlides = doc.totalSlides,
                                    slides = doc.slides.map {
                                        com.danmo.reader.ppt.PptSlide(
                                            slideNumber = it.slideNumber,
                                            title = it.title,
                                            content = it.content,
                                            notes = it.notes
                                        )
                                    },
                                    lastReadSlide = 0
                                ),
                                onBackClick = { resetState() }
                            )
                        }
                        pdfResult != null -> {
                            val doc = pdfResult!!
                            PdfReaderScreen(
                                document = PdfDocument(
                                    filePath = "",
                                    fileName = doc.fileName,
                                    totalPages = doc.totalPages,
                                    pages = doc.pages.map {
                                        com.danmo.reader.pdf.PdfPage(
                                            pageNumber = it.pageNumber,
                                            paragraphs = it.paragraphs
                                        )
                                    },
                                    lastReadPage = 0,
                                    lastReadParagraph = 0
                                ),
                                onBackClick = { resetState() }
                            )
                        }
                        else -> HomeScreen(
                            onFunctionCardClick = { title: String ->
                                when {
                                    title.contains("Word") -> openDocumentPicker(DocumentType.WORD)
                                    title.contains("Excel") -> openDocumentPicker(DocumentType.EXCEL)
                                    title.contains("PPT") -> openDocumentPicker(DocumentType.POWERPOINT)
                                    title.contains("PDF") -> openDocumentPicker(DocumentType.PDF)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun openDocumentPicker(type: DocumentType? = null) {
        DocumentPicker.openPicker(documentPickerLauncher)
    }

    private fun handleSelectedDocument(uri: Uri, docType: DocumentType?) {
        isLoading = true
        parseError = null
        currentUri = uri
        currentDocumentType = docType

        lifecycleScope.launch {
            try {
                when (docType) {
                    DocumentType.WORD -> {
                        when (val result = WordParser().parse(this@MainActivity, uri)) {
                            is ParseResult.Success -> wordResult = result.data
                            is ParseResult.Error -> parseError = result.message
                        }
                    }
                    DocumentType.EXCEL -> {
                        when (val result = ExcelParser().parse(this@MainActivity, uri)) {
                            is ParseResult.Success -> excelResult = result.data
                            is ParseResult.Error -> parseError = result.message
                        }
                    }
                    DocumentType.POWERPOINT -> {
                        when (val result = PptParser().parse(this@MainActivity, uri)) {
                            is ParseResult.Success -> pptResult = result.data
                            is ParseResult.Error -> parseError = result.message
                        }
                    }
                    DocumentType.PDF -> {
                        when (val result = PdfParser().parse(this@MainActivity, uri)) {
                            is ParseResult.Success -> pdfResult = result.data
                            is ParseResult.Error -> parseError = result.message
                        }
                    }
                    else -> parseError = "不支持的文件格式"
                }
            } catch (e: Exception) {
                parseError = "解析异常: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    private fun resetState() {
        currentUri = null
        currentDocumentType = null
        wordResult = null
        excelResult = null
        pptResult = null
        pdfResult = null
        parseError = null
    }

    private fun createSampleExcelDoc(): ExcelDocument {
        return ExcelDocument(
            filePath = "",
            fileName = "示例表格",
            sheetName = "Sheet1",
            headers = listOf("列1", "列2", "列3"),
            rows = listOf(
                listOf("数据1", "数据2", "数据3"),
                listOf("数据4", "数据5", "数据6")
            ),
            lastReadRow = 0
        )
    }
}

@Composable
fun LoadingScreen() {
    androidx.compose.material3.CircularProgressIndicator()
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    androidx.compose.material3.Text(text = message)
}
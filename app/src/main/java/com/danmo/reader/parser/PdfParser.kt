package com.danmo.reader.parser

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.danmo.reader.common.utils.FileUtils
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

// ==================== 数据模型 ====================

data class PdfPageData(
    val pageNumber: Int,
    val paragraphs: List<String>,
    val imagePaths: List<String>
)

data class PdfParseResult(
    val fileName: String,
    val pages: List<PdfPageData>,
    val totalPages: Int,
    val title: String = "",
    val isScanned: Boolean = false
)

// ==================== PDF 解析器 ====================

class PdfParser : DocumentParser<PdfParseResult> {

    companion object {
        private const val PARAGRAPH_SPLIT_THRESHOLD = 20
    }

    override suspend fun parse(context: Context, uri: Uri): ParseResult<PdfParseResult> {
        return withContext(Dispatchers.IO) {
            try {
                val documentFile = DocumentFile.fromSingleUri(context, uri)
                val fileName = documentFile?.name ?: "未知文件"
                val docHash = FileUtils.getUriHash(uri)

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    parseInternal(context, inputStream, fileName, docHash)
                } ?: ParseResult.Error("无法打开文件输入流")
            } catch (e: Exception) {
                ParseResult.Error("解析 PDF 文档失败: ${e.message}", e)
            }
        }
    }

    override suspend fun parse(inputStream: InputStream, fileName: String): ParseResult<PdfParseResult> {
        return withContext(Dispatchers.IO) {
            try {
                parseInternal(null, inputStream, fileName, "temp_${System.currentTimeMillis()}")
            } catch (e: Exception) {
                ParseResult.Error("解析 PDF 文档失败: ${e.message}", e)
            }
        }
    }

    private fun parseInternal(context: Context?, inputStream: InputStream, fileName: String, docHash: String): ParseResult<PdfParseResult> {
        try {
            PDDocument.load(inputStream).use { document ->
                val totalPages = document.numberOfPages
                val pages = mutableListOf<PdfPageData>()
                val docCacheDir = if (context != null) FileUtils.getDocCacheDir(context, docHash) else null

                for (i in 0 until totalPages) {
                    val pageNum = i + 1
                    val stripper = PDFTextStripper()
                    stripper.startPage = pageNum
                    stripper.endPage = pageNum
                    val pageText = stripper.getText(document)?.trim() ?: ""

                    // 1. 提取图片
                    val imagePaths = mutableListOf<String>()
                    if (docCacheDir != null) {
                        val resources = document.getPage(i).resources
                        for (name in resources.xObjectNames) {
                            val xObject = resources.getXObject(name)
                            if (xObject is com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject) {
                                val bitmap = xObject.image
                                if (bitmap != null) {
                                    val picFileName = "pdf_pic_${pageNum}_${imagePaths.size}.png"
                                    val picFile = java.io.File(docCacheDir, picFileName)
                                    
                                    if (!picFile.exists()) {
                                        java.io.FileOutputStream(picFile).use { fos ->
                                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, fos)
                                        }
                                    }
                                    imagePaths.add(picFile.absolutePath)
                                }
                            }
                        }
                    }

                    // 2. 智能切分段落
                    val paragraphs = smartSplitParagraphs(pageText)
                    pages.add(PdfPageData(pageNum, paragraphs, imagePaths))
                }

                return ParseResult.Success(
                    PdfParseResult(
                        fileName = fileName,
                        pages = pages,
                        totalPages = totalPages,
                        title = document.documentInformation.title ?: fileName
                    )
                )
            }
        } catch (e: Exception) {
            return ParseResult.Error("解析失败: ${e.message}", e)
        }
    }

    private fun smartSplitParagraphs(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val paragraphs = mutableListOf<String>()
        val lines = text.split("\n", "\r")
        var currentPara = StringBuilder()

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) {
                if (currentPara.isNotEmpty()) {
                    paragraphs.add(currentPara.toString())
                    currentPara = StringBuilder()
                }
                continue
            }

            // 启发式表格行检测
            val isPossiblyTableRow = line.contains("    ") || line.contains("\t")

            if (isPossiblyTableRow) {
                if (currentPara.isNotEmpty()) {
                    paragraphs.add(currentPara.toString())
                    currentPara = StringBuilder()
                }
                val formattedRow = trimmedLine.replace(Regex("\\s{2,}"), " | ")
                paragraphs.add("Table Data: $formattedRow")
            } else {
                if (currentPara.isNotEmpty()) currentPara.append(" ")
                currentPara.append(trimmedLine)

                // 如果行末不是标点符号，或者这一行特别短，可能段落还没结束（PDF 换行坑）
                if (trimmedLine.endsWith(".") || trimmedLine.endsWith("?") || trimmedLine.endsWith("!") ||
                    trimmedLine.endsWith("。") || trimmedLine.endsWith("？") || trimmedLine.endsWith("！")
                ) {
                    paragraphs.add(currentPara.toString())
                    currentPara = StringBuilder()
                }
            }
        }

        if (currentPara.isNotEmpty()) {
            paragraphs.add(currentPara.toString())
        }

        return paragraphs
    }
}

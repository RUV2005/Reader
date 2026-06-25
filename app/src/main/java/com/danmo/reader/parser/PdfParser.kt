package com.danmo.reader.parser

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * PDF 页面内容
 */
data class PdfPageData(
    val pageNumber: Int,
    val paragraphs: List<String>,
    val imagePaths: List<String> = emptyList()
)

/**
 * PDF 解析结果
 */
data class PdfParseResult(
    val fileName: String,
    val pages: List<PdfPageData>,
    val totalPages: Int = 0,
    val title: String = "",
    val isScanned: Boolean = false // 是否为扫描版PDF
)

/**
 * PDF 文档解析器
 * 使用 PdfBox-Android 库
 */
class PdfParser : DocumentParser<PdfParseResult> {

    companion object {
        private const val PARAGRAPH_SPLIT_THRESHOLD = 80 // 段落拆分阈值（字符数）
    }

    override suspend fun parse(context: Context, uri: Uri): ParseResult<PdfParseResult> {
        return withContext(Dispatchers.IO) {
            try {
                // 初始化 PdfBox（Android 需要）
                PDFBoxResourceLoader.init(context)

                val documentFile = DocumentFile.fromSingleUri(context, uri)
                val fileName = documentFile?.name ?: "未知文件"

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    parseInternal(context, inputStream, fileName)
                } ?: ParseResult.Error("无法打开文件输入流")
            } catch (e: Exception) {
                ParseResult.Error("解析 PDF 文档失败: ${e.message}", e)
            }
        }
    }

    override suspend fun parse(inputStream: InputStream, fileName: String): ParseResult<PdfParseResult> {
        return withContext(Dispatchers.IO) {
            try {
                parseInternal(null, inputStream, fileName)
            } catch (e: Exception) {
                ParseResult.Error("解析 PDF 文档失败: ${e.message}", e)
            }
        }
    }

    private fun parseInternal(context: Context?, inputStream: InputStream, fileName: String): ParseResult<PdfParseResult> {
        var document: PDDocument? = null

        return try {
            document = PDDocument.load(inputStream)
            val totalPages = document.numberOfPages

            if (totalPages == 0) {
                return ParseResult.Error("PDF 文件为空或已损坏")
            }

            val stripper = PDFTextStripper()
            val pages = mutableListOf<PdfPageData>()

            // 逐页提取文本和图片
            for (pageNum in 1..totalPages) {
                stripper.startPage = pageNum
                stripper.endPage = pageNum
                val pageText = stripper.getText(document)?.trim() ?: ""

                // 智能分段
                val paragraphs = smartSplitParagraphs(pageText)
                
                // 提取图片
                val imagePaths = mutableListOf<String>()
                if (context != null) {
                    val page = document.getPage(pageNum - 1)
                    val resources = page.resources
                    if (resources != null) {
                        for (name in resources.xObjectNames) {
                            val xObject = resources.getXObject(name)
                            if (xObject is com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject) {
                                val bitmap = xObject.image
                                if (bitmap != null) {
                                    val picFile = java.io.File(context.cacheDir, "pdf_pic_${System.currentTimeMillis()}_${pageNum}_${imagePaths.size}.png")
                                    try {
                                        java.io.FileOutputStream(picFile).use { fos ->
                                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, fos)
                                        }
                                        imagePaths.add(picFile.absolutePath)
                                    } catch (_: Exception) {
                                        // 忽略
                                    }
                                }
                            }
                        }
                    }
                }

                pages.add(
                    PdfPageData(
                        pageNumber = pageNum,
                        paragraphs = paragraphs,
                        imagePaths = imagePaths
                    )
                )
            }

            // 检测是否为扫描版PDF（如果所有页面都没有文本）
            val isScanned = pages.all { it.paragraphs.isEmpty() }

            // 尝试获取文档信息
            val info = document.documentInformation
            val title = info.title?.takeIf { it.isNotEmpty() } ?: fileName

            ParseResult.Success(
                PdfParseResult(
                    fileName = fileName,
                    pages = pages,
                    totalPages = totalPages,
                    title = title,
                    isScanned = isScanned
                )
            )
        } catch (e: Exception) {
            ParseResult.Error("解析失败: ${e.message}", e)
        } finally {
            try {
                document?.close()
            } catch (e: Exception) {
                // 忽略关闭异常
            }
        }
    }

    /**
     * 智能分段：根据文本特征将页面内容拆分为段落
     * 增加表格行启发式识别
     */
    private fun smartSplitParagraphs(text: String): List<String> {
        if (text.isEmpty()) return emptyList()

        val paragraphs = mutableListOf<String>()

        // 按换行符拆分
        val lines = text.split("\n", "\r")

        val currentParagraph = StringBuilder()

        lines.forEach { line ->
            val trimmedLine = line.trim()

            // 启发式表格检测：如果一行中有多个连续大空格，可能是一个表格行
            val isPossiblyTableRow = line.contains("    ") || line.contains("\t")

            when {
                // 空行表示段落结束
                trimmedLine.isEmpty() -> {
                    if (currentParagraph.isNotEmpty()) {
                        paragraphs.add(currentParagraph.toString().trim())
                        currentParagraph.clear()
                    }
                }
                // 表格行处理
                isPossiblyTableRow -> {
                    if (currentParagraph.isNotEmpty()) {
                        paragraphs.add(currentParagraph.toString().trim())
                        currentParagraph.clear()
                    }
                    // 将多个空格替换为统一的分隔符，方便阅读
                    val formattedRow = trimmedLine.replace(Regex("\\s{2,}"), " | ")
                    paragraphs.add("表格数据：$formattedRow")
                }
                // 标题特征（短行、不以标点结尾）
                (trimmedLine.length < 50) &&
                        !trimmedLine.endsWith("。") &&
                        !trimmedLine.endsWith("，") &&
                        !trimmedLine.endsWith("；") &&
                        !trimmedLine.endsWith("：") &&
                        !trimmedLine.endsWith(".") &&
                        !trimmedLine.endsWith(",") -> {
                    if (currentParagraph.isNotEmpty()) {
                        paragraphs.add(currentParagraph.toString().trim())
                        currentParagraph.clear()
                    }
                    paragraphs.add(trimmedLine)
                }
                // 普通行，追加到当前段落
                else -> {
                    if (currentParagraph.isNotEmpty()) {
                        currentParagraph.append(" ")
                    }
                    currentParagraph.append(trimmedLine)

                    // 如果当前段落太长，强制分段
                    if (currentParagraph.length > PARAGRAPH_SPLIT_THRESHOLD) {
                        paragraphs.add(currentParagraph.toString().trim())
                        currentParagraph.clear()
                    }
                }
            }
        }

        // 处理最后一段
        if (currentParagraph.isNotEmpty()) {
            paragraphs.add(currentParagraph.toString().trim())
        }

        return paragraphs.filter { it.isNotEmpty() }
    }
}

package com.danmo.reader.parser

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.hwpf.HWPFDocument
import java.io.InputStream

/**
 * Word 段落数据
 */
data class WordParagraph(
    val text: String,
    val styleName: String = "",
    val isHeading: Boolean = false,
    val headingLevel: Int = 0,
    val isBold: Boolean = false,
    val alignment: String = "left",
    val index: Int = 0
)

/**
 * Word 文档解析结果
 */
data class WordParseResult(
    val fileName: String,
    val paragraphs: List<WordParagraph>,
    val title: String = "",
    val totalParagraphs: Int = 0
)

/**
 * Word 文档解析器
 * 支持 .doc (HWPF) 和 .docx (XWPF) 格式
 */
class WordParser : DocumentParser<WordParseResult> {

    override suspend fun parse(context: Context, uri: Uri): ParseResult<WordParseResult> {
        return withContext(Dispatchers.IO) {
            try {
                val documentFile = DocumentFile.fromSingleUri(context, uri)
                val fileName = documentFile?.name ?: "未知文件"
                val extension = fileName.substringAfterLast(".", "")

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    parseInternal(inputStream, fileName, extension)
                } ?: ParseResult.Error("无法打开文件输入流")
            } catch (e: Exception) {
                ParseResult.Error("解析 Word 文档失败: ${e.message}", e)
            }
        }
    }

    override suspend fun parse(inputStream: InputStream, fileName: String): ParseResult<WordParseResult> {
        return withContext(Dispatchers.IO) {
            try {
                val extension = fileName.substringAfterLast(".", "")
                parseInternal(inputStream, fileName, extension)
            } catch (e: Exception) {
                ParseResult.Error("解析 Word 文档失败: ${e.message}", e)
            }
        }
    }

    private fun parseInternal(
        inputStream: InputStream,
        fileName: String,
        extension: String
    ): ParseResult<WordParseResult> {
        return try {
            val paragraphs = when (extension.lowercase()) {
                "docx" -> parseDocx(inputStream)
                "doc" -> parseDoc(inputStream)
                else -> parseDocx(inputStream) // 默认尝试 docx
            }

            val title = paragraphs.firstOrNull { it.isHeading }?.text ?: fileName

            ParseResult.Success(
                WordParseResult(
                    fileName = fileName,
                    paragraphs = paragraphs,
                    title = title,
                    totalParagraphs = paragraphs.size
                )
            )
        } catch (e: Exception) {
            ParseResult.Error("解析失败: ${e.message}", e)
        }
    }

    /**
     * 解析 .docx 格式 (Office 2007+)
     */
    private fun parseDocx(inputStream: InputStream): List<WordParagraph> {
        val paragraphs = mutableListOf<WordParagraph>()
        var index = 0

        XWPFDocument(inputStream).use { document ->
            // 解析正文段落
            for (paragraph in document.paragraphs) {
                val text = paragraph.text?.trim() ?: ""
                if (text.isNotEmpty()) {
                    val styleName = paragraph.style ?: ""
                    val alignmentName = getAlignmentName(paragraph.alignment)
                    val headingLevel = extractHeadingLevel(styleName, alignmentName)
                    val isHeading = headingLevel > 0 || styleName.contains("Heading", ignoreCase = true)

                    paragraphs.add(
                        WordParagraph(
                            text = text,
                            styleName = styleName,
                            isHeading = isHeading,
                            headingLevel = headingLevel,
                            isBold = paragraph.runs.any { it.isBold },
                            alignment = alignmentName,
                            index = index++
                        )
                    )
                }
            }

            // 解析表格中的文本（按单元格顺序读取）
            for (table in document.tables) {
                for (row in table.rows) {
                    for (cell in row.tableCells) {
                        for (paragraph in cell.paragraphs) {
                            val text = paragraph.text?.trim() ?: ""
                            if (text.isNotEmpty()) {
                                val alignmentName = getAlignmentName(paragraph.alignment)
                                paragraphs.add(
                                    WordParagraph(
                                        text = text,
                                        styleName = "table_cell",
                                        isHeading = false,
                                        headingLevel = 0,
                                        isBold = false,
                                        alignment = alignmentName,
                                        index = index++
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        return paragraphs
    }

    /**
     * 解析 .doc 格式 (Office 97-2003)
     */
    private fun parseDoc(inputStream: InputStream): List<WordParagraph> {
        val paragraphs = mutableListOf<WordParagraph>()
        var index = 0

        HWPFDocument(inputStream).use { document ->
            // 获取全文 Range，然后按段落拆分
            val range = document.range
            val fullText = range.text()

            // 按段落分隔符拆分（Word .doc 使用 \r 或 \u0007 作为段落分隔符）
            val rawParagraphs = fullText.split("\r", "\n", "\u0007")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            for (paraText in rawParagraphs) {
                // 简单启发式判断标题
                val isHeading = paraText.length < 100 &&
                        (paraText.matches(Regex("^第[一二三四五六七八九十]+章.*")) ||
                                paraText.matches(Regex("^\\d+[\\.、].*")) ||
                                paraText.matches(Regex("^[一二三四五六七八九十]+[、\\.].*")))

                val headingLevel = when {
                    paraText.matches(Regex("^第[一二三四五六七八九十]+章.*")) -> 1
                    paraText.matches(Regex("^\\d+\\.\\d+.*")) -> 2
                    paraText.matches(Regex("^\\d+[\\.、].*")) -> 1
                    else -> 0
                }

                paragraphs.add(
                    WordParagraph(
                        text = paraText,
                        styleName = if (isHeading) "Heading$headingLevel" else "Normal",
                        isHeading = isHeading,
                        headingLevel = headingLevel,
                        isBold = isHeading,
                        alignment = "left",
                        index = index++
                    )
                )
            }
        }

        return paragraphs
    }

    /**
     * 获取对齐方式名称
     */
    private fun getAlignmentName(alignment: Any?): String {
        if (alignment == null) return "left"
        val alignmentStr = alignment.toString()
        return when {
            alignmentStr.contains("LEFT", ignoreCase = true) -> "left"
            alignmentStr.contains("CENTER", ignoreCase = true) -> "center"
            alignmentStr.contains("RIGHT", ignoreCase = true) -> "right"
            alignmentStr.contains("JUSTIFY", ignoreCase = true) -> "justify"
            alignmentStr.contains("BOTH", ignoreCase = true) -> "both"
            else -> "left"
        }
    }

    /**
     * 提取标题级别
     */
    private fun extractHeadingLevel(styleName: String, alignment: String): Int {
        return when {
            styleName.contains("Heading1", ignoreCase = true) -> 1
            styleName.contains("Heading2", ignoreCase = true) -> 2
            styleName.contains("Heading3", ignoreCase = true) -> 3
            styleName.contains("Heading4", ignoreCase = true) -> 4
            styleName.contains("Heading5", ignoreCase = true) -> 5
            styleName.contains("Title", ignoreCase = true) -> 1
            styleName.contains("Subtitle", ignoreCase = true) -> 2
            else -> 0
        }
    }
}
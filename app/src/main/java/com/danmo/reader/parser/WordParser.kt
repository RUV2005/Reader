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
 * Word 内容项类型枚举
 */
enum class WordContentType {
    TEXT,   // 普通段落或标题
    IMAGE,  // 嵌入图片
    TABLE   // 结构化表格
}

/**
 * Word 文档解析出的单个内容项数据
 */
data class WordContentData(
    val type: WordContentType = WordContentType.TEXT,
    val text: String? = null,                // 文本内容
    val imagePath: String? = null,           // 如果是图片，存储在缓存中的路径
    val description: String? = null,         // 图片描述（如有）
    val tableRows: List<List<String>>? = null, // 如果是表格，存储行列二维列表
    val styleName: String = "",              // Word 中的样式名
    val isHeading: Boolean = false,          // 是否识别为标题
    val headingLevel: Int = 0,               // 标题级别 (1-5)
    val isBold: Boolean = false,             // 是否加粗
    val alignment: String = "left",          // 对齐方式
    val index: Int = 0                       // 在文档中的原始序号
)

/**
 * Word 文档解析后的完整结果对象
 */
data class WordParseResult(
    val fileName: String,
    val contents: List<WordContentData>,
    val title: String = "",
    val totalItems: Int = 0
)

/**
 * Word 文档解析器实现类
 * 使用 Apache POI 库，支持 .doc (HWPF) 和 .docx (XWPF) 格式
 */
class WordParser : DocumentParser<WordParseResult> {

    /**
     * 从 Content URI 解析文档（主要入口）
     */
    override suspend fun parse(context: Context, uri: Uri): ParseResult<WordParseResult> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 获取文件名和扩展名
                val documentFile = DocumentFile.fromSingleUri(context, uri)
                val fileName = documentFile?.name ?: "未知文件"
                val extension = fileName.substringAfterLast(".", "")

                // 2. 打开输入流并开始解析
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    parseInternal(context, inputStream, fileName, extension)
                } ?: ParseResult.Error("无法打开文件输入流")
            } catch (e: Exception) {
                ParseResult.Error("解析 Word 文档失败: ${e.message}", e)
            }
        }
    }

    /**
     * 直接从 InputStream 解析（用于单元测试或预览）
     */
    override suspend fun parse(inputStream: InputStream, fileName: String): ParseResult<WordParseResult> {
        return withContext(Dispatchers.IO) {
            try {
                val extension = fileName.substringAfterLast(".", "")
                parseInternal(null, inputStream, fileName, extension)
            } catch (e: Exception) {
                ParseResult.Error("解析 Word 文档失败: ${e.message}", e)
            }
        }
    }

    /**
     * 内部解析核心逻辑：根据扩展名选择不同的 POI 处理引擎
     */
    private fun parseInternal(
        context: Context?,
        inputStream: InputStream,
        fileName: String,
        extension: String
    ): ParseResult<WordParseResult> {
        return try {
            val contents = when (extension.lowercase()) {
                "docx" -> parseDocx(context, inputStream)
                "doc" -> parseDoc(inputStream)
                else -> parseDocx(context, inputStream) // 默认尝试按现代格式解析
            }

            // 自动推断文档标题（通常是第一个识别到的标题）
            val title = contents.firstOrNull { it.isHeading }?.text ?: fileName

            ParseResult.Success(
                WordParseResult(
                    fileName = fileName,
                    contents = contents,
                    title = title,
                    totalItems = contents.size
                )
            )
        } catch (e: Exception) {
            ParseResult.Error("文档内部解析失败: ${e.message}", e)
        }
    }

    /**
     * 解析现代 .docx 格式 (Office 2007+)
     * 使用 XWPFDocument 引擎，支持流式读取 BodyElements
     */
    private fun parseDocx(context: Context?, inputStream: InputStream): List<WordContentData> {
        val contents = mutableListOf<WordContentData>()
        var index = 0

        XWPFDocument(inputStream).use { document ->
            // 关键：遍历 bodyElements 而非 paragraphs，以保持段落和表格的交叉顺序
            for (element in document.bodyElements) {
                when (element) {
                    // 处理普通段落
                    is org.apache.poi.xwpf.usermodel.XWPFParagraph -> {
                        val text = element.text?.trim() ?: ""
                        
                        // 1. 提取并保存段落中的图片（如有）
                        if (context != null) {
                            for (run in element.runs) {
                                for (pic in run.embeddedPictures) {
                                    val picData = pic.pictureData.data
                                    val ext = pic.pictureData.suggestFileExtension()
                                    // 保存到应用缓存目录，供 Coil 加载
                                    val picFile = java.io.File(context.cacheDir, "word_pic_${System.currentTimeMillis()}_${index}.$ext")
                                    try {
                                        java.io.FileOutputStream(picFile).use { fos ->
                                            fos.write(picData)
                                        }
                                        contents.add(
                                            WordContentData(
                                                type = WordContentType.IMAGE,
                                                imagePath = picFile.absolutePath,
                                                description = "文档中的图片",
                                                index = index++
                                            )
                                        )
                                    } catch (_: Exception) {
                                        // 图片提取失败不应中断整个解析过程
                                    }
                                }
                            }
                        }

                        // 2. 处理文字内容
                        if (text.isNotEmpty()) {
                            val styleName = element.style ?: ""
                            val alignmentName = getAlignmentName(element.alignment)
                            val headingLevel = extractHeadingLevel(styleName)
                            val isHeading = headingLevel > 0 || styleName.contains("Heading", ignoreCase = true)

                            contents.add(
                                WordContentData(
                                    type = WordContentType.TEXT,
                                    text = text,
                                    styleName = styleName,
                                    isHeading = isHeading,
                                    headingLevel = headingLevel,
                                    isBold = element.runs.any { it.isBold },
                                    alignment = alignmentName,
                                    index = index++
                                )
                            )
                        }
                    }
                    // 处理表格项
                    is org.apache.poi.xwpf.usermodel.XWPFTable -> {
                        val tableRows = mutableListOf<List<String>>()
                        for (row in element.rows) {
                            val rowData = row.tableCells.map { cell ->
                                cell.text?.trim() ?: ""
                            }
                            if (rowData.any { it.isNotEmpty() }) {
                                tableRows.add(rowData)
                            }
                        }
                        if (tableRows.isNotEmpty()) {
                            contents.add(
                                WordContentData(
                                    type = WordContentType.TABLE,
                                    tableRows = tableRows,
                                    index = index++
                                )
                            )
                        }
                    }
                }
            }
        }
        return contents
    }

    /**
     * 解析旧版 .doc 格式 (Office 97-2003)
     * 注意：由于旧版库支持有限，此方法主要提取文本流
     */
    private fun parseDoc(inputStream: InputStream): List<WordContentData> {
        val contents = mutableListOf<WordContentData>()
        var index = 0

        HWPFDocument(inputStream).use { document ->
            val range = document.range
            val fullText = range.text()

            // 按 Word 内部特有的段落分隔符进行拆分
            val rawParagraphs = fullText.split("\r", "\n", "\u0007")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            for (paraText in rawParagraphs) {
                // 使用启发式正则表达式识别可能的标题和层级
                val isHeading = paraText.length < 100 &&
                        (paraText.matches(Regex("^第[一二三四五六七八九十]+章.*")) ||
                                paraText.matches(Regex("^\\d+[.、].*")) ||
                                paraText.matches(Regex("^[一二三四五六七八九十]+[、.].*")))

                val headingLevel = when {
                    paraText.matches(Regex("^第[一二三四五六七八九十]+章.*")) -> 1
                    paraText.matches(Regex("^\\d+\\.\\d+.*")) -> 2
                    else -> if (isHeading) 1 else 0
                }

                contents.add(
                    WordContentData(
                        type = WordContentType.TEXT,
                        text = paraText,
                        styleName = if (isHeading) "Heading$headingLevel" else "Normal",
                        isHeading = isHeading,
                        headingLevel = headingLevel,
                        isBold = isHeading,
                        index = index++
                    )
                )
            }
        }
        return contents
    }

    /**
     * 将 POI 枚举转换为内部可读的字符串对齐方式
     */
    private fun getAlignmentName(alignment: Any?): String {
        if (alignment == null) return "left"
        val alignmentStr = alignment.toString()
        return when {
            alignmentStr.contains("CENTER", ignoreCase = true) -> "center"
            alignmentStr.contains("RIGHT", ignoreCase = true) -> "right"
            alignmentStr.contains("BOTH", ignoreCase = true) -> "justify"
            else -> "left"
        }
    }

    /**
     * 根据样式名推断标题级别
     */
    private fun extractHeadingLevel(styleName: String): Int {
        return when {
            styleName.contains("Heading1", ignoreCase = true) -> 1
            styleName.contains("Heading2", ignoreCase = true) -> 2
            styleName.contains("Heading3", ignoreCase = true) -> 3
            styleName.contains("Title", ignoreCase = true) -> 1
            else -> 0
        }
    }
}

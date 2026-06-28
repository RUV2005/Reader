package com.danmo.reader.parser

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.danmo.reader.common.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import java.io.InputStream

// ==================== 数据模型 ====================

/**
 * Word 文档内容的类型枚举
 */
enum class WordContentType {
    TEXT, IMAGE, TABLE
}

/**
 * Word 文档单一数据项的封装模型
 */
data class WordContentData(
    val type: WordContentType,
    val text: String? = null,
    val imagePath: String? = null,
    val description: String? = null,
    val tableRows: List<List<String>>? = null,
    val styleName: String = "",
    val isHeading: Boolean = false,
    val headingLevel: Int = 0,
    val isBold: Boolean = false,
    val alignment: String = "left",
    val index: Int = 0
)

/**
 * Word 文档全量解析结果
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
                val docHash = FileUtils.getUriHash(uri)

                // 2. 打开输入流并开始解析
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    parseInternal(context, inputStream, fileName, extension, docHash)
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
                // 无 URI 时使用随机 Hash
                parseInternal(null, inputStream, fileName, extension, "temp_${System.currentTimeMillis()}")
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
        extension: String,
        docHash: String
    ): ParseResult<WordParseResult> {
        return try {
            val contents = when (extension.lowercase()) {
                "docx" -> parseDocx(context, inputStream, docHash)
                "doc" -> parseDoc(inputStream)
                else -> parseDocx(context, inputStream, docHash) // 默认尝试按现代格式解析
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
    private fun parseDocx(context: Context?, inputStream: InputStream, docHash: String): List<WordContentData> {
        val contents = mutableListOf<WordContentData>()
        var index = 0

        XWPFDocument(inputStream).use { document ->
            val docCacheDir = if (context != null) FileUtils.getDocCacheDir(context, docHash) else null
            
            // 关键：遍历 bodyElements 而非 paragraphs，以保持段落和表格的交叉顺序
            for (element in document.bodyElements) {
                when (element) {
                    // 处理普通段落
                    is org.apache.poi.xwpf.usermodel.XWPFParagraph -> {
                        val text = element.text?.trim() ?: ""
                        
                        // 1. 提取并保存段落中的图片（如有）
                        if (docCacheDir != null) {
                            var picIndexInPara = 0
                            for (run in element.runs) {
                                for (pic in run.embeddedPictures) {
                                    val picData = pic.pictureData.data
                                    val ext = pic.pictureData.suggestFileExtension()
                                    // 使用确定性命名：文档Hash + 段落索引 + 段内图片索引
                                    val fileName = "img_${index}_${picIndexInPara++}.$ext"
                                    val picFile = java.io.File(docCacheDir, fileName)
                                    
                                    if (!picFile.exists()) {
                                        try {
                                            java.io.FileOutputStream(picFile).use { fos ->
                                                fos.write(picData)
                                            }
                                        } catch (_: Exception) {}
                                    }
                                    
                                    contents.add(
                                        WordContentData(
                                            type = WordContentType.IMAGE,
                                            imagePath = picFile.absolutePath,
                                            description = "文档中的图片",
                                            index = index++
                                        )
                                    )
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
     * 解析旧版 .doc 格式 (Word 97-2003)
     * 仅支持纯文本提取，不支持表格样式和图片
     */
    private fun parseDoc(inputStream: InputStream): List<WordContentData> {
        val contents = mutableListOf<WordContentData>()
        var index = 0

        HWPFDocument(inputStream).use { document ->
            val extractor = WordExtractor(document)
            val fullText = extractor.text ?: ""
            val rawParagraphs = fullText.split("\r", "\n", "\u0007")

            for (paraText in rawParagraphs) {
                if (paraText.isNotBlank()) {
                    // 启发式识别标题（针对旧版文档）
                    val isHeading = (paraText.length < 50 && 
                        (paraText.matches(Regex("^第[一二三四五六七八九十]+章.*")) ||
                                paraText.matches(Regex("^\\d+[.、].*")) ||
                                paraText.matches(Regex("^[一二三四五六七八九十]+[、.].*"))))

                    val headingLevel = when {
                        !isHeading -> 0
                        paraText.matches(Regex("^第[一二三四五六七八九十]+章.*")) -> 1
                        paraText.matches(Regex("^\\d+\\.\\d+.*")) -> 2
                        else -> 3
                    }

                    contents.add(
                        WordContentData(
                            type = WordContentType.TEXT,
                            text = paraText.trim(),
                            isHeading = isHeading,
                            headingLevel = headingLevel,
                            styleName = if (isHeading) "Heading$headingLevel" else "Normal",
                            index = index++
                        )
                    )
                }
            }
        }
        return contents
    }

    /**
     * 辅助工具：转换 POI 对齐方式为 String 描述
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
     * 辅助工具：从样式名中提取标题等级
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

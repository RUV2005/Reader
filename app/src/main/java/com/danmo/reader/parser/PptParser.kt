package com.danmo.reader.parser

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import org.apache.poi.hslf.usermodel.HSLFSlide
import org.apache.poi.xslf.usermodel.XSLFSlide
import org.apache.poi.xslf.usermodel.XSLFShape
import org.apache.poi.xslf.usermodel.XSLFTextShape
import org.apache.poi.xslf.usermodel.XSLFTable
import java.io.InputStream

/**
 * PPT 幻灯片内容
 */
data class PptSlideData(
    val slideNumber: Int,
    val title: String,
    val content: List<String>,
    val notes: String = "",
    val layout: String = ""
)

/**
 * PPT 解析结果
 */
data class PptParseResult(
    val fileName: String,
    val slides: List<PptSlideData>,
    val totalSlides: Int = 0
)

/**
 * PPT 文档解析器
 * 支持 .ppt (HSLF) 和 .pptx (XSLF) 格式
 */
class PptParser : DocumentParser<PptParseResult> {

    override suspend fun parse(context: Context, uri: Uri): ParseResult<PptParseResult> {
        return withContext(Dispatchers.IO) {
            try {
                val documentFile = DocumentFile.fromSingleUri(context, uri)
                val fileName = documentFile?.name ?: "未知文件"
                val extension = fileName.substringAfterLast(".", "")

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    parseInternal(inputStream, fileName, extension)
                } ?: ParseResult.Error("无法打开文件输入流")
            } catch (e: Exception) {
                ParseResult.Error("解析 PPT 文档失败: ${e.message}", e)
            }
        }
    }

    override suspend fun parse(inputStream: InputStream, fileName: String): ParseResult<PptParseResult> {
        return withContext(Dispatchers.IO) {
            try {
                val extension = fileName.substringAfterLast(".", "")
                parseInternal(inputStream, fileName, extension)
            } catch (e: Exception) {
                ParseResult.Error("解析 PPT 文档失败: ${e.message}", e)
            }
        }
    }

    private fun parseInternal(
        inputStream: InputStream,
        fileName: String,
        extension: String
    ): ParseResult<PptParseResult> {
        return try {
            val slides = when (extension.lowercase()) {
                "pptx" -> parsePptx(inputStream)
                "ppt" -> parsePpt(inputStream)
                else -> parsePptx(inputStream)
            }

            ParseResult.Success(
                PptParseResult(
                    fileName = fileName,
                    slides = slides,
                    totalSlides = slides.size
                )
            )
        } catch (e: Exception) {
            ParseResult.Error("解析失败: ${e.message}", e)
        }
    }

    /**
     * 解析 .pptx 格式
     */
    private fun parsePptx(inputStream: InputStream): List<PptSlideData> {
        val slides = mutableListOf<PptSlideData>()

        XMLSlideShow(inputStream).use { ppt ->
            val slideList = ppt.slides
            for (index in slideList.indices) {
                val slide = slideList[index]
                val title = extractPptxTitle(slide)
                val content = extractPptxContent(slide)
                val notes = extractPptxNotes(slide)

                slides.add(
                    PptSlideData(
                        slideNumber = index + 1,
                        title = title,
                        content = content,
                        notes = notes,
                        layout = ""
                    )
                )
            }
        }

        return slides
    }

    /**
     * 解析 .ppt 格式
     */
    private fun parsePpt(inputStream: InputStream): List<PptSlideData> {
        val slides = mutableListOf<PptSlideData>()

        HSLFSlideShow(inputStream).use { ppt ->
            val slideList = ppt.slides
            for (index in slideList.indices) {
                val slide = slideList[index]
                val title = extractPptTitle(slide)
                val content = extractPptContent(slide)
                val notes = extractPptNotes(slide)

                slides.add(
                    PptSlideData(
                        slideNumber = index + 1,
                        title = title,
                        content = content,
                        notes = notes,
                        layout = ""
                    )
                )
            }
        }

        return slides
    }

    // ========== PPTX 提取方法 ==========

    private fun extractPptxTitle(slide: XSLFSlide): String {
        // 尝试从标题占位符获取
        val titleShape = slide.placeholders.find { it.shapeType?.name?.contains("TITLE") == true }
        if (titleShape != null) {
            val text = titleShape.text?.trim()
            if (!text.isNullOrEmpty()) return text
        }

        // 从第一个文本框获取（通常第一个是标题）
        val shapes = slide.shapes
        for (shape in shapes) {
            if (shape is XSLFTextShape) {
                val text = shape.text?.trim()
                if (!text.isNullOrEmpty()) return text
            }
        }

        return "第${slide.slideNumber}页"
    }

    private fun extractPptxContent(slide: XSLFSlide): List<String> {
        val content = mutableListOf<String>()

        for (shape in slide.shapes) {
            when (shape) {
                is XSLFTextShape -> {
                    val text = shape.text?.trim()
                    if (!text.isNullOrEmpty()) {
                        // 按段落拆分
                        val paragraphs = text.split("\n")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        content.addAll(paragraphs)
                    }
                }
                is XSLFTable -> {
                    // 表格内容
                    for (row in shape.rows) {
                        val rowText = row.cells.map { it.text?.trim() ?: "" }
                            .filter { it.isNotEmpty() }
                            .joinToString("，")
                        if (rowText.isNotEmpty()) {
                            content.add("表格行：$rowText")
                        }
                    }
                }
            }
        }

        return content.distinct()
    }

    private fun extractPptxNotes(slide: XSLFSlide): String {
        return try {
            val notes = slide.notes
            if (notes == null) return ""

            val result = StringBuilder()
            val shapes = notes.shapes
            for (i in 0 until shapes.size) {
                val shape = shapes[i]
                if (shape is XSLFTextShape) {
                    val text = shape.text?.trim()
                    if (!text.isNullOrEmpty()) {
                        result.append(text).append("\n")
                    }
                }
            }
            result.toString().trim()
        } catch (e: Exception) {
            ""
        }
    }

    // ========== PPT 提取方法 ==========

    private fun extractPptTitle(slide: HSLFSlide): String {
        val title = slide.title?.trim()
        return if (!title.isNullOrEmpty()) title else "第${slide.slideNumber + 1}页"
    }

    private fun extractPptContent(slide: HSLFSlide): List<String> {
        val content = mutableListOf<String>()

        for (shape in slide.shapes) {
            if (shape is org.apache.poi.hslf.usermodel.HSLFTextShape) {
                val text = shape.text?.trim()
                if (!text.isNullOrEmpty()) {
                    val paragraphs = text.split("\n")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    content.addAll(paragraphs)
                }
            }
        }

        return content.distinct()
    }

    private fun extractPptNotes(slide: HSLFSlide): String {
        return try {
            val notes = slide.notes
            if (notes == null) return ""

            val result = StringBuilder()
            val shapes = notes.shapes
            for (i in 0 until shapes.size) {
                val shape = shapes[i]
                if (shape is org.apache.poi.hslf.usermodel.HSLFTextShape) {
                    val text = shape.text?.trim()
                    if (!text.isNullOrEmpty()) {
                        result.append(text).append("\n")
                    }
                }
            }
            result.toString().trim()
        } catch (e: Exception) {
            ""
        }
    }
}
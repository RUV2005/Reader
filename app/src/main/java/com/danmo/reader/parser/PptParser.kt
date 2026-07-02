package com.danmo.reader.parser

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.danmo.reader.common.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import org.apache.poi.xslf.usermodel.XMLSlideShow
import java.io.InputStream

// ==================== 数据模型 ====================

data class PptSlideData(
    val slideNumber: Int,
    val title: String,
    val content: List<String>,
    val notes: String = "",
    val layout: String = "",
    val imagePaths: List<String>,
    val tables: List<List<List<String>>> = emptyList()
)

data class PptParseResult(
    val fileName: String,
    val slides: List<PptSlideData>,
    val totalSlides: Int
)

// ==================== PPT 解析器 ====================

class PptParser : DocumentParser<PptParseResult> {

    override suspend fun parse(context: Context, uri: Uri): ParseResult<PptParseResult> {
        return withContext(Dispatchers.IO) {
            try {
                val documentFile = DocumentFile.fromSingleUri(context, uri)
                val fileName = documentFile?.name ?: "未知文件"
                val extension = fileName.substringAfterLast(".", "")
                val docHash = FileUtils.getUriHash(uri)

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    parseInternal(context, inputStream, fileName, extension, docHash)
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
                parseInternal(null, inputStream, fileName, extension, "temp_${System.currentTimeMillis()}")
            } catch (e: Exception) {
                ParseResult.Error("解析 PPT 文档失败: ${e.message}", e)
            }
        }
    }

    private fun parseInternal(context: Context?, inputStream: InputStream, fileName: String, extension: String, docHash: String): ParseResult<PptParseResult> {
        return try {
            val slides = when (extension.lowercase()) {
                "pptx" -> parsePptx(context, inputStream, docHash)
                "ppt" -> parsePpt(inputStream)
                else -> parsePptx(context, inputStream, docHash)
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

    private fun parsePptx(context: Context?, inputStream: InputStream, docHash: String): List<PptSlideData> {
        val slides = mutableListOf<PptSlideData>()
        val docCacheDir = if (context != null) FileUtils.getDocCacheDir(context, docHash) else null
        
        XMLSlideShow(inputStream).use { ppt ->
            ppt.slides.forEachIndexed { index, slide ->
                val slideNumber = index + 1
                val title = extractPptxTitle(slide)
                val content = extractPptxContent(slide)
                val notes = extractPptxNotes(slide)
                val imagePaths = mutableListOf<String>()
                val tables = mutableListOf<List<List<String>>>()

                slide.shapes.forEach { shape ->
                    when (shape) {
                        is org.apache.poi.xslf.usermodel.XSLFTable -> {
                            val tableData = mutableListOf<List<String>>()
                            shape.rows.forEach { row ->
                                val rowCells = row.cells.map { cell -> cell.text?.trim() ?: "" }
                                if (rowCells.any { it.isNotEmpty() }) {
                                    tableData.add(rowCells)
                                }
                            }
                            if (tableData.isNotEmpty()) {
                                tables.add(tableData)
                            }
                        }
                        is org.apache.poi.xslf.usermodel.XSLFPictureShape -> {
                            if (docCacheDir != null) {
                                val picData = shape.pictureData.data
                                val ext = shape.pictureData.suggestFileExtension()
                                val picFileName = "ppt_pic_${index}_${imagePaths.size}.$ext"
                                val picFile = java.io.File(docCacheDir, picFileName)
                                
                                if (!picFile.exists()) {
                                    java.io.FileOutputStream(picFile).use { fos ->
                                        fos.write(picData)
                                    }
                                }
                                imagePaths.add(picFile.absolutePath)
                            }
                        }
                    }
                }

                slides.add(PptSlideData(slideNumber, title, content, notes, "", imagePaths, tables))
            }
        }
        return slides
    }

    private fun parsePpt(inputStream: InputStream): List<PptSlideData> {
        val slides = mutableListOf<PptSlideData>()
        HSLFSlideShow(inputStream).use { ppt ->
            ppt.slides.forEachIndexed { index, slide ->
                val slideNumber = index + 1
                val title = extractPptTitle(slide)
                val content = extractPptContent(slide)
                val notes = extractPptNotes(slide)
                slides.add(PptSlideData(slideNumber, title, content, notes, "", emptyList()))
            }
        }
        return slides
    }

    private fun extractPptxTitle(slide: org.apache.poi.xslf.usermodel.XSLFSlide): String {
        val titleShape = slide.placeholders.find { it.shapeType?.name?.contains("TITLE") == true }
        return (titleShape as? org.apache.poi.xslf.usermodel.XSLFTextShape)?.text?.trim() ?: ""
    }

    private fun extractPptxContent(slide: org.apache.poi.xslf.usermodel.XSLFSlide): List<String> {
        val content = mutableListOf<String>()
        slide.shapes.forEach { shape ->
            if (shape is org.apache.poi.xslf.usermodel.XSLFTextShape) {
                val text = shape.text?.trim() ?: ""
                if (text.isNotEmpty() && !text.contains(extractPptxTitle(slide))) {
                    val paragraphs = text.split("\n")
                    content.addAll(paragraphs.filter { it.isNotBlank() })
                }
            }
        }
        return content
    }

    private fun extractPptxNotes(slide: org.apache.poi.xslf.usermodel.XSLFSlide): String {
        val notes = slide.notes
        val result = StringBuilder()
        notes?.shapes?.forEach { shape ->
            if (shape is org.apache.poi.xslf.usermodel.XSLFTextShape) {
                val text = shape.text?.trim() ?: ""
                if (text.isNotEmpty()) {
                    result.append(text).append("\n")
                }
            }
        }
        return result.toString().trim()
    }

    private fun extractPptTitle(slide: org.apache.poi.hslf.usermodel.HSLFSlide): String {
        return slide.title?.trim() ?: ""
    }

    private fun extractPptContent(slide: org.apache.poi.hslf.usermodel.HSLFSlide): List<String> {
        val content = mutableListOf<String>()
        slide.shapes.forEach { shape ->
            if (shape is org.apache.poi.hslf.usermodel.HSLFTextShape) {
                val text = shape.text?.trim() ?: ""
                if (text.isNotEmpty() && text != extractPptTitle(slide)) {
                    val paragraphs = text.split("\n")
                    content.addAll(paragraphs.filter { it.isNotBlank() })
                }
            }
        }
        return content
    }

    private fun extractPptNotes(slide: org.apache.poi.hslf.usermodel.HSLFSlide): String {
        val notes = slide.notes
        val result = StringBuilder()
        notes?.textParagraphs?.forEach { paras ->
            paras.forEach { para ->
                val text = para.toString().trim()
                if (text.isNotEmpty()) {
                    result.append(text).append("\n")
                }
            }
        }
        return result.toString().trim()
    }
}

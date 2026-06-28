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

                val stripper = PDFTextStripper()
                stripper.sortByPosition = true // 关键：开启位置排序，保证表格内容按行读取

                for (i in 0 until totalPages) {
                    val pageNum = i + 1
                    stripper.startPage = pageNum
                    stripper.endPage = pageNum
                    
                    // 获取当前页文本
                    val pageText = stripper.getText(document)?.trim() ?: ""

                    // 1. 提取图片
                    val imagePaths = mutableListOf<String>()
                    if (docCacheDir != null) {
                        try {
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
                        } catch (_: Exception) {}
                    }

                    // 2. 逻辑化段落切分（针对银行流水优化）
                    val paragraphs = logicalSplit(pageText)
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

    /**
     * 逻辑化切分算法
     * 针对银行流水等表格化 PDF，尝试恢复其横向逻辑
     */
    private fun logicalSplit(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val rawLines = text.split("\n", "\r").filter { it.isNotBlank() }
        val result = mutableListOf<String>()

        for (line in rawLines) {
            val trimmedLine = line.trim()
            
            // 启发式检测：是否包含大量空格或制表符（表格特征）
            // 或者是典型的流水行（包含日期格式 YYYY-MM-DD）
            val isTableLike = line.contains("    ") || 
                             line.contains("\t") || 
                             trimmedLine.matches(Regex(".*\\d{4}-\\d{2}-\\d{2}.*")) ||
                             trimmedLine.matches(Regex(".*\\d{2}/\\d{2}/\\d{4}.*"))

            if (isTableLike) {
                // 表格行处理：将多个空格替换为明显的竖线分隔符，辅助 TTS 感知
                val formattedRow = trimmedLine.replace(Regex("\\s{2,}"), " | ")
                result.add("Table Data: $formattedRow")
            } else {
                // 普通段落处理
                // 如果行末没有终止符，尝试与下一行合并（PDF 自动换行修复）
                if (result.isNotEmpty() && !result.last().startsWith("Table Data") &&
                    !isParagraphEnd(result.last())) {
                    val lastIdx = result.size - 1
                    result[lastIdx] = result[lastIdx] + " " + trimmedLine
                } else {
                    result.add(trimmedLine)
                }
            }
        }

        return result
    }

    private fun isParagraphEnd(text: String): Boolean {
        val endChars = listOf('.', '?', '!', '。', '？', '！', ';', '；')
        return text.isNotEmpty() && endChars.contains(text.last())
    }
}

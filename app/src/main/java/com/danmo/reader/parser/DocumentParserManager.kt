package com.danmo.reader.parser

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/**
 * 文档解析管理器
 * 统一入口，自动识别文档类型并调用对应解析器
 */
object DocumentParserManager {

    private val wordParser = WordParser()
    private val excelParser = ExcelParser()
    private val pptParser = PptParser()
    private val pdfParser = PdfParser()

    /**
     * 自动识别并解析文档
     */
    suspend fun parse(context: Context, uri: Uri): ParseResult<*> {
        val documentFile = DocumentFile.fromSingleUri(context, uri)
        val fileName = documentFile?.name ?: return ParseResult.Error("无法获取文件名")
        val mimeType = documentFile.type

        val documentType = when {
            mimeType != null -> DocumentType.fromMimeType(mimeType)
            else -> DocumentType.fromExtension(fileName.substringAfterLast(".", ""))
        }

        return when (documentType) {
            DocumentType.WORD -> wordParser.parse(context, uri)
            DocumentType.EXCEL -> excelParser.parse(context, uri)
            DocumentType.POWERPOINT -> pptParser.parse(context, uri)
            DocumentType.PDF -> pdfParser.parse(context, uri)
            DocumentType.UNKNOWN -> ParseResult.Error("不支持的文件格式")
        }
    }

    /**
     * 获取文档类型
     */
    fun getDocumentType(context: Context, uri: Uri): DocumentType {
        val documentFile = DocumentFile.fromSingleUri(context, uri)
        val fileName = documentFile?.name ?: return DocumentType.UNKNOWN
        val mimeType = documentFile.type

        return when {
            mimeType != null -> DocumentType.fromMimeType(mimeType)
            else -> DocumentType.fromExtension(fileName.substringAfterLast(".", ""))
        }
    }

    /**
     * 检查是否为支持的文档
     */
    fun isSupported(uri: Uri): Boolean {
        val extension = uri.lastPathSegment?.substringAfterLast(".", "") ?: return false
        return DocumentType.fromExtension(extension) != DocumentType.UNKNOWN
    }
}
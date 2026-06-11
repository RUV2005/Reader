package com.danmo.reader.parser

import android.content.Context
import android.net.Uri
import java.io.InputStream

/**
 * 文档解析结果封装
 */
sealed class ParseResult<out T> {
    data class Success<T>(val data: T) : ParseResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : ParseResult<Nothing>()
}

/**
 * 文档解析器接口
 */
interface DocumentParser<T> {
    suspend fun parse(context: Context, uri: Uri): ParseResult<T>
    suspend fun parse(inputStream: InputStream, fileName: String): ParseResult<T>
}

/**
 * 文档类型识别
 */
enum class DocumentType {
    WORD, EXCEL, POWERPOINT, PDF, UNKNOWN;

    companion object {
        fun fromExtension(extension: String?): DocumentType {
            return when (extension?.lowercase()) {
                "doc", "docx" -> WORD
                "xls", "xlsx" -> EXCEL
                "ppt", "pptx" -> POWERPOINT
                "pdf" -> PDF
                else -> UNKNOWN
            }
        }

        fun fromMimeType(mimeType: String?): DocumentType {
            return when {
                mimeType?.contains("word") == true -> WORD
                mimeType?.contains("excel") == true || mimeType?.contains("sheet") == true -> EXCEL
                mimeType?.contains("powerpoint") == true || mimeType?.contains("presentation") == true -> POWERPOINT
                mimeType?.contains("pdf") == true -> PDF
                else -> UNKNOWN
            }
        }
    }
}
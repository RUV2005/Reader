package com.danmo.reader.filepicker

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResultLauncher
import com.danmo.reader.parser.DocumentType

object DocumentPicker {

    fun createLauncher(
        activity: androidx.activity.ComponentActivity,
        onResult: (android.net.Uri?, DocumentType?) -> Unit
    ): androidx.activity.result.ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val uri = result.data?.data
            // 使用 ContentResolver 查询真实文件名来推断类型
            val type = uri?.let { inferTypeFromUri(activity, it) }
            onResult(uri, type)
        }
    }

    fun openPicker(
        launcher: ActivityResultLauncher<Intent>,
        docType: DocumentType? = null,
    ) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)

            when (docType) {
                DocumentType.WORD -> {
                    type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                }
                DocumentType.EXCEL -> {
                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                }
                DocumentType.POWERPOINT -> {
                    type = "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                }
                DocumentType.PDF -> {
                    type = "application/pdf"
                }
                DocumentType.UNKNOWN, null -> {
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "application/pdf",
                        "application/msword",
                        "application/vnd.ms-excel",
                        "application/vnd.ms-powerpoint",
                    ))
                }
            }
        }
        launcher.launch(intent)
    }

    /**
     * 使用 ContentResolver 查询文件名，从文件名推断类型
     */
    fun inferTypeFromUri(context: Context, uri: android.net.Uri): DocumentType {
        // 先尝试从 ContentResolver 查询显示名
        val displayName = try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) cursor.getString(nameIndex) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }

        // 使用查询到的文件名，或回退到 URI 字符串
        val fileName = displayName ?: uri.toString()
        val path = fileName.lowercase()

        return when {
            path.endsWith(".docx") || path.endsWith(".doc") -> DocumentType.WORD
            path.endsWith(".xlsx") || path.endsWith(".xls") -> DocumentType.EXCEL
            path.endsWith(".pptx") || path.endsWith(".ppt") -> DocumentType.POWERPOINT
            path.endsWith(".pdf") -> DocumentType.PDF
            else -> DocumentType.UNKNOWN
        }
    }
}
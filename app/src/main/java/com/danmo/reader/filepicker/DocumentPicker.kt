package com.danmo.reader.filepicker

import android.content.Context
import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResultLauncher
import com.danmo.reader.parser.DocumentType

object DocumentPicker {

    fun createLauncher(
        activity: androidx.activity.ComponentActivity,
        onResult: (android.net.Uri?, DocumentType?, String?) -> Unit
    ): androidx.activity.result.ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val uri = result.data?.data
            val type = uri?.let { inferTypeFromUri(activity, it) }
            val fileName = uri?.let { getFileName(activity, it) }
            onResult(uri, type, fileName)
        }
    }

    fun openPicker(
        launcher: ActivityResultLauncher<Intent>,
        docType: DocumentType? = null,
    ) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            // 关键：请求持久化权限
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)

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

    fun inferTypeFromUri(context: Context, uri: android.net.Uri): DocumentType {
        val fileName = getFileName(context, uri)?.lowercase() ?: uri.toString().lowercase()

        return when {
            fileName.endsWith(".docx") || fileName.endsWith(".doc") -> DocumentType.WORD
            fileName.endsWith(".xlsx") || fileName.endsWith(".xls") -> DocumentType.EXCEL
            fileName.endsWith(".pptx") || fileName.endsWith(".ppt") -> DocumentType.POWERPOINT
            fileName.endsWith(".pdf") -> DocumentType.PDF
            else -> DocumentType.UNKNOWN
        }
    }

    fun getFileName(context: Context, uri: android.net.Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) cursor.getString(nameIndex) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
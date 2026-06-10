package com.danmo.reader.filepicker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.danmo.reader.parser.DocumentType

/**
 * 文档选择器
 */
class DocumentPicker private constructor() {

    companion object {
        // 支持的 MIME 类型
        private val SUPPORTED_MIME_TYPES = arrayOf(
            "application/msword",                                    // .doc
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "application/vnd.ms-excel",                              // .xls
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",     // .xlsx
            "application/vnd.ms-powerpoint",                       // .ppt
            "application/vnd.openxmlformats-officedocument.presentationml.presentation", // .pptx
            "application/pdf"                                      // .pdf
        )

        /**
         * 创建文档选择器 Launcher（在 Activity 中使用）
         */
        fun createLauncher(
            activity: AppCompatActivity,
            onResult: (Uri?, DocumentType?) -> Unit
        ): ActivityResultLauncher<Intent> {
            return activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val uri = result.data?.data
                    val docType = uri?.let {
                        DocumentType.fromMimeType(activity.contentResolver.getType(it))
                    }
                    onResult(uri, docType)
                } else {
                    onResult(null, null)
                }
            }
        }

        /**
         * 创建文档选择器 Launcher（在 Fragment 中使用）
         */
        fun createLauncher(
            fragment: Fragment,
            onResult: (Uri?, DocumentType?) -> Unit
        ): ActivityResultLauncher<Intent> {
            return fragment.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val uri = result.data?.data
                    val docType = uri?.let {
                        fragment.requireContext().contentResolver.getType(it)?.let { mime ->
                            DocumentType.fromMimeType(mime)
                        }
                    }
                    onResult(uri, docType)
                } else {
                    onResult(null, null)
                }
            }
        }

        /**
         * 打开文档选择器
         */
        fun openPicker(launcher: ActivityResultLauncher<Intent>) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, SUPPORTED_MIME_TYPES)
            }
            launcher.launch(intent)
        }
    }
}
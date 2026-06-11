package com.danmo.reader.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.danmo.reader.parser.DocumentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class FileStorageRepository(private val context: Context) {

    private val storageDir: File
        get() = File(context.filesDir, "documents").apply { mkdirs() }

    /**
     * 将 URI 指向的文件复制到应用私有目录
     * @return 复制后的本地文件路径，失败返回 null
     */
    suspend fun copyToPrivateStorage(uri: Uri, docType: DocumentType): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileNameFromUri(uri) ?: generateFileName(docType)
            val destFile = File(storageDir, fileName)

            // 如果文件已存在，先删除
            if (destFile.exists()) {
                destFile.delete()
            }

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取私有目录中的文件
     */
    fun getPrivateFile(fileName: String): File? {
        val file = File(storageDir, fileName)
        return if (file.exists()) file else null
    }

    /**
     * 获取所有已保存的文件
     */
    fun getAllSavedFiles(): List<File> {
        return storageDir.listFiles()?.toList() ?: emptyList()
    }

    /**
     * 删除私有文件
     */
    fun deletePrivateFile(fileName: String): Boolean {
        return File(storageDir, fileName).delete()
    }

    private fun getFileNameFromUri(uri: Uri): String? {
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

    private fun generateFileName(docType: DocumentType): String {
        val ext = when (docType) {
            DocumentType.WORD -> "docx"
            DocumentType.EXCEL -> "xlsx"
            DocumentType.POWERPOINT -> "pptx"
            DocumentType.PDF -> "pdf"
            DocumentType.UNKNOWN -> "bin"
        }
        return "${UUID.randomUUID()}.$ext"
    }
}
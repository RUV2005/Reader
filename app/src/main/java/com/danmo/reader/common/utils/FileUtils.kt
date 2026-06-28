package com.danmo.reader.common.utils

import android.content.Context
import android.net.Uri
import java.security.MessageDigest

/**
 * 文件处理工具类
 */
object FileUtils {

    /**
     * 生成 URI 的 MD5 哈希值，用于唯一标识文档
     */
    fun getUriHash(uri: Uri): String {
        val bytes = uri.toString().toByteArray()
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * 获取文档专用的缓存目录
     */
    fun getDocCacheDir(context: Context, docHash: String): java.io.File {
        val dir = java.io.File(context.cacheDir, "docs/$docHash")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}

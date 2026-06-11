package com.danmo.reader.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * SAF 持久化 URI 权限管理
 */
object UriPermissionManager {

    /**
     * 持久化 URI 权限
     */
    fun persistUriPermission(context: Context, uri: Uri) {
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * 检查是否仍有权限访问该 URI
     */
    fun hasUriPermission(context: Context, uri: Uri): Boolean {
        val persistedUris = context.contentResolver.persistedUriPermissions
        return persistedUris.any { it.uri == uri && it.isReadPermission }
    }

    /**
     * 释放持久化权限
     */
    fun releaseUriPermission(context: Context, uri: Uri) {
        try {
            context.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
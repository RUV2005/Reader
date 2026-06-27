package com.danmo.reader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 便签/收藏数据实体
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String, // 药品, 生活, 工作, 临时 等
    val timestamp: Long,
    val sourcePath: String? = null // 原始图片或文档路径
)

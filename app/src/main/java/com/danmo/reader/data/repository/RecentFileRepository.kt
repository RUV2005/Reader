package com.danmo.reader.data.repository

import android.content.Context
import com.danmo.reader.R
import com.danmo.reader.data.local.AppDatabase
import com.danmo.reader.data.local.RecentFileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

data class RecentFile(
    val id: String,
    val name: String,
    val type: String,
    val uri: String,           // 持久化 URI（原始 SAF URI）
    val openTimeDisplay: String,
)

class RecentFileRepository(private val context: Context) {
    private val dao = AppDatabase.getDatabase(context).recentFileDao()
    private val settingsRepository = SettingsRepository(context)

    fun getSettingsRepository() = settingsRepository

    fun getRecentFiles(): Flow<List<RecentFile>> = dao.getAll().map { entities ->
        entities.map { it.toRecentFile() }
    }

    suspend fun addRecentFile(
        uri: String,             // 原始 SAF URI
        fileName: String,
        type: String,
    ) {
        val entity = RecentFileEntity(
            id = uri,            // URI 作为唯一 ID
            name = fileName,
            type = type,
            filePath = uri,      // 存 URI 而不是本地路径
            openTime = System.currentTimeMillis(),
        )
        dao.insert(entity)
    }

    private fun RecentFileEntity.toRecentFile(): RecentFile {
        return RecentFile(
            id = id,
            name = name,
            type = type,
            uri = filePath,
            openTimeDisplay = formatTime(openTime),
        )
    }

    private fun formatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> context.getString(R.string.time_just_now)
            diff < 3_600_000 -> context.getString(R.string.time_min_ago, diff / 60_000)
            diff < 86_400_000 -> context.getString(R.string.time_hour_ago, diff / 3_600_000)
            diff < 604_800_000 -> context.getString(R.string.time_day_ago, diff / 86_400_000)
            else -> {
                val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}

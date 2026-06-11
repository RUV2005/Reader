package com.danmo.reader.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "recent_files")
data class RecentFileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,
    val filePath: String,
    val openTime: Long,
)

@Dao
interface RecentFileDao {
    @Query("SELECT * FROM recent_files ORDER BY openTime DESC LIMIT 20")
    fun getAll(): Flow<List<RecentFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: RecentFileEntity)

    @Query("DELETE FROM recent_files WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recent_files")
    suspend fun deleteAll()
}

@Database(entities = [RecentFileEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recentFileDao(): RecentFileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reader_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
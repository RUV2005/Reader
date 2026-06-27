package com.danmo.reader.data.repository

import android.content.Context
import com.danmo.reader.data.local.AppDatabase
import com.danmo.reader.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

class NoteRepository(context: Context) {
    private val noteDao = AppDatabase.getDatabase(context).noteDao()

    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    suspend fun addNote(note: NoteEntity) = noteDao.insertNote(note)

    suspend fun updateNote(note: NoteEntity) = noteDao.updateNote(note)

    suspend fun deleteNote(note: NoteEntity) = noteDao.deleteNote(note)
}

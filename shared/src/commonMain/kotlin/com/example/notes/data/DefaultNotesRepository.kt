package com.example.notes.data

import com.example.notes.data.local.localnotesdatasource.LocalNotesDataSource
import com.example.notes.domain.Note
import com.example.notes.domain.toNote
import com.example.notes.domain.toSavedNoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultNotesRepository(
    private val localNotesDataSource: LocalNotesDataSource
) : NotesRepository {

    override val savedNotesStream: Flow<List<Note>> =
        localNotesDataSource.savedNotesStream.map { savedNoteEntities ->
            savedNoteEntities.map { savedNoteEntity -> savedNoteEntity.toNote() }
        }

    override suspend fun saveNote(note: Note) {
        localNotesDataSource.saveNote(note.toSavedNoteEntity())
    }

    override suspend fun deleteNote(note: Note) {
        localNotesDataSource.markNoteAsDeleted(note.id)
    }

    override suspend fun deleteAllNotesMarkedAsDeleted() {
        localNotesDataSource.deleteAllNotesMarkedAsDeleted()
    }
}
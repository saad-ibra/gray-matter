package com.example.graymatter.notes.data

import com.example.graymatter.notes.data.local.localnotesdatasource.LocalNotesDataSource
import com.example.graymatter.notes.domain.Note
import com.example.graymatter.notes.domain.toNote
import com.example.graymatter.notes.domain.toSavedNoteEntity
import com.example.graymatter.notes.database.SavedNoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultNotesRepository(
    private val localNotesDataSource: LocalNotesDataSource
) : NotesRepository {

    override val savedNotesStream: Flow<List<Note>> =
        localNotesDataSource.savedNotesStream.map { savedNoteEntities: List<SavedNoteEntity> ->
            savedNoteEntities.map { savedNoteEntity: SavedNoteEntity -> savedNoteEntity.toNote() }
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
package com.example.graymatter.notes.data

import com.example.graymatter.notes.domain.Note
import kotlinx.coroutines.flow.Flow

interface NotesRepository {

    /**
     * A [Flow] of all saved notes.
     */
    val savedNotesStream: Flow<List<Note>>

    /**
     * Used to save a [Note].
     */
    suspend fun saveNote(note: Note)

    /**
     * Used to delete a [Note].
     */
    suspend fun deleteNote(note: Note)

    /**
     * Used to delete all saved [Note]s.
     */
    suspend fun deleteAllNotesMarkedAsDeleted()

}
// DEPRECATED
package com.example.graymatter.notes.database
class NotesDatabase
class SavedNoteEntity(
    val id: String,
    val title: String,
    val content: String,
    val createdAtTimestamp: Long,
    val isDeleted: Long
)

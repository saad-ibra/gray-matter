// DEPRECATED
package com.example.graymatter.notes.domain
class SavedNoteEntity
fun SavedNoteEntity.toNote(): Note = Note("")
fun Note.toSavedNoteEntity(): SavedNoteEntity = SavedNoteEntity()

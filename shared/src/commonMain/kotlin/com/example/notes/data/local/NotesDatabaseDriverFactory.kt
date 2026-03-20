package com.example.notes.data.local

import app.cash.sqldelight.db.SqlDriver


object DatabaseDriverConstants {
    const val DATABASE_NAME = "saved_notes.db"
}

expect class NotesDatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

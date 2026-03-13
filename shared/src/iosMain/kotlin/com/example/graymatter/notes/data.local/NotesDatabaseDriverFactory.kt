package com.example.graymatter.notes.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.graymatter.notes.database.NotesDatabase

actual class NotesDatabaseDriverFactory {

    actual fun createDriver(): SqlDriver = NativeSqliteDriver(
        schema = NotesDatabase.Schema,
        name = DatabaseDriverConstants.DATABASE_NAME
    )
}
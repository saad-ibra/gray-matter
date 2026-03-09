package com.example.notes.di

import android.content.Context
import com.example.notes.data.DefaultNotesRepository
import com.example.notes.data.NotesRepository
import com.example.notes.data.local.NotesDatabaseDriverFactory
import com.example.notes.data.local.localnotesdatasource.DefaultLocalNotesDataSource
import com.example.notes.database.NotesDatabase
import kotlinx.coroutines.Dispatchers

actual class AppModule(context: Context) {

    private val database by lazy {
        val driver = NotesDatabaseDriverFactory(context).createDriver()
        NotesDatabase(driver = driver)
    }

    actual fun provideNotesRepository(): NotesRepository {
        val localNotesDataSource = DefaultLocalNotesDataSource(
            database = database,
            ioDispatcher = Dispatchers.IO
        )
        return DefaultNotesRepository(localNotesDataSource = localNotesDataSource)
    }

    actual fun provideDispatchersProvider(): DispatchersProvider = DispatchersProvider()
}
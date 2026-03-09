package com.example.notes.di

import com.example.notes.data.DefaultNotesRepository
import com.example.notes.data.NotesRepository
import com.example.notes.data.local.NotesDatabaseDriverFactory
import com.example.notes.data.local.localnotesdatasource.DefaultLocalNotesDataSource
import com.example.notes.database.NotesDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual class AppModule {

    private val database by lazy {
        val driver = NotesDatabaseDriverFactory().createDriver()
        NotesDatabase(driver)
    }

    actual fun provideNotesRepository(): NotesRepository {
        val localNotesDataSource = DefaultLocalNotesDataSource(
            database = database,
            ioDispatcher = Dispatchers.IO
        )
        return DefaultNotesRepository(localNotesDataSource = localNotesDataSource)
    }

    actual fun provideDispatchersProvider(): DispatchersProvider = DispatchersProvider(
        ioDispatcher = Dispatchers.Main,
        defaultDispatcher = Dispatchers.Main,
        mainDispatcher = Dispatchers.Main
    )
}
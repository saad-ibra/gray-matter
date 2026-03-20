package com.example.notes.di

import com.example.notes.data.NotesRepository

expect class AppModule {
    /**
     * Provides an implementation of [NotesRepository]
     */
    fun provideNotesRepository(): NotesRepository

    /**
     * Used to provide an instance of [DispatchersProvider]
     */
    fun provideDispatchersProvider(): DispatchersProvider

}
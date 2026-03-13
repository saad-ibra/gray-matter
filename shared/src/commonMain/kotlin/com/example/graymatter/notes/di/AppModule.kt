package com.example.graymatter.notes.di

import com.example.graymatter.notes.data.NotesRepository

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
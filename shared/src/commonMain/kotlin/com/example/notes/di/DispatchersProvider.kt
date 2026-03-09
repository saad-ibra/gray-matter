package com.example.notes.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * A class that contains all required [CoroutineDispatcher]'s.
 */
data class DispatchersProvider(
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
)
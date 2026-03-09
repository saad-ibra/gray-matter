package com.example.notes.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notes.data.NotesRepository
import com.example.notes.domain.Note
import com.example.notes.ui.home.HomeViewModel
import kotlinx.coroutines.CoroutineDispatcher

class AndroidHomeViewModel(
    notesRepository: NotesRepository,
    defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val viewModel = HomeViewModel(
        notesRepository = notesRepository,
        coroutineScope = viewModelScope,
        defaultDispatcher = defaultDispatcher
    )

    val uiState = viewModel.uiState
    fun search(searchText: String) = viewModel.search(searchText)
    fun deleteNote(note: Note) = viewModel.deleteNote(note)
    fun restoreRecentlyDeletedNote() = viewModel.restoreRecentlyDeletedNote()
}
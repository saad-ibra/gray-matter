package com.example.graymatter.notes.android.ui.notedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.graymatter.notes.android.ui.navigation.NavigationDestinations
import com.example.graymatter.notes.data.NotesRepository
import com.example.graymatter.notes.ui.notedetail.NoteDetailViewModel

/**
 * The [androidx.lifecycle.ViewModel] equivalent of [NoteDetailViewModel].
 */
class AndroidNoteDetailViewModel(
    savedStateHandle: SavedStateHandle,
    notesRepository: NotesRepository
) : ViewModel() {

    private val viewModel = NoteDetailViewModel(
        currentNoteId = savedStateHandle.get<String>(NavigationDestinations.NoteDetailScreen.NAV_ARG_NOTE_ID),
        notesRepository = notesRepository,
        coroutineScope = viewModelScope
    )

    val titleTextStream = viewModel.titleTextStream
    val contentTextStream = viewModel.contentTextStream

    fun onTitleChange(newTitle: String) {
        viewModel.onTitleChange(newTitle)
    }

    fun onContentChange(newContent: String) {
        viewModel.onContentChange(newContent)
    }
}
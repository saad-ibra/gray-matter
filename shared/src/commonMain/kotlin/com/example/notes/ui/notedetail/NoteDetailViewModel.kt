package com.example.notes.ui.notedetail

import com.example.notes.data.NotesRepository
import com.example.notes.domain.Note
import com.example.notes.utils.UUID
import com.example.notes.utils.asNativeStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(FlowPreview::class)
class NoteDetailViewModel(
    private val currentNoteId: String?,
    private val notesRepository: NotesRepository,
    coroutineScope: CoroutineScope?
) {

    private val viewModelScope =
        coroutineScope ?: CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _titleText = MutableStateFlow("")
    val titleTextStream = _titleText.asNativeStateFlow()

    private val _contentText = MutableStateFlow("")
    val contentTextStream = _contentText.asNativeStateFlow()

    private lateinit var currentNote: Note

    init {
        viewModelScope.launch {
            currentNote = getOrCreateNoteWithId(currentNoteId ?: UUID.randomUUIDString())
            _titleText.update { currentNote.title }
            _contentText.update { currentNote.content }
            val debounceTimeout = 200L
            combine(
                _titleText.debounce(timeoutMillis = debounceTimeout).distinctUntilChanged(),
                _contentText.debounce(timeoutMillis = debounceTimeout).distinctUntilChanged()
            ) { updatedTitleText, updatedContentText ->
                // remove note from database, if note is blank
                if (updatedTitleText.isBlank() && updatedContentText.isBlank()) {
                    notesRepository.deleteNote(currentNote)
                    return@combine
                }
                val updatedNote = currentNote.copy(
                    title = updatedTitleText,
                    content = updatedContentText
                )
                notesRepository.saveNote(updatedNote)
            }.launchIn(this)
        }
    }

    fun onTitleChange(newTitle: String) {
        _titleText.update { newTitle }
    }

    fun onContentChange(newContent: String) {
        _contentText.update { newContent }
    }

    private suspend fun getOrCreateNoteWithId(id: String): Note {
        val savedNotes = notesRepository.savedNotesStream.first()
        val matchingNote = savedNotes.firstOrNull { it.id == id }
        if (matchingNote != null) return matchingNote
        return Note(
            id = id,
            title = "",
            content = "",
            createdAtTimestampMillis = Clock.System.now().toEpochMilliseconds(),
            isDeleted = false
        )
    }

}
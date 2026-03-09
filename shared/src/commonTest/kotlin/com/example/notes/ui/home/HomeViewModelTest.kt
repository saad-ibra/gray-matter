package com.example.notes.ui.home

import com.example.notes.data.NotesRepository
import com.example.notes.domain.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var homeViewModel: HomeViewModel
    private val testScope = TestScope(context = StandardTestDispatcher())
    private val savedNotes = List(10) { index ->
        Note(
            id = index.toString(),
            title = "Note #$index",
            content = "Note #$index",
            isDeleted = false,
            createdAtTimestampMillis = Clock.System.now().epochSeconds * index
        )
    }

    @BeforeTest
    fun setup() {
        val notesRepositoryMock = object : NotesRepository {
            override val savedNotesStream: Flow<List<Note>> = flowOf(savedNotes)

            override suspend fun saveNote(note: Note) {
                TODO("Not yet implemented")
            }

            override suspend fun deleteNote(note: Note) {
                TODO("Not yet implemented")
            }

            override suspend fun deleteAllNotesMarkedAsDeleted() {
                TODO("Not yet implemented")
            }
        }

        homeViewModel = HomeViewModel(
            notesRepository = notesRepositoryMock,
            viewModelScope = testScope,
            defaultDispatcher = StandardTestDispatcher()
        )
    }

    @Test
    fun `searchNotesTest - Notes with titles containing the search text much be fetched successfully`() =
        testScope.runTest {
            homeViewModel.search("Note #1")
            advanceUntilIdle()
            assertTrue {
                val expectedNoteToBeReturned = savedNotes.find { it.title == "Note #1" }
                val expectedNoteListToBeReturned = listOf(expectedNoteToBeReturned)
                homeViewModel.uiState.value.searchResults.containsAll(expectedNoteListToBeReturned)
            }

            homeViewModel.search("Note #2")
            advanceUntilIdle()
            assertTrue {
                val expectedNoteToBeReturned = savedNotes.find { it.title == "Note #2" }
                val expectedNoteListToBeReturned = listOf(expectedNoteToBeReturned)
                homeViewModel.uiState.value.searchResults.containsAll(expectedNoteListToBeReturned)
            }
        }

    @Test
    fun `searchNotesTest - Notes with contents containing the search text much be fetched successfully`() =
        testScope.runTest {
            homeViewModel.search("Note #1")
            advanceUntilIdle()
            assertTrue {
                val expectedNoteToBeReturned = savedNotes.find { it.content == "Note #1" }
                val expectedNoteListToBeReturned = listOf(expectedNoteToBeReturned)
                homeViewModel.uiState.value.searchResults.containsAll(expectedNoteListToBeReturned)
            }

            homeViewModel.search("Note #2")
            advanceUntilIdle()
            assertTrue {
                val expectedNoteToBeReturned = savedNotes.find { it.content == "Note #2" }
                val expectedNoteListToBeReturned = listOf(expectedNoteToBeReturned)
                homeViewModel.uiState.value.searchResults.containsAll(expectedNoteListToBeReturned)
            }
        }

}
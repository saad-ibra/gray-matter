package com.example.notes.data.local.localnotesdatasource

import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.notes.database.NotesDatabase
import com.example.notes.database.SavedNoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import android.content.Context

class DefaultLocalNotesDataSourceTest {

    private lateinit var localNotesDataSource: LocalNotesDataSource
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // https://stackoverflow.com/questions/67718447/can-i-create-an-in-memory-database-with-sqldelight-for-running-in-android
        val androidSqliteDriver = AndroidSqliteDriver(
            schema = NotesDatabase.Schema,
            context = context,
            name = null, // in-memory database
        )
        val notesDatabase = NotesDatabase(androidSqliteDriver)
        localNotesDataSource = DefaultLocalNotesDataSource(
            database = notesDatabase,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun insertNoteTest_ValidNoteEntity_isInsertedSuccessfully() = runTest(testDispatcher) {
        val noteEntity = SavedNoteEntity(
            id = 1,
            title = "title",
            content = "content",
            isDeleted = 0
        )
        assert(localNotesDataSource.savedNotesStream.first().isEmpty())
        localNotesDataSource.saveNote(noteEntity)
        assert(localNotesDataSource.savedNotesStream.first() == listOf(noteEntity))
    }

    @Test
    fun permanentlyDeleteNoteTest_previouslySavedNoteEntity_isPermanentlyDeletedSuccessfully() =
        runTest(testDispatcher) {
            val noteEntity = SavedNoteEntity(
                id = 1,
                title = "title",
                content = "content",
                isDeleted = 0
            ).also { localNotesDataSource.saveNote(it) }

            assert(noteEntity in localNotesDataSource.savedNotesStream.first())
            localNotesDataSource.permanentlyDeleteNoteWithId(noteEntity.id)
            assert(noteEntity !in localNotesDataSource.savedNotesStream.first())
        }

    @Test
    fun markNoteAsDeletedTest_previouslySavedNote_isMarkedAsDeleted() = runTest(testDispatcher) {
        // a note that is initially marked as not deleted
        val noteEntity = SavedNoteEntity(
            id = 1,
            title = "title",
            content = "content",
            isDeleted = 0
        ).also { localNotesDataSource.saveNote(it) }

        localNotesDataSource.savedNotesStream.first().let { list ->
            assert(list.first().isDeleted == 0L)
        }

        localNotesDataSource.markNoteAsDeleted(noteEntity.id)

        localNotesDataSource.savedNotesStream.first().let { list ->
            assert(list.first().isDeleted == 1L)
        }
    }

    @Test
    fun markNoteAsNotDeletedTest_previouslySavedNote_isMarkedAsNotDeleted() =
        runTest(testDispatcher) {
            val noteEntity = SavedNoteEntity(
                id = 1,
                title = "title",
                content = "content",
                isDeleted = 1
            ).also { localNotesDataSource.saveNote(it) }

            localNotesDataSource.savedNotesStream.first().let { list ->
                assert(list.first().isDeleted == 1L)
            }

            localNotesDataSource.markNoteAsNotDeleted(noteEntity.id)

            localNotesDataSource.savedNotesStream.first().let { list ->
                assert(list.first().isDeleted == 0L)
            }
        }

    @Test
    fun deleteAllNotesTest_previouslySavedNotes_getDeletedSuccessfully() =
        runTest(testDispatcher) {
            val previouslySavedNoteEntities = List(10) {
                SavedNoteEntity(
                    id = it.toLong(),
                    title = "title #${it}",
                    content = "content#${it}",
                    isDeleted = 0
                )
            }.onEach { localNotesDataSource.saveNote(it) }

            println(localNotesDataSource.savedNotesStream.first())
            assert(
                localNotesDataSource.savedNotesStream.first()
                    .containsAll(previouslySavedNoteEntities)
            )
            localNotesDataSource.deleteAllNotesMarkedAsDeleted()
            assert(localNotesDataSource.savedNotesStream.first().isEmpty())
        }

    @Test
    fun actionsOnNonExistentNotesTest_performingActionsOnNonExistingNotesMustNotThrowException() =
        runTest(testDispatcher) {
            val nonExistentNoteId = 10L
            with(localNotesDataSource) {
                permanentlyDeleteNoteWithId(nonExistentNoteId)
                markNoteAsDeleted(nonExistentNoteId)
                markNoteAsNotDeleted(nonExistentNoteId)
            }
        }

}
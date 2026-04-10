package com.example.graymatter.data

import com.example.graymatter.domain.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Resource operations.
 */
interface ResourceRepository {
    
    /**
     * A [Flow] of all resources.
     */
    val resourcesStream: Flow<List<Resource>>
    
    /**
     * Get a resource by its ID.
     */
    suspend fun getResourceById(id: String): Resource?
    
    /**
     * Save a resource.
     */
    suspend fun saveResource(resource: Resource)
    
    /**
     * Delete a resource.
     */
    suspend fun deleteResource(id: String)
    
    /**
     * Search resources by query text.
     */
    suspend fun searchResources(query: String): List<Resource>

    /**
     * Update the title/name of a resource.
     */
    suspend fun updateResourceTitle(id: String, title: String)

    /**
     * Update the extracted text content of a resource (for note editing).
     */
    suspend fun updateResourceText(id: String, text: String)

    // -- Reading Progress --

    /**
     * Get reading progress for a resource as a stream.
     */
    fun getReadingProgressStream(resourceId: String): Flow<ReadingProgress?>

    /**
     * Get reading progress for a resource.
     */
    suspend fun getReadingProgress(resourceId: String): ReadingProgress?

    /**
     * Save or update reading progress.
     */
    suspend fun updateReadingProgress(progress: ReadingProgress)

    /**
     * Get the most recently opened document's progress as a stream.
     */
    fun getLastOpenedProgressStream(): Flow<ReadingProgress?>

    /**
     * Get the most recently opened document's progress.
     */
    suspend fun getLastOpenedProgress(): ReadingProgress?

    /**
     * Get all reading progress entries.
     */
    suspend fun getAllReadingProgress(): List<ReadingProgress>

    /**
     * Delete reading progress for a resource.
     */
    suspend fun deleteReadingProgress(resourceId: String)

    // -- Bookmarks --

    /**
     * Get all bookmarks for a resource as a stream.
     */
    fun getBookmarksStream(resourceId: String): Flow<List<Bookmark>>

    /**
     * Get all bookmarks for a resource.
     */
    suspend fun getBookmarks(resourceId: String): List<Bookmark>

    /**
     * Save a bookmark.
     */
    suspend fun saveBookmark(bookmark: Bookmark)

    /**
     * Get a bookmark by its ID.
     */
    suspend fun getBookmarkById(id: String): Bookmark?

    /**
     * Delete a bookmark.
     */
    suspend fun deleteBookmark(id: String)

    /**
     * Delete all bookmarks for a resource.
     */
    suspend fun deleteBookmarksByResourceId(resourceId: String)

    /**
     * Soft delete a bookmark.
     */
    suspend fun softDeleteBookmark(id: String)

    /**
     * Undo soft delete of a bookmark.
     */
    suspend fun undoDeleteBookmark(id: String)

    /**
     * Bulk soft delete bookmarks for a resource.
     */
    suspend fun softDeleteBookmarksByResourceId(resourceId: String, deletedAt: Long)

    /**
     * Bulk undo soft delete bookmarks for a resource by timestamp.
     */
    suspend fun undoDeleteBookmarksByResourceId(resourceId: String, deletedAt: Long)

    /**
     * Get all deleted bookmarks.
     */
    suspend fun getDeletedBookmarks(): List<Bookmark>

    // -- Reading Settings --

    /**
     * Get per-document reading settings.
     */
    suspend fun getReadingSettings(resourceId: String): ReadingSettings?

    /**
     * Save or update per-document reading settings.
     */
    suspend fun updateReadingSettings(settings: ReadingSettings)

    /**
     * Delete per-document reading settings.
     */
    suspend fun deleteReadingSettings(resourceId: String)

    // -- Custom Templates --

    /**
     * Stream of all custom templates.
     */
    val templatesStream: Flow<List<CustomTemplate>>

    /**
     * Save or update a custom template.
     */
    suspend fun saveTemplate(template: CustomTemplate)

    /**
     * Delete a custom template.
     */
    suspend fun deleteTemplate(id: String)

    // -- Data Integrity --

    /**
     * Delete orphan resource rows that have no parent resource entry.
     */
    suspend fun cleanOrphanResources()

    /**
     * Delete orphan reading progress and settings for non-existent resources.
     */
    suspend fun cleanOrphanReadingData()
}

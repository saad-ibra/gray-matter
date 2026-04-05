package com.example.graymatter.data

import com.example.graymatter.domain.ResourceEntry
import com.example.graymatter.domain.ResourceEntryWithDetails
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for ResourceEntry operations.
 */
interface ResourceEntryRepository {
    
    /**
     * A [Flow] of all resource entries.
     */
    val resourceEntriesStream: Flow<List<ResourceEntry>>
    
    /**
     * Get a resource entry by its ID.
     */
    suspend fun getResourceEntryById(id: String): ResourceEntry?
    
    /**
     * Get a resource entry by its resource ID.
     */
    suspend fun getResourceEntryByResourceId(resourceId: String): ResourceEntry?
    
    /**
     * Get full resource entry details including resource, opinions, and topic as a [Flow].
     */
    fun getResourceEntryWithDetailsStream(resourceEntryId: String): Flow<ResourceEntryWithDetails?>
    
    /**
     * Get full resource entry details including resource, opinions, and topic (synchronous).
     */
    suspend fun getResourceEntryWithDetails(resourceEntryId: String): ResourceEntryWithDetails?
    
    /**
     * Get resource entries by topic ID.
     */
    fun getResourceEntriesByTopicId(topicId: String): Flow<List<ResourceEntry>>
    
    /**
     * Create a new resource entry with resource and first opinion atomically.
     */
    suspend fun createResourceEntryWithDetails(
        resourceEntryId: String,
        resourceId: String,
        resourceType: String,
        url: String?,
        filePath: String? = null,
        extractedText: String? = null,
        title: String?,
        description: String? = null,
        opinionId: String,
        opinionText: String,
        confidence: Int,
        now: Long
    )
    
    /**
     * Update resource entry's topic assignment.
     */
    suspend fun updateResourceEntryTopic(resourceEntryId: String, topicId: String?)

    /**
     * Update resource entry's description.
     */
    suspend fun updateResourceEntryDescription(resourceEntryId: String, description: String?)

    /**
     * Update resource entry's opinion metadata (called when new opinion is added).
     */
    suspend fun updateResourceEntryOpinionMetadata(resourceEntryId: String, lastOpinionAt: Long)
    
    /**
     * Delete a resource entry and its associated resource and opinions.
     */
    /**
     * Delete a resource entry completely.
     */
    suspend fun deleteResourceEntry(id: String)
    
    /**
     * Soft delete a resource entry.
     */
    suspend fun softDeleteResourceEntry(id: String)

    /**
     * Undo soft delete of a resource entry.
     */
    suspend fun undoDeleteResourceEntry(id: String)

    /**
     * Get all deleted resource entries.
     */
    suspend fun getDeletedResourceEntries(): List<ResourceEntry>

    /**
     * Bulk soft delete resource entries by topic ID.
     */
    suspend fun softDeleteResourceEntriesByTopicId(topicId: String, deletedAt: Long)

    /**
     * Bulk undo soft delete resource entries by topic ID and timestamp.
     */
    suspend fun undoDeleteResourceEntriesByTopicId(topicId: String, deletedAt: Long)
}

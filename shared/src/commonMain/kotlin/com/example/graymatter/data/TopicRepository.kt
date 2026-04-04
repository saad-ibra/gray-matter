package com.example.graymatter.data

import com.example.graymatter.domain.Topic
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Topic operations.
 */
interface TopicRepository {
    
    /**
     * A [Flow] of all topics.
     */
    val topicsStream: Flow<List<Topic>>
    
    /**
     * Get a topic by its ID.
     */
    suspend fun getTopicById(id: String): Topic?
    
    /**
     * Get all topics.
     */
    suspend fun getAllTopics(): List<Topic>
    
    /**
     * Save a new topic.
     */
    suspend fun saveTopic(topic: Topic)
    
    /**
     * Update topic notes.
     */
    suspend fun updateTopicNotes(topicId: String, notes: String?)
    
    /**
     * Increment topic resource count.
     */
    suspend fun incrementResourceCount(topicId: String)
    
    /**
     * Decrement topic resource count.
     */
    suspend fun decrementResourceCount(topicId: String)
    
    /**
     * Delete a topic.
     */
    /**
     * Delete topic completely
     */
    suspend fun deleteTopic(id: String)
    
    /**
     * Soft delete a topic
     */
    suspend fun softDeleteTopic(id: String)

    /**
     * Undo soft delete of a topic
     */
    suspend fun undoDeleteTopic(id: String)

    /**
     * Get all deleted topics
     */
    suspend fun getDeletedTopics(): List<Topic>
    
    /**
     * Search topics by query text.
     */
    suspend fun searchTopics(query: String): List<Topic>

    /**
     * Rename a topic.
     */
    suspend fun renameTopic(id: String, newName: String)

    /**
     * Update the order of topics.
     */
    suspend fun updateTopicOrder(topicIds: List<String>)
}

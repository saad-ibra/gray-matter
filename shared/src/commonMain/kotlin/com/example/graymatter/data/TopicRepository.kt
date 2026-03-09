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
    suspend fun deleteTopic(id: String)
    
    /**
     * Search topics by query text.
     */
    suspend fun searchTopics(query: String): List<Topic>
}

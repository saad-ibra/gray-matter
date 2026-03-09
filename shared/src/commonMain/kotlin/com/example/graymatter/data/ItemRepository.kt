package com.example.graymatter.data

import com.example.graymatter.domain.Item
import com.example.graymatter.domain.ItemWithDetails
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Item operations.
 */
interface ItemRepository {
    
    /**
     * A [Flow] of all items.
     */
    val itemsStream: Flow<List<Item>>
    
    /**
     * Get an item by its ID.
     */
    suspend fun getItemById(id: String): Item?
    
    /**
     * Get an item by its resource ID.
     */
    suspend fun getItemByResourceId(resourceId: String): Item?
    
    /**
     * Get full item details including resource, opinions, and topic as a [Flow].
     */
    fun getItemWithDetailsStream(itemId: String): Flow<ItemWithDetails?>
    
    /**
     * Get full item details including resource, opinions, and topic (synchronous).
     */
    suspend fun getItemWithDetails(itemId: String): ItemWithDetails?
    
    /**
     * Get items by topic ID.
     */
    fun getItemsByTopicId(topicId: String): Flow<List<Item>>
    
    /**
     * Create a new item with resource and first opinion atomically.
     */
    suspend fun createItemWithDetails(
        itemId: String,
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
     * Update item's topic assignment.
     */
    suspend fun updateItemTopic(itemId: String, topicId: String?)

    /**
     * Update item's description.
     */
    suspend fun updateItemDescription(itemId: String, description: String?)

    /**
     * Update item's opinion metadata (called when new opinion is added).
     */
    suspend fun updateItemOpinionMetadata(itemId: String, lastOpinionAt: Long)
    
    /**
     * Delete an item and its associated resource and opinions.
     */
    suspend fun deleteItem(id: String)
}

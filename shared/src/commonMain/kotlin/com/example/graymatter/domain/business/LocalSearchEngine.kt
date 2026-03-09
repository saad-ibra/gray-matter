package com.example.graymatter.domain.business

import com.example.graymatter.data.ItemRepository
import com.example.graymatter.data.OpinionRepository
import com.example.graymatter.data.ResourceRepository
import com.example.graymatter.data.TopicRepository
import com.example.graymatter.domain.ItemWithDetails
import kotlinx.coroutines.flow.first

/**
 * Offline-first local search engine.
 * indexes and searches across Resources, Opinions, and Topics.
 */
class LocalSearchEngine(
    private val itemRepository: ItemRepository,
    private val resourceRepository: ResourceRepository,
    private val opinionRepository: OpinionRepository,
    private val topicRepository: TopicRepository
) {

    /**
     * Performs a full-text search across all entities.
     * Groups results by Item.
     */
    suspend fun search(query: String): List<ItemWithDetails> {
        if (query.isBlank()) return emptyList()
        
        println("Starting search for: $query")
        
        val matchingResources = resourceRepository.searchResources(query)
        
        // 2. Search Opinions
        val matchingOpinions = opinionRepository.searchOpinions(query)
        val itemIdsFromOpinions = matchingOpinions.map { it.itemId }.toSet()
        
        // Combine all Item IDs
        val allItemIds = mutableSetOf<String>()
        
        // Get items for matching resources
        matchingResources.forEach { resource ->
            val item = itemRepository.getItemByResourceId(resource.id)
            if (item != null) allItemIds.add(item.id)
        }
        
        allItemIds.addAll(itemIdsFromOpinions)
        
        // Fetch full details for all unique items
        val results = allItemIds.mapNotNull { itemId ->
            itemRepository.getItemWithDetails(itemId)
        }
        
        // Sort by relevance (simple heuristic: recent activity first, or match quality)
        // Here we just sort by last activity
        return results.sortedByDescending { it.item.lastOpinionAt }
    }
}

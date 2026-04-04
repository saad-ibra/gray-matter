package com.example.graymatter.domain.business

import com.example.graymatter.data.ResourceEntryRepository
import com.example.graymatter.data.OpinionRepository
import com.example.graymatter.data.ResourceRepository
import com.example.graymatter.data.TopicRepository
import com.example.graymatter.domain.ResourceEntryWithDetails
import kotlinx.coroutines.flow.first

/**
 * Offline-first local search engine.
 * indexes and searches across Resources, Opinions, and Topics.
 */
class LocalSearchEngine(
    private val resourceEntryRepository: ResourceEntryRepository,
    private val resourceRepository: ResourceRepository,
    private val opinionRepository: OpinionRepository,
    private val topicRepository: TopicRepository
) {

    /**
     * Performs a full-text search across all entities.
     * Groups results by ResourceEntry.
     */
    suspend fun search(query: String): List<ResourceEntryWithDetails> {
        if (query.isBlank()) return emptyList()
        
        println("Starting search for: $query")
        
        val matchingResources = resourceRepository.searchResources(query)
        
        // 2. Search Opinions
        val matchingOpinions = opinionRepository.searchOpinions(query)
        val resourceEntryIdsFromOpinions = matchingOpinions.map { it.itemId }.toSet()
        
        // Combine all ResourceEntry IDs
        val allResourceEntryIds = mutableSetOf<String>()
        
        // Get resource entries for matching resources
        matchingResources.forEach { resource ->
            val resourceEntry = resourceEntryRepository.getResourceEntryByResourceId(resource.id)
            if (resourceEntry != null) allResourceEntryIds.add(resourceEntry.id)
        }
        
        allResourceEntryIds.addAll(resourceEntryIdsFromOpinions)
        
        // Fetch full details for all unique resource entries
        val results = allResourceEntryIds.mapNotNull { resourceEntryId ->
            resourceEntryRepository.getResourceEntryWithDetails(resourceEntryId)
        }
        
        // Sort by relevance (simple heuristic: recent activity first, or match quality)
        // Here we just sort by last activity
        return results.sortedByDescending { it.resourceEntry.lastOpinionAt }
    }
}

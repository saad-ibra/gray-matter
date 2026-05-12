package com.example.graymatter.android.ui.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.graymatter.data.OpinionRepository
import com.example.graymatter.data.ResourceEntryRepository
import com.example.graymatter.data.ResourceRepository
import com.example.graymatter.data.TopicRepository
import com.example.graymatter.domain.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Categories for global search results.
 */
sealed class GlobalSearchResult {
    abstract val matchSnippet: String
    abstract val relevanceScore: Int

    data class TopicResult(
        val topic: Topic,
        override val matchSnippet: String,
        override val relevanceScore: Int = 100
    ) : GlobalSearchResult()

    data class ResourceResult(
        val resource: Resource,
        val resourceEntryId: String,
        val topicName: String? = null,
        override val matchSnippet: String,
        val matchType: String, // "title" or "content"
        override val relevanceScore: Int = 80
    ) : GlobalSearchResult()

    data class OpinionResult(
        val opinion: Opinion,
        val resourceEntryId: String,
        val parentResourceTitle: String?,
        override val matchSnippet: String,
        override val relevanceScore: Int = 60
    ) : GlobalSearchResult()
}

enum class SearchFilter {
    ALL, TOPICS, RESOURCES, KNOWLEDGE
}

/**
 * ViewModel for the library global search overlay.
 * Performs debounced cross-content search across topics, resources, and opinions.
 */
class LibrarySearchViewModel(
    private val topicRepository: TopicRepository,
    private val resourceRepository: ResourceRepository,
    private val resourceEntryRepository: ResourceEntryRepository,
    private val opinionRepository: OpinionRepository
) : ViewModel() {

    var searchQuery by mutableStateOf("")
        private set

    var results by mutableStateOf<List<GlobalSearchResult>>(emptyList())
        private set

    var isSearching by mutableStateOf(false)
        private set

    var activeFilter by mutableStateOf(SearchFilter.ALL)
        private set

    var filteredResults by mutableStateOf<List<GlobalSearchResult>>(emptyList())
        private set

    private var searchJob: Job? = null

    fun updateQuery(query: String) {
        searchQuery = query
        searchJob?.cancel()

        if (query.length < 2) {
            results = emptyList()
            filteredResults = emptyList()
            isSearching = false
            return
        }

        isSearching = true
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            performSearch(query)
        }
    }

    fun setFilter(filter: SearchFilter) {
        activeFilter = filter
        applyFilter()
    }

    fun clearSearch() {
        searchQuery = ""
        results = emptyList()
        filteredResults = emptyList()
        isSearching = false
        searchJob?.cancel()
    }

    private suspend fun performSearch(query: String) {
        try {
            val allResults = mutableListOf<GlobalSearchResult>()

            // 1. Search Topics
            val topics = topicRepository.topicsStream.first()
            topics.filter { 
                it.name.contains(query, ignoreCase = true) ||
                it.notes?.contains(query, ignoreCase = true) == true
            }.take(20).forEach { topic ->
                val snippet = if (topic.name.contains(query, ignoreCase = true)) {
                    topic.name
                } else {
                    extractSnippet(topic.notes ?: "", query)
                }
                val score = if (topic.name.equals(query, ignoreCase = true)) 200
                    else if (topic.name.contains(query, ignoreCase = true)) 150
                    else 100
                allResults.add(GlobalSearchResult.TopicResult(topic, snippet, score))
            }

            // 2. Search Resources (title + content)
            val matchedResources = resourceRepository.searchResources(query)
            val entries = resourceEntryRepository.resourceEntriesStream.first()

            matchedResources.take(30).forEach { resource ->
                val entry = entries.find { it.resourceId == resource.id }
                if (entry != null && !entry.isDeleted) {
                    val topicName = entry.topicId?.let { tid ->
                        topics.find { it.id == tid }?.name
                    }
                    val isTitle = resource.title?.contains(query, ignoreCase = true) == true
                    val snippet = if (isTitle) {
                        resource.title ?: ""
                    } else {
                        extractSnippet(resource.extractedText ?: "", query)
                    }
                    val score = when {
                        resource.title?.equals(query, ignoreCase = true) == true -> 180
                        isTitle -> 120
                        else -> 70
                    }
                    allResults.add(
                        GlobalSearchResult.ResourceResult(
                            resource = resource,
                            resourceEntryId = entry.id,
                            topicName = topicName,
                            matchSnippet = snippet,
                            matchType = if (isTitle) "title" else "content",
                            relevanceScore = score
                        )
                    )
                }
            }

            // 3. Search Opinions (knowledge history)
            val matchedOpinions = opinionRepository.searchOpinions(query)
            matchedOpinions.filter { !it.isDeleted }.take(30).forEach { opinion ->
                val entry = entries.find { it.id == opinion.itemId }
                    ?: entries.find { it.resourceId == opinion.itemId }
                if (entry != null && !entry.isDeleted) {
                    val resource = resourceRepository.getResourceById(entry.resourceId)
                    val snippet = extractSnippet(opinion.text, query)
                    // Strip internal markers for cleaner snippets
                    val cleanSnippet = snippet
                        .replace(Regex("\\[INDEX:\\d+\\]\\s*"), "")
                        .replace(Regex("\\[DICT(:\\d+)?\\]\\s*"), "")
                        .replace(Regex("\\[TEMPLATE:[^\\]]*\\]"), "")
                        .replace(Regex("\\[CUSTOM: [^\\]]*\\]"), "")
                        .removePrefix("> ")
                        .trim()
                    allResults.add(
                        GlobalSearchResult.OpinionResult(
                            opinion = opinion,
                            resourceEntryId = entry.id,
                            parentResourceTitle = resource?.title,
                            matchSnippet = cleanSnippet,
                            relevanceScore = 60
                        )
                    )
                }
            }

            // Sort by relevance
            results = allResults.sortedByDescending { it.relevanceScore }
            applyFilter()
        } catch (e: Exception) {
            results = emptyList()
            filteredResults = emptyList()
        } finally {
            isSearching = false
        }
    }

    private fun applyFilter() {
        filteredResults = when (activeFilter) {
            SearchFilter.ALL -> results
            SearchFilter.TOPICS -> results.filterIsInstance<GlobalSearchResult.TopicResult>()
            SearchFilter.RESOURCES -> results.filterIsInstance<GlobalSearchResult.ResourceResult>()
            SearchFilter.KNOWLEDGE -> results.filterIsInstance<GlobalSearchResult.OpinionResult>()
        }
    }

    /**
     * Extracts a context snippet around the matched query within the full text.
     */
    private fun extractSnippet(text: String, query: String, contextChars: Int = 60): String {
        val idx = text.indexOf(query, ignoreCase = true)
        if (idx == -1) return text.take(120)

        val start = (idx - contextChars).coerceAtLeast(0)
        val end = (idx + query.length + contextChars).coerceAtMost(text.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < text.length) "…" else ""
        return "$prefix${text.substring(start, end).trim()}$suffix"
    }
}

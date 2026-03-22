package com.example.graymatter.viewmodel

import com.example.graymatter.data.OpinionRepository
import com.example.graymatter.data.ResourceRepository
import com.example.graymatter.data.TopicRepository
import com.example.graymatter.domain.Opinion
import com.example.graymatter.domain.ReferenceSelectorItem
import com.example.graymatter.domain.Resource
import com.example.graymatter.domain.Topic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReferenceSelectorUiState(
    val items: List<ReferenceSelectorItem> = emptyList(),
    val selectedItems: List<ReferenceSelectorItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class ReferenceSelectorViewModel(
    private val topicRepository: TopicRepository,
    private val resourceRepository: ResourceRepository,
    private val opinionRepository: OpinionRepository,
    coroutineScope: CoroutineScope?,
    private val defaultDispatcher: CoroutineDispatcher
) {
    private val viewModelScope = coroutineScope ?: MainScope()

    private val _uiState = MutableStateFlow(ReferenceSelectorUiState())
    val uiState: StateFlow<ReferenceSelectorUiState> = _uiState.asStateFlow()

    private var allTopics = emptyList<Topic>()
    private var allResources = emptyList<Resource>()
    
    // Map of expanded parent IDs to their children (Opinions/Details)
    private val expandedAnnotations = mutableMapOf<String, List<Opinion>>()
    private val expandedStateMap = mutableMapOf<String, Boolean>()
    private val checkedStateMap = mutableMapOf<String, Boolean>()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch(defaultDispatcher) {
            combine(
                topicRepository.topicsStream,
                resourceRepository.resourcesStream
            ) { topics, resources ->
                allTopics = topics
                allResources = resources
                rebuildItems()
            }.launchIn(this)
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        rebuildItems()
    }

    fun toggleExpand(item: ReferenceSelectorItem) {
        val isExpanded = !(expandedStateMap[item.id] ?: false)
        expandedStateMap[item.id] = isExpanded

        if (isExpanded && item is ReferenceSelectorItem.ResourceItem && !expandedAnnotations.containsKey(item.id)) {
            // Lazily load annotations for this resource
            viewModelScope.launch(defaultDispatcher) {
                // Assuming items mapping: Opinions relate to Items, which relate to Resources
                // Note: The UI requested annotations/bookmarks lazily on expand.
                // We'll search opinions by itemId. However, we need to map resource to item.
                // For simplicity in this demo, if the OpinionRepository requires an Item, we'd need ItemRepository.
                // This is a placeholder since the exact Opinion <-> Resource linkage in UI is via Item.
                // Let's just rebuild items for now.
                rebuildItems()
            }
        } else {
            rebuildItems()
        }
    }

    fun toggleCheck(item: ReferenceSelectorItem) {
        val isChecked = !(checkedStateMap[item.id] ?: false)
        checkedStateMap[item.id] = isChecked
        
        val currentlySelected = _uiState.value.selectedItems.toMutableList()
        if (isChecked) {
            currentlySelected.add(item.copyItem(isChecked = true))
        } else {
            currentlySelected.removeAll { it.id == item.id }
        }
        _uiState.update { it.copy(selectedItems = currentlySelected) }
        rebuildItems()
    }
    
    fun removeSelected(item: ReferenceSelectorItem) {
        checkedStateMap[item.id] = false
        val currentlySelected = _uiState.value.selectedItems.filter { it.id != item.id }
        _uiState.update { it.copy(selectedItems = currentlySelected) }
        rebuildItems()
    }

    private fun ReferenceSelectorItem.copyItem(isChecked: Boolean? = null, isExpanded: Boolean? = null): ReferenceSelectorItem {
        val c = isChecked ?: this.isChecked
        val e = isExpanded ?: this.isExpanded
        return when (this) {
            is ReferenceSelectorItem.TopicItem -> this.copy(isChecked = c, isExpanded = e)
            is ReferenceSelectorItem.ResourceItem -> this.copy(isChecked = c, isExpanded = e)
            is ReferenceSelectorItem.DetailItem -> this.copy(isChecked = c, isExpanded = e)
        }
    }

    private fun rebuildItems() {
        var query = _uiState.value.searchQuery.lowercase()
        val result = mutableListOf<ReferenceSelectorItem>()
        
        val filteredTopics = if (query.isEmpty()) allTopics else allTopics.filter { it.name.lowercase().contains(query) }
        val filteredResources = if (query.isEmpty()) allResources else allResources.filter { it.title?.lowercase()?.contains(query) == true }

        // Build flat hierarchy
        // 1. Topics
        for (topic in filteredTopics) {
            val isExpanded = expandedStateMap[topic.id] ?: false
            val isChecked = checkedStateMap[topic.id] ?: false
            result.add(ReferenceSelectorItem.TopicItem(topic.id, topic.name, isExpanded, isChecked))
            
            if (isExpanded) {
                // Find resources for this topic (assuming we can filter them)
                // Note: ItemEntity has topicId and resourceId. We would need ItemRepository.
                // For layout purposes, we render matching resources here.
            }
        }
        
        // 2. Uncategorized Resources
        for (res in filteredResources) {
            val isExpanded = expandedStateMap[res.id] ?: false
            val isChecked = checkedStateMap[res.id] ?: false
            result.add(ReferenceSelectorItem.ResourceItem(res.id, res.title ?: "Untitled", res.type.name, null, isExpanded, isChecked))
            
            if (isExpanded) {
                // If we fetched opinions, add them here
                val opinions = expandedAnnotations[res.id] ?: emptyList()
                for (op in opinions) {
                    val opChecked = checkedStateMap[op.id] ?: false
                    result.add(ReferenceSelectorItem.DetailItem(op.id, op.text, res.id, true, false, opChecked))
                }
            }
        }
        
        _uiState.update { it.copy(items = result, isLoading = false) }
    }
}

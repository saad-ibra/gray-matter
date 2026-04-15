package com.example.graymatter.viewmodel

import com.example.graymatter.data.OpinionRepository
import com.example.graymatter.data.ResourceRepository
import com.example.graymatter.data.TopicRepository
import com.example.graymatter.domain.Opinion
import com.example.graymatter.domain.ReferenceSelectorItem
import com.example.graymatter.domain.Resource
import com.example.graymatter.domain.ResourceEntry
import com.example.graymatter.domain.Topic
import com.example.graymatter.data.ResourceEntryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ReferenceTab { ALL, TOPICS, RESOURCES, OPINIONS }

data class ReferenceSelectorUiState(
    val items: List<ReferenceSelectorItem> = emptyList(),
    val selectedItems: List<ReferenceSelectorItem> = emptyList(),
    val searchQuery: String = "",
    val activeTab: ReferenceTab = ReferenceTab.ALL,
    val isLoading: Boolean = true
)

class ReferenceSelectorViewModel(
    private val topicRepository: TopicRepository,
    private val resourceRepository: ResourceRepository,
    private val opinionRepository: OpinionRepository,
    private val resourceEntryRepository: ResourceEntryRepository,
    coroutineScope: CoroutineScope?,
    private val defaultDispatcher: CoroutineDispatcher
) {
    private val viewModelScope = coroutineScope ?: MainScope()

    private val _uiState = MutableStateFlow(ReferenceSelectorUiState())
    val uiState: StateFlow<ReferenceSelectorUiState> = _uiState.asStateFlow()

    private var allTopics = emptyList<Topic>()
    private var allResources = emptyList<Resource>()
    private var allResourceEntries = emptyList<ResourceEntry>()
    private var allOpinions = emptyList<Opinion>()
    
    // Map of parent IDs to their state
    private val expandedStateMap = mutableMapOf<String, Boolean>()
    private val checkedStateMap = mutableMapOf<String, Boolean>()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch(defaultDispatcher) {
            combine(
                topicRepository.topicsStream,
                resourceRepository.resourcesStream,
                resourceEntryRepository.resourceEntriesStream,
                opinionRepository.getAllOpinions()
            ) { topics, resources, resourceEntries, opinions ->
                allTopics = topics
                allResources = resources
                allResourceEntries = resourceEntries
                allOpinions = opinions
                rebuildItems()
            }.launchIn(this)
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        rebuildItems()
    }

    fun setTab(tab: ReferenceTab) {
        _uiState.update { it.copy(activeTab = tab) }
        rebuildItems()
    }

    fun toggleExpand(item: ReferenceSelectorItem) {
        val isExpanded = !(expandedStateMap[item.id] ?: false)
        expandedStateMap[item.id] = isExpanded
        rebuildItems()
    }

    fun toggleCheck(item: ReferenceSelectorItem) {
        val wasChecked = checkedStateMap[item.id] ?: false
        checkedStateMap.clear()
        
        if (!wasChecked) {
            checkedStateMap[item.id] = true
            _uiState.update { it.copy(selectedItems = listOf(item.copyItem(isChecked = true))) }
        } else {
            _uiState.update { it.copy(selectedItems = emptyList()) }
        }
        rebuildItems()
    }
    
    fun removeSelected(item: ReferenceSelectorItem) {
        checkedStateMap[item.id] = false
        val currentlySelected = _uiState.value.selectedItems.filter { it.id != item.id }
        _uiState.update { it.copy(selectedItems = currentlySelected) }
        rebuildItems()
    }

    fun clearSelection() {
        checkedStateMap.clear()
        _uiState.update { it.copy(selectedItems = emptyList()) }
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

    /**
     * Computes the set of valid resource entry IDs — entries whose parent topic still exists
     * (or have no topic). This filters out orphaned entries left behind after topic deletion.
     */
    private fun validResourceEntryIds(): Set<String> {
        val topicIds = allTopics.map { it.id }.toSet()
        return allResourceEntries.filter { entry ->
            entry.topicId == null || entry.topicId in topicIds
        }.map { it.id }.toSet()
    }

    /**
     * Returns only opinions that belong to valid (non-orphaned) resource entries.
     */
    private fun validOpinions(): List<Opinion> {
        val validIds = validResourceEntryIds()
        return allOpinions.filter { it.itemId in validIds }
    }

    /**
     * Returns only resources that belong to valid (non-orphaned) resource entries.
     */
    private fun validResources(): List<Resource> {
        val validIds = validResourceEntryIds()
        val validResourceIds = allResourceEntries.filter { it.id in validIds }.map { it.resourceId }.toSet()
        return allResources.filter { it.id in validResourceIds }
    }

    private fun rebuildItems() {
        val query = _uiState.value.searchQuery.lowercase()
        val activeTab = _uiState.value.activeTab
        val result = mutableListOf<ReferenceSelectorItem>()

        if (query.isNotEmpty()) {
            buildSearchResults(query, activeTab, result)
        } else {
            buildTreeStructure(activeTab, result)
        }
        
        _uiState.update { it.copy(items = result, isLoading = false) }
    }

    private fun buildSearchResults(query: String, tab: ReferenceTab, result: MutableList<ReferenceSelectorItem>) {
        // 1. Match Topics
        if (tab == ReferenceTab.ALL || tab == ReferenceTab.TOPICS) {
            allTopics.filter { it.name.lowercase().contains(query) }.forEach { topic ->
                result.add(ReferenceSelectorItem.TopicItem(topic.id, topic.name, false, checkedStateMap[topic.id] ?: false))
            }
        }

        // 2. Match Resources (only valid, non-orphaned)
        if (tab == ReferenceTab.ALL || tab == ReferenceTab.RESOURCES) {
            validResources().filter { it.title?.lowercase()?.contains(query) == true }.forEach { res ->
                val topicId = allResourceEntries.find { it.resourceId == res.id }?.topicId
                val context = topicId?.let { tid -> allTopics.find { it.id == tid }?.name }
                result.add(ReferenceSelectorItem.ResourceItem(
                    id = res.id,
                    title = res.title ?: "Untitled",
                    type = res.type.name,
                    parentTopicId = topicId,
                    isExpanded = false,
                    isChecked = checkedStateMap[res.id] ?: false,
                    indentLevel = 0,
                    parentContext = context
                ))
            }
        }

        // 3. Match Opinions (only valid, non-orphaned)
        if (tab == ReferenceTab.ALL || tab == ReferenceTab.OPINIONS) {
            validOpinions().filter { it.text.lowercase().contains(query) }.forEach { op ->
                val resourceEntry = allResourceEntries.find { it.id == op.itemId }
                val resource = resourceEntry?.let { entry -> allResources.find { it.id == entry.resourceId } }
                val context = resource?.title ?: "Unknown Resource"
                val typeLabel = when {
                    op.text.startsWith("[DICT") -> "LOOKUP"
                    op.pageNumber != null && op.text.startsWith("> ") -> "Annotation"
                    op.text.startsWith("[TEMPLATE:") -> "Template"
                    op.text.startsWith("[CUSTOM: ") -> "Custom"
                    op.pageNumber != null -> "Bookmark"
                    else -> "Opinion"
                }
                result.add(ReferenceSelectorItem.DetailItem(
                    id = op.id,
                    snippet = op.text,
                    resourceId = resource?.id ?: "",
                    typeLabel = typeLabel,
                    isExpanded = false,
                    isChecked = checkedStateMap[op.id] ?: false,
                    indentLevel = 0,
                    parentContext = context
                ))
            }
        }
    }

    private fun buildTreeStructure(tab: ReferenceTab, result: MutableList<ReferenceSelectorItem>) {
        when (tab) {
            ReferenceTab.ALL, ReferenceTab.TOPICS -> {
                for (topic in allTopics) {
                    val isExpanded = expandedStateMap[topic.id] ?: false
                    val isChecked = checkedStateMap[topic.id] ?: false
                    result.add(ReferenceSelectorItem.TopicItem(topic.id, topic.name, isExpanded, isChecked))
                    
                    if (isExpanded) {
                        val topicResourceIds = allResourceEntries.filter { it.topicId == topic.id }.map { it.resourceId }.toSet()
                        val topicResources = allResources.filter { it.id in topicResourceIds }
                        for (res in topicResources) {
                            addResourceWithDetails(res, 1, result)
                        }
                    }
                }
                
                if (tab == ReferenceTab.ALL) {
                    // Show uncategorized resources at the end
                    val categorizedResourceIds = allResourceEntries.filter { it.topicId != null }.map { it.resourceId }.toSet()
                    allResources.filter { it.id !in categorizedResourceIds }.forEach { res ->
                        addResourceWithDetails(res, 0, result)
                    }
                }
            }
            ReferenceTab.RESOURCES -> {
                validResources().forEach { res -> addResourceWithDetails(res, 0, result) }
            }
            ReferenceTab.OPINIONS -> {
                validOpinions().forEach { op ->
                    val resourceEntry = allResourceEntries.find { it.id == op.itemId }
                    val resource = resourceEntry?.let { entry -> allResources.find { it.id == entry.resourceId } }
                    val typeLabel = when {
                        op.text.startsWith("[DICT") -> "LOOKUP"
                        op.pageNumber != null && op.text.startsWith("> ") -> "Annotation"
                        op.text.startsWith("[TEMPLATE:") -> "Template"
                        op.text.startsWith("[CUSTOM: ") -> "Custom"
                        op.pageNumber != null -> "Bookmark"
                        else -> "Opinion"
                    }
                    result.add(ReferenceSelectorItem.DetailItem(
                        id = op.id,
                        snippet = stripMarkdown(op.text),
                        resourceId = resource?.id ?: "",
                        typeLabel = typeLabel,
                        isExpanded = false,
                        isChecked = checkedStateMap[op.id] ?: false,
                        indentLevel = 0,
                        parentContext = resource?.title
                    ))
                }
            }
        }
    }

    private fun addResourceWithDetails(res: Resource, indent: Int, result: MutableList<ReferenceSelectorItem>) {
        val isExpanded = expandedStateMap[res.id] ?: false
        val isChecked = checkedStateMap[res.id] ?: false
        result.add(ReferenceSelectorItem.ResourceItem(res.id, res.title ?: "Untitled", res.type.name, null, isExpanded, isChecked, indent))
        
        if (isExpanded) {
            val dbEntry = allResourceEntries.find { it.resourceId == res.id }
            val opinions = dbEntry?.let { entry -> allOpinions.filter { it.itemId == entry.id } } ?: emptyList()
            for (op in opinions) {
                val opChecked = checkedStateMap[op.id] ?: false
                val typeLabel = when {
                    op.text.startsWith("[DICT") -> "LOOKUP"
                    op.pageNumber != null && op.text.startsWith("> ") -> "Annotation"
                    op.text.startsWith("[TEMPLATE:") -> "Template"
                    op.text.startsWith("[CUSTOM: ") -> "Custom"
                    op.pageNumber != null -> "Bookmark"
                    else -> "Opinion"
                }
                result.add(ReferenceSelectorItem.DetailItem(op.id, stripMarkdown(op.text), res.id, typeLabel, false, opChecked, indent + 1))
            }
        }
    }

    private fun stripMarkdown(text: String): String {
        return text
            .replace(Regex("\\[TEMPLATE:[^\\]]*\\]"), "")
            .replace(Regex("\\[DICT\\]"), "")
            .replace(Regex("\\[CUSTOM: [^\\]]*\\]"), "")
            .replace(Regex("\\[Page \\d+\\]"), "")
            .replace(Regex("[#*>\\[\\]]"), "")
            .trim()
            .replace(Regex("\\s+"), " ") 
    }
}


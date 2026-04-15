package com.example.graymatter.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.graymatter.data.ResourceEntryRepository
import com.example.graymatter.data.ResourceRepository
import com.example.graymatter.domain.ReadingProgress
import com.example.graymatter.domain.ResourceEntry
import com.example.graymatter.domain.ResourceEntryWithDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val resourceEntryRepository: ResourceEntryRepository,
    private val resourceRepository: ResourceRepository
) : ViewModel() {

    // --- Recent Entries ---
    
    val resourceEntriesStream: StateFlow<List<ResourceEntry>> = resourceEntryRepository.resourceEntriesStream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Stream of all resource entries with details, sorted by most recent interaction
     * (either last opened or first opinion timestamp).
     */
    val allRecentResourceEntryDetails: StateFlow<List<ResourceEntryWithDetails>> = combine(
        resourceEntriesStream,
        resourceRepository.getAllReadingProgressStream()
    ) { entries, progressList ->
        if (entries.isEmpty()) return@combine emptyList()

        val progressMap = progressList.associateBy { it.resourceId }

        val entriesWithInteractionTime = entries.map { entry ->
            val progress = progressMap[entry.resourceId]
            // Use lastOpenedAt if available, otherwise fallback to firstOpinionAt
            val interactionTime = maxOf(progress?.lastOpenedAt ?: 0L, entry.firstOpinionAt)
            entry to interactionTime
        }

        val sortedEntries = entriesWithInteractionTime
            .sortedByDescending { it.second }
            .map { it.first }

        // Fetch details for all sorted entries
        // Note: In a large DB, we might want to paginate this, but for now we'll fetch all
        // to support the "Recent Activity" screen.
        combine(sortedEntries.map { resourceEntryRepository.getResourceEntryWithDetailsStream(it.id) }) { detailsArray ->
            detailsArray.filterNotNull()
        }.first()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Stream of the 4 most recent resource entries.
     */
    val recentResourceEntryDetails: StateFlow<List<ResourceEntryWithDetails>> = allRecentResourceEntryDetails
        .map { it.take(4) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- Orphans (Data Integrity) ---

    private val _orphanResourceEntries = MutableStateFlow<List<ResourceEntry>>(emptyList())
    val orphanResourceEntries: StateFlow<List<ResourceEntry>> = _orphanResourceEntries.asStateFlow()

    init {
        checkOrphanResourceEntries()
    }

    /**
     * Checks for resource entries that have no topic assigned (orphans).
     */
    private fun checkOrphanResourceEntries() {
        viewModelScope.launch {
            _orphanResourceEntries.value = resourceEntryRepository.getResourceEntriesWithoutTopic()
        }
    }

    /**
     * Called after assigning a topic to a resource entry (previously orphan).
     */
    fun assignTopicToResourceEntry(resourceEntryId: String, topicId: String) {
        viewModelScope.launch {
            resourceEntryRepository.updateResourceEntryTopic(resourceEntryId, topicId)
            checkOrphanResourceEntries()
        }
    }

    // --- Continue Reading ---

    /**
     * Gets the last opened document's progress for the "Continue Reading" card.
     */
    val lastOpenedProgress: StateFlow<ReadingProgress?> = resourceRepository.getLastOpenedProgressStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Gets the last opened document with full details for the "Continue Reading" card.
     */
    val continueReadingResourceEntry: StateFlow<ResourceEntryWithDetails?> = lastOpenedProgress
        .flatMapLatest { progress ->
            if (progress == null) flowOf(null)
            else {
                val entryFlow = flow {
                    val entry = resourceEntryRepository.getResourceEntryByResourceId(progress.resourceId)
                    emit(entry)
                }
                entryFlow.flatMapLatest { entry ->
                    if (entry != null) resourceEntryRepository.getResourceEntryWithDetailsStream(entry.id)
                    else flowOf(null)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

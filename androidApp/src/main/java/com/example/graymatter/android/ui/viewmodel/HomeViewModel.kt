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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
     * Stream of the most recent resource entries with full details.
     */
    val recentResourceEntryDetails: StateFlow<List<ResourceEntryWithDetails>> = resourceEntriesStream
        .flatMapLatest { entries ->
            if (entries.isEmpty()) return@flatMapLatest flowOf(emptyList())

            // Take 4 most recent entries
            val recentEntries = entries.sortedByDescending { it.firstOpinionAt }.take(4)

            // Combine their individual details streams into one list
            combine(recentEntries.map { resourceEntryRepository.getResourceEntryWithDetailsStream(it.id) }) { detailsArray ->
                detailsArray.filterNotNull()
            }
        }
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

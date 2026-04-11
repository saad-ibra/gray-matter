package com.example.graymatter.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.graymatter.data.ResourceEntryRepository
import com.example.graymatter.data.OpinionRepository
import com.example.graymatter.data.ResourceRepository
import com.example.graymatter.data.TopicRepository
import com.example.graymatter.data.ReferenceLinkRepository
import com.example.graymatter.domain.Bookmark
import com.example.graymatter.domain.ResourceEntry
import com.example.graymatter.domain.ResourceEntryWithDetails
import com.example.graymatter.domain.Opinion
import com.example.graymatter.domain.Topic
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * ViewModel responsible for all trash/deletion operations.
 * Manages the Recently Deleted screen, cascade deletions,
 * purge scheduling, and restore flows.
 */
class TrashViewModel(
    private val resourceEntryRepository: ResourceEntryRepository,
    private val resourceRepository: ResourceRepository,
    private val opinionRepository: OpinionRepository,
    private val topicRepository: TopicRepository,
    private val referenceLinkRepository: ReferenceLinkRepository
) : ViewModel() {

    // -- Deleted items state --

    private val _deletedTopics = MutableStateFlow<List<Topic>>(emptyList())
    val deletedTopics: StateFlow<List<Topic>> = _deletedTopics.asStateFlow()

    private val _deletedOpinions = MutableStateFlow<List<Opinion>>(emptyList())
    val deletedOpinions: StateFlow<List<Opinion>> = _deletedOpinions.asStateFlow()

    private val _deletedResourceEntries = MutableStateFlow<List<ResourceEntryWithDetails>>(emptyList())
    val deletedResourceEntries: StateFlow<List<ResourceEntryWithDetails>> = _deletedResourceEntries.asStateFlow()

    private val _deletedBookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val deletedBookmarks: StateFlow<List<Bookmark>> = _deletedBookmarks.asStateFlow()

    // Restore flow: when non-null, the restored resource entry needs a topic assigned
    private val _restoreNeedsTopicId = MutableStateFlow<String?>(null)
    val restoreNeedsTopicId: StateFlow<String?> = _restoreNeedsTopicId.asStateFlow()

    init {
        purgeOldDeletedItems()
        loadRecentlyDeleted()
    }

    // -- Load & Refresh --

    fun loadRecentlyDeleted() {
        viewModelScope.launch {
            val deletedTopics = topicRepository.getDeletedTopics()
            _deletedTopics.value = deletedTopics
            val deletedTopicIds = deletedTopics.map { it.id }.toSet()

            val allDeletedResourceEntries = resourceEntryRepository.getDeletedResourceEntries()
            val allDeletedResourceEntryIds = allDeletedResourceEntries.map { it.id }.toSet()
            val allDeletedResourceEntryResourceIds = allDeletedResourceEntries.map { it.resourceId }.toSet()

            // Only show ResourceEntries in trash if their parent Topic is NOT in trash (or has no topic)
            _deletedResourceEntries.value = allDeletedResourceEntries
                .filter { it.topicId == null || it.topicId !in deletedTopicIds }
                .mapNotNull { resourceEntryRepository.getResourceEntryWithDetails(it.id) }

            // Only show Opinions if their parent ResourceEntry is NOT in trash
            _deletedOpinions.value = opinionRepository.getDeletedOpinions()
                .filter { it.itemId !in allDeletedResourceEntryIds }

            // Only show Bookmarks if their parent Resource is NOT in trash
            _deletedBookmarks.value = resourceRepository.getDeletedBookmarks()
                .filter { it.resourceId !in allDeletedResourceEntryResourceIds }
        }
    }

    fun purgeOldDeletedItems() {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000

            topicRepository.getDeletedTopics().forEach {
                val deletedAt = it.deletedAt
                if (deletedAt != null && now - deletedAt > thirtyDaysInMillis) permanentlyDeleteTopic(it.id)
            }
            resourceEntryRepository.getDeletedResourceEntries().forEach {
                val deletedAt = it.deletedAt
                if (deletedAt != null && now - deletedAt > thirtyDaysInMillis) permanentlyDeleteResourceEntry(it.id)
            }
            opinionRepository.getDeletedOpinions().forEach {
                val deletedAt = it.deletedAt
                if (deletedAt != null && now - deletedAt > thirtyDaysInMillis) permanentlyDeleteOpinion(it.id)
            }
            resourceRepository.getDeletedBookmarks().forEach {
                val deletedAt = it.deletedAt
                if (deletedAt != null && now - deletedAt > thirtyDaysInMillis) permanentlyDeleteBookmark(it.id)
            }

            // Sweep orphans left behind by any data leak
            resourceRepository.cleanOrphanResources()
            resourceRepository.cleanOrphanReadingData()
            referenceLinkRepository.cleanOrphanReferenceLinks()
        }
    }

    // -- Restore flow --

    fun clearRestoreNeedsTopic() {
        _restoreNeedsTopicId.value = null
    }

    fun cancelRestore(resourceEntryId: String) {
        viewModelScope.launch {
            // Put it back in trash since user declined to pick a topic
            resourceEntryRepository.softDeleteResourceEntry(resourceEntryId)
            _restoreNeedsTopicId.value = null
            loadRecentlyDeleted()
        }
    }

    // -- Opinion delete/undo/permanent --

    fun deleteOpinion(opinionId: String) {
        viewModelScope.launch {
            opinionRepository.softDeleteOpinion(opinionId)
            loadRecentlyDeleted()
        }
    }

    fun undoDeleteOpinion(opinionId: String) {
        viewModelScope.launch {
            opinionRepository.undoDeleteOpinion(opinionId)
            loadRecentlyDeleted()
        }
    }

    fun permanentlyDeleteOpinion(opinionId: String) {
        viewModelScope.launch {
            // Clean reference links before deleting
            referenceLinkRepository.deleteReferenceLinksBySource(opinionId)
            referenceLinkRepository.deleteReferenceLinksByTarget(opinionId)
            opinionRepository.deleteOpinion(opinionId)
            loadRecentlyDeleted()
        }
    }

    // -- Resource Entry delete/undo/permanent --

    fun deleteResourceEntry(resourceEntryId: String) {
        viewModelScope.launch {
            resourceEntryRepository.softDeleteResourceEntry(resourceEntryId)
            loadRecentlyDeleted()
        }
    }

    fun undoDeleteResourceEntry(resourceEntryId: String) {
        viewModelScope.launch {
            resourceEntryRepository.undoDeleteResourceEntry(resourceEntryId)
            loadRecentlyDeleted()

            // Validate parent still exists — if not, prompt user to pick a topic
            val entry = resourceEntryRepository.getResourceEntryById(resourceEntryId)
            if (entry != null) {
                val topicId = entry.topicId
                if (topicId == null) {
                    // No topic assigned — needs re-parenting
                    _restoreNeedsTopicId.value = resourceEntryId
                } else {
                    // Check if the topic still exists (not permanently deleted)
                    val topic = topicRepository.getTopicById(topicId)
                    if (topic == null) {
                        // Topic was permanently deleted — needs re-parenting
                        _restoreNeedsTopicId.value = resourceEntryId
                    }
                }
            }
        }
    }

    fun permanentlyDeleteResourceEntry(resourceEntryId: String) {
        viewModelScope.launch {
            val details = resourceEntryRepository.getResourceEntryWithDetails(resourceEntryId)
            val filePath = details?.resource?.filePath

            // Clean up reference links for all child opinions and the resource itself
            if (details != null) {
                details.opinions.forEach { opinion ->
                    referenceLinkRepository.deleteReferenceLinksBySource(opinion.id)
                    referenceLinkRepository.deleteReferenceLinksByTarget(opinion.id)
                }
                referenceLinkRepository.deleteReferenceLinksBySource(details.resource.id)
                referenceLinkRepository.deleteReferenceLinksByTarget(details.resource.id)
            }

            resourceEntryRepository.deleteResourceEntry(resourceEntryId)

            // Delete physical file if it exists and is in our app's private directory
            if (filePath != null && filePath.contains("/files/resources/")) {
                val file = java.io.File(filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            loadRecentlyDeleted()
        }
    }

    // -- Topic delete/undo/permanent --

    fun deleteTopic(topicId: String) {
        viewModelScope.launch {
            topicRepository.softDeleteTopic(topicId)
            loadRecentlyDeleted()
        }
    }

    fun deleteTopics(topicIds: List<String>) {
        viewModelScope.launch {
            topicIds.forEach { topicRepository.softDeleteTopic(it) }
            loadRecentlyDeleted()
        }
    }

    fun undoDeleteTopics(topicIds: List<String>) {
        viewModelScope.launch {
            topicIds.forEach { topicRepository.undoDeleteTopic(it) }
            loadRecentlyDeleted()
        }
    }

    fun undoDeleteTopic(topicId: String) {
        viewModelScope.launch {
            topicRepository.undoDeleteTopic(topicId)
            loadRecentlyDeleted()
        }
    }

    fun permanentlyDeleteTopic(topicId: String) {
        viewModelScope.launch {
            cascadeDeleteTopic(topicId)
            loadRecentlyDeleted()
        }
    }

    /**
     * Deletes multiple topics and ALL their contents permanently.
     */
    fun deleteTopicsPermanently(topicIds: List<String>) {
        viewModelScope.launch {
            topicIds.forEach { cascadeDeleteTopic(it) }
            loadRecentlyDeleted()
        }
    }

    /**
     * Full cascade delete: cleans up reference links, physical files,
     * opinions, resource entries, and the topic record itself.
     */
    private suspend fun cascadeDeleteTopic(topicId: String) {
        // 1. Get ALL resource entries belonging to this topic (including deleted ones)
        val topicEntries = resourceEntryRepository.getAllResourceEntriesByTopicId(topicId)

        for (entry in topicEntries) {
            // 2. Get ALL opinions for this entry (including deleted ones) and delete their reference links
            val opinions = opinionRepository.getAllOpinionsByItemId(entry.id)
            for (opinion in opinions) {
                referenceLinkRepository.deleteReferenceLinksBySource(opinion.id)
                referenceLinkRepository.deleteReferenceLinksByTarget(opinion.id)
            }

            // 3. Delete reference links where this resource is the target
            referenceLinkRepository.deleteReferenceLinksByTarget(entry.resourceId)

            // 4. Delete physical file if it lives in our private storage
            val resource = resourceRepository.getResourceById(entry.resourceId)
            val filePath = resource?.filePath
            if (filePath != null && filePath.contains("/files/resources/")) {
                val file = java.io.File(filePath)
                if (file.exists()) file.delete()
            }

            // 5. Delete the resource entry (DB cascade deletes opinions + bookmarks + reading progress)
            resourceEntryRepository.deleteResourceEntry(entry.id)
        }

        // 6. Finally delete the topic record itself
        topicRepository.deleteTopic(topicId)
    }

    // -- Bookmark delete/undo/permanent --

    fun deleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            resourceRepository.softDeleteBookmark(bookmarkId)
            loadRecentlyDeleted()
        }
    }

    fun undoDeleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            resourceRepository.undoDeleteBookmark(bookmarkId)
            loadRecentlyDeleted()
        }
    }

    fun permanentlyDeleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            // Clean reference links before deleting
            referenceLinkRepository.deleteReferenceLinksBySource(bookmarkId)
            referenceLinkRepository.deleteReferenceLinksByTarget(bookmarkId)
            resourceRepository.deleteBookmark(bookmarkId)
            loadRecentlyDeleted()
        }
    }
}

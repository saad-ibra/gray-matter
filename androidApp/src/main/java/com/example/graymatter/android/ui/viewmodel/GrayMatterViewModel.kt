package com.example.graymatter.android.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.graymatter.data.ItemRepository
import com.example.graymatter.data.OpinionRepository
import com.example.graymatter.data.ResourceRepository
import com.example.graymatter.data.TopicRepository
import com.example.graymatter.domain.Bookmark
import com.example.graymatter.domain.Item
import com.example.graymatter.domain.ItemWithDetails
import com.example.graymatter.domain.Opinion
import com.example.graymatter.domain.ReadingProgress
import com.example.graymatter.domain.Resource
import com.example.graymatter.domain.ResourceType
import com.example.graymatter.domain.Topic
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * Main ViewModel for Gray Matter app.
 * Handles all business logic for creating items, opinions, and topics.
 */
class GrayMatterViewModel(
    private val itemRepository: ItemRepository,
    private val resourceRepository: ResourceRepository,
    private val opinionRepository: OpinionRepository,
    private val topicRepository: TopicRepository
) : ViewModel() {
    
    val topicsStream: StateFlow<List<Topic>> = topicRepository.topicsStream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val itemsStream: StateFlow<List<Item>> = itemRepository.itemsStream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Stream of the most recent items with full details.
     */
    val recentItemsDetails: StateFlow<List<ItemWithDetails>> = itemsStream
        .flatMapLatest { items ->
            if (items.isEmpty()) return@flatMapLatest flowOf(emptyList())

            // Take 4 most recent items
            val recentItems = items.sortedByDescending { it.firstOpinionAt }.take(4)

            // Combine their individual details streams into one list
            combine(recentItems.map { itemRepository.getItemWithDetailsStream(it.id) }) { detailsArray ->
                detailsArray.filterNotNull()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * Creates a new item with resource and first opinion.
     * Returns the created itemId.
     */
    suspend fun createNewItem(url: String, opinionText: String, confidence: Int, description: String? = null): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val resourceId = generateUuid()
        val itemId = generateUuid()
        val opinionId = generateUuid()
        
        itemRepository.createItemWithDetails(
            itemId = itemId,
            resourceId = resourceId,
            resourceType = ResourceType.WEB_LINK.name,
            url = url,
            filePath = null,
            extractedText = null,
            title = extractTitleFromUrl(url),
            description = description,
            opinionId = opinionId,
            opinionText = opinionText,
            confidence = confidence,
            now = now
        )
        
        return itemId
    }

    /**
     * Creates a new item with a title and first opinion.
     */
    suspend fun createNewItemWithTitle(title: String, opinionText: String, confidence: Int, description: String? = null): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val resourceId = generateUuid()
        val itemId = generateUuid()
        val opinionId = generateUuid()
        
        itemRepository.createItemWithDetails(
            itemId = itemId,
            resourceId = resourceId,
            resourceType = ResourceType.UNSUPPORTED.name,
            url = null,
            filePath = null,
            extractedText = null,
            title = title,
            description = description,
            opinionId = opinionId,
            opinionText = opinionText,
            confidence = confidence,
            now = now
        )
        
        return itemId
    }

    /**
     * Creates a new Note item. Saved with Markdown content.
     */
    suspend fun createNewNote(title: String, content: String, opinionText: String, confidence: Int, description: String? = null): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val resourceId = generateUuid()
        val itemId = generateUuid()
        val opinionId = generateUuid()
        
        itemRepository.createItemWithDetails(
            itemId = itemId,
            resourceId = resourceId,
            resourceType = ResourceType.MARKDOWN.name,
            url = null,
            filePath = "${title}.md",
            extractedText = content,
            title = title,
            description = description,
            opinionId = opinionId,
            opinionText = opinionText,
            confidence = confidence,
            now = now
        )
        
        return itemId
    }
    
    /**
     * Creates a new item from a file resource with first opinion.
     * Returns the created itemId.
     */
    suspend fun createNewItemFromFile(
        fileName: String,
        filePath: String,
        opinionText: String,
        confidence: Int,
        description: String? = null
    ): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val resourceId = generateUuid()
        val itemId = generateUuid()
        val opinionId = generateUuid()
        
        val resourceType = determineResourceType(fileName, filePath)
        
        itemRepository.createItemWithDetails(
            itemId = itemId,
            resourceId = resourceId,
            resourceType = resourceType.name,
            url = null,
            filePath = filePath,
            extractedText = null,
            title = fileName,
            description = description,
            opinionId = opinionId,
            opinionText = opinionText,
            confidence = confidence,
            now = now
        )
        
        return itemId
    }
    
    /**
     * Adds a new opinion to an existing item.
     */
    fun addOpinion(itemId: String, opinionText: String, confidence: Int, createdAt: Long? = null) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val finalCreatedAt = createdAt ?: now
            
            val opinion = Opinion(
                id = generateUuid(),
                itemId = itemId,
                text = opinionText,
                confidenceScore = confidence,
                createdAt = finalCreatedAt,
                updatedAt = now
            )
            opinionRepository.saveOpinion(opinion)
            
            // Update item metadata
            itemRepository.updateItemOpinionMetadata(itemId, finalCreatedAt)
        }
    }

    /**
     * Updates an existing opinion.
     */
    fun updateOpinion(opinionId: String, text: String, confidence: Int, createdAt: Long) {
        viewModelScope.launch {
            val existing = opinionRepository.getOpinionById(opinionId) ?: return@launch
            val updated = existing.copy(
                text = text,
                confidenceScore = confidence,
                createdAt = createdAt,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
            opinionRepository.updateOpinion(updated)
        }
    }

    /**
     * Deletes an opinion.
     */
    fun deleteOpinion(opinionId: String) {
        viewModelScope.launch {
            opinionRepository.deleteOpinion(opinionId)
        }
    }

    /**
     * Deletes an item and its associated resource.
     */
    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            itemRepository.deleteItem(itemId)
        }
    }
    
    /**
     * Creates a new topic.
     */
    suspend fun createTopic(name: String): String {
        val topicId = generateUuid()
        val now = Clock.System.now().toEpochMilliseconds()
        
        val topic = Topic(
            id = topicId,
            name = name,
            notes = null,
            classificationKeywords = extractKeywords(name),
            resourceCount = 0,
            updatedAt = now
        )
        topicRepository.saveTopic(topic)
        
        return topicId
    }

    /**
     * Deletes a topic.
     */
    fun deleteTopic(topicId: String) {
        viewModelScope.launch {
            topicRepository.deleteTopic(topicId)
        }
    }
    
    /**
     * Assigns a topic to an item.
     */
    fun assignTopicToItem(itemId: String, topicId: String) {
        viewModelScope.launch {
            itemRepository.updateItemTopic(itemId, topicId)
        }
    }

    /**
     * Updates an item's description.
     */
    fun updateItemDescription(itemId: String, description: String?) {
        viewModelScope.launch {
            itemRepository.updateItemDescription(itemId, description)
        }
    }
    
    /**
     * Gets full item details as a reactive flow.
     */
    fun getItemDetails(itemId: String): Flow<ItemWithDetails?> {
        return itemRepository.getItemWithDetailsStream(itemId)
    }

    /**
     * Gets items for a topic.
     */
    fun getItemsByTopic(topicId: String): Flow<List<ItemWithDetails>> {
        return itemRepository.itemsStream.map { items ->
            items.filter { it.topicId == topicId }.mapNotNull { item ->
                itemRepository.getItemWithDetails(item.id)
            }
        }
    }
    
    /**
     * Gets a resource by its ID.
     */
    suspend fun getResourceForItem(resourceId: String): Resource? {
        return resourceRepository.getResourceById(resourceId)
    }
    
    /**
     * Updates topic notes (synthesis text).
     */
    fun updateTopicNotes(topicId: String, notes: String) {
        viewModelScope.launch {
            topicRepository.updateTopicNotes(topicId, notes)
        }
    }

    // -- File Viewer & Library Enhancement Methods --

    /**
     * Renames a resource (updates its title).
     */
    fun renameResource(resourceId: String, newTitle: String) {
        viewModelScope.launch {
            resourceRepository.updateResourceTitle(resourceId, newTitle)
        }
    }

    /**
     * Updates a resource's text content (for editing notes).
     */
    fun updateResourceText(resourceId: String, newText: String) {
        viewModelScope.launch {
            resourceRepository.updateResourceText(resourceId, newText)
        }
    }

    /**
     * Opens a URL in the default browser.
     */
    fun openUrlInBrowser(context: Context, url: String) {
        try {
            val fixedUrl = when {
                url.startsWith("http://") || url.startsWith("https://") -> url
                url.startsWith("www.") -> "https://$url"
                else -> "https://$url"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fixedUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // URL could not be opened
        }
    }

    /**
     * Gets reading progress for a resource as a stream.
     */
    fun getReadingProgressStream(resourceId: String): Flow<ReadingProgress?> {
        return resourceRepository.getReadingProgressStream(resourceId)
    }

    /**
     * Gets reading progress for a resource.
     */
    suspend fun getReadingProgress(resourceId: String): ReadingProgress? {
        return resourceRepository.getReadingProgress(resourceId)
    }

    /**
     * Updates reading progress for a resource.
     */
    fun updateReadingProgress(resourceId: String, currentPage: Int, totalPages: Int, currentChapter: String? = null) {
        viewModelScope.launch {
            // Fix: Corrected progress calculation logic.
            // (currentPage + 1) / totalPages ensures that page 1 shows some progress, and last page shows 100%.
            val percent = if (totalPages > 0) (currentPage + 1).toDouble() / totalPages else 0.0
            val progress = ReadingProgress(
                resourceId = resourceId,
                currentPage = currentPage,
                totalPages = totalPages,
                percentComplete = percent,
                currentChapter = currentChapter,
                lastOpenedAt = Clock.System.now().toEpochMilliseconds()
            )
            resourceRepository.updateReadingProgress(progress)
        }
    }

    /**
     * Gets the last opened document's progress for the "Continue Reading" card.
     */
    val lastOpenedProgress: StateFlow<ReadingProgress?> = resourceRepository.getLastOpenedProgressStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Gets the last opened document with full details for the "Continue Reading" card.
     */
    val continueReadingItem: StateFlow<ItemWithDetails?> = lastOpenedProgress
        .flatMapLatest { progress ->
            if (progress == null) flowOf(null)
            else {
                val itemFlow = flow {
                    val item = itemRepository.getItemByResourceId(progress.resourceId)
                    emit(item)
                }
                itemFlow.flatMapLatest { item ->
                    if (item != null) itemRepository.getItemWithDetailsStream(item.id)
                    else flowOf(null)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // -- Bookmark Methods --

    /**
     * Gets bookmarks for a resource.
     */
    suspend fun getBookmarks(resourceId: String): List<Bookmark> {
        return resourceRepository.getBookmarks(resourceId)
    }

    /**
     * Saves a bookmark.
     */
    fun saveBookmark(resourceId: String, page: Int, totalPages: Int, title: String? = null) {
        viewModelScope.launch {
            val bookmark = Bookmark(
                id = generateUuid(),
                resourceId = resourceId,
                page = page,
                percentPosition = if (totalPages > 0) (page + 1).toDouble() / totalPages else 0.0,
                title = title ?: "Page ${page + 1}",
                createdAt = Clock.System.now().toEpochMilliseconds()
            )
            resourceRepository.saveBookmark(bookmark)
        }
    }

    /**
     * Deletes a bookmark.
     */
    fun deleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            resourceRepository.deleteBookmark(bookmarkId)
        }
    }

    /**
     * Exports backup of all data as JSON.
     */
    suspend fun exportBackupData(): String {
        val items = itemsStream.value
        val topics = topicsStream.value
        val resources = mutableListOf<Resource>()
        val opinions = mutableListOf<Opinion>()
        
        for (item in items) {
            val details = itemRepository.getItemWithDetails(item.id)
            if (details != null) {
                resources.add(details.resource)
                opinions.addAll(details.opinions)
            }
        }
        
        // Simple JSON export
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"exportDate\": \"${Clock.System.now()}\",")
        sb.appendLine("  \"itemCount\": ${items.size},")
        sb.appendLine("  \"topicCount\": ${topics.size},")
        sb.appendLine("  \"resourceCount\": ${resources.size},")
        sb.appendLine("  \"opinionCount\": ${opinions.size}")
        sb.appendLine("}")
        return sb.toString()
    }
    
    /**
     * Simple keyword extraction from topic name.
     */
    private fun extractKeywords(text: String): String {
        return text.lowercase()
            .split(" ")
            .filter { it.length > 3 }
            .joinToString(",")
    }
    
    /**
     * Determines the resource type from a file name's extension or path.
     */
    private fun determineResourceType(fileName: String, filePath: String? = null): ResourceType {
        var ext = fileName.substringAfterLast('.', "").lowercase()
        
        // If extension is missing, try to get it from filePath if it's a content URI
        if (ext.isEmpty() && filePath != null && filePath.startsWith("content://")) {
            val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(filePath.substringAfterLast('.'))
                ?: "application/octet-stream"
             // In many cases we don't have the mime type here easily without context.
             // But we can check if the fileName contains keywords.
             if (fileName.lowercase().contains("pdf")) return ResourceType.PDF
        }

        return when (ext) {
            "pdf" -> ResourceType.PDF
            "md", "markdown" -> ResourceType.MARKDOWN
            "jpg", "jpeg", "png", "gif", "webp", "bmp" -> ResourceType.IMAGE
            "epub" -> ResourceType.EPUB
            "mobi" -> ResourceType.MOBI
            "cbz", "cbr" -> ResourceType.CBZ
            else -> {
                if (fileName.lowercase().endsWith("pdf")) ResourceType.PDF
                else ResourceType.UNSUPPORTED
            }
        }
    }
    
    /**
     * Extracts title from URL.
     */
    private fun extractTitleFromUrl(url: String): String {
        return try {
            val cleanUrl = url
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
            cleanUrl.split("/")
                .lastOrNull { it.isNotBlank() }
                ?.replace("-", " ")
                ?.replace("_", " ")
                ?: cleanUrl.split("/").first()
        } catch (e: Exception) {
            url
        }
    }
    
    /**
     * Generates a simple UUID.
     */
    private fun generateUuid(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..32).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}

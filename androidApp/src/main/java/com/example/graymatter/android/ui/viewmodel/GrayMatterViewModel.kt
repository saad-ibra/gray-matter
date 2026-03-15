package com.example.graymatter.android.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.graymatter.android.util.FileUtils
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

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    /**
     * Verifies the integrity of all resources on app start.
     * Checks if internal files still exist.
     */
    fun checkResourceIntegrity() {
        viewModelScope.launch {
            val items = itemRepository.itemsStream.first()
            items.forEach { item ->
                val details = itemRepository.getItemWithDetails(item.id)
                val filePath = details?.resource?.filePath
                if (details?.resource?.type != ResourceType.WEB_LINK && filePath != null) {
                    if (!FileUtils.verifyFileExists(filePath)) {
                        // In a real app, we might mark this in DB or surface a badge
                        // For now, we log it.
                    }
                }
            }
        }
    }
    
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
     * Creates a new Note item. Saved with Markdown content in internal storage.
     */
    suspend fun createNewNote(context: Context, title: String, content: String, opinionText: String, confidence: Int, description: String? = null): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val resourceId = generateUuid()
        val itemId = generateUuid()
        val opinionId = generateUuid()

        // Create the .md file in internal storage
        val outputDir = java.io.File(context.filesDir, "resources")
        if (!outputDir.exists()) outputDir.mkdirs()
        val internalFile = java.io.File(outputDir, "${generateUuid()}.md")
        internalFile.writeText(content)
        
        itemRepository.createItemWithDetails(
            itemId = itemId,
            resourceId = resourceId,
            resourceType = ResourceType.MARKDOWN.name,
            url = null,
            filePath = internalFile.absolutePath,
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
     * Copies the file to internal storage first.
     * Returns the created itemId.
     */
    suspend fun createNewItemFromFile(
        context: Context,
        fileName: String,
        uri: Uri,
        opinionText: String,
        confidence: Int,
        description: String? = null
    ): String? {
        _isImporting.value = true
        return try {
            // 1. Copy file to internal storage
            val internalPath = FileUtils.copyUriToInternalStorage(context, uri, fileName)
            
            if (internalPath == null) {
                _isImporting.value = false
                return null
            }

            // 2. Only after successful copy, write to database
            val now = Clock.System.now().toEpochMilliseconds()
            val resourceId = generateUuid()
            val itemId = generateUuid()
            val opinionId = generateUuid()
            
            val resourceType = determineResourceType(fileName, internalPath)
            
            itemRepository.createItemWithDetails(
                itemId = itemId,
                resourceId = resourceId,
                resourceType = resourceType.name,
                url = null,
                filePath = internalPath,
                extractedText = null,
                title = fileName,
                description = description,
                opinionId = opinionId,
                opinionText = opinionText,
                confidence = confidence,
                now = now
            )
            
            _isImporting.value = false
            itemId
        } catch (e: Exception) {
            e.printStackTrace()
            _isImporting.value = false
            null
        }
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
     * Also deletes the physical file from internal storage.
     */
    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            val details = itemRepository.getItemWithDetails(itemId)
            val filePath = details?.resource?.filePath
            
            itemRepository.deleteItem(itemId)

            // Delete physical file if it exists and is in our app's private directory
            if (filePath != null && filePath.contains("/files/resources/")) {
                val file = java.io.File(filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
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
            
            // If it's a file-based note, update the physical file too
            val resource = resourceRepository.getResourceById(resourceId)
            if (resource?.type == ResourceType.MARKDOWN && resource.filePath != null) {
                try {
                    java.io.File(resource.filePath).writeText(newText)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
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
     * Deletes a bookmark and its associated opinion (if it exists).
     */
    fun deleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            val bookmark = resourceRepository.getBookmarkById(bookmarkId)
            
            if (bookmark != null) {
                val item = itemRepository.getItemByResourceId(bookmark.resourceId)
                if (item != null) {
                    val opinions = opinionRepository.getOpinionsByItemId(item.id).first()
                    val matchingOpinion = opinions.firstOrNull { 
                        it.createdAt == bookmark.createdAt || 
                        (it.pageNumber == bookmark.page && it.text.contains(bookmark.opinion ?: ""))
                    }
                    
                    if (matchingOpinion != null) {
                        opinionRepository.deleteOpinion(matchingOpinion.id)
                    }
                }
            }
            
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
        
        if (ext.isEmpty() && filePath != null) {
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

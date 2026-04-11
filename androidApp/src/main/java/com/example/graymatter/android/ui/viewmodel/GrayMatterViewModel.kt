package com.example.graymatter.android.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.graymatter.android.util.FileUtils
import com.example.graymatter.data.ResourceEntryRepository
import com.example.graymatter.data.OpinionRepository
import com.example.graymatter.data.ResourceRepository
import com.example.graymatter.data.TopicRepository
import com.example.graymatter.data.ReferenceLinkRepository
import com.example.graymatter.domain.Bookmark
import com.example.graymatter.domain.CustomTemplate
import com.example.graymatter.domain.ResourceEntry
import com.example.graymatter.domain.ResourceEntryWithDetails
import com.example.graymatter.domain.Opinion
import com.example.graymatter.domain.ReadingProgress
import com.example.graymatter.domain.Resource
import com.example.graymatter.domain.ResourceType
import com.example.graymatter.domain.Topic
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.Locale
import kotlin.random.Random

/**
 * Main ViewModel for Gray Matter app.
 * Handles all business logic for creating resource entries, opinions, and topics.
 */
class GrayMatterViewModel(
    private val resourceEntryRepository: ResourceEntryRepository,
    private val resourceRepository: ResourceRepository,
    private val opinionRepository: OpinionRepository,
    private val topicRepository: TopicRepository,
    private val referenceLinkRepository: ReferenceLinkRepository,
    private val autoLinkService: com.example.graymatter.domain.business.AutoLinkService
) : ViewModel() {
    
    val topicsStream: StateFlow<List<Topic>> = topicRepository.topicsStream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    // Data Integrity: orphan resource entries (no topic) detected on startup
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
     * Called after assigning a topic to a resource entry (previously orphan or during creation).
     */
    fun assignTopicToResourceEntry(resourceEntryId: String, topicId: String) {
        viewModelScope.launch {
            resourceEntryRepository.updateResourceEntryTopic(resourceEntryId, topicId)
            checkOrphanResourceEntries()
        }
    }

    // Templates
    val templates: StateFlow<List<CustomTemplate>> = resourceRepository.templatesStream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Verifies the integrity of all resources on app start.
     * Checks if internal files still exist.
     */
    fun checkResourceIntegrity() {
        viewModelScope.launch {
            val entries = resourceEntryRepository.resourceEntriesStream.first()
            entries.forEach { entry ->
                val details = resourceEntryRepository.getResourceEntryWithDetails(entry.id)
                val filePath = details?.resource?.filePath
                if (details?.resource?.type != ResourceType.WEB_LINK && filePath != null) {
                    if (!FileUtils.verifyFileExists(filePath)) {
                        // In a real app, we might mark this in DB or surface a badge
                    }
                }
            }
        }
    }
    
    /**
     * Creates a new resource entry with resource and first opinion.
     * Returns the created resourceEntryId.
     */
    suspend fun createNewResourceEntry(url: String, opinionText: String, confidence: Int, title: String? = null, description: String? = null, topicId: String? = null, referenceLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList()): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val resourceId = generateUuid()
        val resourceEntryId = generateUuid()
        val opinionId = generateUuid()
        
        resourceEntryRepository.createResourceEntryWithDetails(
            resourceEntryId = resourceEntryId,
            resourceId = resourceId,
            resourceType = ResourceType.WEB_LINK.name,
            url = url,
            filePath = null,
            extractedText = null,
            title = title ?: extractTitleFromUrl(url),
            description = description,
            opinionId = opinionId,
            opinionText = opinionText,
            confidence = confidence,
            now = now
        )
        
        if (topicId != null) {
            resourceEntryRepository.updateResourceEntryTopic(resourceEntryId, topicId)
        }
        
        autoLinkService.syncLinks(opinionId, com.example.graymatter.domain.ReferenceType.OPINION, opinionText, referenceLinks)
        
        return resourceEntryId
    }

    /**
     * Creates a new Note resource entry. Saved with Markdown content in internal storage.
     */
    suspend fun createNewNote(
        context: Context, 
        title: String, 
        content: String, 
        opinionText: String, 
        confidence: Int, 
        description: String? = null, 
        topicId: String? = null, 
        referenceLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList(),
        opinionReferenceLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList() // NEW
    ): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val resourceId = generateUuid()
        val resourceEntryId = generateUuid()
        val opinionId = generateUuid()

        // Create the .md file in internal storage
        val outputDir = java.io.File(context.filesDir, "resources")
        if (!outputDir.exists()) outputDir.mkdirs()
        val internalFile = java.io.File(outputDir, "${generateUuid()}.md")
        internalFile.writeText(content)
        
        resourceEntryRepository.createResourceEntryWithDetails(
            resourceEntryId = resourceEntryId,
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
        
        if (topicId != null) {
            resourceEntryRepository.updateResourceEntryTopic(resourceEntryId, topicId)
        }
        
        // Save note-level links as RESOURCE type (extracted from content)
        autoLinkService.syncLinks(resourceId, com.example.graymatter.domain.ReferenceType.RESOURCE, content, referenceLinks)
        // Save opinion-level links as OPINION type (extracted from first opinion)
        autoLinkService.syncLinks(opinionId, com.example.graymatter.domain.ReferenceType.OPINION, opinionText, opinionReferenceLinks)
        
        return resourceEntryId
    }
    
    /**
     * Creates a new resource entry from a file resource with first opinion.
     * Copies the file to internal storage first.
     * Returns the created resourceEntryId.
     */
    suspend fun createNewResourceEntryFromFile(
        context: Context,
        fileName: String,
        uri: Uri,
        opinionText: String,
        confidence: Int,
        title: String? = null,
        description: String? = null,
        topicId: String? = null,
        referenceLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList()
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
            val resourceEntryId = generateUuid()
            val opinionId = generateUuid()
            
            val resourceType = determineResourceType(fileName, internalPath)
            
            resourceEntryRepository.createResourceEntryWithDetails(
                resourceEntryId = resourceEntryId,
                resourceId = resourceId,
                resourceType = resourceType.name,
                url = null,
                filePath = internalPath,
                extractedText = null,
                title = title ?: fileName,
                description = description,
                opinionId = opinionId,
                opinionText = opinionText,
                confidence = confidence,
                now = now
            )
            
            if (topicId != null) {
                resourceEntryRepository.updateResourceEntryTopic(resourceEntryId, topicId)
            }
            
            autoLinkService.syncLinks(opinionId, com.example.graymatter.domain.ReferenceType.OPINION, opinionText, referenceLinks)

            _isImporting.value = false
            resourceEntryId
        } catch (e: Exception) {
            e.printStackTrace()
            _isImporting.value = false
            null
        }
    }
    
    /**
     * Adds a new opinion to an existing resource entry.
     */
    fun addOpinion(resourceEntryId: String, opinionText: String, confidence: Int, createdAt: Long? = null, referenceLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList()) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val finalCreatedAt = createdAt ?: now
            
            val opinion = Opinion(
                id = generateUuid(),
                itemId = resourceEntryId,
                text = opinionText,
                confidenceScore = confidence,
                createdAt = finalCreatedAt,
                updatedAt = now
            )
            opinionRepository.saveOpinion(opinion)
            
            autoLinkService.syncLinks(opinion.id, com.example.graymatter.domain.ReferenceType.OPINION, opinionText, referenceLinks)
            
            // Update resource entry metadata
            resourceEntryRepository.updateResourceEntryOpinionMetadata(resourceEntryId, finalCreatedAt)
        }
    }

    /**
     * Updates an existing opinion.
     */
    fun updateOpinion(opinionId: String, text: String, confidence: Int, createdAt: Long, referenceLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList()) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val existing = opinionRepository.getOpinionById(opinionId) ?: return@launch
            val updated = existing.copy(
                text = text,
                confidenceScore = confidence,
                createdAt = createdAt,
                updatedAt = now
            )
            opinionRepository.updateOpinion(updated)
            autoLinkService.syncLinks(opinionId, com.example.graymatter.domain.ReferenceType.OPINION, text, referenceLinks)
        }
    }

    /**
     * Soft deletes an opinion.
     */
    fun deleteOpinion(opinionId: String) {
        viewModelScope.launch {
            opinionRepository.softDeleteOpinion(opinionId)
        }
    }

    fun undoDeleteOpinion(opinionId: String) {
        viewModelScope.launch {
            opinionRepository.undoDeleteOpinion(opinionId)
        }
    }

    /**
     * Soft deletes a resource entry.
     */
    fun deleteResourceEntry(resourceEntryId: String) {
        viewModelScope.launch {
            resourceEntryRepository.softDeleteResourceEntry(resourceEntryId)
        }
    }

    fun undoDeleteResourceEntry(resourceEntryId: String) {
        viewModelScope.launch {
            resourceEntryRepository.undoDeleteResourceEntry(resourceEntryId)
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
            resourceCount = 0,
            updatedAt = now
        )
        topicRepository.saveTopic(topic)
        
        return topicId
    }

    /**
     * Soft deletes a topic and its resources.
     */
    fun deleteTopic(topicId: String) {
        viewModelScope.launch {
            topicRepository.softDeleteTopic(topicId)
        }
    }

    fun deleteTopics(topicIds: List<String>) {
        viewModelScope.launch {
            topicIds.forEach { topicRepository.softDeleteTopic(it) }
        }
    }

    fun undoDeleteTopics(topicIds: List<String>) {
        viewModelScope.launch {
            topicIds.forEach { topicRepository.undoDeleteTopic(it) }
        }
    }

    /**
     * Renames a topic.
     */
    fun renameTopic(topicId: String, newName: String) {
        viewModelScope.launch {
            topicRepository.renameTopic(topicId, newName)
        }
    }

    /**
     * Updates topic order.
     */
    fun updateTopicOrder(topicIds: List<String>) {
        viewModelScope.launch {
            topicRepository.updateTopicOrder(topicIds)
        }
    }
    
    /**
     * Updates a resource entry's description.
     */
    fun updateResourceEntryDescription(resourceEntryId: String, description: String?) {
        viewModelScope.launch {
            resourceEntryRepository.updateResourceEntryDescription(resourceEntryId, description)
        }
    }
    
    /**
     * Gets full resource entry details as a reactive flow.
     */
    fun getResourceEntryDetails(resourceEntryId: String): Flow<ResourceEntryWithDetails?> {
        return resourceEntryRepository.getResourceEntryWithDetailsStream(resourceEntryId)
    }

    /**
     * Gets resource entries for a topic.
     */
    fun getResourceEntriesByTopic(topicId: String): Flow<List<ResourceEntryWithDetails>> {
        return resourceEntryRepository.resourceEntriesStream.map { entries ->
            entries.filter { it.topicId == topicId }.mapNotNull { entry ->
                resourceEntryRepository.getResourceEntryWithDetails(entry.id)
            }
        }
    }
    
    /**
     * Gets a resource by its ID.
     */
    suspend fun getResourceForResourceEntry(resourceId: String): Resource? {
        return resourceRepository.getResourceById(resourceId)
    }
    
    /**
     * Updates topic notes (synthesis text).
     */
    fun updateTopicNotes(topicId: String, notes: String, referenceLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList()) {
        viewModelScope.launch {
            topicRepository.updateTopicNotes(topicId, notes)
            autoLinkService.syncLinks(topicId, com.example.graymatter.domain.ReferenceType.TOPIC, notes, referenceLinks)
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
    fun updateResourceText(resourceId: String, newText: String, referenceLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList()) {
        viewModelScope.launch {
            resourceRepository.updateResourceText(resourceId, newText)
            
            // If it's a file-based note, update the physical file too
            val resource = resourceRepository.getResourceById(resourceId)
            val filePath = resource?.filePath
            if (resource?.type == ResourceType.MARKDOWN && filePath != null) {
                try {
                    java.io.File(filePath).writeText(newText)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            autoLinkService.syncLinks(resourceId, com.example.graymatter.domain.ReferenceType.RESOURCE, newText, referenceLinks)
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
    fun saveBookmark(resourceId: String, page: Int, totalPages: Int, title: String? = null, referenceLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList()) {
        viewModelScope.launch {
            val bookmarkId = generateUuid()
            val bookmark = Bookmark(
                id = bookmarkId,
                resourceId = resourceId,
                page = page,
                percentPosition = if (totalPages > 0) (page + 1).toDouble() / totalPages else 0.0,
                title = title ?: "Page ${page + 1}",
                createdAt = Clock.System.now().toEpochMilliseconds()
            )
            resourceRepository.saveBookmark(bookmark)
            saveReferenceLinksInternal(bookmarkId, com.example.graymatter.domain.ReferenceType.BOOKMARK, referenceLinks)
        }
    }

    /**
     * Soft deletes a bookmark.
     */
    fun deleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            resourceRepository.softDeleteBookmark(bookmarkId)
        }
    }

    fun undoDeleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            resourceRepository.undoDeleteBookmark(bookmarkId)
        }
    }

    /**
     * Exports backup of all data as JSON.
     */
    suspend fun exportBackupData(): String {
        val entries = resourceEntriesStream.value
        val topics = topicsStream.value
        val resources = mutableListOf<Resource>()
        val opinions = mutableListOf<Opinion>()
        
        for (entry in entries) {
            val details = resourceEntryRepository.getResourceEntryWithDetails(entry.id)
            if (details != null) {
                resources.add(details.resource)
                opinions.addAll(details.opinions)
            }
        }
        
        // Simple JSON export
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"exportDate\": \"${Clock.System.now()}\",")
        sb.appendLine("  \"resourceEntryCount\": ${entries.size},")
        sb.appendLine("  \"topicCount\": ${topics.size},")
        sb.appendLine("  \"resourceCount\": ${resources.size},")
        sb.appendLine("  \"opinionCount\": ${opinions.size}")
        sb.appendLine("}")
        return sb.toString()
    }
    
    // -- Custom Template Methods --

    fun saveTemplate(template: CustomTemplate) {
        viewModelScope.launch {
            resourceRepository.saveTemplate(template)
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch {
            resourceRepository.deleteTemplate(id)
        }
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
    fun extractTitleFromUrl(url: String): String {
        return try {
            var clean = url
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
            
            // Remove trailing slash
            if (clean.endsWith("/")) clean = clean.dropLast(1)
            
            // Take the last part of the path (the slug)
            val parts = clean.split("/")
            var slug = parts.lastOrNull { it.isNotBlank() } ?: parts.first()
            
            // Clean hyphens and underscores, replace with spaces
            slug = slug.replace("-", " ").replace("_", " ")
            
            // Basic capitalization (sentence case)
            if (slug.isNotEmpty()) {
                slug = slug.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
            
            slug
        } catch (e: Exception) {
            url
        }
    }
    
    /**
     * Generates a simple UUID.
     */
    fun generateUuid(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..32).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    private suspend fun saveReferenceLinksInternal(sourceId: String, sourceType: com.example.graymatter.domain.ReferenceType, links: List<com.example.graymatter.domain.ReferenceSelectorItem>) {
        if (links.isEmpty()) return
        val now = Clock.System.now().toEpochMilliseconds()
        links.forEach { linkItem ->
            val targetType = when (linkItem) {
                is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> com.example.graymatter.domain.ReferenceType.TOPIC
                is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> com.example.graymatter.domain.ReferenceType.RESOURCE
                is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> com.example.graymatter.domain.ReferenceType.OPINION
            }
            val newLink = com.example.graymatter.domain.ReferenceLink(
                id = generateUuid(),
                sourceType = sourceType,
                sourceId = sourceId,
                targetType = targetType,
                targetId = linkItem.id,
                createdAt = now
            )
            referenceLinkRepository.insertReferenceLink(newLink)
        }
    }

    /**
     * Retrieves reference links for an opinion.
     */
    fun getLinksForOpinion(opinionId: String): Flow<List<com.example.graymatter.domain.ReferenceSelectorItem>> {
        return resolveLinksForSource(opinionId)
    }

    /**
     * Retrieves reference links for a resource (e.g. links added from within a note).
     */
    fun getLinksForResource(resourceId: String): Flow<List<com.example.graymatter.domain.ReferenceSelectorItem>> {
        return resolveLinksForSource(resourceId)
    }

    /**
     * Retrieves reference links for a topic.
     */
    fun getLinksForTopic(topicId: String): Flow<List<com.example.graymatter.domain.ReferenceSelectorItem>> {
        return resolveLinksForSource(topicId)
    }

    /**
     * Shared helper to resolve reference links for any source ID.
     */
    private fun resolveLinksForSource(sourceId: String): Flow<List<com.example.graymatter.domain.ReferenceSelectorItem>> {
        return referenceLinkRepository.getReferenceLinksBySource(sourceId).map { links ->
            links.mapNotNull { link ->
                when (link.targetType) {
                    com.example.graymatter.domain.ReferenceType.TOPIC -> {
                        val topic = topicRepository.getTopicById(link.targetId)
                        if (topic != null) com.example.graymatter.domain.ReferenceSelectorItem.TopicItem(id = topic.id, name = topic.name, isExpanded = false, isChecked = true) else null
                    }
                    com.example.graymatter.domain.ReferenceType.RESOURCE -> {
                        val resource = resourceRepository.getResourceById(link.targetId)
                        if (resource != null) com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem(id = resource.id, title = resource.title ?: "Untitled", type = resource.type.name, parentTopicId = null, isExpanded = false, isChecked = true) else null
                    }
                    com.example.graymatter.domain.ReferenceType.OPINION -> {
                        val op = opinionRepository.getOpinionById(link.targetId)
                        if (op != null) {
                            val resId = resourceEntriesStream.value.find { it.id == op.itemId }?.resourceId ?: op.itemId
                            com.example.graymatter.domain.ReferenceSelectorItem.DetailItem(id = op.id, snippet = stripMarkdown(op.text), resourceId = resId, typeLabel = "Opinion", isExpanded = false, isChecked = true)
                        } else null
                    }
                    com.example.graymatter.domain.ReferenceType.BOOKMARK -> {
                        val bookmark = resourceRepository.getBookmarkById(link.targetId)
                        if (bookmark != null) com.example.graymatter.domain.ReferenceSelectorItem.DetailItem(id = bookmark.id, snippet = bookmark.title ?: "Untitled", resourceId = bookmark.resourceId, typeLabel = "Bookmark", isExpanded = false, isChecked = true) else null
                    }
                    com.example.graymatter.domain.ReferenceType.ANNOTATION -> {
                        val op = opinionRepository.getOpinionById(link.targetId)
                        if (op != null) {
                            val resId = resourceEntriesStream.value.find { it.id == op.itemId }?.resourceId ?: op.itemId
                            com.example.graymatter.domain.ReferenceSelectorItem.DetailItem(id = op.id, snippet = stripMarkdown(op.text), resourceId = resId, typeLabel = "Opinion", isExpanded = false, isChecked = true)
                        } else null
                    }
                }
            }
        }
    }

    /**
     * Retrieves backlinks for a resource or topic.
     * Backlinks are links that point TO this target from opinions or resources (notes).
     */
    fun getResolvedBacklinksForTarget(targetId: String): Flow<List<com.example.graymatter.android.ui.components.BacklinkUiModel>> {
        return referenceLinkRepository.getBacklinksForTarget(targetId).map { links ->
            links.mapNotNull { link ->
                when (link.sourceType) {
                    com.example.graymatter.domain.ReferenceType.OPINION -> {
                        val opinion = opinionRepository.getOpinionById(link.sourceId)
                        if (opinion != null) {
                            val entry = resourceEntryRepository.getResourceEntryByResourceId(opinion.itemId) ?: resourceEntryRepository.getResourceEntryWithDetails(opinion.itemId)?.resourceEntry
                            val resourceDetails = entry?.let { resourceEntryRepository.getResourceEntryWithDetails(it.id) }
                            val resourceTitle = resourceDetails?.resource?.title ?: "Unknown Source"
                            
                            com.example.graymatter.android.ui.components.BacklinkUiModel(
                                id = link.sourceId,
                                title = "Linked from $resourceTitle",
                                subtitle = opinion.text.take(150),
                                sourceType = link.sourceType
                            )
                        } else null
                    }
                    com.example.graymatter.domain.ReferenceType.RESOURCE -> {
                        // A resource (note) links to this target
                        val resource = resourceRepository.getResourceById(link.sourceId)
                        if (resource != null) {
                            com.example.graymatter.android.ui.components.BacklinkUiModel(
                                id = link.sourceId,
                                title = "Referenced in ${resource.title ?: "Untitled Note"}",
                                subtitle = resource.extractedText?.take(150) ?: "",
                                sourceType = link.sourceType
                            )
                        } else null
                    }
                    else -> null
                }
            }
        }
    }

    private fun stripMarkdown(text: String): String {
        return text.replace(Regex("\\[TEMPLATE:[^\\]]*\\]"), "")
            .replace(Regex("\\[DICT\\]"), "")
            .replace(Regex("\\[CUSTOM: [^\\]]*\\]"), "")
            .replace(Regex("\\[Page \\d+\\]"), "")
            .replace(Regex("[#*>\\[\\]]"), "")
            .trim().replace(Regex("\\s+"), " ")
    }
}

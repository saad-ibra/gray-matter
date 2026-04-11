package com.example.graymatter.android.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.graymatter.android.util.FileUtils
import com.example.graymatter.data.ResourceEntryRepository
import com.example.graymatter.domain.ResourceType
import com.example.graymatter.domain.business.AutoLinkService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import java.util.Locale
import java.util.UUID

/**
 * ViewModel dedicated to the heavy logic of creating new Resource Entries 
 * from URLs, Text, or physical File importing.
 */
class DraftingViewModel(
    private val resourceEntryRepository: ResourceEntryRepository,
    private val autoLinkService: AutoLinkService
) : ViewModel() {

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private fun generateUuid(): String = UUID.randomUUID().toString()

    /**
     * Creates a new resource entry for a web link.
     */
    suspend fun createNewResourceEntry(
        url: String, 
        opinionText: String, 
        confidence: Int, 
        title: String? = null, 
        description: String? = null, 
        topicId: String? = null, 
        referenceLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList()
    ): String {
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
        opinionReferenceLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList()
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
            
            if (clean.endsWith("/")) clean = clean.dropLast(1)
            
            val parts = clean.split("/")
            var slug = parts.lastOrNull { it.isNotBlank() } ?: parts.first()
            
            slug = slug.replace("-", " ").replace("_", " ")
            
            if (slug.isNotEmpty()) {
                slug = slug.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
            
            slug
        } catch (e: Exception) {
            url
        }
    }
}

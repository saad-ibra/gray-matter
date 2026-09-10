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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

/**
 * ViewModel dedicated to the heavy logic of creating new Resource Entries 
 * from URLs, Text, or physical File importing.
 */
class DraftingViewModel(
    private val resourceEntryRepository: ResourceEntryRepository,
    private val autoLinkService: AutoLinkService,
    private val tagRepository: com.example.graymatter.data.TagRepository,
    private val opinionRepository: com.example.graymatter.data.OpinionRepository,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    data class OpinionDraft(
        val text: String,
        val confidence: Int,
        val imagePath: String?,
        val referenceLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList(),
        val tags: List<com.example.graymatter.domain.Tag> = emptyList()
    )

    enum class EntryType { LINK, FILE, NOTE }

    val entryType: StateFlow<EntryType> = savedStateHandle.getStateFlow("entryType", EntryType.LINK)

    val draftTitle: StateFlow<String> = savedStateHandle.getStateFlow("draftTitle", "")

    val draftUrl: StateFlow<String> = savedStateHandle.getStateFlow("draftUrl", "")

    val draftOpinion: StateFlow<String> = savedStateHandle.getStateFlow("draftOpinion", "")

    val draftNoteContent: StateFlow<String> = savedStateHandle.getStateFlow("draftNoteContent", "")

    val draftDescription: StateFlow<String> = savedStateHandle.getStateFlow("draftDescription", "")

    val draftConfidence: StateFlow<Float> = savedStateHandle.getStateFlow("draftConfidence", 0f)

    val draftImagePath: StateFlow<String?> = savedStateHandle.getStateFlow("draftImagePath", null)

    fun updateEntryType(type: EntryType) { savedStateHandle["entryType"] = type }
    fun updateTitle(title: String) { savedStateHandle["draftTitle"] = title }
    
    fun updateUrl(url: String) {
        savedStateHandle["draftUrl"] = url
        val currentTitle = savedStateHandle.get<String>("draftTitle") ?: ""
        if (currentTitle.isEmpty() && url.isNotBlank()) {
            val inferred = inferTitleFromUrl(url)
            if (inferred.isNotBlank()) {
                savedStateHandle["draftTitle"] = inferred
            }
        }
    }

    private fun inferTitleFromUrl(url: String): String {
        return try {
            var clean = url
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
            
            if (clean.endsWith("/")) clean = clean.dropLast(1)
            
            // Remove query parameters and fragments
            clean = clean.substringBefore("?").substringBefore("#")
            
            val parts = clean.split("/").filter { it.isNotBlank() }
            if (parts.isEmpty()) return ""

            // Common non-title segments to ignore
            val ignoreKeywords = setOf(
                "articleshow", "article", "post", "blog", "news", "story", "p", "id", 
                "view", "details", "html", "php", "cms", "aspx", "category", "tag", "archives"
            )

            var slug = ""
            
            // Iterate backwards to find the most descriptive part
            for (i in parts.indices.reversed()) {
                val part = parts[i].lowercase()
                
                // Skip domain names (first part usually)
                if (i == 0 && parts.size > 1) continue
                
                // Skip technical IDs or short noise
                if (part.all { it.isDigit() || it == '.' } || 
                    part.length < 4 || 
                    ignoreKeywords.contains(part.substringBeforeLast(".")) ||
                    part.contains("index.")
                ) continue
                
                // If it has hyphens or underscores, it's likely the title slug
                if (part.contains("-") || part.contains("_")) {
                    slug = parts[i]
                    break
                }
                
                // Fallback to the first non-ignored part from the end
                if (slug.isEmpty()) {
                    slug = parts[i]
                }
            }
            
            if (slug.isEmpty()) slug = parts.last()

            // Final cleanup
            var formatted = slug
                .substringBeforeLast(".cms")
                .substringBeforeLast(".html")
                .substringBeforeLast(".php")
                .substringBeforeLast(".htm")
                .replace("-", " ")
                .replace("_", " ")
                .replace(Regex("\\s+"), " ") // Double spaces
                .trim()
            
            // Robust formatting: Title Case
            formatted = formatted.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) java.util.Locale.getDefault().let { loc -> it.titlecase(loc) } else it.toString() }
            }
            
            formatted
        } catch (e: Exception) {
            ""
        }
    }
    fun updateOpinion(opinion: String) { savedStateHandle["draftOpinion"] = opinion }
    fun updateNoteContent(content: String) { savedStateHandle["draftNoteContent"] = content }
    fun updateDescription(desc: String) { savedStateHandle["draftDescription"] = desc }
    fun updateConfidence(confidence: Float) { savedStateHandle["draftConfidence"] = confidence }
    fun updateImagePath(path: String?) { savedStateHandle["draftImagePath"] = path }

    fun resetDraft() {
        savedStateHandle["draftTitle"] = ""
        savedStateHandle["draftUrl"] = ""
        savedStateHandle["draftOpinion"] = ""
        savedStateHandle["draftNoteContent"] = ""
        savedStateHandle["draftDescription"] = ""
        savedStateHandle["draftConfidence"] = 0f
        savedStateHandle["draftImagePath"] = null
    }

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private fun generateUuid(): String = UUID.randomUUID().toString()

    /**
     * Creates a new resource entry for a web link.
     */
    suspend fun createNewResourceEntry(
        url: String, 
        opinions: List<OpinionDraft>, 
        title: String? = null, 
        description: String? = null, 
        topicId: String? = null
    ): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val resourceId = generateUuid()
        val resourceEntryId = generateUuid()
        val firstOpinion = opinions.firstOrNull()
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
            opinionText = firstOpinion?.text ?: "",
            confidence = firstOpinion?.confidence ?: 50,
            now = now,
            imagePath = firstOpinion?.imagePath
        )
        
        if (topicId != null) {
            resourceEntryRepository.updateResourceEntryTopic(resourceEntryId, topicId)
        }
        
        if (firstOpinion != null) {
            autoLinkService.syncLinks(opinionId, com.example.graymatter.domain.ReferenceType.OPINION, firstOpinion.text, firstOpinion.referenceLinks)
            
            firstOpinion.tags.forEach { tag ->
                tagRepository.addTagToEntry(
                    id = generateUuid(),
                    entryId = opinionId,
                    entryType = "OPINION",
                    tagId = tag.id,
                    createdAt = now
                )
            }
        }
        
        // Save subsequent opinions
        if (opinions.size > 1) {
            saveSubsequentOpinions(resourceEntryId, opinions.drop(1), now)
        }
        
        return resourceEntryId
    }

    private suspend fun saveSubsequentOpinions(resourceEntryId: String, subsequentOpinions: List<OpinionDraft>, now: Long) {
        for (opinionDraft in subsequentOpinions) {
            val opId = generateUuid()
            val op = com.example.graymatter.domain.Opinion(
                id = opId,
                itemId = resourceEntryId,
                text = opinionDraft.text,
                confidenceScore = opinionDraft.confidence,
                imagePath = opinionDraft.imagePath,
                createdAt = now,
                updatedAt = now
            )
            opinionRepository.saveOpinion(op)
            
            autoLinkService.syncLinks(opId, com.example.graymatter.domain.ReferenceType.OPINION, opinionDraft.text, opinionDraft.referenceLinks)
            
            opinionDraft.tags.forEach { tag ->
                tagRepository.addTagToEntry(
                    id = generateUuid(),
                    entryId = opId,
                    entryType = "OPINION",
                    tagId = tag.id,
                    createdAt = now
                )
            }
        }
    }

    /**
     * Creates a new Note resource entry. Saved with Markdown content in internal storage.
     */
    suspend fun createNewNote(
        context: Context, 
        title: String, 
        content: String, 
        opinions: List<OpinionDraft>, 
        description: String? = null, 
        topicId: String? = null, 
        referenceLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList()
    ): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val resourceId = generateUuid()
        val resourceEntryId = generateUuid()
        val firstOpinion = opinions.firstOrNull()
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
            opinionText = firstOpinion?.text ?: "",
            confidence = firstOpinion?.confidence ?: 50,
            now = now,
            imagePath = firstOpinion?.imagePath
        )
        
        if (topicId != null) {
            resourceEntryRepository.updateResourceEntryTopic(resourceEntryId, topicId)
        }
        
        // Save note-level links as RESOURCE type (extracted from content)
        autoLinkService.syncLinks(resourceId, com.example.graymatter.domain.ReferenceType.RESOURCE, content, referenceLinks)
        
        if (firstOpinion != null) {
            // Save opinion-level links as OPINION type (extracted from first opinion)
            autoLinkService.syncLinks(opinionId, com.example.graymatter.domain.ReferenceType.OPINION, firstOpinion.text, firstOpinion.referenceLinks)
            
            firstOpinion.tags.forEach { tag ->
                tagRepository.addTagToEntry(
                    id = generateUuid(),
                    entryId = opinionId,
                    entryType = "OPINION",
                    tagId = tag.id,
                    createdAt = now
                )
            }
        }
        
        // Save subsequent opinions
        if (opinions.size > 1) {
            saveSubsequentOpinions(resourceEntryId, opinions.drop(1), now)
        }
        
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
        opinions: List<OpinionDraft>,
        title: String? = null,
        description: String? = null,
        topicId: String? = null
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
            val firstOpinion = opinions.firstOrNull()
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
                opinionText = firstOpinion?.text ?: "",
                confidence = firstOpinion?.confidence ?: 50,
                now = now,
                imagePath = firstOpinion?.imagePath
            )
            
            if (topicId != null) {
                resourceEntryRepository.updateResourceEntryTopic(resourceEntryId, topicId)
            }
            
            if (firstOpinion != null) {
                autoLinkService.syncLinks(opinionId, com.example.graymatter.domain.ReferenceType.OPINION, firstOpinion.text, firstOpinion.referenceLinks)

                firstOpinion.tags.forEach { tag ->
                    tagRepository.addTagToEntry(
                        id = generateUuid(),
                        entryId = opinionId,
                        entryType = "OPINION",
                        tagId = tag.id,
                        createdAt = now
                    )
                }
            }
            
            // Save subsequent opinions
            if (opinions.size > 1) {
                saveSubsequentOpinions(resourceEntryId, opinions.drop(1), now)
            }

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

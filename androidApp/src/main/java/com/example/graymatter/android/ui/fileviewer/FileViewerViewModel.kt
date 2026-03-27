package com.example.graymatter.android.ui.fileviewer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.graymatter.data.ResourceRepository
import com.example.graymatter.data.OpinionRepository
import com.example.graymatter.data.ItemRepository
import com.example.graymatter.domain.CustomTemplate
import com.example.graymatter.domain.Bookmark
import com.example.graymatter.domain.ChapterOutline
import com.example.graymatter.domain.ReadingProgress
import com.example.graymatter.domain.ReadingSettings
import com.example.graymatter.domain.Resource
import com.example.graymatter.domain.ResourceType
import com.example.graymatter.domain.Opinion
import com.example.graymatter.domain.SearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.coroutines.flow.first
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * ViewModel for the File Viewer screen.
 */
class FileViewerViewModel(
    private val resourceRepository: ResourceRepository,
    private val opinionRepository: OpinionRepository,
    private val itemRepository: ItemRepository,
    private val referenceLinkRepository: com.example.graymatter.data.ReferenceLinkRepository
) : ViewModel() {

    private val _resource = MutableStateFlow<Resource?>(null)
    val resource: StateFlow<Resource?> = _resource.asStateFlow()

    var currentPage by mutableStateOf(0)
        private set
    var totalPages by mutableStateOf(1)
        private set
    var currentChapterName by mutableStateOf<String?>(null)
        private set

    private val _chapters = MutableStateFlow<List<ChapterOutline>>(emptyList())
    val chapters: StateFlow<List<ChapterOutline>> = _chapters.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    private val _opinions = MutableStateFlow<List<Opinion>>(emptyList())
    val opinions: StateFlow<List<Opinion>> = _opinions.asStateFlow()

    private val _templates = MutableStateFlow<List<CustomTemplate>>(emptyList())
    val templates: StateFlow<List<CustomTemplate>> = _templates.asStateFlow()

    private val _settings = MutableStateFlow(ReadingSettings(resourceId = ""))
    val settings: StateFlow<ReadingSettings> = _settings.asStateFlow()

    var showTopBar by mutableStateOf(true)
        private set
    var showBottomBar by mutableStateOf(true)
        private set
    var showSettingsSheet by mutableStateOf(false)
        private set
    var showBookmarksSheet by mutableStateOf(false)
        private set
    var showChaptersSheet by mutableStateOf(false)
        private set
    
    var showBookmarkDialog by mutableStateOf(false)
        private set

    var showSelectionAnnotationDialog by mutableStateOf(false)
        private set
    var selectedText by mutableStateOf<String?>(null)
        private set

    // Add Entry state
    var showAddEntrySheet by mutableStateOf(false)
        private set
    var showCustomOpinionDialog by mutableStateOf(false)
        private set
    var showTemplateSelectionDialog by mutableStateOf(false)
        private set
    var selectedTemplateForNewEntry by mutableStateOf<CustomTemplate?>(null)
        private set

    fun toggleAddEntrySheet() { showAddEntrySheet = !showAddEntrySheet }
    fun toggleCustomOpinionDialog() { showCustomOpinionDialog = !showCustomOpinionDialog }
    fun toggleTemplateSelectionDialog() { showTemplateSelectionDialog = !showTemplateSelectionDialog }
    fun selectTemplateForNewEntry(template: CustomTemplate?) { selectedTemplateForNewEntry = template }

    // Search state
    var showSearchPanel by mutableStateOf(false)
        private set
    var searchQuery by mutableStateOf("")
        private set
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()
    var currentSearchIndex by mutableStateOf(0)
        private set
    // Character offset in text to scroll to (for text-based viewers)
    var scrollToMatchOffset by mutableStateOf(-1)
        private set

    private val _pageTextMap = mutableMapOf<Int, String>()

    fun loadResource(resourceId: String, initialPage: Int? = null) {
        viewModelScope.launch {
            val res = resourceRepository.getResourceById(resourceId) ?: return@launch
            _resource.value = res

            val progress = resourceRepository.getReadingProgress(resourceId)
            
            // Set page synchronously before views load
            if (initialPage != null && initialPage >= 0) {
                currentPage = initialPage
            } else if (progress != null) {
                currentPage = progress.currentPage
            }
            
            if (progress != null) {
                totalPages = progress.totalPages
                currentChapterName = progress.currentChapter
            }

            val savedSettings = resourceRepository.getReadingSettings(resourceId)
            _settings.value = savedSettings ?: ReadingSettings(resourceId = resourceId)

            _bookmarks.value = resourceRepository.getBookmarks(resourceId)

            val item = itemRepository.getItemByResourceId(res.id)
            if (item != null) {
                viewModelScope.launch {
                    opinionRepository.getOpinionsByItemId(item.id).collect { list ->
                        _opinions.value = list
                    }
                }
                viewModelScope.launch {
                    resourceRepository.templatesStream.collect { list ->
                        _templates.value = list
                    }
                }
            }

            _pageTextMap.clear()
            
            // Trigger text extraction if missing or if PDF needs page map
            if (res.extractedText == null && res.filePath != null) {
                extractResourceText(res)
            } else if (res.extractedText != null) {
                if (res.type == ResourceType.PDF) {
                    extractResourceText(res) // We need to rebuild the page map
                } else {
                    // If we have aggregated text but no page map, for single-page types it's fine
                    _pageTextMap[0] = res.extractedText!!
                }
            }
        }
    }

    private fun extractResourceText(res: Resource) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val path = res.filePath ?: return@launch
            when (res.type) {
                ResourceType.MARKDOWN -> {
                    val text = try { File(path).readText() } catch (e: Exception) { "" }
                    _pageTextMap[0] = text
                    resourceRepository.updateResourceText(res.id, text)
                }
                ResourceType.PDF -> {
                    try {
                        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(File(path))
                        val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
                        
                        val total = document.numberOfPages
                        val fullTextBuilder = StringBuilder()
                        for (i in 0 until total) {
                            stripper.startPage = i + 1
                            stripper.endPage = i + 1
                            val text = stripper.getText(document) ?: ""
                            _pageTextMap[i] = text
                            if (res.extractedText == null) {
                                fullTextBuilder.append(text).append("\n")
                            }
                        }
                        
                        if (res.extractedText == null) {
                            val fullText = fullTextBuilder.toString()
                            resourceRepository.updateResourceText(res.id, fullText)
                            _resource.value = res.copy(extractedText = fullText)
                        }
                        
                        document.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                else -> {}
            }
        }
    }

    /**
     * Updates the content of a resource (Markdown).
     */
    fun updateResourceText(newText: String, referenceLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList()) {
        val res = _resource.value ?: return
        viewModelScope.launch {
            resourceRepository.updateResourceText(res.id, newText)
            _resource.value = res.copy(extractedText = newText)
            _pageTextMap[0] = newText
            
            if (referenceLinks.isNotEmpty()) {
                saveReferenceLinksInternal(res.id, com.example.graymatter.domain.ReferenceType.RESOURCE, referenceLinks)
            }
        }
    }

    fun onPageChanged(page: Int, total: Int) {
        currentPage = page
        totalPages = total
        updateChapterInfo()
        saveProgress()
    }

    fun updatePageCount(total: Int) {
        totalPages = total
        updateChapterInfo()
        saveProgress()
    }

    fun setChapters(chapters: List<ChapterOutline>) {
        _chapters.value = chapters
        updateChapterInfo()
    }

    private fun updateChapterInfo() {
        val chapterList = _chapters.value
        if (chapterList.isEmpty()) return

        val flatChapters = flattenChapters(chapterList)
        val current = flatChapters.lastOrNull { it.targetPage <= currentPage }
        currentChapterName = current?.title
    }

    private fun flattenChapters(chapters: List<ChapterOutline>): List<ChapterOutline> {
        val result = mutableListOf<ChapterOutline>()
        for (chapter in chapters) {
            result.add(chapter)
            result.addAll(flattenChapters(chapter.children))
        }
        return result
    }

    fun jumpToPage(page: Int) {
        currentPage = page.coerceIn(0, totalPages - 1)
        updateChapterInfo()
        saveProgress()
    }

    fun jumpToChapter(chapter: ChapterOutline) {
        jumpToPage(chapter.targetPage)
        showChaptersSheet = false
    }

    fun nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++
            updateChapterInfo()
            saveProgress()
        }
    }

    fun previousPage() {
        if (currentPage > 0) {
            currentPage--
            updateChapterInfo()
            saveProgress()
        }
    }

    fun toggleBars() {
        val show = !showTopBar
        showTopBar = show
        showBottomBar = show
    }

    fun toggleSettingsSheet() { showSettingsSheet = !showSettingsSheet }
    fun toggleBookmarksSheet() { showBookmarksSheet = !showBookmarksSheet }
    fun toggleChaptersSheet() { showChaptersSheet = !showChaptersSheet }
    
    fun openBookmarkDialog() {
        showBookmarkDialog = true
    }
    
    fun closeBookmarkDialog() {
        showBookmarkDialog = false
    }
    
    fun onTextSelected(action: String, text: String, x: Float = 0f, y: Float = 0f) {
        if (action == "annotate") {
            selectedText = text
            showSelectionAnnotationDialog = true
        }
    }

    fun closeSelectionAnnotationDialog() {
        showSelectionAnnotationDialog = false
        selectedText = null
    }

    fun closePanels() {
        showSearchPanel = false
        showSettingsSheet = false
        showBookmarksSheet = false
    }

    // -- Search --
    fun toggleSearchPanel() {
        showSearchPanel = !showSearchPanel
        if (!showSearchPanel) {
            searchQuery = ""
            _searchResults.value = emptyList()
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery = query
        if (query.length < 3) {
            _searchResults.value = emptyList()
            return
        }
        
        val results = mutableListOf<SearchResult>()
        
        // Search across all pages in the map
        _pageTextMap.forEach { (pageIndex, text) ->
            var index = text.indexOf(query, ignoreCase = true)
            while (index != -1) {
                val ctxStart = (index - 30).coerceAtLeast(0)
                val ctxEnd = (index + query.length + 30).coerceAtMost(text.length)
                val snippet = text.substring(ctxStart, ctxEnd).replace("\n", " ")
                
                results.add(SearchResult(
                    page = pageIndex,
                    snippet = "...$snippet...",
                    matchStart = index
                ))
                index = text.indexOf(query, index + 1, ignoreCase = true)
            }
        }
        
        _searchResults.value = results
        currentSearchIndex = 0
        // Auto-scroll/jump to first result if found
        if (results.isNotEmpty()) {
            val first = results[0]
            jumpToPage(first.page)
            scrollToMatchOffset = first.matchStart
        }
    }

    fun nextSearchResult() {
        if (_searchResults.value.isNotEmpty()) {
            currentSearchIndex = (currentSearchIndex + 1) % _searchResults.value.size
            jumpToSearchResult(_searchResults.value[currentSearchIndex])
        }
    }

    fun previousSearchResult() {
        if (_searchResults.value.isNotEmpty()) {
            currentSearchIndex = if (currentSearchIndex > 0) currentSearchIndex - 1 else _searchResults.value.size - 1
            jumpToSearchResult(_searchResults.value[currentSearchIndex])
        }
    }

    fun jumpToSearchResult(result: SearchResult) {
        jumpToPage(result.page)
        // For text-based viewers, also scroll to the match offset
        scrollToMatchOffset = result.matchStart
    }

    fun clearScrollToMatch() {
        scrollToMatchOffset = -1
    }

    // -- Bookmarks & Opinions --
    fun saveBookmarkAndOpinion(opinionText: String, confidence: Int, referenceLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList()) {
        val res = _resource.value ?: return
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val bookmarkId = generateUuid()
            
            val bookmark = Bookmark(
                id = bookmarkId,
                resourceId = res.id,
                page = currentPage,
                percentPosition = if (totalPages > 0) (currentPage + 1).toDouble() / totalPages else 0.0,
                title = "Page ${currentPage + 1}",
                opinion = opinionText,
                confidenceScore = confidence,
                createdAt = now
            )
            resourceRepository.saveBookmark(bookmark)
            saveReferenceLinksInternal(bookmarkId, com.example.graymatter.domain.ReferenceType.BOOKMARK, referenceLinks)
            
            val item = itemRepository.getItemByResourceId(res.id)
            if (item != null) {
                val opinionId = generateUuid()
                val opinion = Opinion(
                    id = opinionId,
                    itemId = item.id,
                    text = "[Page ${currentPage + 1}] $opinionText",
                    confidenceScore = confidence,
                    pageNumber = currentPage,
                    createdAt = now,
                    updatedAt = now
                )
                opinionRepository.saveOpinion(opinion)
                saveReferenceLinksInternal(opinionId, com.example.graymatter.domain.ReferenceType.OPINION, referenceLinks)
                itemRepository.updateItemOpinionMetadata(item.id, now)
            }
            
            _bookmarks.value = resourceRepository.getBookmarks(res.id)
            showBookmarkDialog = false
        }
    }

    fun saveAnnotation(opinionText: String, confidence: Int, referenceLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList()) {
        val res = _resource.value ?: return
        val textSnippet = selectedText ?: return

        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val item = itemRepository.getItemByResourceId(res.id)
            
            if (item != null) {
                // Add the snippet inline as a blockquote
                val fullOpinion = "> $textSnippet\n\n$opinionText"
                val opinionId = generateUuid()
                
                val opinion = Opinion(
                    id = opinionId,
                    itemId = item.id,
                    text = fullOpinion,
                    confidenceScore = confidence,
                    pageNumber = currentPage,
                    createdAt = now,
                    updatedAt = now
                )
                opinionRepository.saveOpinion(opinion)
                saveReferenceLinksInternal(opinionId, com.example.graymatter.domain.ReferenceType.OPINION, referenceLinks)
                itemRepository.updateItemOpinionMetadata(item.id, now)
            }
            showSelectionAnnotationDialog = false
            selectedText = null
        }
    }

    fun saveDictionaryEntry(phrase: String, existingOpinionId: String? = null) {
        val res = _resource.value ?: return
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val item = itemRepository.getItemByResourceId(res.id)
            if (item != null) {
                val cleanPhrase = phrase.trim()
                val existing = _opinions.value.find { 
                    (existingOpinionId != null && it.id == existingOpinionId) ||
                    (it.text.startsWith("[DICT] ") && 
                    it.text.removePrefix("[DICT] ").trim().equals(cleanPhrase, ignoreCase = true))
                }
                
                if (existing != null) {
                    // Prevent duplicate by updating the existing one's timestamp
                    val updatedOpinion = existing.copy(
                        createdAt = now,
                        updatedAt = now,
                        pageNumber = currentPage
                    )
                    opinionRepository.updateOpinion(updatedOpinion)
                } else {
                    val opinion = Opinion(
                        id = generateUuid(),
                        itemId = item.id,
                        text = "[DICT] $cleanPhrase",
                        confidenceScore = 100, // Dictionary entries are high confidence by default
                        pageNumber = currentPage,
                        createdAt = now,
                        updatedAt = now
                    )
                    opinionRepository.saveOpinion(opinion)
                }
                itemRepository.updateItemOpinionMetadata(item.id, now)
            }
        }
    }

    fun updateAnnotation(id: String, text: String, confidence: Int, referenceLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList()) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val existing = opinionRepository.getOpinionById(id) ?: return@launch
            opinionRepository.updateOpinion(
                existing.copy(
                    text = text,
                    confidenceScore = confidence,
                    updatedAt = now
                )
            )
            saveReferenceLinksInternal(id, com.example.graymatter.domain.ReferenceType.OPINION, referenceLinks)
        }
    }

    fun deleteAnnotation(id: String) {
        viewModelScope.launch {
            opinionRepository.deleteOpinion(id)
        }
    }

    fun saveGeneralOpinion(content: String, confidence: Int, referenceLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList()) {
        val res = _resource.value ?: return
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val item = itemRepository.getItemByResourceId(res.id)
            if (item != null) {
                val opinionId = generateUuid()
                val opinion = Opinion(
                    id = opinionId,
                    itemId = item.id,
                    text = content,
                    confidenceScore = confidence,
                    pageNumber = null,
                    createdAt = now,
                    updatedAt = now
                )
                opinionRepository.saveOpinion(opinion)
                saveReferenceLinksInternal(opinionId, com.example.graymatter.domain.ReferenceType.OPINION, referenceLinks)
                itemRepository.updateItemOpinionMetadata(item.id, now)
            }
        }
    }

    fun saveTemplateOpinion(formattedText: String, confidence: Int, referenceLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList()) {
        val res = _resource.value ?: return
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val item = itemRepository.getItemByResourceId(res.id)
            if (item != null) {
                val opinionId = generateUuid()
                val opinion = Opinion(
                    id = opinionId,
                    itemId = item.id,
                    text = formattedText, // text is exactly formatted in UI
                    confidenceScore = confidence,
                    pageNumber = null,
                    createdAt = now,
                    updatedAt = now
                )
                opinionRepository.saveOpinion(opinion)
                saveReferenceLinksInternal(opinionId, com.example.graymatter.domain.ReferenceType.OPINION, referenceLinks)
                itemRepository.updateItemOpinionMetadata(item.id, now)
            }
        }
    }

    fun removeBookmark(bookmarkId: String) {
        val res = _resource.value ?: return
        viewModelScope.launch {
            val bookmark = resourceRepository.getBookmarkById(bookmarkId)

            if (bookmark != null) {
                val item = itemRepository.getItemByResourceId(res.id)
                if (item != null) {
                    // Collect the first list of opinions from the Flow
                    val opinions = opinionRepository.getOpinionsByItemId(item.id).first()

                    val matchingOpinion = opinions.firstOrNull {
                        it.createdAt == bookmark.createdAt ||
                                (it.pageNumber == bookmark.page && (bookmark.opinion != null && it.text.contains(bookmark.opinion!!)))
                    }
                    if (matchingOpinion != null) {
                        opinionRepository.deleteOpinion(matchingOpinion.id)
                    }
                }
            }

            // Delete the bookmark and refresh the local state
            resourceRepository.deleteBookmark(bookmarkId)
            _bookmarks.value = resourceRepository.getBookmarks(res.id)
        }
    }

    // -- Settings --
    fun updateSettings(update: ReadingSettings.() -> ReadingSettings) {
        val current = _settings.value
        val updated = current.update()
        _settings.value = updated
        viewModelScope.launch {
            resourceRepository.updateReadingSettings(updated)
        }
    }

    // -- Persistence --
    private fun saveProgress() {
        val res = _resource.value ?: return
        viewModelScope.launch {
            val progressPercent = if (totalPages > 0) (currentPage + 1).toDouble() / totalPages else 0.0
            
            val progress = ReadingProgress(
                resourceId = res.id,
                currentPage = currentPage,
                totalPages = totalPages,
                percentComplete = progressPercent,
                currentChapter = currentChapterName,
                lastOpenedAt = Clock.System.now().toEpochMilliseconds()
            )
            resourceRepository.updateReadingProgress(progress)
        }
    }

    fun saveProgressOnExit() {
        saveProgress()
    }

    /**
     * Retrieves reference links for a resource.
     */
    fun getLinksForResource(resourceId: String): Flow<List<com.example.graymatter.domain.ReferenceSelectorItem>> {
        return resolveLinksForSource(resourceId)
    }

    /**
     * Shared helper to resolve reference links for any source ID.
     */
    private fun resolveLinksForSource(sourceId: String): Flow<List<com.example.graymatter.domain.ReferenceSelectorItem>> {
        return referenceLinkRepository.getReferenceLinksBySource(sourceId).map { links ->
            links.mapNotNull { link ->
                when (link.targetType) {
                    com.example.graymatter.domain.ReferenceType.TOPIC -> {
                        val topic = (resourceRepository as? com.example.graymatter.data.DefaultResourceRepository)?.let { null } // We need topicRepository here too
                        // Actually, looking at what's available...
                        null // Placeholder as FileViewerViewModel doesn't have topicRepository
                    }
                    com.example.graymatter.domain.ReferenceType.RESOURCE -> {
                        val resource = resourceRepository.getResourceById(link.targetId)
                        if (resource != null) com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem(id = resource.id, title = resource.title ?: "Untitled", type = resource.type.name, parentTopicId = null, isExpanded = false, isChecked = true) else null
                    }
                    com.example.graymatter.domain.ReferenceType.OPINION -> {
                        val op = opinionRepository.getOpinionById(link.targetId)
                        if (op != null) com.example.graymatter.domain.ReferenceSelectorItem.DetailItem(id = op.id, snippet = op.text, resourceId = op.itemId, typeLabel = "Opinion", isExpanded = false, isChecked = true) else null
                    }
                    com.example.graymatter.domain.ReferenceType.BOOKMARK -> {
                        val bookmark = resourceRepository.getBookmarkById(link.targetId)
                        if (bookmark != null) com.example.graymatter.domain.ReferenceSelectorItem.DetailItem(id = bookmark.id, snippet = bookmark.title ?: "Untitled", resourceId = bookmark.resourceId, typeLabel = "Bookmark", isExpanded = false, isChecked = true) else null
                    }
                    else -> null
                }
            }
        }
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

    private fun generateUuid(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..32).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}

package com.example.graymatter.android.ui.fileviewer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.graymatter.data.ResourceRepository
import com.example.graymatter.data.OpinionRepository
import com.example.graymatter.data.ItemRepository
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

/**
 * ViewModel for the File Viewer screen.
 */
class FileViewerViewModel(
    private val resourceRepository: ResourceRepository,
    private val opinionRepository: OpinionRepository,
    private val itemRepository: ItemRepository
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
            }

            // Trigger text extraction if missing
            if (res.extractedText == null && res.filePath != null) {
                extractResourceText(res)
            } else if (res.extractedText != null) {
                // If we have aggregated text but no page map, for single-page types it's fine
                _pageTextMap[0] = res.extractedText!!
            }
        }
    }

    private fun extractResourceText(res: Resource) {
        viewModelScope.launch {
            val path = res.filePath ?: return@launch
            when (res.type) {
                ResourceType.MARKDOWN -> {
                    val text = try { File(path).readText() } catch (e: Exception) { "" }
                    _pageTextMap[0] = text
                    resourceRepository.updateResourceText(res.id, text)
                }
                else -> {}
            }
        }
    }

    /**
     * Updates the content of a resource (Markdown).
     */
    fun updateResourceText(newText: String) {
        val res = _resource.value ?: return
        viewModelScope.launch {
            resourceRepository.updateResourceText(res.id, newText)
            _resource.value = res.copy(extractedText = newText)
            _pageTextMap[0] = newText
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
    fun saveBookmarkAndOpinion(opinionText: String, confidence: Int) {
        val res = _resource.value ?: return
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            
            val bookmark = Bookmark(
                id = generateUuid(),
                resourceId = res.id,
                page = currentPage,
                percentPosition = if (totalPages > 0) (currentPage + 1).toDouble() / totalPages else 0.0,
                title = "Page ${currentPage + 1}",
                opinion = opinionText,
                confidenceScore = confidence,
                createdAt = now
            )
            resourceRepository.saveBookmark(bookmark)
            
            val item = itemRepository.getItemByResourceId(res.id)
            if (item != null) {
                val opinion = Opinion(
                    id = generateUuid(),
                    itemId = item.id,
                    text = "[Page ${currentPage + 1}] $opinionText",
                    confidenceScore = confidence,
                    pageNumber = currentPage,
                    createdAt = now,
                    updatedAt = now
                )
                opinionRepository.saveOpinion(opinion)
                itemRepository.updateItemOpinionMetadata(item.id, now)
            }
            
            _bookmarks.value = resourceRepository.getBookmarks(res.id)
            showBookmarkDialog = false
        }
    }

    fun saveAnnotation(opinionText: String, confidence: Int) {
        val res = _resource.value ?: return
        val textSnippet = selectedText ?: return

        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val item = itemRepository.getItemByResourceId(res.id)
            
            if (item != null) {
                // Add the snippet inline as a blockquote
                val fullOpinion = "> $textSnippet\n\n$opinionText"
                
                val opinion = Opinion(
                    id = generateUuid(),
                    itemId = item.id,
                    text = fullOpinion,
                    confidenceScore = confidence,
                    pageNumber = currentPage,
                    createdAt = now,
                    updatedAt = now
                )
                opinionRepository.saveOpinion(opinion)
                itemRepository.updateItemOpinionMetadata(item.id, now)
            }
            showSelectionAnnotationDialog = false
            selectedText = null
        }
    }

    fun saveDictionaryEntry(phrase: String) {
        val res = _resource.value ?: return
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val item = itemRepository.getItemByResourceId(res.id)
            if (item != null) {
                val opinion = Opinion(
                    id = generateUuid(),
                    itemId = item.id,
                    text = "[DICT] $phrase",
                    confidenceScore = 100, // Dictionary entries are high confidence by default
                    pageNumber = currentPage,
                    createdAt = now,
                    updatedAt = now
                )
                opinionRepository.saveOpinion(opinion)
                itemRepository.updateItemOpinionMetadata(item.id, now)
            }
        }
    }

    fun updateAnnotation(id: String, text: String, confidence: Int) {
        viewModelScope.launch {
            val existing = opinionRepository.getOpinionById(id) ?: return@launch
            opinionRepository.updateOpinion(
                existing.copy(
                    text = text,
                    confidenceScore = confidence,
                    updatedAt = Clock.System.now().toEpochMilliseconds()
                )
            )
        }
    }

    fun deleteAnnotation(id: String) {
        viewModelScope.launch {
            opinionRepository.deleteOpinion(id)
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

    private fun generateUuid(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..32).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}

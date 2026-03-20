package com.example.graymatter.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.example.graymatter.database.GrayMatterDatabase
import com.example.graymatter.database.BookmarkEntity
import com.example.graymatter.database.CustomTemplateEntity
import com.example.graymatter.database.ReadingProgressEntity
import com.example.graymatter.database.ReadingSettingsEntity
import com.example.graymatter.database.ResourceEntity
import com.example.graymatter.domain.Bookmark
import com.example.graymatter.domain.CustomTemplate
import com.example.graymatter.domain.ReadingProgress
import com.example.graymatter.domain.ReadingSettings
import com.example.graymatter.domain.Resource
import com.example.graymatter.domain.ResourceType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultResourceRepository(
    private val database: GrayMatterDatabase,
    private val dispatcher: CoroutineDispatcher
) : ResourceRepository {

    private val queries = database.grayMatterDatabaseQueries

    override val resourcesStream: Flow<List<Resource>> = queries
        .getAllResources()
        .asFlow()
        .mapToList(dispatcher)
        .map { entities -> entities.map { it.toResource() } }

    override suspend fun getResourceById(id: String): Resource? = withContext(dispatcher) {
        queries.getResourceById(id).executeAsOneOrNull()?.toResource()
    }

    override suspend fun saveResource(resource: Resource) = withContext(dispatcher) {
        queries.insertResource(
            id = resource.id,
            type = resource.type.name,
            url = resource.url,
            filePath = resource.filePath,
            extractedText = resource.extractedText,
            title = resource.title,
            createdAt = resource.createdAt
        )
    }

    override suspend fun deleteResource(id: String) = withContext(dispatcher) {
        queries.deleteResource(id)
    }

    override suspend fun searchResources(query: String): List<Resource> = withContext(dispatcher) {
        // Using % wildcards if not already handled in the SQL file
        val searchQuery = "%$query%"
        queries.searchResources(searchQuery, searchQuery, searchQuery).executeAsList().map { it.toResource() }
    }

    override suspend fun updateResourceTitle(id: String, title: String) = withContext(dispatcher) {
        queries.updateResourceTitle(title, id)
    }

    override suspend fun updateResourceText(id: String, text: String) = withContext(dispatcher) {
        queries.updateResourceExtractedText(text, id)
    }

    // -- Reading Progress --

    override fun getReadingProgressStream(resourceId: String): Flow<ReadingProgress?> = queries
        .getReadingProgressByResourceId(resourceId)
        .asFlow()
        .mapToOneOrNull(dispatcher)
        .map { it?.toReadingProgress() }

    override suspend fun getReadingProgress(resourceId: String): ReadingProgress? = withContext(dispatcher) {
        queries.getReadingProgressByResourceId(resourceId).executeAsOneOrNull()?.toReadingProgress()
    }

    override suspend fun updateReadingProgress(progress: ReadingProgress) = withContext(dispatcher) {
        queries.insertOrUpdateReadingProgress(
            resourceId = progress.resourceId,
            currentPage = progress.currentPage.toLong(),
            totalPages = progress.totalPages.toLong(),
            percentComplete = progress.percentComplete,
            currentChapter = progress.currentChapter,
            lastOpenedAt = progress.lastOpenedAt
        )
    }

    override fun getLastOpenedProgressStream(): Flow<ReadingProgress?> = queries
        .getLastOpenedProgress()
        .asFlow()
        .mapToOneOrNull(dispatcher)
        .map { it?.toReadingProgress() }

    override suspend fun getLastOpenedProgress(): ReadingProgress? = withContext(dispatcher) {
        queries.getLastOpenedProgress().executeAsOneOrNull()?.toReadingProgress()
    }

    override suspend fun getAllReadingProgress(): List<ReadingProgress> = withContext(dispatcher) {
        queries.getAllReadingProgress().executeAsList().map { it.toReadingProgress() }
    }

    override suspend fun deleteReadingProgress(resourceId: String) = withContext(dispatcher) {
        queries.deleteReadingProgress(resourceId)
    }

    // -- Bookmarks --

    override fun getBookmarksStream(resourceId: String): Flow<List<Bookmark>> = queries
        .getBookmarksByResourceId(resourceId)
        .asFlow()
        .mapToList(dispatcher)
        .map { entities -> entities.map { it.toBookmark() } }

    override suspend fun getBookmarks(resourceId: String): List<Bookmark> = withContext(dispatcher) {
        queries.getBookmarksByResourceId(resourceId).executeAsList().map { it.toBookmark() }
    }

    override suspend fun getBookmarkById(id: String): Bookmark? = withContext(dispatcher) {
        queries.getBookmarkById(id).executeAsOneOrNull()?.toBookmark()
    }

    override suspend fun saveBookmark(bookmark: Bookmark) = withContext(dispatcher) {
        queries.insertBookmark(
            id = bookmark.id,
            resourceId = bookmark.resourceId,
            page = bookmark.page.toLong(),
            percentPosition = bookmark.percentPosition,
            title = bookmark.title,
            opinion = bookmark.opinion,
            confidenceScore = bookmark.confidenceScore?.toLong(),
            createdAt = bookmark.createdAt
        )
    }

    override suspend fun deleteBookmark(id: String) = withContext(dispatcher) {
        queries.deleteBookmark(id)
    }

    override suspend fun deleteBookmarksByResourceId(resourceId: String) = withContext(dispatcher) {
        queries.deleteBookmarksByResourceId(resourceId)
    }

    // -- Reading Settings --

    override suspend fun getReadingSettings(resourceId: String): ReadingSettings? = withContext(dispatcher) {
        queries.getReadingSettingsByResourceId(resourceId).executeAsOneOrNull()?.toReadingSettings()
    }

    override suspend fun updateReadingSettings(settings: ReadingSettings) = withContext(dispatcher) {
        queries.insertOrUpdateReadingSettings(
            resourceId = settings.resourceId,
            fontSize = settings.fontSize.toLong(),
            typeface = settings.typeface,
            lineSpacing = settings.lineSpacing,
            margins = settings.margins.toLong(),
            theme = settings.theme,
            scrollMode = settings.scrollMode,
            brightness = settings.brightness,
            keepScreenOn = if (settings.keepScreenOn) 1L else 0L,
            textReflow = if (settings.textReflow) 1L else 0L
        )
    }

    override suspend fun deleteReadingSettings(resourceId: String) = withContext(dispatcher) {
        queries.deleteReadingSettings(resourceId)
    }

    // -- Custom Templates --

    override val templatesStream: Flow<List<CustomTemplate>> = queries
        .getAllTemplates()
        .asFlow()
        .mapToList(dispatcher)
        .map { entities -> entities.map { it.toCustomTemplate() } }

    override suspend fun saveTemplate(template: CustomTemplate) = withContext(dispatcher) {
        queries.insertTemplate(
            id = template.id,
            name = template.name,
            headings = template.headings.joinToString("|") // Use | as delimiter
        )
    }

    override suspend fun deleteTemplate(id: String) = withContext(dispatcher) {
        queries.deleteTemplate(id)
    }
}

// -- Entity to Domain mappers (Enhanced with safety) --

private fun ResourceEntity.toResource(): Resource = Resource(
    id = id,
    type = try { ResourceType.valueOf(type) } catch (e: Exception) { ResourceType.UNSUPPORTED },
    url = url ?: "",
    filePath = filePath,
    extractedText = extractedText,
    title = title ?: "Untitled",
    createdAt = createdAt
)

private fun ReadingProgressEntity.toReadingProgress(): ReadingProgress = ReadingProgress(
    resourceId = resourceId,
    currentPage = currentPage.toInt(),
    totalPages = totalPages.toInt(),
    percentComplete = percentComplete,
    currentChapter = currentChapter,
    lastOpenedAt = lastOpenedAt
)

private fun BookmarkEntity.toBookmark(): Bookmark = Bookmark(
    id = id,
    resourceId = resourceId,
    page = page.toInt(),
    percentPosition = percentPosition,
    title = title ?: "",
    opinion = opinion,
    confidenceScore = confidenceScore?.toInt(),
    createdAt = createdAt
)

private fun ReadingSettingsEntity.toReadingSettings(): ReadingSettings = ReadingSettings(
    resourceId = resourceId,
    fontSize = fontSize.toInt(),
    typeface = typeface,
    lineSpacing = lineSpacing,
    margins = margins.toInt(),
    theme = theme,
    scrollMode = scrollMode,
    brightness = brightness,
    keepScreenOn = keepScreenOn != 0L,
    textReflow = textReflow != 0L
)

private fun CustomTemplateEntity.toCustomTemplate(): CustomTemplate = CustomTemplate(
    id = id,
    name = name,
    headings = headings.split("|").filter { it.isNotBlank() }
)

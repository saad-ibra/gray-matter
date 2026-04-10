package com.example.graymatter.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.example.graymatter.database.GrayMatterDatabase
import com.example.graymatter.database.ItemEntity
import com.example.graymatter.domain.Bookmark
import com.example.graymatter.domain.ResourceEntry
import com.example.graymatter.domain.ResourceEntryWithDetails
import com.example.graymatter.domain.Opinion
import com.example.graymatter.domain.Resource
import com.example.graymatter.domain.Topic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultResourceEntryRepository(
    database: GrayMatterDatabase,
    private val resourceRepository: ResourceRepository,
    private val opinionRepository: OpinionRepository,
    private val topicRepository: TopicRepository,
    private val dispatcher: CoroutineDispatcher
) : ResourceEntryRepository {
    
    private val queries = database.grayMatterDatabaseQueries
    
    override val resourceEntriesStream: Flow<List<ResourceEntry>> = queries
        .getAllResourceEntries()
        .asFlow()
        .mapToList(dispatcher)
        .map { entities: List<ItemEntity> -> entities.map { it.toResourceEntry() } }
    
    override suspend fun getResourceEntryById(id: String): ResourceEntry? = withContext(dispatcher) {
        queries.getResourceEntryById(id).executeAsOneOrNull()?.toResourceEntry()
    }
    
    override suspend fun getResourceEntryByResourceId(resourceId: String): ResourceEntry? = withContext(dispatcher) {
        queries.getResourceEntryByResourceId(resourceId).executeAsOneOrNull()?.toResourceEntry()
    }
    
    override fun getResourceEntryWithDetailsStream(resourceEntryId: String): Flow<ResourceEntryWithDetails?> {
        val entryFlow = queries.getResourceEntryById(resourceEntryId)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { it?.toResourceEntry() }

        return entryFlow.flatMapLatest { resourceEntry: ResourceEntry? ->
            if (resourceEntry == null) return@flatMapLatest flowOf(null)

            combine(
                resourceRepository.resourcesStream.map { resources -> 
                    resources.find { it.id == resourceEntry.resourceId } 
                },
                opinionRepository.getOpinionsByItemId(resourceEntryId),
                resourceRepository.getBookmarksStream(resourceEntry.resourceId),
                topicRepository.topicsStream.map { topics ->
                    resourceEntry.topicId?.let { tid -> topics.find { it.id == tid } }
                }
            ) { resource: Resource?, opinions: List<Opinion>, bookmarks: List<Bookmark>, topic: Topic? ->
                if (resource == null) return@combine null
                ResourceEntryWithDetails(
                    resourceEntry = resourceEntry,
                    resource = resource,
                    opinions = opinions,
                    bookmarks = bookmarks,
                    topic = topic
                )
            }
        }
    }

    override suspend fun getResourceEntryWithDetails(resourceEntryId: String): ResourceEntryWithDetails? = withContext(dispatcher) {
        val resourceEntry = getResourceEntryById(resourceEntryId) ?: return@withContext null
        val resource = resourceRepository.getResourceById(resourceEntry.resourceId) ?: return@withContext null
        val opinions = opinionRepository.getOpinionsByItemId(resourceEntryId).first()
        val bookmarks = resourceRepository.getBookmarks(resourceEntry.resourceId)
        val topic = resourceEntry.topicId?.let { topicRepository.getTopicById(it) }
        
        ResourceEntryWithDetails(
            resourceEntry = resourceEntry,
            resource = resource,
            opinions = opinions,
            bookmarks = bookmarks,
            topic = topic
        )
    }
    
    override fun getResourceEntriesByTopicId(topicId: String): Flow<List<ResourceEntry>> = queries
        .getResourceEntriesByTopicId(topicId)
        .asFlow()
        .mapToList(dispatcher)
        .map { entities: List<ItemEntity> -> entities.map { it.toResourceEntry() } }
    
    override suspend fun createResourceEntryWithDetails(
        resourceEntryId: String,
        resourceId: String,
        resourceType: String,
        url: String?,
        filePath: String?,
        extractedText: String?,
        title: String?,
        description: String?,
        opinionId: String,
        opinionText: String,
        confidence: Int,
        now: Long
    ) = withContext(dispatcher) {
        queries.transaction {
            queries.insertResource(
                id = resourceId,
                type = resourceType,
                url = url,
                filePath = filePath,
                extractedText = extractedText,
                title = title,
                createdAt = now
            )
            
            queries.insertResourceEntry(
                id = resourceEntryId,
                resourceId = resourceId,
                topicId = null,
                description = description,
                firstOpinionAt = now,
                lastOpinionAt = now,
                opinionCount = 1,
                isDeleted = 0L,
                deletedAt = null
            )
            
            queries.insertOpinion(
                id = opinionId,
                itemId = resourceEntryId,
                text = opinionText,
                confidenceScore = confidence.toLong(),
                pageNumber = null,
                createdAt = now,
                updatedAt = now,
                isDeleted = 0L,
                deletedAt = null
            )
        }
    }
    
    override suspend fun updateResourceEntryTopic(resourceEntryId: String, topicId: String?) = withContext(dispatcher) {
        val oldEntry = getResourceEntryById(resourceEntryId)
        queries.transaction {
            queries.updateResourceEntryTopic(topicId, resourceEntryId)
            if (oldEntry?.topicId != topicId) {
                oldEntry?.topicId?.let { queries.decrementTopicResourceCount(Clock.System.now().toEpochMilliseconds(), it) }
                topicId?.let { queries.incrementTopicResourceCount(Clock.System.now().toEpochMilliseconds(), it) }
            }
        }
    }

    override suspend fun updateResourceEntryDescription(resourceEntryId: String, description: String?) = withContext(dispatcher) {
        queries.updateResourceEntryDescription(description, resourceEntryId)
    }
    
    override suspend fun updateResourceEntryOpinionMetadata(resourceEntryId: String, lastOpinionAt: Long) = withContext(dispatcher) {
        queries.updateResourceEntryOpinionMetadata(lastOpinionAt, resourceEntryId)
    }
    
    override suspend fun deleteResourceEntry(id: String) = withContext(dispatcher) {
        val resourceEntry = getResourceEntryById(id)
        if (resourceEntry != null) {
            queries.transaction {
                resourceEntry.topicId?.let { queries.decrementTopicResourceCount(Clock.System.now().toEpochMilliseconds(), it) }
                queries.deleteResource(resourceEntry.resourceId)
            }
        }
    }
    
    override suspend fun softDeleteResourceEntry(id: String) = withContext(dispatcher) {
        val entry = getResourceEntryById(id) ?: return@withContext
        val now = Clock.System.now().toEpochMilliseconds()
        queries.softDeleteResourceEntry(now, id)
        opinionRepository.softDeleteOpinionsByItemId(id, now)
        resourceRepository.softDeleteBookmarksByResourceId(entry.resourceId, now)
    }

    override suspend fun undoDeleteResourceEntry(id: String) = withContext(dispatcher) {
        val entry = getResourceEntryById(id) ?: return@withContext
        val deletedAt = entry.deletedAt ?: return@withContext
        queries.undoDeleteResourceEntry(id)
        opinionRepository.undoDeleteOpinionsByItemId(id, deletedAt)
        resourceRepository.undoDeleteBookmarksByResourceId(entry.resourceId, deletedAt)
    }

    override suspend fun getDeletedResourceEntries(): List<ResourceEntry> = withContext(dispatcher) {
        queries.getDeletedResourceEntries().executeAsList().map { it.toResourceEntry() }
    }

    override suspend fun softDeleteResourceEntriesByTopicId(topicId: String, deletedAt: Long) = withContext(dispatcher) {
        val entries = queries.getResourceEntriesByTopicId(topicId).executeAsList()
        entries.forEach { entry ->
            opinionRepository.softDeleteOpinionsByItemId(entry.id, deletedAt)
            resourceRepository.softDeleteBookmarksByResourceId(entry.resourceId, deletedAt)
        }
        queries.softDeleteResourceEntriesByTopicId(deletedAt, topicId)
    }

    override suspend fun undoDeleteResourceEntriesByTopicId(topicId: String, deletedAt: Long) = withContext(dispatcher) {
        val entries = queries.getResourceEntriesByTopicId(topicId).executeAsList()
        entries.forEach { entry ->
            opinionRepository.undoDeleteOpinionsByItemId(entry.id, deletedAt)
            resourceRepository.undoDeleteBookmarksByResourceId(entry.resourceId, deletedAt)
        }
        queries.undoDeleteResourceEntriesByTopicId(topicId, deletedAt)
    }

    override suspend fun getResourceEntriesWithoutTopic(): List<ResourceEntry> = withContext(dispatcher) {
        queries.getResourceEntriesWithoutTopic().executeAsList().map { it.toResourceEntry() }
    }

    override suspend fun getAllResourceEntriesByTopicId(topicId: String): List<ResourceEntry> = withContext(dispatcher) {
        queries.getAllResourceEntriesByTopicId(topicId).executeAsList().map { it.toResourceEntry() }
    }
}

private fun ItemEntity.toResourceEntry(): ResourceEntry = ResourceEntry(
    id = id,
    resourceId = resourceId,
    topicId = topicId,
    description = description,
    firstOpinionAt = firstOpinionAt,
    lastOpinionAt = lastOpinionAt,
    opinionCount = opinionCount.toInt(),
    isDeleted = isDeleted == 1L,
    deletedAt = deletedAt
)

package com.example.graymatter.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.example.graymatter.database.GrayMatterDatabase
import com.example.graymatter.database.ItemEntity
import com.example.graymatter.domain.Bookmark
import com.example.graymatter.domain.Item
import com.example.graymatter.domain.ItemWithDetails
import com.example.graymatter.domain.Opinion
import com.example.graymatter.domain.Resource
import com.example.graymatter.domain.Topic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultItemRepository(
    private val database: GrayMatterDatabase,
    private val resourceRepository: ResourceRepository,
    private val opinionRepository: OpinionRepository,
    private val topicRepository: TopicRepository,
    private val dispatcher: CoroutineDispatcher
) : ItemRepository {
    
    private val queries = database.grayMatterDatabaseQueries
    
    override val itemsStream: Flow<List<Item>> = queries
        .getAllItems()
        .asFlow()
        .mapToList(dispatcher)
        .map { entities: List<ItemEntity> -> entities.map { it.toItem() } }
    
    override suspend fun getItemById(id: String): Item? = withContext(dispatcher) {
        queries.getItemById(id).executeAsOneOrNull()?.toItem()
    }
    
    override suspend fun getItemByResourceId(resourceId: String): Item? = withContext(dispatcher) {
        queries.getItemByResourceId(resourceId).executeAsOneOrNull()?.toItem()
    }
    
    override fun getItemWithDetailsStream(itemId: String): Flow<ItemWithDetails?> {
        val itemFlow = queries.getItemById(itemId)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { it?.toItem() }

        return itemFlow.flatMapLatest { item: Item? ->
            if (item == null) return@flatMapLatest flowOf(null)

            combine(
                resourceRepository.resourcesStream.map { resources -> 
                    resources.find { it.id == item.resourceId } 
                },
                opinionRepository.getOpinionsByItemId(itemId),
                resourceRepository.getBookmarksStream(item.resourceId),
                topicRepository.topicsStream.map { topics ->
                    item.topicId?.let { tid -> topics.find { it.id == tid } }
                }
            ) { resource: Resource?, opinions: List<Opinion>, bookmarks: List<Bookmark>, topic: Topic? ->
                if (resource == null) return@combine null
                ItemWithDetails(
                    item = item,
                    resource = resource,
                    opinions = opinions,
                    bookmarks = bookmarks,
                    topic = topic
                )
            }
        }
    }

    override suspend fun getItemWithDetails(itemId: String): ItemWithDetails? = withContext(dispatcher) {
        val item = getItemById(itemId) ?: return@withContext null
        val resource = resourceRepository.getResourceById(item.resourceId) ?: return@withContext null
        val opinions = opinionRepository.getOpinionsByItemId(itemId).first()
        val bookmarks = resourceRepository.getBookmarks(item.resourceId)
        val topic = item.topicId?.let { topicRepository.getTopicById(it) }
        
        ItemWithDetails(
            item = item,
            resource = resource,
            opinions = opinions,
            bookmarks = bookmarks,
            topic = topic
        )
    }
    
    override fun getItemsByTopicId(topicId: String): Flow<List<Item>> = queries
        .getItemsByTopicId(topicId)
        .asFlow()
        .mapToList(dispatcher)
        .map { entities: List<ItemEntity> -> entities.map { it.toItem() } }
    
    override suspend fun createItemWithDetails(
        itemId: String,
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
            
            queries.insertItem(
                id = itemId,
                resourceId = resourceId,
                topicId = null,
                description = description,
                firstOpinionAt = now,
                lastOpinionAt = now,
                opinionCount = 1
            )
            
            queries.insertOpinion(
                id = opinionId,
                itemId = itemId,
                text = opinionText,
                confidenceScore = confidence.toLong(),
                pageNumber = null,
                createdAt = now,
                updatedAt = now
            )
        }
    }
    
    override suspend fun updateItemTopic(itemId: String, topicId: String?) = withContext(dispatcher) {
        val oldItem = getItemById(itemId)
        queries.transaction {
            queries.updateItemTopic(topicId, itemId)
            if (oldItem?.topicId != topicId) {
                oldItem?.topicId?.let { queries.decrementTopicResourceCount(Clock.System.now().toEpochMilliseconds(), it) }
                topicId?.let { queries.incrementTopicResourceCount(Clock.System.now().toEpochMilliseconds(), it) }
            }
        }
    }

    override suspend fun updateItemDescription(itemId: String, description: String?) = withContext(dispatcher) {
        queries.updateItemDescription(description, itemId)
    }
    
    override suspend fun updateItemOpinionMetadata(itemId: String, lastOpinionAt: Long) = withContext(dispatcher) {
        queries.updateItemOpinionMetadata(lastOpinionAt, itemId)
    }
    
    override suspend fun deleteItem(id: String) = withContext(dispatcher) {
        val item = getItemById(id)
        if (item != null) {
            queries.transaction {
                item.topicId?.let { queries.decrementTopicResourceCount(Clock.System.now().toEpochMilliseconds(), it) }
                queries.deleteResource(item.resourceId)
            }
        }
    }
}

private fun ItemEntity.toItem(): Item = Item(
    id = id,
    resourceId = resourceId,
    topicId = topicId,
    description = description,
    firstOpinionAt = firstOpinionAt,
    lastOpinionAt = lastOpinionAt,
    opinionCount = opinionCount.toInt()
)

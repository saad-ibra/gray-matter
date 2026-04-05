package com.example.graymatter.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.graymatter.database.GrayMatterDatabase
import com.example.graymatter.database.TopicEntity
import com.example.graymatter.domain.Topic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class DefaultTopicRepository(
    private val database: GrayMatterDatabase,
    private val dispatcher: CoroutineDispatcher
) : TopicRepository {
    
    private val queries = database.grayMatterDatabaseQueries
    
    override val topicsStream: Flow<List<Topic>> = queries
        .getAllTopics()
        .asFlow()
        .mapToList(dispatcher)
        .map { entities -> entities.map { it.toTopic() } }
    
    override suspend fun getTopicById(id: String): Topic? = withContext(dispatcher) {
        queries.getTopicById(id).executeAsOneOrNull()?.toTopic()
    }
    
    override suspend fun getAllTopics(): List<Topic> = withContext(dispatcher) {
        queries.getAllTopics().executeAsList().map { it.toTopic() }
    }
    
    override suspend fun saveTopic(topic: Topic) = withContext(dispatcher) {
        queries.insertTopic(
            id = topic.id,
            name = topic.name,
            notes = topic.notes,
            resourceCount = topic.resourceCount.toLong(),
            sortOrder = topic.sortOrder.toLong(),
            updatedAt = topic.updatedAt,
            isDeleted = if (topic.isDeleted) 1L else 0L,
            deletedAt = topic.deletedAt
        )
    }
    
    override suspend fun softDeleteTopic(id: String) = withContext(dispatcher) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.transaction {
            queries.softDeleteTopic(now, id)
            queries.softDeleteResourceEntriesByTopicId(now, id)
            queries.softDeleteOpinionsByTopicId(now, id)
            queries.softDeleteBookmarksByTopicId(now, id)
        }
    }

    override suspend fun undoDeleteTopic(id: String) = withContext(dispatcher) {
        val topic = getTopicById(id) ?: return@withContext
        val deletedAt = topic.deletedAt ?: return@withContext
        queries.transaction {
            queries.undoDeleteTopic(id)
            queries.undoDeleteResourceEntriesByTopicId(id, deletedAt)
            queries.undoDeleteOpinionsByTopicId(id, deletedAt)
            queries.undoDeleteBookmarksByTopicId(id, deletedAt)
        }
    }

    override suspend fun getDeletedTopics(): List<Topic> = withContext(dispatcher) {
        queries.getDeletedTopics().executeAsList().map { it.toTopic() }
    }
    
    override suspend fun updateTopicNotes(topicId: String, notes: String?) = withContext(dispatcher) {
        queries.updateTopicNotes(notes, Clock.System.now().toEpochMilliseconds(), topicId)
    }
    
    override suspend fun incrementResourceCount(topicId: String) = withContext(dispatcher) {
        queries.incrementTopicResourceCount(Clock.System.now().toEpochMilliseconds(), topicId)
    }
    
    override suspend fun decrementResourceCount(topicId: String) = withContext(dispatcher) {
        queries.decrementTopicResourceCount(Clock.System.now().toEpochMilliseconds(), topicId)
    }
    
    override suspend fun deleteTopic(id: String) = withContext(dispatcher) {
        queries.transaction {
            // Cascade delete child resource entries/resources first
            val resourceEntries = queries.getResourceEntriesByTopicId(id).executeAsList()
            resourceEntries.forEach { resourceEntry ->
                queries.deleteResource(resourceEntry.resourceId) // cascades to ResourceEntry and Opinions
            }
            queries.deleteTopic(id)
        }
    }
    
    override suspend fun searchTopics(query: String): List<Topic> = withContext(dispatcher) {
        // SQLDelight generates multiple parameters if :query is used multiple times
        queries.searchTopics(query, query).executeAsList().map { it.toTopic() }
    }

    override suspend fun renameTopic(id: String, newName: String) = withContext(dispatcher) {
        queries.renameTopic(newName, Clock.System.now().toEpochMilliseconds(), id)
    }

    override suspend fun updateTopicOrder(topicIds: List<String>) = withContext(dispatcher) {
        queries.transaction {
            val now = Clock.System.now().toEpochMilliseconds()
            topicIds.forEachIndexed { index, id ->
                queries.updateTopicOrder(index.toLong(), now, id)
            }
        }
    }
}

private fun TopicEntity.toTopic(): Topic = Topic(
    id = id,
    name = name,
    notes = notes,
    resourceCount = resourceCount.toInt(),
    sortOrder = sortOrder.toInt(),
    updatedAt = updatedAt,
    isDeleted = isDeleted == 1L,
    deletedAt = deletedAt
)

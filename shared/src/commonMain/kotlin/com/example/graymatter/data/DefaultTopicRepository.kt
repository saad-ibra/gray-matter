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
            updatedAt = topic.updatedAt
        )
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
            // Cascade delete child items/resources first
            val items = queries.getItemsByTopicId(id).executeAsList()
            items.forEach { item ->
                queries.deleteResource(item.resourceId) // cascades to Item and Opinions
            }
            queries.deleteTopic(id)
        }
    }
    
    override suspend fun searchTopics(query: String): List<Topic> = withContext(dispatcher) {
        // SQLDelight generates multiple parameters if :query is used multiple times
        queries.searchTopics(query, query).executeAsList().map { it.toTopic() }
    }
}

private fun TopicEntity.toTopic(): Topic = Topic(
    id = id,
    name = name,
    notes = notes,
    resourceCount = resourceCount.toInt(),
    updatedAt = updatedAt
)

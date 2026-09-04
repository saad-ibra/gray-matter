package com.example.graymatter.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.graymatter.database.GrayMatterDatabase
import com.example.graymatter.database.TagEntity
import com.example.graymatter.domain.Tag
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultTagRepository(
    database: GrayMatterDatabase,
    private val dispatcher: CoroutineDispatcher
) : TagRepository {

    private val queries = database.grayMatterDatabaseQueries

    override fun allTagsStream(): Flow<List<Tag>> {
        return queries.getAllTags()
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map { it.toTag() } }
    }

    override fun getTagsByEntryId(entryId: String): Flow<List<Tag>> {
        return queries.getTagsByEntryId(entryId)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map { it.toTag() } }
    }

    override suspend fun getTagByName(name: String): Tag? = withContext(dispatcher) {
        queries.getTagByName(name).executeAsOneOrNull()?.toTag()
    }

    override suspend fun getEntryCountByTagId(tagId: String): Long = withContext(dispatcher) {
        queries.getEntryCountByTagId(tagId).executeAsOne()
    }

    override suspend fun insertTag(tag: Tag) = withContext(dispatcher) {
        queries.insertTag(
            id = tag.id,
            name = tag.name,
            createdAt = tag.createdAt
        )
    }

    override suspend fun updateTagName(id: String, name: String) = withContext(dispatcher) {
        queries.updateTagName(
            name = name,
            id = id
        )
    }

    override suspend fun deleteTag(id: String) = withContext(dispatcher) {
        queries.deleteTag(id)
    }

    override suspend fun addTagToEntry(
        id: String,
        entryId: String,
        entryType: String,
        tagId: String,
        createdAt: Long
    ) = withContext(dispatcher) {
        queries.insertEntryTag(
            id = id,
            entryId = entryId,
            entryType = entryType,
            tagId = tagId,
            createdAt = createdAt
        )
    }

    override suspend fun removeTagFromEntry(entryId: String, tagId: String) = withContext(dispatcher) {
        queries.deleteEntryTag(
            entryId = entryId,
            tagId = tagId
        )
    }

    override suspend fun removeAllTagsFromEntry(entryId: String) = withContext(dispatcher) {
        queries.deleteEntryTagsByEntryId(entryId)
    }

    override suspend fun searchTags(query: String): List<Tag> = withContext(dispatcher) {
        queries.searchTags(query).executeAsList().map { it.toTag() }
    }

    override suspend fun cleanOrphanEntryTags() = withContext(dispatcher) {
        queries.deleteOrphanEntryTags()
    }

    private fun TagEntity.toTag() = Tag(
        id = id,
        name = name,
        createdAt = createdAt
    )
}

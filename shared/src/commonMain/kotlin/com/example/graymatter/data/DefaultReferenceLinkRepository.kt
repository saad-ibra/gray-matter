package com.example.graymatter.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.graymatter.database.GrayMatterDatabase
import com.example.graymatter.database.ReferenceLinkEntity
import com.example.graymatter.domain.ReferenceLink
import com.example.graymatter.domain.ReferenceType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultReferenceLinkRepository(
    database: GrayMatterDatabase,
    private val dispatcher: CoroutineDispatcher
) : ReferenceLinkRepository {

    private val queries = database.grayMatterDatabaseQueries

    override fun getBacklinksForTarget(targetId: String): Flow<List<ReferenceLink>> = queries
        .getBacklinksForTarget(targetId)
        .asFlow()
        .mapToList(dispatcher)
        .map { entities -> entities.map { it.toReferenceLink() } }

    override fun getReferenceLinksBySource(sourceId: String): Flow<List<ReferenceLink>> = queries
        .getReferenceLinksBySource(sourceId)
        .asFlow()
        .mapToList(dispatcher)
        .map { entities -> entities.map { it.toReferenceLink() } }
        
    override fun getAllReferenceLinks(): Flow<List<ReferenceLink>> = queries
        .getAllReferenceLinks()
        .asFlow()
        .mapToList(dispatcher)
        .map { entities -> entities.map { it.toReferenceLink() } }

    override suspend fun insertReferenceLink(link: ReferenceLink) = withContext(dispatcher) {
        queries.insertReferenceLink(
            id = link.id,
            sourceType = link.sourceType.name,
            sourceId = link.sourceId,
            targetType = link.targetType.name,
            targetId = link.targetId,
            createdAt = link.createdAt
        )
    }

    override suspend fun deleteReferenceLink(id: String) = withContext(dispatcher) {
        queries.deleteReferenceLink(id)
    }

    override suspend fun deleteReferenceLinksBySource(sourceId: String) = withContext(dispatcher) {
        queries.deleteReferenceLinksBySource(sourceId)
    }

    override suspend fun deleteReferenceLinksByTarget(targetId: String) = withContext(dispatcher) {
        queries.deleteReferenceLinksByTarget(targetId)
    }

    override suspend fun cleanOrphanReferenceLinks() = withContext(dispatcher) {
        queries.deleteOrphanReferenceLinks()
    }

    private fun ReferenceLinkEntity.toReferenceLink() = ReferenceLink(
        id = id,
        sourceType = ReferenceType.valueOf(sourceType),
        sourceId = sourceId,
        targetType = ReferenceType.valueOf(targetType),
        targetId = targetId,
        createdAt = createdAt
    )
}

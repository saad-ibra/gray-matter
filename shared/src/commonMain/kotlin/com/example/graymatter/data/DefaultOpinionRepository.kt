package com.example.graymatter.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.graymatter.database.GrayMatterDatabase
import com.example.graymatter.database.OpinionEntity
import com.example.graymatter.domain.Opinion
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class DefaultOpinionRepository(
    private val database: GrayMatterDatabase,
    private val dispatcher: CoroutineDispatcher
) : OpinionRepository {
    
    private val queries = database.grayMatterDatabaseQueries
    
    override fun getOpinionsByItemId(itemId: String): Flow<List<Opinion>> = queries
        .getOpinionsByItemId(itemId)
        .asFlow()
        .mapToList(dispatcher)
        .map { entities: List<OpinionEntity> -> entities.map { it.toOpinion() } }
    
    override suspend fun getOpinionById(id: String): Opinion? = withContext(dispatcher) {
        queries.getOpinionById(id).executeAsOneOrNull()?.toOpinion()
    }
    
    override fun getAllOpinions(): Flow<List<Opinion>> = queries
        .getAllOpinions()
        .asFlow()
        .mapToList(dispatcher)
        .map { entities: List<OpinionEntity> -> entities.map { it.toOpinion() } }

    override val deletedOpinionsStream: Flow<List<Opinion>> = queries
        .getDeletedOpinions()
        .asFlow()
        .mapToList(dispatcher)
        .map { entities: List<OpinionEntity> -> entities.map { it.toOpinion() } }
    
    override suspend fun saveOpinion(opinion: Opinion) = withContext(dispatcher) {
        queries.insertOpinion(
            id = opinion.id,
            itemId = opinion.itemId,
            text = opinion.text,
            confidenceScore = opinion.confidenceScore.toLong(),
            pageNumber = opinion.pageNumber?.toLong(),
            createdAt = opinion.createdAt,
            updatedAt = opinion.updatedAt,
            isDeleted = if (opinion.isDeleted) 1L else 0L,
            deletedAt = opinion.deletedAt
        )
    }
    
    override suspend fun softDeleteOpinion(id: String) = withContext(dispatcher) {
        queries.softDeleteOpinion(Clock.System.now().toEpochMilliseconds(), id)
    }

    override suspend fun undoDeleteOpinion(id: String) = withContext(dispatcher) {
        queries.undoDeleteOpinion(id)
    }

    override suspend fun getDeletedOpinions(): List<Opinion> = withContext(dispatcher) {
        queries.getDeletedOpinions().executeAsList().map { it.toOpinion() }
    }
    
    override suspend fun updateOpinion(opinion: Opinion) = withContext(dispatcher) {
        queries.updateOpinion(
            text = opinion.text,
            confidenceScore = opinion.confidenceScore.toLong(),
            createdAt = opinion.createdAt,
            pageNumber = opinion.pageNumber?.toLong(),
            updatedAt = opinion.updatedAt,
            id = opinion.id
        )
    }
    
    override suspend fun deleteOpinion(id: String) = withContext(dispatcher) {
        queries.deleteOpinion(id)
    }
    
    override suspend fun searchOpinions(query: String): List<Opinion> = withContext(dispatcher) {
        queries.searchOpinions(query).executeAsList().map { it.toOpinion() }
    }

    override suspend fun softDeleteOpinionsByItemId(itemId: String, deletedAt: Long) = withContext(dispatcher) {
        queries.softDeleteOpinionsByItemId(deletedAt, itemId)
    }

    override suspend fun undoDeleteOpinionsByItemId(itemId: String, deletedAt: Long) = withContext(dispatcher) {
        queries.undoDeleteOpinionsByItemId(itemId, deletedAt)
    }

    override suspend fun getAllOpinionsByItemId(itemId: String): List<Opinion> = withContext(dispatcher) {
        queries.getAllOpinionsByItemId(itemId).executeAsList().map { it.toOpinion() }
    }
}

private fun OpinionEntity.toOpinion(): Opinion = Opinion(
    id = id,
    itemId = itemId,
    text = text,
    confidenceScore = confidenceScore.toInt(),
    pageNumber = pageNumber?.toInt(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted == 1L,
    deletedAt = deletedAt
)

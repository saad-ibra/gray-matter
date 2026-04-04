package com.example.graymatter.data

import com.example.graymatter.domain.Opinion
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Opinion operations.
 */
interface OpinionRepository {
    
    /**
     * Get opinions for a specific item.
     */
    fun getOpinionsByItemId(itemId: String): Flow<List<Opinion>>
    
    /**
     * Get an opinion by its ID.
     */
    suspend fun getOpinionById(id: String): Opinion?
    
    /**
     * Get all opinions as a Flow.
     */
    fun getAllOpinions(): Flow<List<Opinion>>
    
    /**
     * Save a new opinion.
     */
    suspend fun saveOpinion(opinion: Opinion)
    
    /**
     * Update an existing opinion.
     */
    suspend fun updateOpinion(opinion: Opinion)
    
    /**
     * Delete an opinion.
     */
    /**
     * Delete an opinion completely.
     */
    suspend fun deleteOpinion(id: String)
    
    /**
     * Soft delete an opinion.
     */
    suspend fun softDeleteOpinion(id: String)

    /**
     * Undo soft delete of an opinion.
     */
    suspend fun undoDeleteOpinion(id: String)

    /**
     * Get all deleted opinions.
     */
    suspend fun getDeletedOpinions(): List<Opinion>
    
    /**
     * Search opinions by query text.
     */
    suspend fun searchOpinions(query: String): List<Opinion>
}

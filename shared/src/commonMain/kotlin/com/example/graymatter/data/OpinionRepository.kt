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
    suspend fun deleteOpinion(id: String)
    
    /**
     * Search opinions by query text.
     */
    suspend fun searchOpinions(query: String): List<Opinion>
}

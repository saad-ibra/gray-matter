package com.example.graymatter.data

import com.example.graymatter.domain.ReferenceLink
import kotlinx.coroutines.flow.Flow

interface ReferenceLinkRepository {
    fun getBacklinksForTarget(targetId: String): Flow<List<ReferenceLink>>
    fun getReferenceLinksBySource(sourceId: String): Flow<List<ReferenceLink>>
    fun getAllReferenceLinks(): Flow<List<ReferenceLink>>
    suspend fun insertReferenceLink(link: ReferenceLink)
    suspend fun deleteReferenceLink(id: String)
    suspend fun deleteReferenceLinksBySource(sourceId: String)
    suspend fun deleteReferenceLinksByTarget(targetId: String)
}

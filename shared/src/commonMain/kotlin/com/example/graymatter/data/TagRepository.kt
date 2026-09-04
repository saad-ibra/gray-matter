package com.example.graymatter.data

import com.example.graymatter.domain.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun allTagsStream(): Flow<List<Tag>>
    fun getTagsByEntryId(entryId: String): Flow<List<Tag>>
    suspend fun getTagByName(name: String): Tag?
    suspend fun getEntryCountByTagId(tagId: String): Long
    suspend fun insertTag(tag: Tag)
    suspend fun updateTagName(id: String, name: String)
    suspend fun deleteTag(id: String)
    suspend fun addTagToEntry(id: String, entryId: String, entryType: String, tagId: String, createdAt: Long)
    suspend fun removeTagFromEntry(entryId: String, tagId: String)
    suspend fun removeAllTagsFromEntry(entryId: String)
    suspend fun searchTags(query: String): List<Tag>
    suspend fun cleanOrphanEntryTags()
}

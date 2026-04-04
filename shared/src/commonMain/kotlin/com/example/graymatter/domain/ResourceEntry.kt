package com.example.graymatter.domain

/**
 * A resource entry binds one resource to multiple opinions.
 * A resource entry is created only when the first opinion is submitted.
 */
data class ResourceEntry(
    val id: String,
    val resourceId: String,
    val topicId: String? = null,
    val description: String? = null,
    val firstOpinionAt: Long,
    val lastOpinionAt: Long,
    val opinionCount: Int,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

/**
 * Full resource entry with associated resource, opinions, and bookmarks for display.
 */
data class ResourceEntryWithDetails(
    val resourceEntry: ResourceEntry,
    val resource: Resource,
    val opinions: List<Opinion>,
    val bookmarks: List<Bookmark> = emptyList(),
    val topic: Topic? = null
)

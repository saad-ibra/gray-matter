package com.example.graymatter.domain

/**
 * An item binds one resource to multiple opinions.
 * An item is created only when the first opinion is submitted.
 */
data class Item(
    val id: String,
    val resourceId: String,
    val topicId: String? = null,
    val description: String? = null,
    val firstOpinionAt: Long,
    val lastOpinionAt: Long,
    val opinionCount: Int
)

/**
 * Full item with associated resource, opinions, and bookmarks for display.
 */
data class ItemWithDetails(
    val item: Item,
    val resource: Resource,
    val opinions: List<Opinion>,
    val bookmarks: List<Bookmark> = emptyList(),
    val topic: Topic? = null
)

package com.example.graymatter.domain

/**
 * A saved position within a document.
 */
data class Bookmark(
    val id: String,
    val resourceId: String,
    val page: Int,
    val percentPosition: Double = 0.0,
    val title: String? = null,
    val opinion: String? = null,
    val confidenceScore: Int? = null,
    val createdAt: Long,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

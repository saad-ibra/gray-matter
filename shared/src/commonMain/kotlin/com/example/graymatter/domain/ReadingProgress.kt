package com.example.graymatter.domain

/**
 * Tracks reading position for a document/resource.
 */
data class ReadingProgress(
    val resourceId: String,
    val currentPage: Int,
    val totalPages: Int,
    val percentComplete: Double,
    val currentChapter: String? = null,
    val lastOpenedAt: Long
)

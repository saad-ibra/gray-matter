package com.example.graymatter.domain

/**
 * An opinion is a user's thought about a specific resource at a point in time.
 */
data class Opinion(
    val id: String,
    val itemId: String,
    val text: String,
    val confidenceScore: Int,
    val pageNumber: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

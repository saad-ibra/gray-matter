package com.example.graymatter.domain

/**
 * Topics are internal organizational groupings.
 */
data class Topic(
    val id: String,
    val name: String,
    val notes: String? = null,
    val resourceCount: Int = 0,
    val updatedAt: Long
)

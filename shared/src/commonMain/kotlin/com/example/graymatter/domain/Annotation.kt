package com.example.graymatter.domain

/**
 * Represents the type of annotation (highlight, note, etc.).
 */
enum class AnnotationType {
    HIGHLIGHT, NOTE, STRIKETHROUGH, UNDERLINE
}

/**
 * Represents a user annotation (highlight, note, etc.) on a resource.
 */
data class Annotation(
    val id: String,
    val resourceId: String,
    val page: Int,
    val text: String,
    val annotationType: AnnotationType,
    val color: String? = null,
    val note: String? = null,
    val createdAt: Long
)

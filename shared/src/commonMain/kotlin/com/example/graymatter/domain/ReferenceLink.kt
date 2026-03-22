package com.example.graymatter.domain

/**
 * Represents a directed link from a source entity to a target entity.
 */
data class ReferenceLink(
    val id: String,
    val sourceType: ReferenceType,
    val sourceId: String,
    val targetType: ReferenceType,
    val targetId: String,
    val createdAt: Long
)

enum class ReferenceType {
    TOPIC, RESOURCE, OPINION, BOOKMARK, ANNOTATION
}

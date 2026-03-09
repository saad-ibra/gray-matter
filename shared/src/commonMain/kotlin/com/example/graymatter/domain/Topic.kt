package com.example.graymatter.domain

/**
 * Topics are internal organizational groupings.
 * Users cannot directly manage or assign topics - assignment is automatic but user-overridable.
 */
data class Topic(
    val id: String,
    val name: String,
    val notes: String? = null,
    val classificationKeywords: String = "", // comma-separated keywords for matching
    val resourceCount: Int = 0,
    val updatedAt: Long
)

/**
 * Represents a topic classification result.
 */
data class TopicClassification(
    val topicId: String,
    val topicName: String,
    val confidenceScore: Int, // 0-100
    val source: ClassificationSource
)

/**
 * Source of topic classification.
 */
enum class ClassificationSource {
    AUTOMATIC,      // Classified by keyword matching
    USER_SELECTED,  // User explicitly chose the topic
    USER_CREATED    // User created a new topic
}

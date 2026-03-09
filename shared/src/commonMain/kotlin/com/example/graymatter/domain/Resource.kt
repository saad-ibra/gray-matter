package com.example.graymatter.domain

/**
 * Represents the type of external resource.
 */
enum class ResourceType {
    PDF, MARKDOWN, UNSUPPORTED, IMAGE, WEB_LINK, EPUB, MOBI, CBZ, VIDEO, AUDIO
}

/**
 * Represents an external reference (web link, document, or image).
 * Each resource is linked to exactly one item.
 */
data class Resource(
    val id: String,
    val type: ResourceType,
    val url: String? = null,
    val filePath: String? = null,
    val extractedText: String? = null,
    val title: String? = null,
    val createdAt: Long
)

package com.example.graymatter.domain

/**
 * Represents a chapter/section entry in a document's table of contents.
 * Supports nested hierarchy via the level property.
 */
data class ChapterOutline(
    val title: String,
    val level: Int,           // 0 = top-level, 1 = sub-chapter, 2 = sub-sub, etc.
    val targetPage: Int,      // page number where this chapter starts
    val children: List<ChapterOutline> = emptyList()
)

package com.example.graymatter.domain

/**
 * Represents items in the Reference Selector tree.
 */
sealed class ReferenceSelectorItem {
    abstract val id: String
    abstract val isExpanded: Boolean
    abstract val isChecked: Boolean
    abstract val indentLevel: Int

    data class TopicItem(
        override val id: String,
        val name: String,
        override val isExpanded: Boolean = false,
        override val isChecked: Boolean = false
    ) : ReferenceSelectorItem() {
        override val indentLevel = 0
    }

    data class ResourceItem(
        override val id: String,
        val title: String,
        val type: String,
        val parentTopicId: String?,
        override val isExpanded: Boolean = false,
        override val isChecked: Boolean = false
    ) : ReferenceSelectorItem() {
        override val indentLevel = 1
    }

    data class DetailItem(
        override val id: String,
        val snippet: String,
        val parentResourceId: String,
        val isAnnotation: Boolean = false,
        override val isExpanded: Boolean = false, // Usually always false
        override val isChecked: Boolean = false
    ) : ReferenceSelectorItem() {
        override val indentLevel = 2
    }
}

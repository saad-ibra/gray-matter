package com.example.graymatter.domain.business

import com.example.graymatter.data.OpinionRepository
import com.example.graymatter.data.ReferenceLinkRepository
import com.example.graymatter.data.ResourceRepository
import com.example.graymatter.data.TopicRepository
import com.example.graymatter.domain.ReferenceLink
import com.example.graymatter.domain.ReferenceType
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * Service to automatically extract and synchronize [[reference]] links from markdown text.
 */
class AutoLinkService(
    private val topicRepository: TopicRepository,
    private val resourceRepository: ResourceRepository,
    private val opinionRepository: OpinionRepository,
    private val referenceLinkRepository: ReferenceLinkRepository
) {
    private val referenceRegex = Regex("\\[\\[(.*?)\\]\\]")

    /**
     * Extracts and synchronizes reference links from the provided text.
     * This will update the ReferenceLink database table for the given source.
     */
    suspend fun syncLinks(
        sourceId: String,
        sourceType: ReferenceType,
        text: String,
        manualLinks: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList()
    ) {
        // 1. Delete all existing links for this source to ensure a clean sync
        referenceLinkRepository.deleteReferenceLinksBySource(sourceId)

        val now = Clock.System.now().toEpochMilliseconds()
        val finalLinks = mutableListOf<ReferenceLink>()

        // 2. Add manual links
        manualLinks.forEach { linkItem ->
            val targetType = when (linkItem) {
                is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> ReferenceType.TOPIC
                is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> ReferenceType.RESOURCE
                is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> ReferenceType.OPINION
            }
            finalLinks.add(createLink(sourceId, sourceType, linkItem.id, targetType, now))
        }

        // 3. Extract and add auto links from [[Title]] markers
        val rawTitles = extractTitles(text)
        if (rawTitles.isNotEmpty()) {
            val allTopics = topicRepository.getAllTopics()
            val allResources = resourceRepository.resourcesStream.first()
            val allOpinions = opinionRepository.getAllOpinions().first()
            val prefixes = listOf("Topic: ", "Resource: ", "Opinion: ", "Knowledge: ")

            rawTitles.forEach { rawTitle ->
                // Strip known prefixes for better matching
                var title = rawTitle
                prefixes.forEach { prefix ->
                    if (title.startsWith(prefix, ignoreCase = true)) {
                        title = title.substring(prefix.length).trim()
                    }
                }

                // Try to find a matching Topic
                val topicMatch = allTopics.find { it.name.equals(title, ignoreCase = true) }
                if (topicMatch != null) {
                    if (finalLinks.none { it.targetId == topicMatch.id }) {
                        finalLinks.add(createLink(sourceId, sourceType, topicMatch.id, ReferenceType.TOPIC, now))
                    }
                }

                // Try to find a matching Resource
                val resourceMatch = allResources.find { it.title?.equals(title, ignoreCase = true) == true }
                if (resourceMatch != null) {
                    if (finalLinks.none { it.targetId == resourceMatch.id }) {
                        finalLinks.add(createLink(sourceId, sourceType, resourceMatch.id, ReferenceType.RESOURCE, now))
                    }
                }

                // Try to find a matching Opinion (Substring match since titles are snippets)
                val opinionMatch = allOpinions.find { it.text.contains(title, ignoreCase = true) }
                if (opinionMatch != null) {
                    if (finalLinks.none { it.targetId == opinionMatch.id }) {
                        finalLinks.add(createLink(sourceId, sourceType, opinionMatch.id, ReferenceType.OPINION, now))
                    }
                }
            }
        }

        // 4. Batch insert all unique links
        finalLinks.distinctBy { it.targetId }.forEach { link ->
            referenceLinkRepository.insertReferenceLink(link)
        }
    }

    private fun extractTitles(text: String): List<String> {
        return referenceRegex.findAll(text)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    private fun createLink(
        sourceId: String,
        sourceType: ReferenceType,
        targetId: String,
        targetType: ReferenceType,
        timestamp: Long
    ): ReferenceLink {
        return ReferenceLink(
            id = generateUuid(),
            sourceType = sourceType,
            sourceId = sourceId,
            targetType = targetType,
            targetId = targetId,
            createdAt = timestamp
        )
    }

    private fun generateUuid(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..32).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}

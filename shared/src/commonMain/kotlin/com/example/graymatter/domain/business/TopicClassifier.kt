package com.example.graymatter.domain.business

import com.example.graymatter.domain.Opinion
import com.example.graymatter.domain.Resource
import com.example.graymatter.domain.Topic
import com.example.graymatter.domain.TopicClassification
import com.example.graymatter.domain.ClassificationSource

/**
 * Simple deterministic topic classifier.
 * Uses keyword matching and simple heuristics to suggest topics.
 * Zero external dependencies, runs purely offline.
 */
class TopicClassifier {

    /**
     * Classifies an item based on its resource, opinions, and available topics.
     * Returns a list of suggested classifications sorted by confidence.
     */
    fun classify(
        resource: Resource,
        opinions: List<Opinion>,
        availableTopics: List<Topic>
    ): List<TopicClassification> {
        val combinedText = buildString {
            append(resource.title ?: "")
            append(" ")
            append(resource.extractedText ?: "")
            append(" ")
            opinions.forEach { append(it.text).append(" ") }
        }.lowercase()

        return availableTopics.map { topic ->
            val score = calculateScore(combinedText, topic)
            TopicClassification(
                topicId = topic.id,
                topicName = topic.name,
                confidenceScore = score,
                source = ClassificationSource.AUTOMATIC
            )
        }.filter { it.confidenceScore > 0 }
        .sortedByDescending { it.confidenceScore }
    }

    private fun calculateScore(text: String, topic: Topic): Int {
        var score = 0
        
        // Exact name match (high weight)
        if (text.contains(topic.name.lowercase())) {
            score += 50
        }
        
        // Keyword matching
        val keywords = topic.classificationKeywords.split(",").filter { it.isNotBlank() }
        for (keyword in keywords) {
            if (text.contains(keyword.trim().lowercase())) {
                score += 15
            }
        }
        
        return score.coerceAtMost(100)
    }
}

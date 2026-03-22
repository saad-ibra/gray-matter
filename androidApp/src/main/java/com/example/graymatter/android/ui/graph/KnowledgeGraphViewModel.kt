package com.example.graymatter.android.ui.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.graymatter.data.ItemRepository
import com.example.graymatter.data.OpinionRepository
import com.example.graymatter.data.ReferenceLinkRepository
import com.example.graymatter.data.ResourceRepository
import com.example.graymatter.data.TopicRepository
import com.example.graymatter.domain.ReferenceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class GraphDataState(
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList(),
    val isLoading: Boolean = true
)

class KnowledgeGraphViewModel(
    private val topicRepository: TopicRepository,
    private val resourceRepository: ResourceRepository,
    private val itemRepository: ItemRepository,
    private val opinionRepository: OpinionRepository,
    private val referenceLinkRepository: ReferenceLinkRepository
) : ViewModel() {

    private val _graphState = MutableStateFlow(GraphDataState())
    val graphState: StateFlow<GraphDataState> = _graphState.asStateFlow()

    init {
        loadGraphData()
    }

    fun loadGraphData() {
        viewModelScope.launch {
            _graphState.value = GraphDataState(isLoading = true)

            val nodes = mutableListOf<GraphNode>()
            val edges = mutableListOf<GraphEdge>()

            // 1. Fetch Topics
            val topics = topicRepository.getAllTopics()
            topics.forEach { topic ->
                nodes.add(
                    GraphNode(
                        id = topic.id,
                        type = NodeType.TOPIC,
                        label = topic.name,
                        radius = 30f + (topic.resourceCount * 5f).coerceAtMost(30f) // Scale by connections
                    )
                )
            }

            // 2. Fetch Resources and Items (Resources act as hubs)
            val items = itemRepository.itemsStream.first()
            val opinions = opinionRepository.getAllOpinions()
            val referenceLinks = referenceLinkRepository.getAllReferenceLinks().first()

            items.forEach { item ->
                val resource = resourceRepository.getResourceById(item.resourceId)
                if (resource != null) {
                    nodes.add(
                        GraphNode(
                            id = resource.id,
                            type = NodeType.RESOURCE,
                            label = resource.title ?: "Untitled",
                            radius = 20f
                        )
                    )

                    // Edge: Topic -> Resource
                    if (item.topicId != null) {
                        edges.add(
                            GraphEdge(
                                id = "${item.topicId}_${resource.id}",
                                source = nodes.find { it.id == item.topicId } ?: return@forEach,
                                target = nodes.last(),
                                weight = 1.5f
                            )
                        )
                    }
                }
            }

            // 3. Fetch Opinions
            opinions.forEach { opinion ->
                val nodeType = when {
                    opinion.text.startsWith("[DICT]") -> NodeType.DICTIONARY
                    opinion.text.startsWith("[TEMPLATE:") -> NodeType.TEMPLATE
                    opinion.text.startsWith("[CUSTOM:") -> NodeType.CUSTOM
                    opinion.pageNumber != null && opinion.text.startsWith(">") -> NodeType.ANNOTATION
                    opinion.pageNumber != null -> NodeType.BOOKMARK
                    else -> NodeType.OPINION
                }
                
                val displayLabel = when (nodeType) {
                    NodeType.DICTIONARY -> opinion.text.substringAfter("[DICT]").trim().take(20) + "..."
                    NodeType.TEMPLATE -> opinion.text.substringAfter("]\n").replace("|", ": ").replace("\n", " • ").trim().take(40) + "..."
                    NodeType.CUSTOM -> opinion.text.substringAfter("]\n").replace("|", ": ").replace("\n", " • ").trim().take(40) + "..."
                    NodeType.ANNOTATION -> opinion.text.substringAfter(">").trim().take(20) + "..."
                    NodeType.BOOKMARK -> if (opinion.text.isNotBlank()) opinion.text.take(20) + "..." else "Bookmark pg ${opinion.pageNumber}"
                    else -> opinion.text.take(20) + "..."
                }

                nodes.add(
                    GraphNode(
                        id = opinion.id,
                        type = nodeType,
                        label = displayLabel,
                        radius = when (nodeType) {
                            NodeType.TOPIC -> 44f
                            NodeType.RESOURCE -> 30f
                            else -> 18f
                        }
                    )
                )

                // Edge: Resource -> Opinion
                val item = itemRepository.getItemById(opinion.itemId)
                if (item != null) {
                    val resourceNode = nodes.find { it.id == item.resourceId }
                    if (resourceNode != null) {
                        edges.add(
                            GraphEdge(
                                id = "${resourceNode.id}_${opinion.id}",
                                source = resourceNode,
                                target = nodes.last(),
                                weight = 0.8f
                            )
                        )
                    }
                }
            }

            // 4. Setup Reference Links
            referenceLinks.forEach { link ->
                // Links are created from Opinions to other Types
                val sourceNode = nodes.find { it.id == link.sourceId }
                
                val targetNodeId = when (link.targetType) {
                    ReferenceType.TOPIC -> link.targetId
                    ReferenceType.RESOURCE -> link.targetId
                    ReferenceType.OPINION -> link.targetId
                    ReferenceType.BOOKMARK -> link.targetId
                    ReferenceType.ANNOTATION -> link.targetId
                }
                
                val targetNode = nodes.find { it.id == targetNodeId }

                if (sourceNode != null && targetNode != null) {
                    edges.add(
                        GraphEdge(
                            id = "${sourceNode.id}_${targetNode.id}_ref",
                            source = sourceNode,
                            target = targetNode,
                            weight = 0.5f // Weaker bond for references
                        )
                    )
                }
            }

            _graphState.value = GraphDataState(
                nodes = nodes,
                edges = edges,
                isLoading = false
            )
        }
    }
}

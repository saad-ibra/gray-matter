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
            val existingEdgePairs = mutableSetOf<Pair<String, String>>()
            val oldNodesMap = _graphState.value.nodes.associateBy { it.id }

            // 1. Fetch Topics
            val topics = topicRepository.getAllTopics()
            topics.forEach { topic ->
                val node = GraphNode(
                    id = topic.id,
                    type = NodeType.TOPIC,
                    label = topic.name,
                    radius = 30f + (topic.resourceCount * 5f).coerceAtMost(30f) // Scale by connections
                )
                oldNodesMap[node.id]?.let { old ->
                    node.x = old.x; node.y = old.y; node.z = old.z
                    node.vx = old.vx; node.vy = old.vy; node.vz = old.vz
                }
                nodes.add(node)
            }

            // 2. Fetch Resources and Items (Resources act as hubs)
            val allItems = itemRepository.itemsStream.first()
            val allOpinions = opinionRepository.getAllOpinions().first()
            val allReferenceLinks = referenceLinkRepository.getAllReferenceLinks().first()

            val resourceMap = resourceRepository.resourcesStream.first().associateBy { it.id }
            val itemMap = allItems.associateBy { it.id }

            allItems.forEach { item ->
                val resource = resourceMap[item.resourceId]
                if (resource != null) {
                    val resourceNode = GraphNode(
                        id = resource.id,
                        type = NodeType.RESOURCE,
                        label = resource.title ?: "Untitled",
                        radius = 20f
                    )
                    oldNodesMap[resourceNode.id]?.let { old ->
                        resourceNode.x = old.x; resourceNode.y = old.y; resourceNode.z = old.z
                        resourceNode.vx = old.vx; resourceNode.vy = old.vy; resourceNode.vz = old.vz
                    }
                    nodes.add(resourceNode)

                    // Edge: Topic -> Resource
                    if (item.topicId != null) {
                        val topicNode = nodes.find { it.id == item.topicId }
                        if (topicNode != null && existingEdgePairs.add(topicNode.id to resourceNode.id)) {
                            edges.add(
                                GraphEdge(
                                    id = "${item.topicId}_${resource.id}",
                                    source = topicNode,
                                    target = resourceNode,
                                    weight = 1.5f
                                )
                            )
                        }
                    }
                }
            }

            // 3. Process Opinions
            allOpinions.forEach { opinion ->
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
                    NodeType.TEMPLATE -> opinion.text.substringAfter("]\n").replace("### ", "").replace("|", ": ").replace("\n", " • ").trim().take(40) + "..."
                    NodeType.CUSTOM -> opinion.text.substringAfter("]\n").replace("### ", "").replace("|", ": ").replace("\n", " • ").trim().take(40) + "..."
                    NodeType.ANNOTATION -> opinion.text.substringAfter(">").trim().take(20) + "..."
                    NodeType.BOOKMARK -> if (opinion.text.isNotBlank()) opinion.text.take(20) + "..." else "Bookmark pg ${opinion.pageNumber}"
                    else -> opinion.text.take(20) + "..."
                }

                val opinionNode = GraphNode(
                    id = opinion.id,
                    type = nodeType,
                    label = displayLabel,
                    radius = when (nodeType) {
                        NodeType.TOPIC -> 44f
                        NodeType.RESOURCE -> 30f
                        else -> 18f
                    }
                )
                oldNodesMap[opinionNode.id]?.let { old ->
                    opinionNode.x = old.x; opinionNode.y = old.y; opinionNode.z = old.z
                    opinionNode.vx = old.vx; opinionNode.vy = old.vy; opinionNode.vz = old.vz
                }
                nodes.add(opinionNode)

                // Edge: Resource -> Opinion
                val item = itemMap[opinion.itemId]
                if (item != null) {
                    val resourceNode = nodes.find { it.id == item.resourceId }
                    if (resourceNode != null && existingEdgePairs.add(resourceNode.id to opinionNode.id)) {
                        edges.add(
                            GraphEdge(
                                id = "${resourceNode.id}_${opinion.id}",
                                source = resourceNode,
                                target = opinionNode,
                                weight = 0.8f
                            )
                        )
                    }
                }
            }

            // 4. Setup Reference Links
            allReferenceLinks.forEach { link ->
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

                if (sourceNode != null && targetNode != null && existingEdgePairs.add(sourceNode.id to targetNode.id)) {
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

    fun deleteNodeById(node: GraphNode) {
        viewModelScope.launch {
            when (node.type) {
                NodeType.TOPIC -> topicRepository.deleteTopic(node.id)
                NodeType.RESOURCE -> {
                    val item = itemRepository.getItemByResourceId(node.id)
                    if (item != null) {
                        itemRepository.deleteItem(item.id)
                        
                        // Delete physical file if it's a markdown resource in our private dir
                        val resource = resourceRepository.getResourceById(node.id)
                        val filePath = resource?.filePath
                        if (filePath != null && filePath.contains("/files/resources/")) {
                            val file = java.io.File(filePath)
                            if (file.exists()) {
                                file.delete()
                            }
                        }
                        resourceRepository.deleteResource(node.id)
                    }
                }
                NodeType.OPINION,
                NodeType.ANNOTATION,
                NodeType.BOOKMARK,
                NodeType.DICTIONARY,
                NodeType.TEMPLATE,
                NodeType.CUSTOM -> {
                    opinionRepository.deleteOpinion(node.id)
                    referenceLinkRepository.deleteReferenceLinksBySource(node.id)
                }
            }
            loadGraphData()
        }
    }
}

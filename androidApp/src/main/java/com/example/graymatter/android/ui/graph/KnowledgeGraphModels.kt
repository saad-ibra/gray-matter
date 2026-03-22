package com.example.graymatter.android.ui.graph

import androidx.compose.ui.geometry.Offset
import kotlin.math.max
import kotlin.math.sqrt

enum class NodeType {
    TOPIC, RESOURCE, OPINION, ANNOTATION, BOOKMARK, TEMPLATE, CUSTOM, DICTIONARY
}

data class GraphNode(
    val id: String,
    val type: NodeType,
    val label: String,
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var radius: Float = 20f,
    var isPinned: Boolean = false // e.g. when being dragged
) {
    // Helper to calculate distance to another point
    fun distanceTo(otherX: Float, otherY: Float): Float {
        val dx = x - otherX
        val dy = y - otherY
        return sqrt(dx * dx + dy * dy)
    }
}

data class GraphEdge(
    val id: String,
    val source: GraphNode,
    val target: GraphNode,
    val weight: Float = 1f
)

class ForceSimulator(
    val nodes: MutableList<GraphNode> = mutableListOf(),
    val edges: MutableList<GraphEdge> = mutableListOf()
) {
    var repulsionStrength = 3500f
    var springStrength = 0.15f // Increased to pull clusters tight
    var springLength = 90f // Decreased to keep children close to parents
    var centerAttraction = 0.02f
    var damping = 0.85f
    
    // Bounds for initial random placement
    var width = 1000f
    var height = 1000f

    fun clear() {
        nodes.clear()
        edges.clear()
    }

    fun addNode(node: GraphNode) {
        if (node.x == 0f && node.y == 0f) {
            node.x = (Math.random() * width).toFloat()
            node.y = (Math.random() * height).toFloat()
        }
        nodes.add(node)
    }

    fun addEdge(sourceId: String, targetId: String, weight: Float = 1f) {
        val source = nodes.find { it.id == sourceId }
        val target = nodes.find { it.id == targetId }
        if (source != null && target != null) {
            edges.add(GraphEdge("${sourceId}_${targetId}", source, target, weight))
        }
    }

    fun tick() {
        // 1. Repulsion between all pairs of nodes
        for (i in 0 until nodes.size) {
            val n1 = nodes[i]
            for (j in i + 1 until nodes.size) {
                val n2 = nodes[j]
                val dx = n2.x - n1.x
                val dy = n2.y - n1.y
                var distSq = dx * dx * 0.4f + dy * dy * 1.2f // Elliptical distance: repels horizontally more to prevent text overlap
                
                // Prevent division by zero and extreme forces
                if (distSq < 100f) {
                    distSq = 100f
                }
                
                // Expand repulsion between topic nodes to separate clusters
                var currentRepulsion = repulsionStrength
                
                // Stronger repulsion for Topics
                if (n1.type == NodeType.TOPIC && n2.type == NodeType.TOPIC) {
                    currentRepulsion *= 35f
                } else if (n1.type != NodeType.TOPIC && n1.type != NodeType.RESOURCE) {
                    currentRepulsion *= 5f
                }
                
                // Emergency collision padding (increased to accommodate larger radii and text)
                val minDistance = n1.radius + n2.radius + 60f 
                if (distSq < minDistance * minDistance) {
                    currentRepulsion *= 8f
                }

                val force = currentRepulsion / distSq
                val dist = sqrt(dx * dx + dy * dy + 0.1f) // True distance for vector normalization
                
                // Direction vector normalized
                val nx = dx / dist
                val ny = dy / dist
                
                val fx = nx * force
                val fy = ny * force
                
                if (!n1.isPinned) {
                    n1.vx -= fx
                    n1.vy -= fy
                }
                if (!n2.isPinned) {
                    n2.vx += fx
                    n2.vy += fy
                }
            }
        }
        
        // 2. Spring attraction between connected nodes
        for (edge in edges) {
            val n1 = edge.source
            val n2 = edge.target
            
            val dx = n2.x - n1.x
            val dy = n2.y - n1.y
            val dist = max(sqrt(dx * dx + dy * dy), 1f)
            
            val displacement = dist - springLength
            val force = displacement * springStrength * edge.weight
            
            val nx = dx / dist
            val ny = dy / dist
            
            val fx = nx * force
            val fy = ny * force
            
            if (!n1.isPinned) {
                n1.vx += fx
                n1.vy += fy
            }
            if (!n2.isPinned) {
                n2.vx -= fx
                n2.vy -= fy
            }
        }
        
        // 3. Center attraction and Tree-gravity bias
        val centerX = width / 2f
        
        for (node in nodes) {
            if (node.isPinned) continue
            
            // Gentle center attraction to keep things in view
            val dx = centerX - node.x
            node.vx += dx * (centerAttraction * 0.3f)
            
            // Cluster physics: Topics float up (buoyancy), children pull down (gravity)
            // Combined with spring tension, this naturally forms tree-like clusters hanging from topics!
            if (node.type == NodeType.TOPIC) {
                // Buoyancy pulling cluster anchors to the top
                node.vy -= 1.8f
            } else if (node.type == NodeType.RESOURCE) {
                // Mild gravity for resources so they sit under topics
                node.vy += 0.8f
            } else {
                // Stronger gravity for leaf nodes (opinions, etc.) so they hang at the bottom of the cluster
                node.vy += 1.2f
            }
        }
        
        // 4. Apply velocities and damping
        for (node in nodes) {
            if (node.isPinned) {
                node.vx = 0f
                node.vy = 0f
                continue
            }
            
            node.x += node.vx
            node.y += node.vy
            
            node.vx *= damping
            node.vy *= damping
        }
    }
    
    /**
     * Run the simulation for a fixed number of ticks (useful for initial layout convergence)
     */
    fun converge(ticks: Int = 80) {
        repeat(ticks) {
            tick()
        }
    }
}

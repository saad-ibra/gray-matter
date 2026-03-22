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
    var z: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vz: Float = 0f,
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
    var repulsionStrength = 2000f
    var springStrength = 0.08f // Softened to reduce harsh jiggling
    var springLength = 100f 
    var centerAttraction = 0.02f
    var damping = 0.8f // Increased friction to stabilize nodes faster
    
    // Bounds for initial random placement
    var width = 1000f
    var height = 1000f

    fun clear() {
        nodes.clear()
        edges.clear()
    }

    fun addNode(node: GraphNode) {
        if (node.x == 0f && node.y == 0f && node.z == 0f) {
            node.x = (Math.random() * width).toFloat()
            node.y = (Math.random() * height).toFloat()
            node.z = (Math.random() * 400f - 200f).toFloat() // Spawn in 3D volume
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
                val dz = n2.z - n1.z
                var distSq = dx * dx * 0.4f + dy * dy * 1.2f + dz * dz * 0.8f // Elliptical 3D distance
                
                // Prevent division by zero and extreme forces
                if (distSq < 100f) {
                    distSq = 100f
                }
                
                // Expand repulsion between topic nodes to separate clusters
                var currentRepulsion = repulsionStrength
                
                // Stronger repulsion for Topics
                if (n1.type == NodeType.TOPIC && n2.type == NodeType.TOPIC) {
                    currentRepulsion *= 25f
                } else if (n1.type != NodeType.TOPIC && n1.type != NodeType.RESOURCE) {
                    currentRepulsion *= 4f
                }
                
                // Emergency collision padding (decreased slightly so clusters can pack efficiently)
                val minDistance = n1.radius + n2.radius + 45f 
                if (distSq < minDistance * minDistance) {
                    currentRepulsion *= 4f // Softened repel to prevent dancing
                }

                val force = currentRepulsion / distSq
                val dist = sqrt(dx * dx + dy * dy + dz * dz + 0.1f) // True 3D distance for vector normalization
                
                // Direction vector normalized
                val nx = dx / dist
                val ny = dy / dist
                val nz = dz / dist
                
                val fx = nx * force
                val fy = ny * force
                val fz = nz * force
                
                if (!n1.isPinned) {
                    n1.vx -= fx
                    n1.vy -= fy
                    n1.vz -= fz
                }
                if (!n2.isPinned) {
                    n2.vx += fx
                    n2.vy += fy
                    n2.vz += fz
                }
            }
        }
        
        // 2. Spring attraction between connected nodes
        for (edge in edges) {
            val n1 = edge.source
            val n2 = edge.target
            
            val dx = n2.x - n1.x
            val dy = n2.y - n1.y
            val dz = n2.z - n1.z
            val dist = max(sqrt(dx * dx + dy * dy + dz * dz), 1f)
            
            val displacement = dist - springLength
            val force = displacement * springStrength * edge.weight
            
            val nx = dx / dist
            val ny = dy / dist
            val nz = dz / dist
            
            val fx = nx * force
            val fy = ny * force
            val fz = nz * force
            
            if (!n1.isPinned) {
                n1.vx += fx
                n1.vy += fy
                n1.vz += fz
            }
            if (!n2.isPinned) {
                n2.vx -= fx
                n2.vy -= fy
                n2.vz -= fz
            }
        }
        
        // 3. Center attraction and Tree-gravity bias
        val centerX = width / 2f
        
        for (node in nodes) {
            if (node.isPinned) continue
            
            // Gentle center attraction to keep things in view
            val dx = centerX - node.x
            val dz = 0f - node.z // Attract slightly to Z=0
            node.vx += dx * (centerAttraction * 0.5f)
            node.vz += dz * (centerAttraction * 0.5f)
            
            // Cluster physics: Topics settle natively at a higher bounded Y instead of infinitely drifting
            if (node.type == NodeType.TOPIC) {
                val targetY = height * 0.15f
                node.vy += (targetY - node.y) * 0.05f 
            } else if (node.type == NodeType.RESOURCE) {
                // Mild gravity for resources so they sit under topics
                node.vy += 0.5f
            } else {
                // Stronger gravity for leaf nodes (opinions, etc.) so they hang at the bottom of the cluster
                node.vy += 0.8f
            }
        }
        
        // 4. Apply velocities and damping
        for (node in nodes) {
            if (node.isPinned) {
                node.vx = 0f
                node.vy = 0f
                node.vz = 0f
                continue
            }
            
            node.x += node.vx
            node.y += node.vy
            node.z += node.vz
            
            node.vx *= damping
            node.vy *= damping
            node.vz *= damping
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

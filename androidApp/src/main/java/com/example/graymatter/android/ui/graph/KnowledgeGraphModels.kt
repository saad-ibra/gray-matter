package com.example.graymatter.android.ui.graph

import androidx.compose.ui.geometry.Offset
import kotlin.math.max
import kotlin.math.sqrt

enum class NodeType {
    TOPIC, RESOURCE, OPINION, ANNOTATION, BOOKMARK, TEMPLATE, CUSTOM, LOOKUP
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

// ─── Spatial hash for O(1) tap hit-testing ────────────────────────────────────

class SpatialHashGrid(private val cellSize: Float = 80f) {
    private val cells = HashMap<Long, MutableList<Int>>(128)

    fun clear() { cells.clear() }

    fun insert(index: Int, sx: Float, sy: Float, radius: Float) {
        val expand = radius
        val minCx = ((sx - expand) / cellSize).toInt()
        val maxCx = ((sx + expand) / cellSize).toInt()
        val minCy = ((sy - expand) / cellSize).toInt()
        val maxCy = ((sy + expand) / cellSize).toInt()
        for (cx in minCx..maxCx) {
            for (cy in minCy..maxCy) {
                val key = cx.toLong() shl 32 or (cy.toLong() and 0xFFFFFFFFL)
                cells.getOrPut(key) { mutableListOf() }.add(index)
            }
        }
    }

    fun query(px: Float, py: Float): List<Int> {
        val cx = (px / cellSize).toInt()
        val cy = (py / cellSize).toInt()
        val key = cx.toLong() shl 32 or (cy.toLong() and 0xFFFFFFFFL)
        return cells[key] ?: emptyList()
    }
}

// ─── Barnes-Hut Octree ────────────────────────────────────────────────────────

private class OctreeNode(
    val cx: Float, val cy: Float, val cz: Float,  // center of the cell
    val halfSize: Float
) {
    var mass: Int = 0
    var comX: Float = 0f  // center of mass
    var comY: Float = 0f
    var comZ: Float = 0f
    var maxRepulsionMultiplier: Float = 1f
    var singleNodeIdx: Int = -1  // if this is a leaf with exactly one node
    var children: Array<OctreeNode?>? = null
}

private class Octree {
    private var root: OctreeNode? = null

    fun build(
        px: FloatArray, py: FloatArray, pz: FloatArray,
        nodeTypes: Array<NodeType>,
        count: Int
    ) {
        if (count == 0) { root = null; return }

        // Find bounding box
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        for (i in 0 until count) {
            if (px[i] < minX) minX = px[i]; if (px[i] > maxX) maxX = px[i]
            if (py[i] < minY) minY = py[i]; if (py[i] > maxY) maxY = py[i]
            if (pz[i] < minZ) minZ = pz[i]; if (pz[i] > maxZ) maxZ = pz[i]
        }

        val halfSize = max(max(maxX - minX, maxY - minY), max(maxZ - minZ, 1f)) / 2f + 1f
        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        val cz = (minZ + maxZ) / 2f

        root = OctreeNode(cx, cy, cz, halfSize)
        for (i in 0 until count) {
            insert(root!!, i, px[i], py[i], pz[i], repulsionMultiplierFor(nodeTypes[i]), 0)
        }
    }

    private fun repulsionMultiplierFor(type: NodeType): Float = when (type) {
        NodeType.TOPIC -> 25f  // highest — they'll be multiplied with the target's multiplier
        NodeType.RESOURCE -> 8f
        else -> 4f
    }

    private fun insert(
        node: OctreeNode, idx: Int,
        x: Float, y: Float, z: Float,
        repMul: Float, depth: Int
    ) {
        if (depth > 40) return // safety — prevents infinite recursion for coincident points

        if (node.mass == 0) {
            // Empty leaf — place here
            node.mass = 1
            node.comX = x; node.comY = y; node.comZ = z
            node.singleNodeIdx = idx
            node.maxRepulsionMultiplier = repMul
            return
        }

        // Subdivide if this was a leaf
        if (node.children == null) {
            node.children = arrayOfNulls(8)
            // Re-insert existing single node
            val oldIdx = node.singleNodeIdx
            if (oldIdx >= 0) {
                val octant = octantFor(node, node.comX, node.comY, node.comZ)
                val child = getOrCreateChild(node, octant)
                insert(child, oldIdx, node.comX, node.comY, node.comZ, node.maxRepulsionMultiplier, depth + 1)
                node.singleNodeIdx = -1
            }
        }

        // Insert new node
        val octant = octantFor(node, x, y, z)
        val child = getOrCreateChild(node, octant)
        insert(child, idx, x, y, z, repMul, depth + 1)

        // Update mass and center-of-mass
        val totalMass = node.mass + 1
        node.comX = (node.comX * node.mass + x) / totalMass
        node.comY = (node.comY * node.mass + y) / totalMass
        node.comZ = (node.comZ * node.mass + z) / totalMass
        node.mass = totalMass
        if (repMul > node.maxRepulsionMultiplier) node.maxRepulsionMultiplier = repMul
    }

    private fun octantFor(node: OctreeNode, x: Float, y: Float, z: Float): Int {
        var octant = 0
        if (x > node.cx) octant = octant or 1
        if (y > node.cy) octant = octant or 2
        if (z > node.cz) octant = octant or 4
        return octant
    }

    private fun getOrCreateChild(parent: OctreeNode, octant: Int): OctreeNode {
        val children = parent.children!!
        var child = children[octant]
        if (child == null) {
            val hs = parent.halfSize / 2f
            val cx = parent.cx + if (octant and 1 != 0) hs else -hs
            val cy = parent.cy + if (octant and 2 != 0) hs else -hs
            val cz = parent.cz + if (octant and 4 != 0) hs else -hs
            child = OctreeNode(cx, cy, cz, hs)
            children[octant] = child
        }
        return child
    }

    /**
     * Compute repulsion force on node at index [idx].
     * Results accumulated directly into [outFx], [outFy], [outFz] arrays.
     */
    fun calculateForce(
        idx: Int,
        nodePx: Float, nodePy: Float, nodePz: Float,
        nodeType: NodeType,
        nodeRadius: Float,
        baseRepulsion: Float,
        theta: Float,
        outFx: FloatArray, outFy: FloatArray, outFz: FloatArray,
        allRadii: FloatArray, allTypes: Array<NodeType>
    ) {
        val r = root ?: return
        walkForce(r, idx, nodePx, nodePy, nodePz, nodeType, nodeRadius,
            baseRepulsion, theta, outFx, outFy, outFz, allRadii, allTypes)
    }

    private fun walkForce(
        cell: OctreeNode,
        idx: Int,
        nodePx: Float, nodePy: Float, nodePz: Float,
        nodeType: NodeType,
        nodeRadius: Float,
        baseRepulsion: Float,
        theta: Float,
        outFx: FloatArray, outFy: FloatArray, outFz: FloatArray,
        allRadii: FloatArray, allTypes: Array<NodeType>
    ) {
        if (cell.mass == 0) return

        // Single-node leaf — exact force
        if (cell.mass == 1 && cell.singleNodeIdx >= 0) {
            if (cell.singleNodeIdx == idx) return // skip self
            applyRepulsion(
                idx, nodePx, nodePy, nodePz, nodeType, nodeRadius,
                cell.comX, cell.comY, cell.comZ,
                cell.singleNodeIdx, allRadii, allTypes,
                baseRepulsion, 1,
                outFx, outFy, outFz
            )
            return
        }

        // Check Barnes-Hut criterion: cellSize / distance < theta
        val dx = cell.comX - nodePx
        val dy = cell.comY - nodePy
        val dz = cell.comZ - nodePz
        val distSq = dx * dx + dy * dy + dz * dz + 0.1f
        val cellSize = cell.halfSize * 2f

        if (cellSize * cellSize / distSq < theta * theta) {
            // Treat entire cell as a pseudo-node at its center-of-mass
            applyRepulsionApprox(
                idx, nodePx, nodePy, nodePz, nodeType,
                cell.comX, cell.comY, cell.comZ,
                cell.mass, cell.maxRepulsionMultiplier,
                baseRepulsion, outFx, outFy, outFz
            )
            return
        }

        // Recurse into children
        val children = cell.children ?: return
        for (i in 0 until 8) {
            children[i]?.let {
                walkForce(it, idx, nodePx, nodePy, nodePz, nodeType, nodeRadius,
                    baseRepulsion, theta, outFx, outFy, outFz, allRadii, allTypes)
            }
        }
    }

    /** Exact pairwise repulsion — preserves all type-specific multipliers and collision padding */
    private fun applyRepulsion(
        idx: Int,
        px1: Float, py1: Float, pz1: Float, type1: NodeType, radius1: Float,
        px2: Float, py2: Float, pz2: Float,
        otherIdx: Int, allRadii: FloatArray, allTypes: Array<NodeType>,
        baseRepulsion: Float, mass: Int,
        outFx: FloatArray, outFy: FloatArray, outFz: FloatArray
    ) {
        val type2 = allTypes[otherIdx]
        val radius2 = allRadii[otherIdx]

        val dx = px1 - px2
        val dy = py1 - py2
        val dz = pz1 - pz2
        var distSq = dx * dx * 0.4f + dy * dy * 1.2f + dz * dz * 0.8f
        if (distSq < 100f) distSq = 100f

        var currentRepulsion = baseRepulsion
        val isTopicVsResource = (type1 == NodeType.TOPIC && type2 == NodeType.RESOURCE) ||
                (type1 == NodeType.RESOURCE && type2 == NodeType.TOPIC)

        if (type1 == NodeType.TOPIC && type2 == NodeType.TOPIC) {
            currentRepulsion *= 25f
        } else if (isTopicVsResource) {
            currentRepulsion *= 8f
        } else if (type1 != NodeType.TOPIC && type1 != NodeType.RESOURCE) {
            currentRepulsion *= 4f
        }

        var minDistance = radius1 + radius2 + 45f
        if (isTopicVsResource) {
            minDistance *= 2f
        }
        if (distSq < minDistance * minDistance) {
            currentRepulsion *= 4f
        }

        val force = currentRepulsion * mass / distSq
        val dist = sqrt(dx * dx + dy * dy + dz * dz + 0.1f)
        val nx = dx / dist
        val ny = dy / dist
        val nz = dz / dist

        outFx[idx] += nx * force
        outFy[idx] += ny * force
        outFz[idx] += nz * force
    }

    /** Approximate repulsion for distant clusters — uses max multiplier conservatively */
    private fun applyRepulsionApprox(
        idx: Int,
        px1: Float, py1: Float, pz1: Float, type1: NodeType,
        comX: Float, comY: Float, comZ: Float,
        mass: Int, clusterMaxMul: Float,
        baseRepulsion: Float,
        outFx: FloatArray, outFy: FloatArray, outFz: FloatArray
    ) {
        val dx = px1 - comX
        val dy = py1 - comY
        val dz = pz1 - comZ
        var distSq = dx * dx * 0.4f + dy * dy * 1.2f + dz * dz * 0.8f
        if (distSq < 100f) distSq = 100f

        // Use conservative multiplier: max of this node's type multiplier and the cluster's max
        val selfMul = when {
            type1 == NodeType.TOPIC -> 25f
            type1 == NodeType.RESOURCE -> 8f
            else -> 4f
        }
        // Geometric mean gives a reasonable approximation of the pairwise interaction
        val effectiveMul = sqrt(selfMul * clusterMaxMul)
        val currentRepulsion = baseRepulsion * effectiveMul

        val force = currentRepulsion * mass / distSq
        val dist = sqrt(dx * dx + dy * dy + dz * dz + 0.1f)
        val nx = dx / dist
        val ny = dy / dist
        val nz = dz / dist

        outFx[idx] += nx * force
        outFy[idx] += ny * force
        outFz[idx] += nz * force
    }
}

// ─── Force Simulator ──────────────────────────────────────────────────────────

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

    // ── SoA hot arrays (cache-friendly) ──
    private var count = 0
    private var px = FloatArray(0)
    private var py = FloatArray(0)
    private var pz = FloatArray(0)
    private var vxArr = FloatArray(0)
    private var vyArr = FloatArray(0)
    private var vzArr = FloatArray(0)
    private var radii = FloatArray(0)
    private var pinned = BooleanArray(0)
    private var types = arrayOf<NodeType>()

    // ── Force accumulation scratch arrays ──
    private var fx = FloatArray(0)
    private var fy = FloatArray(0)
    private var fz = FloatArray(0)

    // ── Barnes-Hut octree ──
    private val octree = Octree()
    private val theta = 0.8f

    // ── Edge index for O(1) lookups ──
    private val nodeIndex = HashMap<String, Int>(64)

    // ── Kinetic energy cutoff ──
    var isSettled: Boolean = false
        private set
    private var settledFrameCount = 0
    private val settleThreshold = 0.5f

    fun wake() {
        isSettled = false
        settledFrameCount = 0
    }

    fun clear() {
        nodes.clear()
        edges.clear()
        nodeIndex.clear()
        count = 0
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

    /** Rebuild internal SoA arrays from the current [nodes] list. Call after setup. */
    fun rebuildArrays() {
        count = nodes.size
        if (px.size < count) {
            px = FloatArray(count)
            py = FloatArray(count)
            pz = FloatArray(count)
            vxArr = FloatArray(count)
            vyArr = FloatArray(count)
            vzArr = FloatArray(count)
            radii = FloatArray(count)
            pinned = BooleanArray(count)
            types = Array(count) { NodeType.TOPIC }
            fx = FloatArray(count)
            fy = FloatArray(count)
            fz = FloatArray(count)
        }
        nodeIndex.clear()
        for (i in 0 until count) {
            val n = nodes[i]
            px[i] = n.x; py[i] = n.y; pz[i] = n.z
            vxArr[i] = n.vx; vyArr[i] = n.vy; vzArr[i] = n.vz
            radii[i] = n.radius
            pinned[i] = n.isPinned
            types[i] = n.type
            nodeIndex[n.id] = i
        }
    }

    /** Write SoA positions back into the GraphNode objects (for external reads). */
    private fun syncBack() {
        for (i in 0 until count) {
            val n = nodes[i]
            n.x = px[i]; n.y = py[i]; n.z = pz[i]
            n.vx = vxArr[i]; n.vy = vyArr[i]; n.vz = vzArr[i]
        }
    }

    fun tick(speedMultiplier: Float = 1.0f) {
        if (count == 0) {
            if (nodes.isNotEmpty()) rebuildArrays()
            if (count == 0) return
        }

        // ── Zero force accumulators ──
        for (i in 0 until count) {
            fx[i] = 0f; fy[i] = 0f; fz[i] = 0f
        }

        // ── 1. Barnes-Hut repulsion O(N log N) ──
        octree.build(px, py, pz, types, count)
        for (i in 0 until count) {
            if (pinned[i]) continue
            octree.calculateForce(
                i, px[i], py[i], pz[i],
                types[i], radii[i],
                repulsionStrength, theta,
                fx, fy, fz,
                radii, types
            )
        }
        
        // ── 2. Spring attraction between connected nodes ──
        for (edge in edges) {
            val i1 = nodeIndex[edge.source.id] ?: continue
            val i2 = nodeIndex[edge.target.id] ?: continue
            
            val dx = px[i2] - px[i1]
            val dy = py[i2] - py[i1]
            val dz = pz[i2] - pz[i1]
            val dist = max(sqrt(dx * dx + dy * dy + dz * dz), 1f)
            
            val isTopicVsResource = (types[i1] == NodeType.TOPIC && types[i2] == NodeType.RESOURCE) || 
                                   (types[i1] == NodeType.RESOURCE && types[i2] == NodeType.TOPIC)
            
            val effectiveSpringLength = if (isTopicVsResource) springLength * 2f else springLength
            
            val displacement = dist - effectiveSpringLength
            val force = displacement * springStrength * edge.weight
            
            val nx = dx / dist
            val ny = dy / dist
            val nz = dz / dist
            
            val sfx = nx * force
            val sfy = ny * force
            val sfz = nz * force
            
            // Orbital tangential velocity for leaf nodes orbiting resources
            var ox1 = 0f; var oy1 = 0f; var oz1 = 0f
            var ox2 = 0f; var oy2 = 0f; var oz2 = 0f
            
            val isN1Leaf = types[i1] != NodeType.TOPIC && types[i1] != NodeType.RESOURCE
            val isN2Leaf = types[i2] != NodeType.TOPIC && types[i2] != NodeType.RESOURCE
            val orbitSpeed = 2.5f * speedMultiplier
            
            if (types[i1] == NodeType.RESOURCE && isN2Leaf) {
                val len = sqrt(dz * dz + dx * dx + 0.1f)
                ox2 = (dz / len) * orbitSpeed
                oz2 = (-dx / len) * orbitSpeed
            } else if (types[i2] == NodeType.RESOURCE && isN1Leaf) {
                val len = sqrt(dz * dz + dx * dx + 0.1f)
                ox1 = (-dz / len) * orbitSpeed
                oz1 = (dx / len) * orbitSpeed
            }
            
            if (!pinned[i1]) {
                fx[i1] += sfx + ox1
                fy[i1] += sfy + oy1
                fz[i1] += sfz + oz1
            }
            if (!pinned[i2]) {
                fx[i2] -= sfx - ox2
                fy[i2] -= sfy - oy2
                fz[i2] -= sfz - oz2
            }
        }
        
        // ── 3. Center attraction and tree-gravity bias ──
        val centerX = width / 2f
        val centerY = height / 2f
        
        for (i in 0 until count) {
            if (pinned[i]) continue
            
            val dx = centerX - px[i]
            val dy = centerY - py[i]
            val dz = 0f - pz[i]
            
            when (types[i]) {
                NodeType.TOPIC -> {
                    fx[i] += dx * 0.15f * speedMultiplier
                    fy[i] += dy * 0.15f * speedMultiplier
                    fz[i] += dz * 0.15f * speedMultiplier
                }
                NodeType.RESOURCE -> {
                    fx[i] -= dx * 0.02f * speedMultiplier
                    fy[i] -= dy * 0.02f * speedMultiplier
                    fz[i] -= dz * 0.02f * speedMultiplier
                }
                else -> {
                    fx[i] += dx * 0.005f * speedMultiplier
                    fy[i] += dy * 0.005f * speedMultiplier
                    fz[i] += dz * 0.005f * speedMultiplier
                }
            }
        }
        
        // ── 4. Apply velocities and damping; compute kinetic energy ──
        var totalKE = 0f
        for (i in 0 until count) {
            if (pinned[i]) {
                vxArr[i] = 0f; vyArr[i] = 0f; vzArr[i] = 0f
                continue
            }
            
            vxArr[i] += fx[i]
            vyArr[i] += fy[i]
            vzArr[i] += fz[i]
            
            px[i] += vxArr[i] * speedMultiplier
            py[i] += vyArr[i] * speedMultiplier
            pz[i] += vzArr[i] * speedMultiplier
            
            vxArr[i] *= damping
            vyArr[i] *= damping
            vzArr[i] *= damping
            
            totalKE += vxArr[i] * vxArr[i] + vyArr[i] * vyArr[i] + vzArr[i] * vzArr[i]
        }

        // ── Kinetic energy cutoff ──
        if (totalKE < settleThreshold) settledFrameCount++ else settledFrameCount = 0
        isSettled = settledFrameCount > 15

        // ── Sync positions back to GraphNode objects ──
        syncBack()
    }
    
    /**
     * Run the simulation for a fixed number of ticks (useful for initial layout convergence)
     */
    fun converge(ticks: Int = 80) {
        repeat(ticks) {
            tick(speedMultiplier = 1.0f)
        }
    }
}

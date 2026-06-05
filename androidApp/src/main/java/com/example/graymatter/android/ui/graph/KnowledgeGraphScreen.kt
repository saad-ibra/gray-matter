package com.example.graymatter.android.ui.graph

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.example.graymatter.android.ui.theme.GrayMatterTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material.icons.filled.DatasetLinked
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.sqrt
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

// ─── Pre-computed static geometry (allocated once, never recreated) ────────────

private val ICOSAHEDRON_FACES = arrayOf(
    intArrayOf(0, 8, 4), intArrayOf(0, 4, 6), intArrayOf(0, 6, 9), intArrayOf(0, 9, 2), intArrayOf(0, 2, 8),
    intArrayOf(8, 10, 4), intArrayOf(4, 1, 6), intArrayOf(6, 11, 9), intArrayOf(9, 7, 2), intArrayOf(2, 5, 8),
    intArrayOf(4, 10, 1), intArrayOf(6, 1, 11), intArrayOf(9, 11, 7), intArrayOf(2, 7, 5), intArrayOf(8, 5, 10),
    intArrayOf(3, 10, 5), intArrayOf(3, 5, 7), intArrayOf(3, 7, 11), intArrayOf(3, 11, 1), intArrayOf(3, 1, 10)
)

private fun buildIcosahedronVertices(r: Float): Array<FloatArray> {
    val phi = (1f + sqrt(5f)) / 2f
    val length = sqrt(1f + phi * phi)
    val vA = r / length
    val vB = r * phi / length
    return arrayOf(
        floatArrayOf(0f, vA, vB), floatArrayOf(0f, vA, -vB),
        floatArrayOf(0f, -vA, vB), floatArrayOf(0f, -vA, -vB),
        floatArrayOf(vA, vB, 0f), floatArrayOf(vA, -vB, 0f),
        floatArrayOf(-vA, vB, 0f), floatArrayOf(-vA, -vB, 0f),
        floatArrayOf(vB, 0f, vA), floatArrayOf(-vB, 0f, vA),
        floatArrayOf(vB, 0f, -vA), floatArrayOf(-vB, 0f, -vA)
    )
}

private val TETRAHEDRON_FACES = arrayOf(
    intArrayOf(0, 1, 2), intArrayOf(0, 2, 3), intArrayOf(0, 3, 1), intArrayOf(1, 3, 2)
)

private fun buildTetrahedronVertices(r: Float): Array<FloatArray> {
    val sq23 = sqrt(2f / 3f)
    val sq3 = sqrt(3f)
    return arrayOf(
        floatArrayOf(0f, -r, 0f),
        floatArrayOf(-r * sq23, r / 3f, -r / sq3),
        floatArrayOf(r * sq23, r / 3f, -r / sq3),
        floatArrayOf(0f, r / 3f, r * 2f / sq3)
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun KnowledgeGraphScreen(
    viewModel: KnowledgeGraphViewModel,
    initialSelectedNodeId: String? = null,
    onBackClick: () -> Unit,
    onNavigateHome: () -> Unit,
    onNodeDoubleTap: (GraphNode) -> Unit
) {
    val graphState by viewModel.graphState.collectAsState()
    
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var speedMultiplier by remember { mutableFloatStateOf(0.25f) }
    var globalRotX by remember { mutableFloatStateOf(0f) }
    var globalRotY by remember { mutableFloatStateOf(0f) }
    var ambientRotEnabled by remember { mutableStateOf(true) }
    var showPhysicsPanel by remember { mutableStateOf(false) }
    var repulsionSlider by remember { mutableFloatStateOf(0.5f) }  // 0..1 mapped to 1000..4000
    var springSlider by remember { mutableFloatStateOf(0.4f) }    // 0..1 mapped to 60..200
    
    // Filters
    var showTopics by remember { mutableStateOf(true) }
    var showResources by remember { mutableStateOf(true) }
    var showOpinions by remember { mutableStateOf(true) }
    var showAnnotations by remember { mutableStateOf(true) }
    var showBookmarks by remember { mutableStateOf(true) }
    var showTemplates by remember { mutableStateOf(true) }
    var showCustom by remember { mutableStateOf(true) }
    var showLookup by remember { mutableStateOf(true) }
    var showVisuals by remember { mutableStateOf(true) }
    
    // Simulation
    val simulator = remember { ForceSimulator() }
    var ticks by remember { mutableIntStateOf(0) } // trigger redraws

    // Selection
    var selectedNode by remember { mutableStateOf<GraphNode?>(null) }
    var showDeleteDialog by remember { mutableStateOf<GraphNode?>(null) }
    var deletedNodeInfo by remember { mutableStateOf<GraphNode?>(null) }

    // Canvas size tracking for proper centering
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // Physics settling flag for cinematic zoom timing
    var physicsSettled by remember { mutableStateOf(false) }

    // Cinematic zoom tracking
    var wasSelected by remember { mutableStateOf(false) }

    // Volume button navigation
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current
    var lastVolumeChangeTime by remember { mutableLongStateOf(0L) }

    // ── Pre-cached Color instances (avoids Color.copy() allocations per frame) ──
    val themeTextPrimary = GrayMatterTheme.colors.textPrimary
    val nodeColorMap = remember(themeTextPrimary) {
        mapOf(
            NodeType.TOPIC to themeTextPrimary,
            NodeType.RESOURCE to themeTextPrimary,
            NodeType.ANNOTATION to GrayMatterColors.TypeAnnotation,
            NodeType.BOOKMARK to GrayMatterColors.TypeBookmark,
            NodeType.TEMPLATE to GrayMatterColors.TypeTemplate,
            NodeType.CUSTOM to GrayMatterColors.TypeTemplate,
            NodeType.LOOKUP to GrayMatterColors.TypeLookupMain,
            NodeType.VISUAL to GrayMatterColors.TypeVisual,
            NodeType.OPINION to GrayMatterColors.TypeOpinion
        )
    }
    // (alpha now computed inline per-node with depth fog factor)

    // ── Reusable Path object (zero allocations per frame) ──
    val reusablePath = remember { androidx.compose.ui.graphics.Path() }

    // ── Spatial hash for O(1) tap hit-testing ──
    val hitGrid = remember { SpatialHashGrid(cellSize = 80f) }

    // ── Projection cache arrays (grown as needed, never shrunk) ──
    // Using remember (not State) to avoid triggering recomposition from Canvas draws
    val projectedXRef = remember { mutableListOf(FloatArray(0)) }
    val projectedYRef = remember { mutableListOf(FloatArray(0)) }
    val projectedZRef = remember { mutableListOf(FloatArray(0)) }
    val screenXRef = remember { mutableListOf(FloatArray(0)) }
    val screenYRef = remember { mutableListOf(FloatArray(0)) }
    val sortedIndicesRef = remember { mutableListOf(IntArray(0)) }

    LaunchedEffect(Unit) {
        if (graphState.nodes.isEmpty()) {
            viewModel.loadGraphData()
        }
    }

    LaunchedEffect(graphState.nodes, graphState.edges) {
        if (graphState.nodes.isNotEmpty()) {
            physicsSettled = false
            simulator.clear()
            // Clone nodes to allow filter hiding without losing physics state if we wanted, 
            // but for simplicity we simulate all and just don't draw hidden ones.
            graphState.nodes.forEach {
                it.x = (Math.random() * 800f).toFloat()
                it.y = (Math.random() * 800f).toFloat()
                simulator.addNode(it)
            }
            
            // Build SoA arrays first for O(1) node index lookup
            simulator.rebuildArrays()
            
            // Use the nodeIndex for O(1) edge setup instead of O(N) find
            graphState.edges.forEach { edge ->
                val srcIdx = simulator.nodeIndex[edge.source.id]
                val tgtIdx = simulator.nodeIndex[edge.target.id]
                if (srcIdx != null && tgtIdx != null) {
                    simulator.edges.add(GraphEdge(edge.id, simulator.nodes[srcIdx], simulator.nodes[tgtIdx], edge.weight))
                }
            }
            
            // Pre-warm physics on background thread (safe: @Synchronized prevents races)
            withContext(Dispatchers.Default) {
                repeat(150) { simulator.tick(speedMultiplier) }
            }
            physicsSettled = true
            
            // Render loop — tick on Main thread (eliminates all concurrent-access crashes)
            while (isActive) {
                // Apply live physics slider values each frame
                simulator.repulsionStrength = 1000f + repulsionSlider * 3000f
                simulator.springLength = 60f + springSlider * 140f
                simulator.tick(speedMultiplier * 0.5f)
                // Ambient rotation when settled and no active selection
                if (simulator.isSettled && ambientRotEnabled) {
                    globalRotY += 0.0012f
                }
                ticks++
                delay(if (simulator.isSettled) 32L else 16L)
            }
        }
    }

    // Explicit Sync: Ensure target node from route is highlighted even if graph data is cached
    LaunchedEffect(initialSelectedNodeId, graphState.nodes, physicsSettled) {
        if (initialSelectedNodeId != null && physicsSettled && graphState.nodes.isNotEmpty()) {
            val node = simulator.nodes.find { it.id == initialSelectedNodeId }
            if (node != null) {
                selectedNode = node
            }
        }
    }

    // Cinematic zoom animation on node selection
    LaunchedEffect(selectedNode, physicsSettled, canvasSize) {
        if (canvasSize.width <= 0f) return@LaunchedEffect

        if (selectedNode != null && physicsSettled) {
            wasSelected = true
            val node = selectedNode ?: return@LaunchedEffect

            // Calculate 3D projected position of the target node
            val targetScale = 2.5f
            val cosX = kotlin.math.cos(globalRotX)
            val sinX = kotlin.math.sin(globalRotX)
            val cosY = kotlin.math.cos(globalRotY)
            val sinY = kotlin.math.sin(globalRotY)

            val rx = node.x - simulator.width / 2f
            val ry = node.y - simulator.height / 2f
            val rz = node.z
            val x1 = rx * cosY - rz * sinY
            val z1 = rz * cosY + rx * sinY
            val y2 = ry * cosX - z1 * sinX
            val z2 = z1 * cosX + ry * sinX
            val zScaleNode = (z2 + 400f).coerceIn(100f, 800f) / 400f

            val targetOffsetX = canvasSize.width / 2f - x1 * targetScale * zScaleNode - simulator.width / 2f * targetScale
            val targetOffsetY = canvasSize.height / 2f - y2 * targetScale * zScaleNode - simulator.height / 2f * targetScale

            val startScale = scale
            val startOffsetX = offset.x
            val startOffsetY = offset.y
            val anim = Animatable(0f)
            anim.animateTo(1f, animationSpec = tween(300, easing = FastOutSlowInEasing)) {
                val t = this.value
                scale = startScale + (targetScale - startScale) * t
                offset = Offset(
                    startOffsetX + (targetOffsetX - startOffsetX) * t,
                    startOffsetY + (targetOffsetY - startOffsetY) * t
                )
            }
        } else if (selectedNode == null && wasSelected) {
            wasSelected = false
            // Zoom out to centered overview
            val targetScale = 1f
            val targetOffsetX = canvasSize.width / 2f - simulator.width / 2f * targetScale
            val targetOffsetY = canvasSize.height / 2f - simulator.height / 2f * targetScale

            val startScale = scale
            val startOffsetX = offset.x
            val startOffsetY = offset.y
            val anim = Animatable(0f)
            anim.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing)) {
                val t = this.value
                scale = startScale + (targetScale - startScale) * t
                offset = Offset(
                    startOffsetX + (targetOffsetX - startOffsetX) * t,
                    startOffsetY + (targetOffsetY - startOffsetY) * t
                )
            }
        }
    }

    // Request focus for volume button capture
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Depth-First Search for deterministic navigation
    val dfsOrderedNodes by remember {
        derivedStateOf {
            val visibleNodes = simulator.nodes.filter { node ->
                when (node.type) {
                    NodeType.TOPIC -> showTopics
                    NodeType.RESOURCE -> showResources
                    NodeType.ANNOTATION -> showAnnotations
                    NodeType.BOOKMARK -> showBookmarks
                    NodeType.TEMPLATE -> showTemplates
                    NodeType.CUSTOM -> showCustom
                    NodeType.LOOKUP -> showLookup
                    NodeType.OPINION -> showOpinions
                    NodeType.VISUAL -> showVisuals
                }
            }.toSet()

            val adjList = mutableMapOf<String, MutableList<String>>()
            for (edge in simulator.edges) {
                if (edge.source in visibleNodes && edge.target in visibleNodes) {
                    adjList.getOrPut(edge.source.id) { mutableListOf() }.add(edge.target.id)
                    adjList.getOrPut(edge.target.id) { mutableListOf() }.add(edge.source.id)
                }
            }

            val ordered = mutableListOf<GraphNode>()
            val visited = mutableSetOf<String>()

            fun dfs(node: GraphNode) {
                if (!visited.add(node.id)) return
                ordered.add(node)
                
                // Priority sort for children: Resources first, then others
                val neighbors = adjList[node.id]?.mapNotNull { id -> visibleNodes.find { it.id == id } }
                    ?.sortedBy { if (it.type == NodeType.RESOURCE) 0 else 1 }
                    
                neighbors?.forEach { child -> dfs(child) }
            }

            // Start DFS from Topics (roots)
            val topics = visibleNodes.filter { it.type == NodeType.TOPIC }
            topics.forEach { dfs(it) }

            // Then from any unvisited Resources
            val resources = visibleNodes.filter { it.type == NodeType.RESOURCE }
            resources.forEach { if (it.id !in visited) dfs(it) }

            // Append any disconnected nodes at the end
            visibleNodes.forEach { if (it.id !in visited) dfs(it) }

            ordered
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayMatterTheme.colors.background)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.VolumeUp -> {
                            val now = System.currentTimeMillis()
                            if (now - lastVolumeChangeTime > 200L) {
                                lastVolumeChangeTime = now
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                // Cycle to previous node in DFS order
                                if (dfsOrderedNodes.isNotEmpty()) {
                                    val currentIdx = dfsOrderedNodes.indexOf(selectedNode)
                                    val prevIdx = if (currentIdx <= 0) dfsOrderedNodes.lastIndex else currentIdx - 1
                                    selectedNode = dfsOrderedNodes[prevIdx]
                                }
                            }
                            true
                        }
                        Key.VolumeDown -> {
                            val now = System.currentTimeMillis()
                            if (now - lastVolumeChangeTime > 200L) {
                                lastVolumeChangeTime = now
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                // Cycle to next node in DFS order
                                if (dfsOrderedNodes.isNotEmpty()) {
                                    val currentIdx = dfsOrderedNodes.indexOf(selectedNode)
                                    val nextIdx = if (currentIdx < 0 || currentIdx >= dfsOrderedNodes.lastIndex) 0 else currentIdx + 1
                                    selectedNode = dfsOrderedNodes[nextIdx]
                                }
                            }
                            true
                        }
                        else -> false
                    }
                } else if (keyEvent.type == KeyEventType.KeyUp) {
                    // Consume KeyUp for Volume keys to suppress system volume UI
                    when (keyEvent.key) {
                        Key.VolumeUp, Key.VolumeDown -> true
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        if (graphState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = GrayMatterTheme.colors.primary
            )
        } else if (graphState.nodes.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DatasetLinked,
                    contentDescription = null,
                    tint = GrayMatterTheme.colors.neutral600,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "No connections yet. Start by adding an opinion to a resource.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GrayMatterTheme.colors.neutral500,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 48.dp)
                )
                Button(
                    onClick = onNavigateHome,
                    colors = ButtonDefaults.buttonColors(containerColor = GrayMatterTheme.colors.primary)
                ) {
                    Text("Go Home", color = GrayMatterTheme.colors.onPrimary)
                }
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
                    .pointerInput(Unit) {
                        // Two-finger: zoom + rotate 3D. One-finger: camera pan.
                        detectTransformGestures(panZoomLock = false) { centroid, pan, zoom, _ ->
                            // Count active pointers via zoom deviation (1.0 = no pinch)
                            val isPinch = zoom != 1f
                            scale = (scale * zoom).coerceIn(0.1f, 5f)
                            offset = offset * zoom + centroid * (1f - zoom)

                            if (isPinch) {
                                // Two-finger: rotate 3D space
                                globalRotY += pan.x * 0.008f
                                globalRotX += pan.y * 0.008f
                            } else {
                                // One-finger: pan the camera
                                offset += pan
                            }
                            simulator.wake()
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                // O(1) hit-test via spatial hash grid
                                val candidates = hitGrid.query(tapOffset.x, tapOffset.y)
                                val sxArr = screenXRef[0]
                                val syArr = screenYRef[0]
                                var hitNode: GraphNode? = null
                                for (ci in candidates) {
                                    if (ci >= simulator.nodes.size) continue
                                    val node = simulator.nodes[ci]
                                    if (ci >= sxArr.size) continue
                                    val sx = sxArr[ci]
                                    val sy = syArr[ci]
                                    val dx = sx - tapOffset.x
                                    val dy = sy - tapOffset.y
                                    if (sqrt(dx * dx + dy * dy) <= node.radius * scale + 20f) {
                                        hitNode = node // keep last (topmost in Z-order)
                                    }
                                }
                                selectedNode = hitNode
                            },
                            onDoubleTap = { tapOffset ->
                                // O(1) hit-test via spatial hash grid
                                val candidates = hitGrid.query(tapOffset.x, tapOffset.y)
                                val sxArr = screenXRef[0]
                                val syArr = screenYRef[0]
                                var hitNode: GraphNode? = null
                                for (ci in candidates) {
                                    if (ci >= simulator.nodes.size) continue
                                    val node = simulator.nodes[ci]
                                    if (ci >= sxArr.size) continue
                                    val sx = sxArr[ci]
                                    val sy = syArr[ci]
                                    val dx = sx - tapOffset.x
                                    val dy = sy - tapOffset.y
                                    if (sqrt(dx * dx + dy * dy) <= node.radius * scale + 20f) {
                                        hitNode = node
                                    }
                                }
                                hitNode?.let { onNodeDoubleTap(it) }
                            }
                        )
                    }
            ) {
                // Access 'ticks' to observe state changes
                val currentTicks = ticks
                val nodeCount = simulator.nodes.size
                if (nodeCount == 0 || !physicsSettled) return@Canvas

                // ── Grow projection cache arrays if needed (ref-based, no State write) ──
                var projectedX = projectedXRef[0]
                var projectedY = projectedYRef[0]
                var projectedZ = projectedZRef[0]
                var screenX = screenXRef[0]
                var screenY = screenYRef[0]
                var sortedIndices = sortedIndicesRef[0]
                if (projectedX.size < nodeCount) {
                    projectedX = FloatArray(nodeCount)
                    projectedY = FloatArray(nodeCount)
                    projectedZ = FloatArray(nodeCount)
                    screenX = FloatArray(nodeCount)
                    screenY = FloatArray(nodeCount)
                    sortedIndices = IntArray(nodeCount)
                    projectedXRef[0] = projectedX
                    projectedYRef[0] = projectedY
                    projectedZRef[0] = projectedZ
                    screenXRef[0] = screenX
                    screenYRef[0] = screenY
                    sortedIndicesRef[0] = sortedIndices
                }

                // ── Pre-compute visibility bitmask (replaces 4 when-blocks) ──
                val visibilityByType = BooleanArray(NodeType.entries.size)
                visibilityByType[NodeType.TOPIC.ordinal] = showTopics
                visibilityByType[NodeType.RESOURCE.ordinal] = showResources
                visibilityByType[NodeType.ANNOTATION.ordinal] = showAnnotations
                visibilityByType[NodeType.BOOKMARK.ordinal] = showBookmarks
                visibilityByType[NodeType.TEMPLATE.ordinal] = showTemplates
                visibilityByType[NodeType.CUSTOM.ordinal] = showCustom
                visibilityByType[NodeType.LOOKUP.ordinal] = showLookup
                visibilityByType[NodeType.OPINION.ordinal] = showOpinions
                visibilityByType[NodeType.VISUAL.ordinal] = showVisuals

                // ── Pre-compute rotation matrix once per frame ──
                val cosX = kotlin.math.cos(globalRotX)
                val sinX = kotlin.math.sin(globalRotX)
                val cosY = kotlin.math.cos(globalRotY)
                val sinY = kotlin.math.sin(globalRotY)
                val halfW = simulator.width / 2f
                val halfH = simulator.height / 2f
                val offsetX = offset.x
                val offsetY = offset.y
                val canvasW = size.width
                val canvasH = size.height

                // ── Project all nodes ONCE per frame (cached) ──
                for (i in 0 until nodeCount) {
                    if (i >= simulator.nodes.size) break // defensive
                    val node = simulator.nodes[i]
                    val rx = node.x - halfW
                    val ry = node.y - halfH
                    val rz = node.z
                    val x1 = rx * cosY - rz * sinY
                    val z1 = rz * cosY + rx * sinY
                    val y2 = ry * cosX - z1 * sinX
                    val z2 = z1 * cosX + ry * sinX
                    projectedX[i] = x1
                    projectedY[i] = y2
                    projectedZ[i] = z2
                    val zS = (z2 + 400f).coerceIn(100f, 800f) / 400f
                    screenX[i] = x1 * scale * zS + halfW * scale + offsetX
                    screenY[i] = y2 * scale * zS + halfH * scale + offsetY
                }

                // ── Build spatial hash grid for hit-testing ──
                hitGrid.clear()
                for (i in 0 until nodeCount) {
                    if (i >= simulator.nodes.size) break
                    hitGrid.insert(i, screenX[i], screenY[i], simulator.nodes[i].radius * scale + 20f)
                }

                // ── In-place Z-sort (no allocation) ──
                for (i in 0 until nodeCount) sortedIndices[i] = i
                // Simple insertion sort on projected Z (fast for nearly-sorted data across frames)
                for (i in 1 until nodeCount) {
                    val key = sortedIndices[i]
                    val keyZ = projectedZ[key]
                    var j = i - 1
                    while (j >= 0 && projectedZ[sortedIndices[j]] > keyZ) {
                        sortedIndices[j + 1] = sortedIndices[j]
                        j--
                    }
                    sortedIndices[j + 1] = key
                }

                // Center everything initially
                if (currentTicks == 1 && offset == Offset.Zero) {
                    offset = Offset(canvasW / 2f - halfW * scale, canvasH / 2f - halfH * scale)
                }

                val frustumMargin = 150f

                // ── Draw Edges (gradient + depth fog) ──
                simulator.edges.forEach { edge ->
                    val srcIdx = simulator.nodeIndex[edge.source.id] ?: return@forEach
                    val tgtIdx = simulator.nodeIndex[edge.target.id] ?: return@forEach
                    if (srcIdx >= nodeCount || tgtIdx >= nodeCount) return@forEach

                    val srcVisible = visibilityByType[edge.source.type.ordinal]
                    val tgtVisible = visibilityByType[edge.target.type.ordinal]
                    if (!srcVisible || !tgtVisible) return@forEach

                    val startSx = screenX[srcIdx]
                    val startSy = screenY[srcIdx]
                    val endSx = screenX[tgtIdx]
                    val endSy = screenY[tgtIdx]

                    if ((startSx < -frustumMargin && endSx < -frustumMargin) ||
                        (startSx > canvasW + frustumMargin && endSx > canvasW + frustumMargin) ||
                        (startSy < -frustumMargin && endSy < -frustumMargin) ||
                        (startSy > canvasH + frustumMargin && endSy > canvasH + frustumMargin)) return@forEach

                    val start = Offset(startSx, startSy)
                    val end = Offset(endSx, endSy)
                    val isHighlighted = selectedNode == edge.source || selectedNode == edge.target

                    // Depth fog: use per-endpoint Z for a more realistic gradient fade
                    val srcAlpha = ((projectedZ[srcIdx] + 400f).coerceIn(80f, 800f) / 800f)
                        .let { if (isHighlighted) 1f else (it * 0.75f).coerceIn(0.15f, 0.9f) }
                    val tgtAlpha = ((projectedZ[tgtIdx] + 400f).coerceIn(80f, 800f) / 800f)
                        .let { if (isHighlighted) 1f else (it * 0.75f).coerceIn(0.15f, 0.9f) }

                    val srcNodeColor = nodeColorMap[edge.source.type] ?: Color.White
                    val tgtNodeColor = nodeColorMap[edge.target.type] ?: Color.White

                    val srcEdgeColor = if (isHighlighted) GrayMatterColors.Primary else srcNodeColor.copy(alpha = srcAlpha)
                    val tgtEdgeColor = if (isHighlighted) GrayMatterColors.Primary else tgtNodeColor.copy(alpha = tgtAlpha)

                    val strokeW = if (isHighlighted) 4.5f * scale else 2.0f * scale
                    val isExplicit = edge.id.endsWith("_ref")

                    val edgeBrush = Brush.linearGradient(
                        colors = listOf(srcEdgeColor, tgtEdgeColor),
                        start = start,
                        end = end
                    )

                    if (isExplicit) {
                        val dashPhase = currentTicks * 2f
                        val midX = (start.x + end.x) / 2f
                        val midY = (start.y + end.y) / 2f
                        val dx = end.x - start.x; val dy = end.y - start.y
                        val length = kotlin.math.sqrt(dx * dx + dy * dy)
                        val nx = if (length > 0f) -dy / length else 0f
                        val ny = if (length > 0f) dx / length else 0f
                        val curveOffset = (length * 0.2f).coerceIn(20f * scale, 60f * scale)
                        reusablePath.reset()
                        reusablePath.moveTo(start.x, start.y)
                        reusablePath.quadraticBezierTo(
                            x1 = midX + nx * curveOffset, y1 = midY + ny * curveOffset,
                            x2 = end.x, y2 = end.y
                        )
                        drawPath(
                            path = reusablePath,
                            brush = edgeBrush,
                            style = Stroke(width = strokeW, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f * scale, 12f * scale), dashPhase))
                        )
                    } else {
                        drawLine(brush = edgeBrush, start = start, end = end, strokeWidth = strokeW)
                    }
                }

                // ── Draw Nodes (back-to-front via sorted indices) ──
                for (si in 0 until nodeCount) {
                    val i = sortedIndices[si]
                    val node = simulator.nodes[i]

                    if (!visibilityByType[node.type.ordinal]) continue

                    val sx = screenX[i]
                    val sy = screenY[i]

                    if (sx < -frustumMargin || sx > canvasW + frustumMargin ||
                        sy < -frustumMargin || sy > canvasH + frustumMargin) continue

                    val nodeColor = nodeColorMap[node.type] ?: Color.White
                    val zScale = (projectedZ[i] + 400f).coerceIn(100f, 800f) / 400f
                    // Enhanced depth fog: nodes far behind fade more aggressively
                    val depthAlpha = ((projectedZ[i] + 400f).coerceIn(60f, 800f) / 800f)
                        .let { it * it } // Quadratic falloff — subtle close, dramatic far
                        .coerceIn(0.12f, 1f)
                    val screenCenter = Offset(sx, sy)
                    val scaledRadius = node.radius * scale * zScale
                    val isSelected = selectedNode == node

                    if (isSelected) {
                        val pulsePhase = (kotlin.math.sin(currentTicks * 0.05f).toFloat() * 0.5f + 0.5f)
                        val pulseRadius = scaledRadius + 15f * scale * zScale + (pulsePhase * 25f * scale * zScale)
                        drawCircle(
                            color = nodeColor.copy(alpha = (0.4f - pulsePhase * 0.2f) * depthAlpha),
                            radius = pulseRadius, center = screenCenter
                        )
                        drawCircle(
                            color = nodeColor.copy(alpha = 0.6f * depthAlpha),
                            radius = scaledRadius + 15f * scale * zScale, center = screenCenter
                        )
                    }

                    if (node.type == NodeType.TOPIC) {
                        val r = node.radius * 1.7f
                        val vertices = buildIcosahedronVertices(r)
                        for (faceIdx in ICOSAHEDRON_FACES) {
                            val pA = projectVertex(node.x, node.y, node.z, vertices[faceIdx[0]], cosX, sinX, cosY, sinY, halfW, halfH)
                            val pB = projectVertex(node.x, node.y, node.z, vertices[faceIdx[1]], cosX, sinX, cosY, sinY, halfW, halfH)
                            val pC = projectVertex(node.x, node.y, node.z, vertices[faceIdx[2]], cosX, sinX, cosY, sinY, halfW, halfH)
                            val sA = toScreen(pA, scale, halfW, halfH, offsetX, offsetY)
                            val sB = toScreen(pB, scale, halfW, halfH, offsetX, offsetY)
                            val sC = toScreen(pC, scale, halfW, halfH, offsetX, offsetY)
                            // Only draw front-facing faces (simple Z-based culling for icosahedron)
                            val avgFaceZ = (pA[2] + pB[2] + pC[2]) / 3f
                            reusablePath.reset()
                            reusablePath.moveTo(sA.x, sA.y)
                            reusablePath.lineTo(sB.x, sB.y)
                            reusablePath.lineTo(sC.x, sC.y)
                            reusablePath.close()
                            // Translucent fill on front-facing faces
                            if (avgFaceZ > projectedZ[i]) {
                                drawPath(path = reusablePath, color = nodeColor.copy(alpha = 0.06f * depthAlpha))
                            }
                            // Wireframe with depth-aware alpha
                            drawPath(path = reusablePath, color = nodeColor.copy(alpha = (if (isSelected) 1f else 0.85f) * depthAlpha), style = Stroke(width = (if (isSelected) 2.2f else 1.5f) * scale))
                        }
                        // Inner glow sphere
                        drawCircle(color = nodeColor.copy(alpha = 0.12f * depthAlpha), radius = scaledRadius * 0.55f, center = screenCenter)

                    } else if (node.type == NodeType.RESOURCE) {
                        val r = node.radius * 1.5f
                        val vertices = buildTetrahedronVertices(r)
                        for (faceIdx in TETRAHEDRON_FACES) {
                            val pA = projectVertex(node.x, node.y, node.z, vertices[faceIdx[0]], cosX, sinX, cosY, sinY, halfW, halfH)
                            val pB = projectVertex(node.x, node.y, node.z, vertices[faceIdx[1]], cosX, sinX, cosY, sinY, halfW, halfH)
                            val pC = projectVertex(node.x, node.y, node.z, vertices[faceIdx[2]], cosX, sinX, cosY, sinY, halfW, halfH)
                            val sA = toScreen(pA, scale, halfW, halfH, offsetX, offsetY)
                            val sB = toScreen(pB, scale, halfW, halfH, offsetX, offsetY)
                            val sC = toScreen(pC, scale, halfW, halfH, offsetX, offsetY)
                            val avgFaceZ = (pA[2] + pB[2] + pC[2]) / 3f
                            reusablePath.reset()
                            reusablePath.moveTo(sA.x, sA.y)
                            reusablePath.lineTo(sB.x, sB.y)
                            reusablePath.lineTo(sC.x, sC.y)
                            reusablePath.close()
                            if (avgFaceZ > projectedZ[i]) {
                                drawPath(path = reusablePath, color = nodeColor.copy(alpha = 0.09f * depthAlpha))
                            }
                            drawPath(path = reusablePath, color = nodeColor.copy(alpha = (if (isSelected) 1f else 0.85f) * depthAlpha), style = Stroke(width = (if (isSelected) 2.2f else 1.5f) * scale))
                        }
                        drawCircle(color = nodeColor.copy(alpha = 0.10f * depthAlpha), radius = scaledRadius * 0.45f, center = screenCenter)

                    } else {
                        // Spherical leaf nodes with depth fog
                        drawCircle(color = nodeColor.copy(alpha = 0.28f * depthAlpha), radius = scaledRadius, center = screenCenter)
                        drawCircle(color = nodeColor.copy(alpha = depthAlpha), radius = scaledRadius, center = screenCenter, style = Stroke(width = 1.8f * scale))
                        drawCircle(color = nodeColor.copy(alpha = 0.85f * depthAlpha), radius = scaledRadius * 0.32f, center = screenCenter)
                    }
                }
            }

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = GrayMatterTheme.colors.textPrimary)
            }
            Text(
                text = "Relatrix",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = GrayMatterTheme.colors.textPrimary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        // ─── Controls Console (Compact Bottom-Right) ──────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 90.dp, end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Main console card
            Surface(
                color = Color(0xFF0D1117).copy(alpha = 0.92f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    0.7.dp, Color.White.copy(alpha = 0.08f)
                ),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // ── Zoom row ──
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Zoom out
                        Surface(
                            color = Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.size(28.dp).repeatingClickable { scale = (scale / 1.1f).coerceIn(0.1f, 5f) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("−", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.Light)
                            }
                        }
                        // Scale readout
                        Text(
                            text = "${(scale * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = GrayMatterColors.Neutral400,
                            modifier = Modifier.width(32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        // Zoom in
                        Surface(
                            color = Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.size(28.dp).repeatingClickable { scale = (scale * 1.1f).coerceIn(0.1f, 5f) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, "Zoom In", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // ── Divider ──
                    Box(modifier = Modifier.width(96.dp).height(0.5.dp).background(Color.White.copy(alpha = 0.07f)))

                    // ── D-pad rotation ──
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Up
                        Surface(
                            color = Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.size(28.dp).repeatingClickable { globalRotX -= 0.05f; simulator.wake() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.KeyboardArrowUp, "Up", tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(16.dp))
                            }
                        }
                        // Middle row (Left & Right only)
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Surface(
                                color = Color.White.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.size(28.dp).repeatingClickable { globalRotY -= 0.05f; simulator.wake() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Left", tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(16.dp))
                                }
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.size(28.dp).repeatingClickable { globalRotY += 0.05f; simulator.wake() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Right", tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        // Down
                        Surface(
                            color = Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.size(28.dp).repeatingClickable { globalRotX += 0.05f; simulator.wake() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.KeyboardArrowDown, "Down", tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // ── Divider ──
                    Box(modifier = Modifier.width(96.dp).height(0.5.dp).background(Color.White.copy(alpha = 0.07f)))

                    // ── Physics toggle ──
                    Row(
                        modifier = Modifier
                            .width(96.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (showPhysicsPanel) GrayMatterColors.Primary.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { showPhysicsPanel = !showPhysicsPanel }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (showPhysicsPanel) "⬥ Physics" else "⬦ Physics",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                            color = if (showPhysicsPanel) GrayMatterColors.Primary else GrayMatterColors.Neutral400
                        )
                    }
                }
            }

            // Physics sliders panel (below console)
            AnimatedVisibility(visible = showPhysicsPanel, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    color = Color(0xFF0D1117).copy(alpha = 0.92f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(0.7.dp, Color.White.copy(alpha = 0.08f)),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).width(146.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(GrayMatterColors.Primary))
                            Spacer(Modifier.width(5.dp))
                            Text("Repulsion", style = MaterialTheme.typography.labelSmall, color = GrayMatterColors.Neutral400)
                        }
                        Slider(
                            value = repulsionSlider,
                            onValueChange = { repulsionSlider = it; simulator.wake() },
                            modifier = Modifier.height(26.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = GrayMatterColors.Primary,
                                activeTrackColor = GrayMatterColors.Primary.copy(alpha = 0.6f),
                                inactiveTrackColor = GrayMatterColors.Neutral600
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(GrayMatterColors.TypeLink))
                            Spacer(Modifier.width(5.dp))
                            Text("Spring", style = MaterialTheme.typography.labelSmall, color = GrayMatterColors.Neutral400)
                        }
                        Slider(
                            value = springSlider,
                            onValueChange = { springSlider = it; simulator.wake() },
                            modifier = Modifier.height(26.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = GrayMatterColors.TypeLink,
                                activeTrackColor = GrayMatterColors.TypeLink.copy(alpha = 0.6f),
                                inactiveTrackColor = GrayMatterColors.Neutral600
                            )
                        )
                    }
                }
            }
        }

        // Bottom Filters
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                color = GrayMatterColors.SurfaceDark.copy(alpha = 0.85f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                val filterOptions = listOf(
                    "Topics" to showTopics,
                    "Resources" to showResources,
                    "Opinions" to showOpinions,
                    "Annotations" to showAnnotations,
                    "Bookmarks" to showBookmarks,
                    "Templates" to showTemplates,
                    "Lookup" to showLookup,
                    "Visuals" to showVisuals
                )
                
                LazyRow(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterOptions) { (name, isSelected) ->
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                when (name) {
                                    "Topics" -> showTopics = !showTopics
                                    "Resources" -> showResources = !showResources
                                    "Opinions" -> showOpinions = !showOpinions
                                    "Annotations" -> showAnnotations = !showAnnotations
                                    "Bookmarks" -> showBookmarks = !showBookmarks
                                    "Templates" -> showTemplates = !showTemplates
                                    "Lookup" -> showLookup = !showLookup
                                    "Visuals" -> showVisuals = !showVisuals
                                }
                            },
                            label = { Text(name, color = Color.Unspecified) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when(name) {
                                    "Topics"    -> GrayMatterTheme.colors.textPrimary.copy(alpha = 0.22f)
                                    "Resources" -> GrayMatterTheme.colors.textPrimary.copy(alpha = 0.22f)
                                    "Opinions"  -> GrayMatterColors.TypeOpinion.copy(alpha = 0.25f)
                                    "Annotations"  -> GrayMatterColors.TypeAnnotation.copy(alpha = 0.25f)
                                    "Bookmarks" -> GrayMatterColors.TypeBookmark.copy(alpha = 0.25f)
                                    "Templates" -> GrayMatterColors.TypeTemplate.copy(alpha = 0.25f)
                                    "Lookup"    -> GrayMatterColors.TypeLookupMain.copy(alpha = 0.25f)
                                    "Visuals"   -> GrayMatterColors.TypeVisual.copy(alpha = 0.25f)
                                    else        -> GrayMatterColors.TypeOpinion.copy(alpha = 0.25f)
                                },
                                selectedLabelColor = Color.White,
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Selected Node Card
        AnimatedVisibility(
            visible = selectedNode != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp, start = 24.dp, end = 24.dp)
        ) {
            selectedNode?.let { node ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = GrayMatterColors.SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val nodeColor = nodeColorMap[node.type] ?: Color.White
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(nodeColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = node.type.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = nodeColor
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { selectedNode = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, "Dismiss", tint = GrayMatterColors.Neutral500, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stripMarkdown(node.label),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onNodeDoubleTap(node) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GrayMatterColors.TypeLink.copy(alpha = 0.15f),
                                    contentColor = GrayMatterColors.TypeLink
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Go to Details")
                            }
                            Button(
                                onClick = { showDeleteDialog = node },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GrayMatterColors.Error.copy(alpha = 0.15f),
                                    contentColor = GrayMatterColors.Error
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Delete", color = GrayMatterColors.Error)
                            }
                        }
                    }
                }
            }
        }

        showDeleteDialog?.let { nodeToDelete ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                containerColor = GrayMatterColors.SurfaceDark,
                title = { Text("Delete ${nodeToDelete.type.name}?", color = Color.White) },
                text = { Text("This will also delete all its connections and instances. This action can be undone for 10 seconds.", color = GrayMatterColors.Neutral500) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteNodeById(nodeToDelete)
                        deletedNodeInfo = nodeToDelete
                        showDeleteDialog = null
                        selectedNode = null
                    }) {
                        Text("Delete", color = GrayMatterColors.Error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancel", color = GrayMatterColors.TextPrimary)
                    }
                }
            )
        }

        if (deletedNodeInfo != null) {
            com.example.graymatter.android.ui.components.UndoSnackbar(
                message = "${deletedNodeInfo!!.type.name} deleted",
                onUndo = {
                    viewModel.undoDeleteNode(deletedNodeInfo!!)
                    deletedNodeInfo = null
                },
                onDismissRequest = {
                    deletedNodeInfo = null
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .imePadding()
            )
        }
        }
    }
}

// ─── Projection helpers (inlined for hot-path performance) ────────────────────

private fun projectVertex(
    nodeX: Float, nodeY: Float, nodeZ: Float,
    vertex: FloatArray,
    cosX: Float, sinX: Float, cosY: Float, sinY: Float,
    halfW: Float, halfH: Float
): FloatArray {
    val rx = (nodeX + vertex[0]) - halfW
    val ry = (nodeY + vertex[1]) - halfH
    val rz = nodeZ + vertex[2]
    val x1 = rx * cosY - rz * sinY
    val z1 = rz * cosY + rx * sinY
    val y2 = ry * cosX - z1 * sinX
    val z2 = z1 * cosX + ry * sinX
    return floatArrayOf(x1, y2, z2)
}

private fun toScreen(
    projected: FloatArray,
    scale: Float,
    halfW: Float, halfH: Float,
    offsetX: Float, offsetY: Float
): Offset {
    val zS = (projected[2] + 400f).coerceIn(100f, 800f) / 400f
    return Offset(
        projected[0] * scale * zS + halfW * scale + offsetX,
        projected[1] * scale * zS + halfH * scale + offsetY
    )
}

private fun stripMarkdown(text: String): String {
    return text.replace(Regex("\\[TEMPLATE:[^\\]]*\\]"), "")
        .replace(Regex("\\[DICT:?\\d*\\]"), "")
        .replace(Regex("\\[CUSTOM: [^\\]]*\\]"), "")
        .replace(Regex("\\[Page \\d+\\]"), "")
        .replace(Regex("[#*>\\[\\]]"), "")
        .trim().replace(Regex("\\s+"), " ")
}

// ─── Continuous Press Extension ───────────────────────────────────────────────

private fun Modifier.repeatingClickable(
    initialDelayMillis: Long = 300,
    repeatDelayMillis: Long = 30,
    onClick: () -> Unit
): Modifier = this.pointerInput(Unit) {
    coroutineScope {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val heldJob = launch {
                onClick()
                delay(initialDelayMillis)
                while (isActive) {
                    onClick()
                    delay(repeatDelayMillis)
                }
            }
            waitForUpOrCancellation()
            heldJob.cancel()
        }
    }
}

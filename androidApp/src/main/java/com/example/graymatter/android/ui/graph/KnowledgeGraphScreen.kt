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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.sqrt

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
    
    // Filters
    var showTopics by remember { mutableStateOf(true) }
    var showResources by remember { mutableStateOf(true) }
    var showOpinions by remember { mutableStateOf(true) }
    var showAnnotations by remember { mutableStateOf(true) }
    var showBookmarks by remember { mutableStateOf(true) }
    var showTemplates by remember { mutableStateOf(true) }
    var showCustom by remember { mutableStateOf(true) }
    var showDictionary by remember { mutableStateOf(true) }
    
    // Simulation
    val simulator = remember { ForceSimulator() }
    var ticks by remember { mutableIntStateOf(0) } // trigger redraws

    // Selection
    var selectedNode by remember { mutableStateOf<GraphNode?>(null) }
    var showDeleteDialog by remember { mutableStateOf<GraphNode?>(null) }

    // Canvas size tracking for proper centering
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // Physics settling flag for cinematic zoom timing
    var physicsSettled by remember { mutableStateOf(initialSelectedNodeId == null) }

    // Cinematic zoom tracking
    var wasSelected by remember { mutableStateOf(false) }

    // Volume button navigation
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current
    var lastVolumeChangeTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        if (graphState.nodes.isEmpty()) {
            viewModel.loadGraphData()
        }
    }

    LaunchedEffect(graphState.nodes, graphState.edges) {
        if (graphState.nodes.isNotEmpty()) {
            simulator.clear()
            // Clone nodes to allow filter hiding without losing physics state if we wanted, 
            // but for simplicity we simulate all and just don't draw hidden ones.
            graphState.nodes.forEach {
                it.x = (Math.random() * 800f).toFloat()
                it.y = (Math.random() * 800f).toFloat()
                simulator.addNode(it)
            }
            graphState.edges.forEach { edge ->
                simulator.nodes.find { it.id == edge.source.id }?.let { src ->
                    simulator.nodes.find { it.id == edge.target.id }?.let { tgt ->
                        simulator.edges.add(GraphEdge(edge.id, src, tgt, edge.weight))
                    }
                }
            }
            
            // Pre-warm physics so graph is stable before first render
            repeat(150) { simulator.tick(speedMultiplier) }
            physicsSettled = true
            
            // Render loop
            while (isActive) {
                simulator.tick(speedMultiplier * 0.5f) // Slower tick after stable to reduce jitter
                ticks++ // force redraw
                delay(16) // ~60fps
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayMatterColors.BackgroundDark)
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
                                // Cycle to previous node (visible nodes only)
                                val visibleNodes = simulator.nodes.filter { node ->
                                    when (node.type) {
                                        NodeType.TOPIC -> showTopics
                                        NodeType.RESOURCE -> showResources
                                        NodeType.ANNOTATION -> showAnnotations
                                        NodeType.BOOKMARK -> showBookmarks
                                        NodeType.TEMPLATE -> showTemplates
                                        NodeType.CUSTOM -> showCustom
                                        NodeType.DICTIONARY -> showDictionary
                                        NodeType.OPINION -> showOpinions
                                    }
                                }
                                if (visibleNodes.isNotEmpty()) {
                                    val currentIdx = visibleNodes.indexOf(selectedNode)
                                    val prevIdx = if (currentIdx <= 0) visibleNodes.lastIndex else currentIdx - 1
                                    selectedNode = visibleNodes[prevIdx]
                                }
                            }
                            true
                        }
                        Key.VolumeDown -> {
                            val now = System.currentTimeMillis()
                            if (now - lastVolumeChangeTime > 200L) {
                                lastVolumeChangeTime = now
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                // Cycle to next node (visible nodes only)
                                val visibleNodes = simulator.nodes.filter { node ->
                                    when (node.type) {
                                        NodeType.TOPIC -> showTopics
                                        NodeType.RESOURCE -> showResources
                                        NodeType.ANNOTATION -> showAnnotations
                                        NodeType.BOOKMARK -> showBookmarks
                                        NodeType.TEMPLATE -> showTemplates
                                        NodeType.CUSTOM -> showCustom
                                        NodeType.DICTIONARY -> showDictionary
                                        NodeType.OPINION -> showOpinions
                                    }
                                }
                                if (visibleNodes.isNotEmpty()) {
                                    val currentIdx = visibleNodes.indexOf(selectedNode)
                                    val nextIdx = if (currentIdx < 0 || currentIdx >= visibleNodes.lastIndex) 0 else currentIdx + 1
                                    selectedNode = visibleNodes[nextIdx]
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
                color = GrayMatterColors.Primary
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
                    tint = GrayMatterColors.Neutral600,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "No connections yet. Start by adding an opinion to a resource.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GrayMatterColors.Neutral500,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 48.dp)
                )
                Button(
                    onClick = onNavigateHome,
                    colors = ButtonDefaults.buttonColors(containerColor = GrayMatterColors.Primary)
                ) {
                    Text("Go Home", color = Color.Black)
                }
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val oldScale = scale
                            scale = (scale * zoom).coerceIn(0.1f, 5f)
                            
                            // Adjust offset to keep the point under the fingers exactly at the same screen coordinates
                            offset = offset * zoom + centroid * (1f - zoom)
                            
                            // Spin the 3D space with horizontal/vertical panning
                            globalRotY += pan.x * 0.01f
                            globalRotX += pan.y * 0.01f
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                // Find clicked node (taking scale and offset into account)
                                val cosX = kotlin.math.cos(globalRotX)
                                val sinX = kotlin.math.sin(globalRotX)
                                val cosY = kotlin.math.cos(globalRotY)
                                val sinY = kotlin.math.sin(globalRotY)
                                
                                val hitNode = simulator.nodes.findLast { node ->
                                    val rx = (node.x - simulator.width / 2f)
                                    val ry = (node.y - simulator.height / 2f)
                                    val rz = node.z
                                    
                                    val x1 = rx * cosY - rz * sinY
                                    val z1 = rz * cosY + rx * sinY
                                    val y1 = ry
                                    
                                    val y2 = y1 * cosX - z1 * sinX
                                    val z2 = z1 * cosX + y1 * sinX
                                    
                                    val zScale = (z2 + 400f).coerceIn(100f, 800f) / 400f
                                    val screenX = x1 * scale * zScale + simulator.width / 2f * scale + offset.x
                                    val screenY = y2 * scale * zScale + simulator.height / 2f * scale + offset.y
                                    
                                    val dx = screenX - tapOffset.x
                                    val dy = screenY - tapOffset.y
                                    sqrt(dx * dx + dy * dy) <= node.radius * scale + 20f
                                }
                                selectedNode = hitNode
                            },
                            onDoubleTap = { tapOffset ->
                                val cosX = kotlin.math.cos(globalRotX)
                                val sinX = kotlin.math.sin(globalRotX)
                                val cosY = kotlin.math.cos(globalRotY)
                                val sinY = kotlin.math.sin(globalRotY)
                                
                                val hitNode = simulator.nodes.findLast { node ->
                                    val rx = (node.x - simulator.width / 2f)
                                    val ry = (node.y - simulator.height / 2f)
                                    val rz = node.z
                                    
                                    val x1 = rx * cosY - rz * sinY
                                    val z1 = rz * cosY + rx * sinY
                                    val y1 = ry
                                    
                                    val y2 = y1 * cosX - z1 * sinX
                                    val z2 = z1 * cosX + y1 * sinX
                                    
                                    val zScale = (z2 + 400f).coerceIn(100f, 800f) / 400f
                                    val screenX = x1 * scale * zScale + simulator.width / 2f * scale + offset.x
                                    val screenY = y2 * scale * zScale + simulator.height / 2f * scale + offset.y
                                    
                                    val dx = screenX - tapOffset.x
                                    val dy = screenY - tapOffset.y
                                    sqrt(dx * dx + dy * dy) <= node.radius * scale + 20f
                                }
                                hitNode?.let { onNodeDoubleTap(it) }
                            }
                        )
                    }
            ) {
                // Pre-calculate rotation matrices once per draw frame
                val cosX = kotlin.math.cos(globalRotX)
                val sinX = kotlin.math.sin(globalRotX)
                val cosY = kotlin.math.cos(globalRotY)
                val sinY = kotlin.math.sin(globalRotY)
                
                val project3D = { x: Float, y: Float, z: Float ->
                    val rx = x - simulator.width / 2f
                    val ry = y - simulator.height / 2f
                    val rz = z
                    
                    val x1 = rx * cosY - rz * sinY
                    val z1 = rz * cosY + rx * sinY
                    
                    val y2 = ry * cosX - z1 * sinX
                    val z2 = z1 * cosX + ry * sinX
                    
                    floatArrayOf(x1, y2, z2)
                }

                // Access 'ticks' to observe state changes
                val currentTicks = ticks

                // Center everything initially
                if (currentTicks == 1 && offset == Offset.Zero) {
                    offset = Offset(size.width / 2f - simulator.width / 2f * scale, size.height / 2f - simulator.height / 2f * scale)
                }

                // Axes removed as requested by user

                // Draw Edges
                simulator.edges.forEach { edge ->
                    val srcVisible = when (edge.source.type) {
                        NodeType.TOPIC -> showTopics
                        NodeType.RESOURCE -> showResources
                        NodeType.ANNOTATION -> showAnnotations
                        NodeType.BOOKMARK -> showBookmarks
                        NodeType.TEMPLATE -> showTemplates
                        NodeType.CUSTOM -> showCustom
                        NodeType.DICTIONARY -> showDictionary
                        NodeType.OPINION -> showOpinions
                    }
                    val tgtVisible = when (edge.target.type) {
                        NodeType.TOPIC -> showTopics
                        NodeType.RESOURCE -> showResources
                        NodeType.ANNOTATION -> showAnnotations
                        NodeType.BOOKMARK -> showBookmarks
                        NodeType.TEMPLATE -> showTemplates
                        NodeType.CUSTOM -> showCustom
                        NodeType.DICTIONARY -> showDictionary
                        NodeType.OPINION -> showOpinions
                    }

                    if (srcVisible && tgtVisible) {
                        // Connect linear nodes directly mathematically solving projection detachment!
                        val p1 = project3D(edge.source.x, edge.source.y, edge.source.z)
                        val p2 = project3D(edge.target.x, edge.target.y, edge.target.z)
                        
                        val zScaleSrc = (p1[2] + 400f).coerceIn(100f, 800f) / 400f
                        val zScaleTgt = (p2[2] + 400f).coerceIn(100f, 800f) / 400f
                        
                        val start = Offset(
                            p1[0] * scale * zScaleSrc + simulator.width / 2f * scale + offset.x,
                            p1[1] * scale * zScaleSrc + simulator.height / 2f * scale + offset.y
                        )
                        val end = Offset(
                            p2[0] * scale * zScaleTgt + simulator.width / 2f * scale + offset.x,
                            p2[1] * scale * zScaleTgt + simulator.height / 2f * scale + offset.y
                        )
                        
                        val isHighlighted = selectedNode == edge.source || selectedNode == edge.target
                        
                        // Z-based opacity (farther = fainter)
                        val midZ = (p1[2] + p2[2]) / 2f
                        val baseAlpha = (midZ + 400f).coerceIn(100f, 800f) / 800f
                        val opacity = if (isHighlighted) 1.0f else (baseAlpha * 0.7f).coerceIn(0.25f, 0.95f)
                        
                        val isExplicit = edge.id.endsWith("_ref")
                        // Animate dashed lines: dash phase moves towards the referencing (source) node
                        val dashPhase = if (isExplicit) (ticks * 2f) else 0f
                        
                        // Draw clean 3D straight line
                        drawLine(
                            start = start,
                            end = end,
                            color = if (isHighlighted) GrayMatterColors.Primary else Color(0xFF42A5F5).copy(alpha = opacity),
                            strokeWidth = if (isHighlighted) 4.5f * scale else 2.2f * scale,
                            pathEffect = if (isExplicit) PathEffect.dashPathEffect(floatArrayOf(20f * scale, 15f * scale), dashPhase) else null
                        )
                    }
                }

                // Sort nodes by Z-axis to draw back-to-front (true 3D layering)
                val sortedNodes = simulator.nodes.sortedBy { it.z }

                // Draw Nodes
                sortedNodes.forEach { node ->
                    val isVisible = when (node.type) {
                        NodeType.TOPIC -> showTopics
                        NodeType.RESOURCE -> showResources
                        NodeType.ANNOTATION -> showAnnotations
                        NodeType.BOOKMARK -> showBookmarks
                        NodeType.TEMPLATE -> showTemplates
                        NodeType.CUSTOM -> showTemplates
                        NodeType.DICTIONARY -> showDictionary
                        NodeType.OPINION -> showOpinions
                    }

                    if (isVisible) {
                        val nodeColor = when (node.type) {
                            NodeType.TOPIC -> Color.White
                            NodeType.RESOURCE -> Color.LightGray
                            NodeType.ANNOTATION -> GrayMatterColors.Gamboge
                            NodeType.BOOKMARK -> GrayMatterColors.Jonquil
                            NodeType.TEMPLATE -> GrayMatterColors.CustomizedAccent
                            NodeType.CUSTOM -> GrayMatterColors.CustomizedAccent
                            NodeType.DICTIONARY -> Color(0xFFC6280B)
                            NodeType.OPINION -> GrayMatterColors.Success
                        }
                        // 3D Perspective Scale & Position
                        val pNode = project3D(node.x, node.y, node.z)
                        val zScale = (pNode[2] + 400f).coerceIn(100f, 800f) / 400f // depth scaling
                        
                        val screenCenter = Offset(
                            pNode[0] * scale * zScale + simulator.width / 2f * scale + offset.x,
                            pNode[1] * scale * zScale + simulator.height / 2f * scale + offset.y
                        )
                        val scaledRadius = node.radius * scale * zScale
                        
                        val isSelected = selectedNode == node

                        if (isSelected) {
                            val pulsePhase = (kotlin.math.sin(currentTicks * 0.05f).toFloat() * 0.5f + 0.5f)
                            val pulseRadius = scaledRadius + 15f * scale * zScale + (pulsePhase * 25f * scale * zScale)
                            
                            drawCircle(
                                color = nodeColor.copy(alpha = 0.4f - (pulsePhase * 0.2f)),
                                radius = pulseRadius,
                                center = screenCenter
                            )
                            drawCircle(
                                color = nodeColor.copy(alpha = 0.6f),
                                radius = scaledRadius + 15f * scale * zScale,
                                center = screenCenter
                            )
                        }

                        // Draw True 3D Shapes (Painter's Algorithm)
                        val drawSolidFaces = { vertices: Array<FloatArray>, facesDef: Array<IntArray>, color: Color ->
                            // Transform global projected vertices
                            val projected = vertices.map { v ->
                                project3D(node.x + v[0], node.y + v[1], node.z + v[2])
                            }
                            
                            // Map to Screen Space and sort faces by Z (back to front)
                            val faces: List<Triple<Float, androidx.compose.ui.graphics.Path, Color>> = facesDef.map { faceIdx ->
                                val pA = projected[faceIdx[0]]
                                val pB = projected[faceIdx[1]]
                                val pC = projected[faceIdx[2]]
                                
                                val avgZ = (pA[2] + pB[2] + pC[2]) / 3f
                                
                                val toScr = { g: FloatArray ->
                                    val zS = (g[2] + 400f).coerceIn(100f, 800f) / 400f
                                    Offset(
                                        g[0] * scale * zS + simulator.width / 2f * scale + offset.x,
                                        g[1] * scale * zS + simulator.height / 2f * scale + offset.y
                                    )
                                }
                                val sA = toScr(pA)
                                val sB = toScr(pB)
                                val sC = toScr(pC)
                                
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(sA.x, sA.y)
                                    lineTo(sB.x, sB.y)
                                    lineTo(sC.x, sC.y)
                                    close()
                                }
                                
                                // True transparent wireframes
                                val faceColor = color.copy(alpha = 0f)
                                
                                Triple(avgZ, path, faceColor)
                            }.sortedBy { -it.first } // sort descending Z (higher Z = further away)
                            
                            // Draw 
                            faces.forEach { triple ->
                                val path = triple.second
                                // Glowing sci-fi neon edge wireframe (only outlined shapes)
                                drawPath(path = path, color = color.copy(alpha=1f), style = Stroke(width = 1.5f * scale))
                            }
                        }

                        if (node.type == NodeType.TOPIC) {
                            val r = node.radius * 1.7f
                            val phi = (1f + sqrt(5f)) / 2f
                            val length = sqrt(1f + phi * phi)
                            val vA = r / length
                            val vB = r * phi / length

                            val vertices = arrayOf(
                                floatArrayOf(  0f,  vA,  vB), // 0
                                floatArrayOf(  0f,  vA, -vB), // 1
                                floatArrayOf(  0f, -vA,  vB), // 2
                                floatArrayOf(  0f, -vA, -vB), // 3
                                floatArrayOf(  vA,  vB,  0f), // 4
                                floatArrayOf(  vA, -vB,  0f), // 5
                                floatArrayOf( -vA,  vB,  0f), // 6
                                floatArrayOf( -vA, -vB,  0f), // 7
                                floatArrayOf(  vB,  0f,  vA), // 8
                                floatArrayOf( -vB,  0f,  vA), // 9
                                floatArrayOf(  vB,  0f, -vA), // 10
                                floatArrayOf( -vB,  0f, -vA)  // 11
                            )
                            val facesDef = arrayOf(
                                intArrayOf(0, 8, 4), intArrayOf(0, 4, 6), intArrayOf(0, 6, 9), intArrayOf(0, 9, 2), intArrayOf(0, 2, 8),
                                intArrayOf(8, 10, 4), intArrayOf(4, 1, 6), intArrayOf(6, 11, 9), intArrayOf(9, 7, 2), intArrayOf(2, 5, 8),
                                intArrayOf(4, 10, 1), intArrayOf(6, 1, 11), intArrayOf(9, 11, 7), intArrayOf(2, 7, 5), intArrayOf(8, 5, 10),
                                intArrayOf(3, 10, 5), intArrayOf(3, 5, 7), intArrayOf(3, 7, 11), intArrayOf(3, 11, 1), intArrayOf(3, 1, 10)
                            )
                            drawSolidFaces(vertices, facesDef, nodeColor)

                        } else if (node.type == NodeType.RESOURCE) {
                            val r = node.radius * 1.5f
                            val sq23 = sqrt(2f/3f)
                            val sq3 = sqrt(3f)
                            val vertices = arrayOf(
                                floatArrayOf(0f, -r, 0f), 
                                floatArrayOf(-r * sq23, r/3f, -r/sq3),
                                floatArrayOf( r * sq23, r/3f, -r/sq3),
                                floatArrayOf(0f, r/3f, r * 2f/sq3)
                            )
                            val facesDef = arrayOf(
                                intArrayOf(0, 1, 2), 
                                intArrayOf(0, 2, 3), 
                                intArrayOf(0, 3, 1), 
                                intArrayOf(1, 3, 2)
                            )
                            drawSolidFaces(vertices, facesDef, nodeColor)
                            
                        } else {
                            // Spherical leaf nodes
                            drawCircle(color = nodeColor.copy(alpha=0.3f), radius = scaledRadius, center = screenCenter)
                            // Glowing Ring Frame
                            drawCircle(color = nodeColor.copy(alpha=1f), radius = scaledRadius, center = screenCenter, style = Stroke(width = 2f * scale))
                            // Solid core dot
                            drawCircle(color = nodeColor.copy(alpha=0.8f), radius = scaledRadius * 0.3f, center = screenCenter)
                        }
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
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
            }
            Text(
                text = "Rela-trix",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        // Controls Overlay (Navigation)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 72.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            
            // Zoom Controls
            Surface(
                color = GrayMatterColors.SurfaceDark.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(8.dp)) {
                    IconButton(onClick = { scale = (scale / 1.25f).coerceIn(0.1f, 5f) }, modifier = Modifier.size(32.dp)) {
                        Text("-", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { scale = (scale * 1.25f).coerceIn(0.1f, 5f) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, "Zoom In", tint = Color.White)
                    }
                }
            }

            // Navigation & Rotation Controls
            Surface(
                color = GrayMatterColors.SurfaceDark.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { globalRotX -= 0.15f }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, "Rotate Up", tint = Color.White)
                    }
                    Row {
                        IconButton(onClick = { globalRotY -= 0.15f }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Rotate Left", tint = Color.White)
                        }
                        IconButton(onClick = { offset = Offset.Zero; scale = 1f; globalRotX = 0f; globalRotY = 0f }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, "Reset", tint = Color.White)
                        }
                        IconButton(onClick = { globalRotY += 0.15f }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Rotate Right", tint = Color.White)
                        }
                    }
                    IconButton(onClick = { globalRotX += 0.15f }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, "Rotate Down", tint = Color.White)
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
                    "Opinions" to (showOpinions || showAnnotations),
                    "Bookmarks" to showBookmarks,
                    "Templates" to showTemplates,
                    "Dict" to showDictionary
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
                                    "Opinions" -> {
                                        val newState = !(showOpinions && showAnnotations)
                                        showOpinions = newState
                                        showAnnotations = newState
                                    }
                                    "Bookmarks" -> showBookmarks = !showBookmarks
                                    "Templates" -> showTemplates = !showTemplates
                                    "Dict" -> showDictionary = !showDictionary
                                }
                            },
                            label = { Text(name, color = if ((name == "Topics" || name == "Resources") && isSelected) Color.Black else Color.Unspecified) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when(name) {
                                    "Topics" -> Color.White
                                    "Resources" -> Color.LightGray
                                    "Annotations" -> GrayMatterColors.Gamboge.copy(alpha = 0.3f)
                                    "Bookmarks" -> GrayMatterColors.Jonquil.copy(alpha = 0.3f)
                                    "Templates" -> GrayMatterColors.CustomizedAccent.copy(alpha = 0.3f)
                                    "Dict" -> Color(0xFFC6280B).copy(alpha = 0.3f)
                                    else -> GrayMatterColors.Success.copy(alpha = 0.3f)
                                },
                                selectedLabelColor = when(name) {
                                    "Topics" -> Color.Black
                                    "Resources" -> Color.Black
                                    "Annotations" -> GrayMatterColors.Gamboge
                                    "Bookmarks" -> GrayMatterColors.Jonquil
                                    "Templates" -> GrayMatterColors.CustomizedAccent
                                    "Dict" -> Color(0xFFC6280B)
                                    else -> GrayMatterColors.Success
                                }
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
                            val nodeColor = when (node.type) {
                                NodeType.TOPIC -> Color.White
                                NodeType.RESOURCE -> Color.LightGray
                                NodeType.ANNOTATION -> GrayMatterColors.Gamboge
                                NodeType.BOOKMARK -> GrayMatterColors.Jonquil
                                NodeType.TEMPLATE, NodeType.CUSTOM -> GrayMatterColors.CustomizedAccent
                                NodeType.DICTIONARY -> Color(0xFFC6280B)
                                NodeType.OPINION -> GrayMatterColors.Success
                            }
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
                                    containerColor = GrayMatterColors.KnowledgeBlue.copy(alpha = 0.15f),
                                    contentColor = GrayMatterColors.KnowledgeBlue
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
                text = { Text("This will also delete all its connections and instances. This action cannot be undone.", color = GrayMatterColors.Neutral500) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteNodeById(nodeToDelete)
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
        }
    }
}

private fun stripMarkdown(text: String): String {
    return text.replace(Regex("\\[TEMPLATE:[^\\]]*\\]"), "")
        .replace(Regex("\\[DICT\\]"), "")
        .replace(Regex("\\[CUSTOM: [^\\]]*\\]"), "")
        .replace(Regex("\\[Page \\d+\\]"), "")
        .replace(Regex("[#*>\\[\\]]"), "")
        .trim().replace(Regex("\\s+"), " ")
}

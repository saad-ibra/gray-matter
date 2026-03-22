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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun KnowledgeGraphScreen(
    viewModel: KnowledgeGraphViewModel,
    onBackClick: () -> Unit,
    onNodeDoubleTap: (GraphNode) -> Unit
) {
    val graphState by viewModel.graphState.collectAsState()
    
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    val textMeasurer = rememberTextMeasurer()

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
    
    // Icons
    val annotationIcon = rememberVectorPainter(Icons.Default.FormatQuote)
    val bookmarkIcon = rememberVectorPainter(Icons.Default.Bookmark)
    val dictIcon = rememberVectorPainter(Icons.Default.MenuBook)
    val templateIcon = rememberVectorPainter(Icons.Default.DashboardCustomize)
    val opinionIcon = rememberVectorPainter(Icons.Default.QuestionAnswer)
    val topicIcon = rememberVectorPainter(Icons.Default.Topic)
    val resourceIcon = rememberVectorPainter(Icons.Default.DatasetLinked)

    LaunchedEffect(Unit) {
        viewModel.loadGraphData()
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
            
            // Loop
            while (isActive) {
                simulator.tick()
                ticks++ // force redraw
                delay(16) // ~60fps
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(GrayMatterColors.BackgroundDark)) {
        if (graphState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = GrayMatterColors.Primary
            )
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.1f, 5f)
                            offset += pan
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                // Find clicked node (taking scale and offset into account)
                                val hitNode = simulator.nodes.findLast { node ->
                                    val screenX = node.x * scale + offset.x
                                    val screenY = node.y * scale + offset.y
                                    val dx = screenX - tapOffset.x
                                    val dy = screenY - tapOffset.y
                                    sqrt(dx * dx + dy * dy) <= node.radius * scale + 20f
                                }
                                selectedNode = hitNode
                            },
                            onDoubleTap = { tapOffset ->
                                val hitNode = simulator.nodes.findLast { node ->
                                    val screenX = node.x * scale + offset.x
                                    val screenY = node.y * scale + offset.y
                                    val dx = screenX - tapOffset.x
                                    val dy = screenY - tapOffset.y
                                    sqrt(dx * dx + dy * dy) <= node.radius * scale + 20f
                                }
                                hitNode?.let { onNodeDoubleTap(it) }
                            }
                        )
                    }
            ) {
                // Access 'ticks' to observe state changes
                val currentTicks = ticks

                // Center everything initially
                if (currentTicks == 1 && offset == Offset.Zero) {
                    offset = Offset(size.width / 2f - 400f, size.height / 2f - 400f)
                }

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
                        val start = Offset(edge.source.x * scale + offset.x, edge.source.y * scale + offset.y)
                        val end = Offset(edge.target.x * scale + offset.x, edge.target.y * scale + offset.y)
                        
                        // Bezier curve
                        val path = Path().apply {
                            moveTo(start.x, start.y)
                            val midX = (start.x + end.x) / 2
                            val midY = (start.y + end.y) / 2
                            // Control points
                            val cp1x = start.x
                            val cp1y = midY
                            val cp2x = end.x
                            val cp2y = midY
                            cubicTo(cp1x, cp1y, cp2x, cp2y, end.x, end.y)
                        }
                        
                        val isHighlighted = selectedNode == edge.source || selectedNode == edge.target
                        
                        // Explicit connections end with "_ref", visually differentiate with PathEffect
                        val isExplicit = edge.id.endsWith("_ref")
                        val pathStyle = Stroke(
                            width = if (isHighlighted) 4.5f * scale else 3f * scale,
                            pathEffect = if (isExplicit) PathEffect.dashPathEffect(floatArrayOf(15f * scale, 10f * scale), 0f) else null
                        )
                        
                        // Fix for faint lines: Increase alpha and stroke width
                        drawPath(
                            path = path,
                            color = if (isHighlighted) GrayMatterColors.Primary.copy(alpha = 1.0f) else GrayMatterColors.Neutral700.copy(alpha = 0.8f),
                            style = pathStyle
                        )
                    }
                }

                // Draw Nodes
                simulator.nodes.forEach { node ->
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
                            NodeType.TOPIC -> Color.Black
                            NodeType.RESOURCE -> Color.Gray
                            NodeType.ANNOTATION -> GrayMatterColors.Gamboge
                            NodeType.BOOKMARK -> GrayMatterColors.Jonquil
                            NodeType.TEMPLATE -> GrayMatterColors.CustomizedAccent
                            NodeType.CUSTOM -> GrayMatterColors.CustomizedAccent
                            NodeType.DICTIONARY -> Color(0xFFC6280B)
                            NodeType.OPINION -> GrayMatterColors.Citrine
                        }
                        
                        val screenCenter = Offset(node.x * scale + offset.x, node.y * scale + offset.y)
                        val scaledRadius = node.radius * scale
                        
                        val isSelected = selectedNode == node

                        if (isSelected) {
                            drawCircle(
                                color = nodeColor.copy(alpha = 0.4f),
                                radius = scaledRadius + 15f * scale,
                                center = screenCenter
                            )
                        }

                        // Draw Circle Base for all nodes (with a white border for visibility if it's black)
                        if (node.type == NodeType.TOPIC) {
                            drawCircle(
                                color = Color.White,
                                radius = scaledRadius + 1f * scale,
                                center = screenCenter
                            )
                        }
                        drawCircle(
                            color = nodeColor,
                            radius = scaledRadius,
                            center = screenCenter
                        )
                        
                        // Draw corresponding Icon inside the node
                        val painter = when (node.type) {
                            NodeType.TOPIC -> topicIcon
                            NodeType.RESOURCE -> resourceIcon
                            NodeType.ANNOTATION -> annotationIcon
                            NodeType.BOOKMARK -> bookmarkIcon
                            NodeType.TEMPLATE, NodeType.CUSTOM -> templateIcon
                            NodeType.DICTIONARY -> dictIcon
                            else -> opinionIcon
                        }
                        
                        val iconSize = scaledRadius * 1.5f
                        translate(left = screenCenter.x - iconSize / 2f, top = screenCenter.y - iconSize / 2f) {
                            with(painter) {
                                draw(
                                    size = androidx.compose.ui.geometry.Size(iconSize, iconSize),
                                    colorFilter = ColorFilter.tint(if (node.type == NodeType.TOPIC) Color.White else GrayMatterColors.BackgroundDark)
                                )
                            }
                        }

                        // Draw Label
                        if (scale > 0.5f) {
                            val textLayoutResult = textMeasurer.measure(
                                text = node.label,
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = (9f * scale).coerceIn(6f, 16f).sp, // Scaled down for precise futuristic look relative to larger nodes
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            val textOffsetX = screenCenter.x - textLayoutResult.size.width / 2f
                            val textOffsetY = screenCenter.y + scaledRadius + 4.dp.toPx()
                            
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(textOffsetX, textOffsetY)
                            )
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
                                    "Opinions" -> showOpinions = !showOpinions
                                    "Annotations" -> showAnnotations = !showAnnotations
                                    "Bookmarks" -> showBookmarks = !showBookmarks
                                    "Templates" -> showTemplates = !showTemplates
                                    "Dict" -> showDictionary = !showDictionary
                                }
                            },
                            label = { Text(name, color = if (name == "Topics" && isSelected) Color.White else Color.Unspecified) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when(name) {
                                    "Topics" -> Color.Black
                                    "Resources" -> Color.Gray.copy(alpha = 0.5f)
                                    "Annotations" -> GrayMatterColors.Gamboge.copy(alpha = 0.3f)
                                    "Bookmarks" -> GrayMatterColors.Jonquil.copy(alpha = 0.3f)
                                    "Templates" -> GrayMatterColors.CustomizedAccent.copy(alpha = 0.3f)
                                    "Dict" -> Color(0xFFC6280B).copy(alpha = 0.3f)
                                    else -> GrayMatterColors.Citrine.copy(alpha = 0.3f)
                                },
                                selectedLabelColor = when(name) {
                                    "Topics" -> Color.White
                                    "Resources" -> Color.LightGray
                                    "Annotations" -> GrayMatterColors.Gamboge
                                    "Bookmarks" -> GrayMatterColors.Jonquil
                                    "Templates" -> GrayMatterColors.CustomizedAccent
                                    "Dict" -> Color(0xFFC6280B)
                                    else -> GrayMatterColors.Citrine
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
                            Text(
                                text = node.type.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = GrayMatterColors.Neutral500,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { selectedNode = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, "Dismiss", tint = GrayMatterColors.Neutral500, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = node.label,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onNodeDoubleTap(node) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GrayMatterColors.Primary.copy(alpha = 0.15f),
                                contentColor = GrayMatterColors.Primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Go to Details")
                        }
                    }
                }
            }
        }
    }
    }
}

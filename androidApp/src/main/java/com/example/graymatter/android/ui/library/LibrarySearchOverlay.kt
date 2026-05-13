package com.example.graymatter.android.ui.library

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.domain.ResourceType
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

/**
 * Premium full-screen search overlay for the Library.
 * Features: frosted glass, filter chips, categorized results with highlighted matches,
 * and direct navigation to content.
 */
@Composable
fun LibrarySearchOverlay(
    viewModel: LibrarySearchViewModel,
    onDismiss: () -> Unit,
    onNavigateToTopic: (topicId: String) -> Unit,
    onNavigateToResourceDetail: (resourceEntryId: String, focusOpinionId: String?) -> Unit,
    onNavigateToFileViewer: (resourceId: String, searchQuery: String?) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.clearSearch()
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020204))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Search Header ──
            SearchHeader(
                query = viewModel.searchQuery,
                onQueryChange = { viewModel.updateQuery(it) },
                onClose = onDismiss,
                isSearching = viewModel.isSearching,
                resultCount = viewModel.filteredResults.size,
                focusRequester = focusRequester
            )

            // ── Filter Chips ──
            if (viewModel.results.isNotEmpty() || viewModel.searchQuery.length >= 2) {
                FilterChipsRow(
                    activeFilter = viewModel.activeFilter,
                    onFilterChange = { viewModel.setFilter(it) },
                    topicCount = viewModel.results.count { it is GlobalSearchResult.TopicResult },
                    resourceCount = viewModel.results.count { it is GlobalSearchResult.ResourceResult },
                    knowledgeCount = viewModel.results.count { it is GlobalSearchResult.OpinionResult }
                )
            }

            // ── Results ──
            when {
                viewModel.isSearching -> {
                    SearchLoadingState()
                }
                viewModel.filteredResults.isNotEmpty() -> {
                    SearchResultsList(
                        results = viewModel.filteredResults,
                        query = viewModel.searchQuery,
                        onNavigateToTopic = onNavigateToTopic,
                        onNavigateToResourceDetail = onNavigateToResourceDetail,
                        onNavigateToFileViewer = onNavigateToFileViewer,
                        onDismiss = onDismiss
                    )
                }
                viewModel.searchQuery.length >= 2 && !viewModel.isSearching -> {
                    EmptySearchState(query = viewModel.searchQuery)
                }
                else -> {
                    val recentSearches by viewModel.recentSearches.collectAsState()
                    if (recentSearches.isNotEmpty()) {
                        RecentSearchesList(
                            recentSearches = recentSearches,
                            onSearchSelected = { viewModel.updateQuery(it) },
                            onClearSearches = { viewModel.clearRecentSearches() }
                        )
                    } else {
                        SearchIdleState()
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSearchesList(
    recentSearches: List<String>,
    onSearchSelected: (String) -> Unit,
    onClearSearches: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Recent Searches",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
            TextButton(
                onClick = onClearSearches,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
            ) {
                Text(
                    "Clear",
                    color = Color(0xFFFFB300).copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            itemsIndexed(recentSearches) { _, query ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSearchSelected(query) }
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = query,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    isSearching: Boolean,
    resultCount: Int,
    focusRequester: FocusRequester
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.ArrowBack, "Close", tint = Color.White)
            }

            // Search field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFFFFB300),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Search,
                            null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                "Search",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                fontSize = 15.sp
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(Color(0xFFFFB300)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {})
                        )
                    }
                    if (query.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                "Clear",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Subtle divider
        HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
    }
}

@Composable
private fun FilterChipsRow(
    activeFilter: SearchFilter,
    onFilterChange: (SearchFilter) -> Unit,
    topicCount: Int,
    resourceCount: Int,
    knowledgeCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SearchFilterChip("All", null, activeFilter == SearchFilter.ALL) { onFilterChange(SearchFilter.ALL) }
        if (topicCount > 0)
            SearchFilterChip("Topics", topicCount, activeFilter == SearchFilter.TOPICS) { onFilterChange(SearchFilter.TOPICS) }
        if (resourceCount > 0)
            SearchFilterChip("Resources", resourceCount, activeFilter == SearchFilter.RESOURCES) { onFilterChange(SearchFilter.RESOURCES) }
        if (knowledgeCount > 0)
            SearchFilterChip("Knowledge", knowledgeCount, activeFilter == SearchFilter.KNOWLEDGE) { onFilterChange(SearchFilter.KNOWLEDGE) }
    }
}

@Composable
private fun SearchFilterChip(
    label: String,
    count: Int?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isSelected) Color(0xFFFFB300).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f),
        animationSpec = tween(200),
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        if (isSelected) Color(0xFFFFB300) else Color.White.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "chipText"
    )
    val borderColor by animateColorAsState(
        if (isSelected) Color(0xFFFFB300).copy(alpha = 0.3f) else Color.Transparent,
        animationSpec = tween(200),
        label = "chipBorder"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            if (count != null) {
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    "$count",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<GlobalSearchResult>,
    query: String,
    onNavigateToTopic: (String) -> Unit,
    onNavigateToResourceDetail: (String, String?) -> Unit,
    onNavigateToFileViewer: (String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    // Group results by type for section headers
    val grouped = remember(results) {
        val topics = results.filterIsInstance<GlobalSearchResult.TopicResult>()
        val resources = results.filterIsInstance<GlobalSearchResult.ResourceResult>()
        val knowledge = results.filterIsInstance<GlobalSearchResult.OpinionResult>()
        buildList {
            if (topics.isNotEmpty()) {
                add("TOPICS" to topics)
            }
            if (resources.isNotEmpty()) {
                add("RESOURCES" to resources)
            }
            if (knowledge.isNotEmpty()) {
                add("KNOWLEDGE HISTORY" to knowledge)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        grouped.forEach { (sectionTitle, sectionResults) ->
            item(key = "header_$sectionTitle") {
                SectionHeader(title = sectionTitle, count = sectionResults.size)
            }
            itemsIndexed(
                sectionResults,
                key = { _, result ->
                    when (result) {
                        is GlobalSearchResult.TopicResult -> "topic_${result.topic.id}"
                        is GlobalSearchResult.ResourceResult -> "resource_${result.resource.id}"
                        is GlobalSearchResult.OpinionResult -> "opinion_${result.opinion.id}"
                    }
                }
            ) { index, result ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(200, delayMillis = index * 30)) +
                            slideInVertically(tween(250, delayMillis = index * 30)) { it / 4 }
                ) {
                    when (result) {
                        is GlobalSearchResult.TopicResult -> {
                            SearchResultCard(
                                icon = Icons.Default.Folder,
                                iconColor = GrayMatterColors.Jonquil,
                                title = result.topic.name,
                                snippet = if (result.topic.notes?.isNotEmpty() == true)
                                    result.topic.notes?.take(100) ?: "" else "${result.topic.resourceCount} resources",
                                query = query,
                                badge = null,
                                onClick = {
                                    onDismiss()
                                    onNavigateToTopic(result.topic.id)
                                }
                            )
                        }
                        is GlobalSearchResult.ResourceResult -> {
                            val isPdf = result.resource.type == ResourceType.PDF
                            SearchResultCard(
                                icon = when (result.resource.type) {
                                    ResourceType.PDF -> Icons.Default.PictureAsPdf
                                    ResourceType.MARKDOWN -> Icons.Default.Description
                                    ResourceType.WEB_LINK -> Icons.Default.Link
                                    ResourceType.IMAGE -> Icons.Default.Image
                                    else -> Icons.Default.InsertDriveFile
                                },
                                iconColor = when (result.resource.type) {
                                    ResourceType.PDF -> Color(0xFFEF5350)
                                    ResourceType.MARKDOWN -> GrayMatterColors.TypeNoteDescription
                                    ResourceType.WEB_LINK -> GrayMatterColors.TypeLink
                                    else -> GrayMatterColors.Neutral400
                                },
                                title = result.resource.title ?: "Untitled",
                                snippet = result.matchSnippet,
                                query = query,
                                badge = result.topicName,
                                onClick = {
                                    onDismiss()
                                    if (isPdf && result.matchType == "content") {
                                        onNavigateToFileViewer(result.resource.id, query)
                                    } else {
                                        onNavigateToResourceDetail(result.resourceEntryId, null)
                                    }
                                }
                            )
                        }
                        is GlobalSearchResult.OpinionResult -> {
                            SearchResultCard(
                                icon = Icons.Default.Lightbulb,
                                iconColor = GrayMatterColors.TypeOpinion,
                                title = result.parentResourceTitle ?: "Knowledge Entry",
                                snippet = result.matchSnippet,
                                query = query,
                                badge = "Knowledge",
                                onClick = {
                                    onDismiss()
                                    onNavigateToResourceDetail(result.resourceEntryId, result.opinion.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
            Text(
                "$count",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider(
            modifier = Modifier.weight(3f),
            color = Color.White.copy(alpha = 0.04f)
        )
    }
}

@Composable
private fun SearchResultCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    snippet: String,
    query: String,
    badge: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Type icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Title with highlighted match
            Text(
                text = buildHighlightedText(title, query),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Snippet with highlighted match
            if (snippet.isNotEmpty() && snippet != title) {
                Text(
                    text = buildHighlightedText(snippet, query, baseAlpha = 0.5f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
            }

            // Badge
            if (badge != null) {
                Spacer(modifier = Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        badge,
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Chevron
        Icon(
            Icons.Default.ChevronRight,
            null,
            tint = Color.White.copy(alpha = 0.15f),
            modifier = Modifier
                .size(18.dp)
                .align(Alignment.CenterVertically)
        )
    }
}

/**
 * Builds AnnotatedString with query matches highlighted.
 */
@Composable
private fun buildHighlightedText(
    text: String,
    query: String,
    baseAlpha: Float = 0.85f
) = buildAnnotatedString {
    if (query.length < 2) {
        withStyle(SpanStyle(color = Color.White.copy(alpha = baseAlpha))) {
            append(text)
        }
        return@buildAnnotatedString
    }

    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    var currentIndex = 0

    while (currentIndex < text.length) {
        val matchIndex = lowerText.indexOf(lowerQuery, currentIndex)
        if (matchIndex == -1) {
            withStyle(SpanStyle(color = Color.White.copy(alpha = baseAlpha))) {
                append(text.substring(currentIndex))
            }
            break
        }

        if (matchIndex > currentIndex) {
            withStyle(SpanStyle(color = Color.White.copy(alpha = baseAlpha))) {
                append(text.substring(currentIndex, matchIndex))
            }
        }

        withStyle(SpanStyle(
            color = Color(0xFFFFB300),
            fontWeight = FontWeight.Bold,
            background = Color(0xFFFFB300).copy(alpha = 0.1f)
        )) {
            append(text.substring(matchIndex, matchIndex + query.length))
        }

        currentIndex = matchIndex + query.length
    }
}

@Composable
private fun SearchLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = Color(0xFFFFB300).copy(alpha = 0.6f),
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Searching…",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun EmptySearchState(query: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.SearchOff,
                null,
                tint = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No results found",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Nothing matched \"$query\"",
                color = Color.White.copy(alpha = 0.2f),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Try different keywords or check spelling",
                color = Color.White.copy(alpha = 0.15f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SearchIdleState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ManageSearch,
                null,
                tint = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Search",
                color = Color.White.copy(alpha = 0.25f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Topics • PDFs • Notes • Knowledge History",
                color = Color.White.copy(alpha = 0.12f),
                fontSize = 12.sp
            )
        }
    }
}

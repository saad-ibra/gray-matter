package com.example.graymatter.android.ui.fileviewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.domain.SearchResult

/**
 * Full-text search panel with match navigation, highlighted query text, and auto-scroll.
 */
@Composable
fun SearchPanel(
    query: String,
    results: List<SearchResult>,
    currentIndex: Int,
    onQueryChanged: (String) -> Unit,
    onNextResult: () -> Unit,
    onPreviousResult: () -> Unit,
    onResultClick: (SearchResult) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Auto-scroll to current result
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && results.isNotEmpty()) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color(0xFF0E0E12),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White)
                }

                TextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Search in document...", color = Color.White.copy(alpha = 0.35f)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFFFB300),
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (results.isNotEmpty()) onNextResult()
                    }),
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { onQueryChanged("") },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, "Clear", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )

                // Match count badge + navigation
                if (results.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    // Match count badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFB300).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "${currentIndex + 1}/${results.size}",
                            color = Color(0xFFFFB300),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onPreviousResult, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, "Previous", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onNextResult, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, "Next", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                } else if (query.length >= 2) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(GrayMatterColors.Error.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "0",
                            color = GrayMatterColors.Error.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Search results list
            if (results.isNotEmpty()) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    itemsIndexed(results) { index, result ->
                        val isSelected = index == currentIndex
                        val itemAlpha by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0.7f,
                            animationSpec = tween(200),
                            label = "itemAlpha"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) Color(0xFFFFB300).copy(alpha = 0.08f)
                                    else Color.Transparent
                                )
                                .clickable { onResultClick(result) }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .alpha(itemAlpha),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Active indicator dot
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFB300))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            // Page badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) Color(0xFFFFB300).copy(alpha = 0.15f)
                                        else Color.White.copy(alpha = 0.06f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "p.${result.page + 1}",
                                    color = if (isSelected) Color(0xFFFFB300) else Color.White.copy(alpha = 0.4f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Snippet with highlighted query
                            Text(
                                text = buildHighlightedSnippet(result.snippet, query),
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else if (query.length >= 2) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.SearchOff,
                        null,
                        tint = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "No results for \"$query\"",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Builds an AnnotatedString with query matches highlighted in amber/bold.
 */
@Composable
private fun buildHighlightedSnippet(
    snippet: String,
    query: String
) = buildAnnotatedString {
    if (query.length < 2) {
        withStyle(SpanStyle(color = Color.White.copy(alpha = 0.8f))) {
            append(snippet)
        }
        return@buildAnnotatedString
    }

    val lowerSnippet = snippet.lowercase()
    val lowerQuery = query.lowercase()
    var currentIndex = 0

    while (currentIndex < snippet.length) {
        val matchIndex = lowerSnippet.indexOf(lowerQuery, currentIndex)
        if (matchIndex == -1) {
            // No more matches — append the rest
            withStyle(SpanStyle(color = Color.White.copy(alpha = 0.7f))) {
                append(snippet.substring(currentIndex))
            }
            break
        }

        // Append text before match
        if (matchIndex > currentIndex) {
            withStyle(SpanStyle(color = Color.White.copy(alpha = 0.7f))) {
                append(snippet.substring(currentIndex, matchIndex))
            }
        }

        // Append highlighted match
        withStyle(SpanStyle(
            color = Color(0xFFFFB300),
            fontWeight = FontWeight.Bold,
            background = Color(0xFFFFB300).copy(alpha = 0.12f)
        )) {
            append(snippet.substring(matchIndex, matchIndex + query.length))
        }

        currentIndex = matchIndex + query.length
    }
}

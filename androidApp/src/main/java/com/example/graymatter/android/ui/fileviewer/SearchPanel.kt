package com.example.graymatter.android.ui.fileviewer

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.domain.SearchResult

/**
 * Full-text search panel with match navigation.
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

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color(0xFF1A1A1E)
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
                    placeholder = { Text("Search in document...", color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { /* trigger search */ })
                )

                // Match count + navigation
                if (results.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${currentIndex + 1}/${results.size}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                    IconButton(onClick = onPreviousResult, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, "Previous", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onNextResult, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, "Next", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Search results list
            if (results.isNotEmpty()) {
                Divider(color = Color.White.copy(alpha = 0.1f))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    itemsIndexed(results) { index, result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (index == currentIndex) Color.White.copy(alpha = 0.06f)
                                    else Color.Transparent
                                )
                                .clickable { onResultClick(result) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "p.${result.page + 1}",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 12.sp,
                                modifier = Modifier.width(40.dp)
                            )
                            Text(
                                result.snippet,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else if (query.isNotEmpty()) {
                Text(
                    "No results",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

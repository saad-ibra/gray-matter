package com.example.graymatter.android.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.android.ui.theme.PlayfairDisplayFontFamily
import com.example.graymatter.domain.Topic

/**
 * Library Screen matching the "Topics & Synthesis" design mockup.
 * Shows topics in a 2-column grid with Roman numeral icons.
 */
@Composable
fun LibraryScreen(
    topics: List<Topic>,
    onTopicClick: (Topic) -> Unit,
    onNavigateToHome: () -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    var searchQuery by remember { mutableStateOf("") }

    if (topics.isEmpty()) {
        EmptyLibraryState(onCreateClick = onCreateClick)
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(GrayMatterColors.BackgroundDark)
                .padding(paddingValues)
                .statusBarsPadding()
        ) {
            // Header removed
            Spacer(modifier = Modifier.height(24.dp))

            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Topics section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Topics",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = GrayMatterColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Topics grid
            val filteredTopics = if (searchQuery.isBlank()) {
                topics
            } else {
                topics.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filteredTopics) { topic ->
                    TopicCard(
                        topic = topic,
                        romanNumeral = toRomanNumeral(filteredTopics.indexOf(topic) + 1),
                        onClick = { onTopicClick(topic) }
                    )
                }
                
                // Bottom padding for the footer
                item { Spacer(modifier = Modifier.height(80.dp)) }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Your Library is Empty",
            style = MaterialTheme.typography.headlineMedium,
            color = GrayMatterColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start by creating your first entry to build your knowledge base.",
            style = MaterialTheme.typography.bodyMedium,
            color = GrayMatterColors.Neutral500,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(GrayMatterColors.TextPrimary)
                .clickable(onClick = onCreateClick)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Create New Resource",
                style = MaterialTheme.typography.labelLarge,
                color = GrayMatterColors.BackgroundDark
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GrayMatterColors.SurfaceDark)
            .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = GrayMatterColors.Neutral500,
                modifier = Modifier.size(22.dp)
            )
            
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = GrayMatterColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(GrayMatterColors.TextPrimary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search Library...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GrayMatterColors.Neutral600
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun TopicCard(
    topic: Topic,
    romanNumeral: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrayMatterColors.SurfaceDark)
            .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Roman numeral icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(GrayMatterColors.BackgroundDark.copy(alpha = 0.4f))
                    .border(1.dp, GrayMatterColors.Neutral700.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = romanNumeral,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlayfairDisplayFontFamily,
                        fontWeight = FontWeight.Normal
                    ),
                    color = GrayMatterColors.TextPrimary
                )
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = topic.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = GrayMatterColors.TextPrimary
                )
                
                Text(
                    text = "${topic.resourceCount} resources",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = GrayMatterColors.Neutral500
                )
            }
            
            Text(
                text = "Updated ${formatTimeAgo(topic.updatedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = GrayMatterColors.Neutral600
            )
        }
    }
}

private fun toRomanNumeral(num: Int): String {
    val romanNumerals = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")
    return if (num in 1..10) romanNumerals[num - 1] else num.toString()
}

private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val hours = diff / (1000 * 60 * 60)
    val days = hours / 24
    
    return when {
        hours < 1 -> "just now"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        days < 30 -> "${days / 7}w ago"
        else -> "${days / 30}mo ago"
    }
}

package com.example.graymatter.android.ui.addtotopic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterTheme
import com.example.graymatter.domain.Topic

@Composable
fun AddToTopicScreen(
    topics: List<Topic>,
    onSelectTopic: (Topic) -> Unit,
    onCreateNewTopic: (String) -> Unit,
    currentTopicName: String? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        // Enforce Topic > Resource hierarchy
    }

    var searchQuery by remember { mutableStateOf("") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GrayMatterTheme.colors.background)
    ) {
        // Header
        AddToTopicHeader(
            currentTopicName = currentTopicName,
            modifier = Modifier.statusBarsPadding()
        )
        
        // Single Search/Create Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GrayMatterTheme.colors.neutral900)
                .border(1.dp, GrayMatterTheme.colors.surfaceBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = GrayMatterTheme.colors.neutral600,
                    modifier = Modifier.size(22.dp)
                )
                
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = GrayMatterTheme.colors.textPrimary,
                        fontSize = 16.sp
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(GrayMatterTheme.colors.textPrimary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search or create a topic...",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                                    color = GrayMatterTheme.colors.neutral600
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = GrayMatterTheme.colors.neutral500
                        )
                    }
                }
            }
        }

        // List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filteredTopics = if (searchQuery.isBlank()) {
                topics
            } else {
                topics.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }
            
            // "Create new topic" button if typed query doesn't match an exact topic
            if (searchQuery.isNotBlank() && !topics.any { it.name.equals(searchQuery, ignoreCase = true) }) {
                item {
                    CreateTopicListItem(
                        topicName = searchQuery,
                        onClick = { onCreateNewTopic(searchQuery) }
                    )
                }
            }

            items(filteredTopics) { topic ->
                TopicListItem(
                    topic = topic,
                    onClick = { onSelectTopic(topic) }
                )
            }
        }
    }
}

@Composable
private fun AddToTopicHeader(
    currentTopicName: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        if (currentTopicName != null) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = currentTopicName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = GrayMatterTheme.colors.neutral500
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = GrayMatterTheme.colors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Organize",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = GrayMatterTheme.colors.textPrimary
                )
            }
        } else {
            Text(
                text = "Organize Entry",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = GrayMatterTheme.colors.textPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun TopicListItem(
    topic: Topic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GrayMatterTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = GrayMatterTheme.colors.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = topic.name,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = GrayMatterTheme.colors.textPrimary
        )
    }
}

@Composable
private fun CreateTopicListItem(
    topicName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GrayMatterTheme.colors.primary.copy(alpha = 0.15f))
            .border(1.dp, GrayMatterTheme.colors.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(GrayMatterTheme.colors.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = GrayMatterTheme.colors.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Text(
            text = "Create \"$topicName\"",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = GrayMatterTheme.colors.primary
        )
    }
}

package com.example.graymatter.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.domain.Topic

/**
 * A bottom sheet for selecting an existing topic or creating a new one.
 * Used during restoration or orphan resolution.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicPickerSheet(
    title: String,
    topics: List<Topic>,
    onDismiss: () -> Unit,
    onSelectTopic: (Topic) -> Unit,
    onCreateNewTopic: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GrayMatterColors.SurfaceDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = GrayMatterColors.Neutral700) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                text = "Assign Topic",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = GrayMatterColors.TextPrimary
            )
            Text(
                text = "Regarding: $title",
                style = MaterialTheme.typography.bodyMedium,
                color = GrayMatterColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Search/Create section
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search or create topic...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = GrayMatterColors.SurfaceInput,
                    focusedContainerColor = GrayMatterColors.SurfaceInput,
                    unfocusedBorderColor = GrayMatterColors.Neutral800,
                    focusedBorderColor = GrayMatterColors.Primary,
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    if (searchQuery.isNotBlank() && topics.none { it.name.equals(searchQuery, true) }) {
                        IconButton(onClick = { onCreateNewTopic(searchQuery) }) {
                            Icon(Icons.Default.Add, null, tint = GrayMatterColors.Primary)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            val filteredTopics = topics.filter { it.name.contains(searchQuery, ignoreCase = true) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                if (searchQuery.isNotBlank() && topics.none { it.name.equals(searchQuery, true) }) {
                    item {
                        TextButton(
                            onClick = { onCreateNewTopic(searchQuery) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create \"$searchQuery\"")
                        }
                    }
                }

                items(filteredTopics) { topic ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTopic(topic) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, null, tint = GrayMatterColors.Primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(topic.name, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GrayMatterColors.Neutral800),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cancel", color = Color.White)
            }
        }
    }
}

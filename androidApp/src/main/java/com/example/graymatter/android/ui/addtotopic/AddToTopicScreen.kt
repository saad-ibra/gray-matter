package com.example.graymatter.android.ui.addtotopic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.graymatter.android.ui.theme.GrayMatterTheme
import com.example.graymatter.domain.Topic

/**
 * Add to Topic Screen matching the "Organize Entry" design mockup.
 * Allows user to select existing topic or create new one.
 */
@Composable
fun AddToTopicScreen(
    topics: List<Topic>,
    onSelectTopic: (Topic) -> Unit,
    onCreateNewTopic: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Block back-navigation to enforce Topic > Resource hierarchy
    BackHandler(enabled = true) {
        // Do nothing, forcing user to pick a topic or finish creation
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTopicId by remember { mutableStateOf<String?>(null) }
    var newTopicName by remember { mutableStateOf("") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GrayMatterTheme.colors.background)
    ) {
        // Header
        AddToTopicHeader(
            modifier = Modifier.statusBarsPadding()
        )
        
        // Content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Create New Topic section - NOW ON TOP
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "CREATE NEW TOPIC",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = GrayMatterTheme.colors.textSecondary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GrayMatterTheme.colors.neutral900)
                        .border(2.dp, GrayMatterTheme.colors.surfaceBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = newTopicName,
                            onValueChange = { 
                                newTopicName = it
                                if (it.isNotBlank()) selectedTopicId = null
                            },
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                color = GrayMatterTheme.colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(GrayMatterTheme.colors.textPrimary),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (newTopicName.isEmpty()) {
                                        Text(
                                            text = "New Topic Title",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = GrayMatterTheme.colors.neutral600
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = GrayMatterTheme.colors.neutral600,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // OR divider
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(GrayMatterTheme.colors.surfaceBorder)
                    )
                    Text(
                        text = "OR",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = GrayMatterTheme.colors.neutral700
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(GrayMatterTheme.colors.surfaceBorder)
                    )
                }
            }

            // Add to Existing Topic section
            item {
                Text(
                    text = "ADD TO EXISTING TOPIC",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = GrayMatterTheme.colors.textSecondary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Search box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GrayMatterTheme.colors.neutral900)
                        .border(1.dp, GrayMatterTheme.colors.surfaceBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
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
                                color = GrayMatterTheme.colors.textPrimary
                            ),
                            cursorBrush = SolidColor(GrayMatterTheme.colors.textPrimary),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search topics...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = GrayMatterTheme.colors.neutral600
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
            }
            
            // Topic list
            val filteredTopics = if (searchQuery.isBlank()) {
                topics
            } else {
                topics.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }
            
            itemsIndexed(filteredTopics) { index, topic ->
                TopicListItem(
                    topic = topic,
                    romanNumeral = toRomanNumeral(index + 1),
                    isSelected = selectedTopicId == topic.id,
                    onClick = { 
                        selectedTopicId = topic.id
                        newTopicName = "" // Clear new topic name if existing topic is picked
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        
        // Confirm Button
        Button(
            onClick = {
                if (newTopicName.isNotBlank()) {
                    onCreateNewTopic(newTopicName)
                } else {
                    val selectedTopic = topics.find { it.id == selectedTopicId }
                    selectedTopic?.let { onSelectTopic(it) }
                }
            },
            enabled = selectedTopicId != null || newTopicName.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GrayMatterTheme.colors.primary,
                contentColor = GrayMatterTheme.colors.onPrimary,
                disabledContainerColor = GrayMatterTheme.colors.neutral700,
                disabledContentColor = GrayMatterTheme.colors.neutral500
            )
        ) {
            Text(
                text = "Confirm and Save",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun AddToTopicHeader(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(GrayMatterTheme.colors.background.copy(alpha = 0.8f))
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Organize Entry",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = GrayMatterTheme.colors.textPrimary,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun TopicListItem(
    topic: Topic,
    romanNumeral: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GrayMatterTheme.colors.neutral900)
            .border(
                width = 1.dp,
                color = if (isSelected) GrayMatterTheme.colors.primary else GrayMatterTheme.colors.surfaceBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$romanNumeral.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = null // monospace
                ),
                color = GrayMatterTheme.colors.neutral600
            )
            
            Text(
                text = topic.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = GrayMatterTheme.colors.textPrimary
            )
        }
        
        // Radio button
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(
                    width = if (isSelected) 6.dp else 1.dp,
                    color = if (isSelected) GrayMatterTheme.colors.primary else GrayMatterTheme.colors.neutral700,
                    shape = CircleShape
                )
        )
    }
}

private fun toRomanNumeral(num: Int): String {
    val romanNumerals = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")
    return if (num in 1..10) romanNumerals[num - 1] else num.toString()
}

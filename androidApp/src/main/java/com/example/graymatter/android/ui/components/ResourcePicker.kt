package com.example.graymatter.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.graymatter.domain.ResourceEntryWithDetails
import com.example.graymatter.domain.Opinion
import com.example.graymatter.domain.Resource
import com.example.graymatter.domain.ResourceType

/**
 * A searchable dropdown component for selecting resources or specific opinions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcePicker(
    availableItems: List<ResourceEntryWithDetails>,
    onReferenceSelected: (Resource, Opinion?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredResults = remember(availableItems, searchQuery) {
        if (searchQuery.isBlank()) availableItems
        else {
            availableItems.filter { itemWithDetails ->
                val resourceMatch = itemWithDetails.resource.title?.contains(searchQuery, ignoreCase = true) == true
                val opinionMatch = itemWithDetails.opinions.any { it.text.contains(searchQuery, ignoreCase = true) }
                resourceMatch || opinionMatch
            }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Library") },
            placeholder = { Text("Resource title or opinion text...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (filteredResults.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No results found", color = MaterialTheme.colorScheme.outline) },
                    onClick = { },
                    enabled = false
                )
            } else {
                filteredResults.forEach { item ->
                    // Resource Item
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = item.resource.title ?: "Untitled Resource",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.resource.url != null) {
                                    Text(item.resource.url!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        },
                        leadingIcon = {
                            val icon = when (item.resource.type) {
                                ResourceType.WEB_LINK -> Icons.Default.Link
                                ResourceType.PDF -> Icons.Default.PictureAsPdf
                                ResourceType.MARKDOWN -> Icons.Default.EditNote
                                else -> Icons.Default.Description
                            }
                            Icon(icon, contentDescription = null)
                        },
                        onClick = {
                            onReferenceSelected(item.resource, null)
                            searchQuery = ""
                            expanded = false
                        }
                    )

                    // Matching opinions
                    val matchingOpinions = if (searchQuery.isBlank()) {
                        item.opinions.take(2)
                    } else {
                        item.opinions.filter { it.text.contains(searchQuery, ignoreCase = true) }
                    }

                    matchingOpinions.forEach { opinion ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = opinion.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.FormatQuote,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp).padding(start = 16.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            },
                            onClick = {
                                onReferenceSelected(item.resource, opinion)
                                searchQuery = ""
                                expanded = false
                            }
                        )
                    }
                    
                    if (item != filteredResults.last()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

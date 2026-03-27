package com.example.graymatter.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.domain.ReferenceSelectorItem
import com.example.graymatter.viewmodel.ReferenceSelectorViewModel
import com.example.graymatter.viewmodel.ReferenceTab
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CheckboxDefaults

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReferenceSelectorSheet(
    viewModel: ReferenceSelectorViewModel,
    onDismissRequest: () -> Unit,
    onConfirm: (List<ReferenceSelectorItem>) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Invisible touch scrim for dismissal
            Box(modifier = Modifier.fillMaxSize().clickable(onClick = onDismissRequest))
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false, onClick = {}),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Select References",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Selected Chips (Horizontally Scrolling)
                    AnimatedVisibility(
                        visible = uiState.selectedItems.isNotEmpty(),
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            uiState.selectedItems.forEach { item ->
                                val text = when (item) {
                                    is ReferenceSelectorItem.TopicItem -> item.name
                                    is ReferenceSelectorItem.ResourceItem -> item.title
                                    is ReferenceSelectorItem.DetailItem -> item.snippet
                                }
                                InputChip(
                                    selected = true,
                                    onClick = { viewModel.removeSelected(item) },
                                    label = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                    },
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.search(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        placeholder = { Text("Search everything...", color = GrayMatterColors.Neutral500) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GrayMatterColors.KnowledgeBlue) },
                        trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { viewModel.search("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = GrayMatterColors.Neutral500)
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GrayMatterColors.KnowledgeBlue,
                            unfocusedBorderColor = GrayMatterColors.Neutral700,
                            focusedContainerColor = GrayMatterColors.SurfaceDark,
                            unfocusedContainerColor = GrayMatterColors.SurfaceDark
                        )
                    )

                    // Tabs
                    TabRow(
                        selectedTabIndex = uiState.activeTab.ordinal,
                        containerColor = Color.Transparent,
                        contentColor = GrayMatterColors.KnowledgeBlue,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[uiState.activeTab.ordinal]),
                                color = GrayMatterColors.KnowledgeBlue
                            )
                        },
                        divider = {},
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        ReferenceTab.values().forEach { tab ->
                            Tab(
                                selected = uiState.activeTab == tab,
                                onClick = { viewModel.setTab(tab) },
                                text = { 
                                    Text(
                                        text = tab.name.lowercase().capitalize(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (uiState.activeTab == tab) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    // Tree/Search List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(uiState.items, key = { it.id }) { item ->
                            val paddingStart = (item.indentLevel * 16).dp
                            val isSearch = uiState.searchQuery.isNotEmpty()
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItemPlacement()
                                    .clickable { 
                                        if (item is ReferenceSelectorItem.DetailItem) {
                                            viewModel.toggleCheck(item)
                                        } else {
                                            viewModel.toggleExpand(item)
                                        }
                                    }
                                    .padding(start = paddingStart, top = 4.dp, bottom = 4.dp, end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isChecked,
                                    onCheckedChange = { viewModel.toggleCheck(item) },
                                    colors = CheckboxDefaults.colors(checkedColor = GrayMatterColors.KnowledgeBlue)
                                )
                                
                                val icon = when (item) {
                                    is ReferenceSelectorItem.TopicItem -> Icons.Default.Folder
                                    is ReferenceSelectorItem.ResourceItem -> {
                                        if (item.type == "WEB_LINK") Icons.Default.Link else Icons.Default.Description
                                    }
                                    is ReferenceSelectorItem.DetailItem -> {
                                        if (item.typeLabel == "Bookmark") Icons.Default.Bookmark else Icons.Default.FormatQuote
                                    }
                                }
                                
                                val iconColor = when (item) {
                                    is ReferenceSelectorItem.TopicItem -> GrayMatterColors.KnowledgeBlue
                                    is ReferenceSelectorItem.ResourceItem -> GrayMatterColors.KnowledgeBlue
                                    is ReferenceSelectorItem.DetailItem -> GrayMatterColors.KnowledgeBlue
                                }

                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp).padding(horizontal = 4.dp),
                                    tint = iconColor.copy(alpha = 0.8f)
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    val titleText = when (item) {
                                        is ReferenceSelectorItem.TopicItem -> item.name
                                        is ReferenceSelectorItem.ResourceItem -> item.title
                                        is ReferenceSelectorItem.DetailItem -> item.snippet
                                    }
                                    
                                    Text(
                                        text = titleText,
                                        style = if (item is ReferenceSelectorItem.TopicItem) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodyMedium,
                                        maxLines = if (item is ReferenceSelectorItem.DetailItem) 3 else 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = GrayMatterColors.TextPrimary
                                    )
                                    
                                    if (isSearch && item.parentContext != null) {
                                        Text(
                                            text = "in ${item.parentContext}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GrayMatterColors.Neutral500
                                        )
                                    } else if (item is ReferenceSelectorItem.ResourceItem) {
                                        Text(
                                            text = item.type.lowercase().capitalize(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GrayMatterColors.Neutral600
                                        )
                                    } else if (item is ReferenceSelectorItem.DetailItem) {
                                        val label = item.typeLabel
                                        if (label != null) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GrayMatterColors.KnowledgeBlue.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }

                                if (item !is ReferenceSelectorItem.DetailItem && !isSearch) {
                                    IconButton(onClick = { viewModel.toggleExpand(item) }) {
                                        Icon(
                                            imageVector = if (item.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = "Expand/Collapse",
                                            tint = GrayMatterColors.Neutral500
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Confirm Button
                    Button(
                        onClick = { onConfirm(uiState.selectedItems) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        enabled = uiState.selectedItems.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm Selection (${uiState.selectedItems.size})")
                    }
                }
            }
        }
    }
}

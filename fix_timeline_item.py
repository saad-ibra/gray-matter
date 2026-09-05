import sys

with open('androidApp/src/main/java/com/example/graymatter/android/ui/resourcedetail/ResourceDetailScreen.kt', 'r') as f:
    content = f.read()

timeline_links_block = '''                    // Knowledge Connections in Edit Mode
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("TIMELINE LINKS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = GrayMatterTheme.colors.neutral500)
                        TextButton(
                            onClick = {
                                referenceSelectorViewModel?.clearSelection()
                                showReferenceSelector = true
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.AddLink, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                    if (selectedReferences.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedReferences.forEach { ref ->
                                val refText = when (ref) {
                                    is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> ref.name
                                    is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> ref.title
                                    is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> ref.snippet
                                }
                                InputChip(
                                    selected = true,
                                    onClick = { 
                                        selectedReferences = selectedReferences.filter { it.id != ref.id }
                                        onUpdate(text, (confidence * 100).toInt(), opinion.createdAt, selectedReferences, selectedTags, opinion.imagePath)
                                    },
                                    label = { Text(refText, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) },
                                    colors = InputChipDefaults.inputChipColors(
                                        containerColor = GrayMatterTheme.colors.surfaceInput,
                                        labelColor = GrayMatterTheme.colors.textPrimary,
                                        trailingIconColor = GrayMatterTheme.colors.neutral500
                                    ),
                                    border = null
                                )
                            }
                        }
                    }'''

if timeline_links_block in content:
    content = content.replace(timeline_links_block, '')

# Now find the end of the `if (isEditing && !isDictionary)` block
end_marker = '''                            onConfidenceChange = { 
                                confidence = it 
                                onUpdate(text, (it * 100).toInt(), opinion.createdAt, selectedReferences, selectedTags, opinion.imagePath)
                            }
                        )
                    }
                }
            } else {
                if (isVisual) {'''

dropdown_block = '''                            onConfidenceChange = { 
                                confidence = it 
                                onUpdate(text, (it * 100).toInt(), opinion.createdAt, selectedReferences, selectedTags, opinion.imagePath)
                            }
                        )
                    }

                    // Unified Connections Dropdown
                    var isConnectionsExpanded by remember { mutableStateOf(false) }
                    var showTagConsole by remember { mutableStateOf(false) }
                    
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isConnectionsExpanded = !isConnectionsExpanded }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "CONNECTIONS",
                                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold),
                                color = GrayMatterTheme.colors.textSecondary
                            )
                            Icon(
                                if (isConnectionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null,
                                tint = GrayMatterTheme.colors.neutral500
                            )
                        }

                        AnimatedVisibility(
                            visible = isConnectionsExpanded,
                            enter = androidx.compose.animation.expandVertically(),
                            exit = androidx.compose.animation.shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GrayMatterTheme.colors.surfaceInput)
                                    .border(1.dp, GrayMatterTheme.colors.surfaceBorder, RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (selectedTags.isEmpty() && selectedReferences.isEmpty()) {
                                    Text("No connections added.", color = GrayMatterTheme.colors.neutral600, style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                                    androidx.compose.foundation.layout.FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        selectedTags.forEach { tag ->
                                            androidx.compose.material3.InputChip(
                                                selected = true,
                                                onClick = { 
                                                    selectedTags = selectedTags.filter { it.id != tag.id } 
                                                    onUpdate(text, (confidence * 100).toInt(), opinion.createdAt, selectedReferences, selectedTags, opinion.imagePath)
                                                },
                                                label = { Text(tag.name, style = MaterialTheme.typography.labelSmall) },
                                                leadingIcon = { Icon(Icons.Default.Sell, null, modifier = Modifier.size(14.dp)) },
                                                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) },
                                                colors = androidx.compose.material3.InputChipDefaults.inputChipColors(
                                                    containerColor = GrayMatterTheme.colors.surfaceInput,
                                                    labelColor = GrayMatterTheme.colors.textPrimary,
                                                    leadingIconColor = GrayMatterTheme.colors.textPrimary,
                                                    trailingIconColor = GrayMatterTheme.colors.neutral500
                                                ),
                                                border = androidx.compose.material3.InputChipDefaults.inputChipBorder(
                                                    enabled = true,
                                                    selected = true,
                                                    borderColor = GrayMatterTheme.colors.surfaceBorder
                                                )
                                            )
                                        }
                                        selectedReferences.forEach { ref ->
                                            val refText = when (ref) {
                                                is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> ref.name
                                                is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> ref.title
                                                is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> ref.snippet
                                            }
                                            androidx.compose.material3.InputChip(
                                                selected = true,
                                                onClick = { 
                                                    selectedReferences = selectedReferences.filter { it.id != ref.id } 
                                                    onUpdate(text, (confidence * 100).toInt(), opinion.createdAt, selectedReferences, selectedTags, opinion.imagePath)
                                                },
                                                label = { Text(refText, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                                                leadingIcon = { Icon(Icons.Default.Link, null, modifier = Modifier.size(14.dp)) },
                                                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) },
                                                colors = androidx.compose.material3.InputChipDefaults.inputChipColors(
                                                    containerColor = com.example.graymatter.android.ui.theme.GrayMatterColors.TypeLink.copy(alpha = 0.1f),
                                                    labelColor = com.example.graymatter.android.ui.theme.GrayMatterColors.TypeLink,
                                                    leadingIconColor = com.example.graymatter.android.ui.theme.GrayMatterColors.TypeLink,
                                                    trailingIconColor = com.example.graymatter.android.ui.theme.GrayMatterColors.TypeLink
                                                ),
                                                border = androidx.compose.material3.InputChipDefaults.inputChipBorder(
                                                    enabled = true,
                                                    selected = true,
                                                    borderColor = com.example.graymatter.android.ui.theme.GrayMatterColors.TypeLink.copy(alpha = 0.3f)
                                                )
                                            )
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { showTagConsole = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = GrayMatterTheme.colors.primary.copy(alpha = 0.1f), contentColor = GrayMatterTheme.colors.primary)
                                    ) {
                                        Icon(Icons.Default.Sell, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Add Tag")
                                    }
                                    Button(
                                        onClick = { 
                                            referenceSelectorViewModel?.clearSelection()
                                            showReferenceSelector = true 
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = com.example.graymatter.android.ui.theme.GrayMatterColors.TypeLink.copy(alpha = 0.1f), contentColor = com.example.graymatter.android.ui.theme.GrayMatterColors.TypeLink)
                                    ) {
                                        Icon(Icons.Default.Link, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Add Link")
                                    }
                                }
                            }
                        }
                    }
                    
                    if (showTagConsole) {
                        com.example.graymatter.android.ui.components.TagConsoleSheet(
                            viewModel = org.koin.androidx.compose.koinViewModel(),
                            onDismissRequest = { showTagConsole = false },
                            onTagSelected = { tag ->
                                showTagConsole = false
                                if (!selectedTags.any { it.id == tag.id }) {
                                    selectedTags = selectedTags + tag
                                    onUpdate(text, (confidence * 100).toInt(), opinion.createdAt, selectedReferences, selectedTags, opinion.imagePath)
                                }
                            }
                        )
                    }

                }
            } else {
                if (isVisual) {'''

if end_marker in content:
    content = content.replace(end_marker, dropdown_block)

with open('androidApp/src/main/java/com/example/graymatter/android/ui/resourcedetail/ResourceDetailScreen.kt', 'w') as f:
    f.write(content)

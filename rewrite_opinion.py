import re

with open('androidApp/src/main/java/com/example/graymatter/android/ui/newentry/NewEntryScreen.kt', 'r') as f:
    lines = f.readlines()

start_idx = 499
end_idx = 684

new_code = """
            HorizontalDivider(
                color = GrayMatterTheme.colors.surfaceBorder, 
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Add Opinion Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        opinionBlocks.add(0, OpinionBlockState())
                    }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = GrayMatterTheme.colors.primary)
                Spacer(Modifier.width(8.dp))
                Text("Add Another Opinion", color = GrayMatterTheme.colors.primary, style = MaterialTheme.typography.titleMedium)
            }

            // Loop over all opinion blocks
            opinionBlocks.forEachIndexed { index, blockState ->
                val entryAccentColor = when {
                    blockState.imagePath != null -> GrayMatterColors.TypeVisual
                    blockState.selectedTemplate != null -> GrayMatterColors.TypeTemplate
                    else -> GrayMatterColors.TypeOpinion
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(entryAccentColor.copy(alpha = 0.1f))
                        .border(1.dp, entryAccentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Opinion Section
                    CustomizedOpinionSection(
                        opinionInput = blockState.text,
                        onOpinionChange = { newText ->
                            opinionBlocks[index] = blockState.copy(text = newText)
                        },
                        templates = templates,
                        selectedTemplate = blockState.selectedTemplate,
                        onTemplateSelect = { template ->
                            opinionBlocks[index] = blockState.copy(
                                selectedTemplate = template,
                                imagePath = null,
                                templateFieldValues = template?.headings?.associateWith { "" } ?: emptyMap()
                            )
                        },
                        templateFieldValues = blockState.templateFieldValues,
                        onFieldValueChange = { heading, value ->
                            val newMap = blockState.templateFieldValues.toMutableMap().apply { put(heading, value) }
                            opinionBlocks[index] = blockState.copy(templateFieldValues = newMap)
                        },
                        onCreateTemplate = { showTemplateEditor = true },
                        onShowImageSourcePicker = { showImageSourcePicker = true },
                        currentImagePath = blockState.imagePath,
                        onImagePathChange = { path ->
                            opinionBlocks[index] = blockState.copy(
                                imagePath = path,
                                selectedTemplate = null
                            )
                        }
                    )

                    // Confidence Level Section
                    ConfidenceLevelSection(
                        confidence = blockState.confidence,
                        onConfidenceChange = { newConf ->
                            opinionBlocks[index] = blockState.copy(confidence = newConf)
                        },
                        accentColor = entryAccentColor
                    )

                    // Unified Connections Dropdown
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    opinionBlocks[index] = blockState.copy(isConnectionsExpanded = !blockState.isConnectionsExpanded)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Link,
                                    null,
                                    tint = entryAccentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "CONNECTIONS (${blockState.selectedTags.size + blockState.selectedReferences.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                                    color = GrayMatterTheme.colors.textSecondary
                                )
                            }
                            Icon(
                                if (blockState.isConnectionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null,
                                tint = GrayMatterTheme.colors.neutral500
                            )
                        }

                        AnimatedVisibility(
                            visible = blockState.isConnectionsExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
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
                                if (blockState.selectedTags.isEmpty() && blockState.selectedReferences.isEmpty()) {
                                    Text("No connections added.", color = GrayMatterTheme.colors.neutral600, style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    @OptIn(ExperimentalLayoutApi::class)
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        blockState.selectedTags.forEach { tag ->
                                            InputChip(
                                                selected = true,
                                                onClick = { 
                                                    val newTags = blockState.selectedTags.filter { it.id != tag.id }
                                                    opinionBlocks[index] = blockState.copy(selectedTags = newTags)
                                                },
                                                label = { Text(tag.name, style = MaterialTheme.typography.labelSmall) },
                                                leadingIcon = { Icon(Icons.Default.Sell, null, modifier = Modifier.size(14.dp)) },
                                                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) },
                                                colors = InputChipDefaults.inputChipColors(
                                                    containerColor = GrayMatterTheme.colors.surface,
                                                    labelColor = GrayMatterTheme.colors.textPrimary,
                                                    leadingIconColor = GrayMatterTheme.colors.neutral500,
                                                    trailingIconColor = GrayMatterTheme.colors.neutral500
                                                ),
                                                border = InputChipDefaults.inputChipBorder(
                                                    enabled = true,
                                                    selected = true,
                                                    borderColor = GrayMatterTheme.colors.surfaceBorder
                                                )
                                            )
                                        }
                                        blockState.selectedReferences.forEach { ref ->
                                            val text = when (ref) {
                                                is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> ref.name
                                                is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> ref.title
                                                is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> ref.snippet
                                            }
                                            InputChip(
                                                selected = true,
                                                onClick = { 
                                                    val newRefs = blockState.selectedReferences.filter { it.id != ref.id }
                                                    opinionBlocks[index] = blockState.copy(selectedReferences = newRefs)
                                                },
                                                label = { Text(text, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                                                leadingIcon = { Icon(Icons.Default.Link, null, modifier = Modifier.size(14.dp)) },
                                                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) },
                                                colors = InputChipDefaults.inputChipColors(
                                                    containerColor = GrayMatterColors.TypeLink.copy(alpha = 0.1f),
                                                    labelColor = GrayMatterColors.TypeLink,
                                                    leadingIconColor = GrayMatterColors.TypeLink,
                                                    trailingIconColor = GrayMatterColors.TypeLink
                                                ),
                                                border = InputChipDefaults.inputChipBorder(
                                                    enabled = true,
                                                    selected = true,
                                                    borderColor = GrayMatterColors.TypeLink.copy(alpha = 0.3f)
                                                )
                                            )
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { 
                                            opinionBlocks[index] = blockState.copy(showTagConsole = true)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GrayMatterTheme.colors.primary.copy(alpha = 0.1f), contentColor = GrayMatterTheme.colors.primary)
                                    ) {
                                        Icon(Icons.Default.Sell, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Add Tag")
                                    }
                                    Button(
                                        onClick = { 
                                            referenceSelectorViewModel.clearSelection()
                                            showReferenceSelector = true 
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GrayMatterColors.TypeLink.copy(alpha = 0.1f), contentColor = GrayMatterColors.TypeLink)
                                    ) {
                                        Icon(Icons.Default.Link, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Add Link")
                                    }
                                }
                            }
                        }
                    }
                    
                    if (blockState.showTagConsole) {
                        com.example.graymatter.android.ui.tags.TagConsoleSheet(
                            viewModel = tagViewModel,
                            onDismissRequest = { 
                                opinionBlocks[index] = blockState.copy(showTagConsole = false)
                            },
                            onTagSelected = { tag ->
                                val newTags = if (!blockState.selectedTags.any { it.id == tag.id }) {
                                    blockState.selectedTags + tag
                                } else blockState.selectedTags
                                opinionBlocks[index] = blockState.copy(showTagConsole = false, selectedTags = newTags)
                            }
                        )
                    }
                } // End Column for Opinion Block
            } // End forEachIndexed
            
            HorizontalDivider(
                color = GrayMatterTheme.colors.surfaceBorder, 
                modifier = Modifier.padding(vertical = 12.dp)
            )
            
            val firstBlock = opinionBlocks.firstOrNull() ?: OpinionBlockState()
            val selectedTemplate = firstBlock.selectedTemplate
            val currentImagePath = firstBlock.imagePath
            val templateFieldValues = firstBlock.templateFieldValues
            val opinionSelectedReferences = firstBlock.selectedReferences
            val opinionSelectedTags = firstBlock.selectedTags
            val confidenceScore = firstBlock.confidence
            val opinionText = firstBlock.text
"""

lines[start_idx:end_idx+1] = [new_code + "\n"]

with open('androidApp/src/main/java/com/example/graymatter/android/ui/newentry/NewEntryScreen.kt', 'w') as f:
    f.writelines(lines)

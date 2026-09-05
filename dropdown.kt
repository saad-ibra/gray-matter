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
                            if (selectedTags.isEmpty() && selectedReferences.isEmpty()) {
                                Text("No connections added.", color = GrayMatterTheme.colors.neutral600, style = MaterialTheme.typography.bodyMedium)
                            } else {
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    selectedTags.forEach { tag ->
                                        InputChip(
                                            selected = true,
                                            onClick = { selectedTags = selectedTags.filter { it.id != tag.id } },
                                            label = { Text(tag.name, style = MaterialTheme.typography.labelSmall) },
                                            leadingIcon = { Icon(Icons.Default.Sell, null, modifier = Modifier.size(14.dp)) },
                                            trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) },
                                            colors = InputChipDefaults.inputChipColors(
                                                containerColor = GrayMatterTheme.colors.surfaceInput,
                                                labelColor = GrayMatterTheme.colors.textPrimary,
                                                leadingIconColor = GrayMatterTheme.colors.textPrimary,
                                                trailingIconColor = GrayMatterTheme.colors.neutral500
                                            ),
                                            border = InputChipDefaults.inputChipBorder(
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
                                        InputChip(
                                            selected = true,
                                            onClick = { selectedReferences = selectedReferences.filter { it.id != ref.id } },
                                            label = { Text(refText, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
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
                                    onClick = { showTagConsole = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = GrayMatterTheme.colors.primary.copy(alpha = 0.1f), contentColor = GrayMatterTheme.colors.primary)
                                ) {
                                    Icon(Icons.Default.Sell, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add Tag")
                                }
                                Button(
                                    onClick = { 
                                        viewModel?.clearSelection()
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
                
                if (showTagConsole) {
                    com.example.graymatter.android.ui.components.TagConsoleSheet(
                        viewModel = org.koin.androidx.compose.koinViewModel(), // Need to get tagViewModel
                        onDismissRequest = { showTagConsole = false },
                        onTagSelected = { tag ->
                            showTagConsole = false
                            if (!selectedTags.any { it.id == tag.id }) {
                                selectedTags = selectedTags + tag
                            }
                        }
                    )
                }

                // Actions
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {

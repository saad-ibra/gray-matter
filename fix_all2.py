import sys, re

with open('androidApp/src/main/java/com/example/graymatter/android/ui/resourcedetail/ResourceDetailScreen.kt', 'r') as f:
    content = f.read()

# 1. Signatures of ResourceDetailScreen
content = content.replace(
    'onAddOpinion: (String, Int, List<com.example.graymatter.domain.ReferenceSelectorItem>, String?) -> Unit,',
    'onAddOpinion: (String, Int, List<com.example.graymatter.domain.ReferenceSelectorItem>, List<com.example.graymatter.domain.Tag>, String?) -> Unit,'
)
content = content.replace(
    'onUpdateOpinion: (String, String, Int, Long, List<com.example.graymatter.domain.ReferenceSelectorItem>, String?) -> Unit,',
    'onUpdateOpinion: (String, String, Int, Long, List<com.example.graymatter.domain.ReferenceSelectorItem>, List<com.example.graymatter.domain.Tag>, String?) -> Unit,'
)

# 2. visual auto-save (lines 140/143 roughly)
content = content.replace(
    'onUpdateOpinion(showEditDialogId!!, visualText, visualConfidence, kotlinx.datetime.Clock.System.now().toEpochMilliseconds(), emptyList(), imageResultPath)',
    'onUpdateOpinion(showEditDialogId!!, visualText, visualConfidence, kotlinx.datetime.Clock.System.now().toEpochMilliseconds(), emptyList(), emptyList(), imageResultPath)'
)
content = content.replace(
    'onAddOpinion(visualText, visualConfidence, emptyList(), imageResultPath)',
    'onAddOpinion(visualText, visualConfidence, emptyList(), emptyList(), imageResultPath)'
)

# 3. OpinionTimeline signature and call
content = content.replace(
    '''                        onUpdateOpinion = { opinionId, newText, newConfidence, newCreatedAt, newLinks, newImagePath ->
                            onUpdateOpinion(opinionId, newText, newConfidence, newCreatedAt, newLinks, newImagePath)
                        },''',
    '''                        onUpdateOpinion = { opinionId, newText, newConfidence, newCreatedAt, newLinks, newTags, newImagePath ->
                            onUpdateOpinion(opinionId, newText, newConfidence, newCreatedAt, newLinks, newTags, newImagePath)
                        },'''
)
content = content.replace(
    '''    onUpdateOpinion: (String, String, Int, Long, List<com.example.graymatter.domain.ReferenceSelectorItem>, String?) -> Unit,''',
    '''    onUpdateOpinion: (String, String, Int, Long, List<com.example.graymatter.domain.ReferenceSelectorItem>, List<com.example.graymatter.domain.Tag>, String?) -> Unit,'''
)
content = content.replace(
    '''                onUpdate = { text, confidence, date, links, imagePath -> onUpdateOpinion(opinion.id, text, confidence, date, links, imagePath) },''',
    '''                onUpdate = { text, confidence, date, links, tags, imagePath -> onUpdateOpinion(opinion.id, text, confidence, date, links, tags, imagePath) },'''
)

# 4. OpinionTimelineItem signature
content = content.replace(
    '''    onUpdate: (String, Int, Long, List<com.example.graymatter.domain.ReferenceSelectorItem>, String?) -> Unit,''',
    '''    onUpdate: (String, Int, Long, List<com.example.graymatter.domain.ReferenceSelectorItem>, List<com.example.graymatter.domain.Tag>, String?) -> Unit,'''
)
# OpinionTimelineItem state
content = content.replace(
    '''    var selectedReferences by remember(flowLinks) { mutableStateOf(flowLinks) }
    var showReferenceSelector by remember { mutableStateOf(false) }''',
    '''    var selectedReferences by remember(flowLinks) { mutableStateOf(flowLinks) }
    var showReferenceSelector by remember { mutableStateOf(false) }
    
    val flowTags by onLoadTags(opinion.id).collectAsState(initial = emptyList())
    var selectedTags by remember(flowTags) { mutableStateOf(flowTags) }'''
)

# Replace all onUpdate(..., selectedReferences, opinion.imagePath) in OpinionTimelineItem
content = re.sub(
    r'onUpdate\(([^,]+),\s*([^,]+),\s*opinion\.createdAt,\s*selectedReferences,\s*opinion\.imagePath\)',
    r'onUpdate(\1, \2, opinion.createdAt, selectedReferences, selectedTags, opinion.imagePath)',
    content
)
content = re.sub(
    r'onUpdate\(([^,]+),\s*([^,]+),\s*it,\s*selectedReferences,\s*opinion\.imagePath\)',
    r'onUpdate(\1, \2, it, selectedReferences, selectedTags, opinion.imagePath)',
    content
)

# 5. OpinionEditDialog signature
content = content.replace(
    '''private fun OpinionEditDialog(
    viewModel: com.example.graymatter.viewmodel.ReferenceSelectorViewModel? = null,
    templates: List<com.example.graymatter.domain.CustomTemplate> = emptyList(),
    initialText: String = "",
    initialConfidence: Int = 0,
    onDismiss: () -> Unit, 
    onCreateTemplate: () -> Unit,
    onNavigateToImageEditor: (Uri, String, Int) -> Unit,
    onConfirm: (String, Int, List<com.example.graymatter.domain.ReferenceSelectorItem>, String?) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }''',
    '''private fun OpinionEditDialog(
    viewModel: com.example.graymatter.viewmodel.ReferenceSelectorViewModel? = null,
    templates: List<com.example.graymatter.domain.CustomTemplate> = emptyList(),
    initialText: String = "",
    initialConfidence: Int = 0,
    initialTags: List<com.example.graymatter.domain.Tag> = emptyList(),
    onDismiss: () -> Unit, 
    onCreateTemplate: () -> Unit,
    onNavigateToImageEditor: (Uri, String, Int) -> Unit,
    onConfirm: (String, Int, List<com.example.graymatter.domain.ReferenceSelectorItem>, List<com.example.graymatter.domain.Tag>, String?) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var selectedTags by remember(initialTags) { mutableStateOf(initialTags) }'''
)

# 6. OpinionEditDialog calls in ResourceDetailScreen
dialog_call_old = '''            OpinionEditDialog(
                viewModel = referenceSelectorViewModel,
                templates = templates,
                initialText = opinionToEdit?.text ?: "",
                initialConfidence = opinionToEdit?.confidenceScore ?: 0,
                onDismiss = { showAddDialog = false; showEditDialogId = null },
                onCreateTemplate = { showTemplateEditor = true },
                onNavigateToImageEditor = onNavigateToImageEditor,
                onConfirm = { text, confidence, referenceLinks, imagePath ->
                    if (opinionToEdit != null) {
                        // Preserve the original createdAt — do NOT update to now
                        onUpdateOpinion(opinionToEdit.id, text, confidence, opinionToEdit.createdAt, referenceLinks, imagePath)
                    } else {
                        onAddOpinion(text, confidence, referenceLinks, imagePath)
                    }
                    showAddDialog = false
                    showEditDialogId = null
                }
            )'''
dialog_call_new = '''            val initialTags by (if (opinionToEdit != null) onLoadTags(opinionToEdit.id) else kotlinx.coroutines.flow.flowOf(emptyList())).collectAsState(initial = emptyList())
            OpinionEditDialog(
                viewModel = referenceSelectorViewModel,
                templates = templates,
                initialText = opinionToEdit?.text ?: "",
                initialConfidence = opinionToEdit?.confidenceScore ?: 0,
                initialTags = initialTags,
                onDismiss = { showAddDialog = false; showEditDialogId = null },
                onCreateTemplate = { showTemplateEditor = true },
                onNavigateToImageEditor = onNavigateToImageEditor,
                onConfirm = { text, confidence, referenceLinks, tags, imagePath ->
                    if (opinionToEdit != null) {
                        onUpdateOpinion(opinionToEdit.id, text, confidence, opinionToEdit.createdAt, referenceLinks, tags, imagePath)
                    } else {
                        onAddOpinion(text, confidence, referenceLinks, tags, imagePath)
                    }
                    showAddDialog = false
                    showEditDialogId = null
                }
            )'''
content = content.replace(dialog_call_old, dialog_call_new)

# 7. Modify the OpinionEditDialog UI
# Replace knowledge conn block with empty
kc_idx_start = content.find('                    // Action row: Knowledge Connections + Add Image button')
kc_idx_end = content.find('                    // Text input or template')
if kc_idx_start != -1 and kc_idx_end != -1:
    content = content[:kc_idx_start] + content[kc_idx_end:]


# Find Confidence slider and insert dropdown after it
conf_slider_block = '''                // Confidence slider
                Column {
                    Text("Confidence: ${(confidence * 10).toInt()}/10", style = MaterialTheme.typography.labelMedium, color = GrayMatterTheme.colors.neutral500)
                    Slider(
                        value = confidence,
                        onValueChange = { confidence = it },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = GrayMatterTheme.colors.neutral800
                        )
                    )
                }'''

dropdown_block = '''
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
                                            onClick = { selectedTags = selectedTags.filter { it.id != tag.id } },
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
                                            onClick = { selectedReferences = selectedReferences.filter { it.id != ref.id } },
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
                                        viewModel?.clearSelection()
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
                            }
                        }
                    )
                }'''

content = content.replace(conf_slider_block, conf_slider_block + '\n' + dropdown_block)

# Fix onConfirm call to include selectedTags
content = content.replace(
    '''onConfirm(finalText, (confidence * 100).toInt(), selectedReferences, currentImagePath)''',
    '''onConfirm(finalText, (confidence * 100).toInt(), selectedReferences, selectedTags, currentImagePath)'''
)

with open('androidApp/src/main/java/com/example/graymatter/android/ui/resourcedetail/ResourceDetailScreen.kt', 'w') as f:
    f.write(content)

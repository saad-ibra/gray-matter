package com.example.graymatter.android.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterTheme
import com.example.graymatter.android.ui.viewmodel.TagViewModel
import com.example.graymatter.domain.Tag
import kotlinx.coroutines.launch

@Composable
fun TagManagementScreen(
    tagViewModel: TagViewModel,
    onBackClick: () -> Unit
) {
    val tags: List<Tag> by tagViewModel.allTags.collectAsState()
    var showCreateDialog: Boolean by remember { mutableStateOf(false) }
    var editingTag: Tag? by remember { mutableStateOf(null) }
    var lastDeletedTag: Tag? by remember { mutableStateOf(null) }

    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayMatterTheme.colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = GrayMatterTheme.colors.textPrimary
                    )
                }
                Text(
                    text = "Tag Management",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = GrayMatterTheme.colors.textPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Section header with create button
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "YOUR TAGS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = GrayMatterTheme.colors.neutral500
                        )
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Create tag",
                                tint = GrayMatterTheme.colors.textPrimary
                            )
                        }
                    }
                }

                if (tags.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Style,
                                    contentDescription = null,
                                    tint = GrayMatterTheme.colors.neutral700,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No tags yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = GrayMatterTheme.colors.neutral500
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap + to create your first tag",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GrayMatterTheme.colors.neutral700
                                )
                            }
                        }
                    }
                }

                items(tags, key = { it.id }) { tag ->
                    TagItem(
                        tag = tag,
                        tagViewModel = tagViewModel,
                        onEdit = { editingTag = tag },
                        onDelete = {
                            lastDeletedTag = tag
                            tagViewModel.deleteTag(tag.id)
                            scope.launch {
                                val result: SnackbarResult = snackbarHostState.showSnackbar(
                                    message = "Tag '${tag.name}' deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Long
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    lastDeletedTag?.let { deleted ->
                                        scope.launch { tagViewModel.createTag(deleted.name) }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        // Create Tag Dialog
        if (showCreateDialog) {
            CreateTagDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name ->
                    scope.launch { tagViewModel.createTag(name) }
                    showCreateDialog = false
                }
            )
        }

        // Edit Tag Dialog
        if (editingTag != null) {
            EditTagDialog(
                tag = editingTag!!,
                onDismiss = { editingTag = null },
                onSave = { newName ->
                    editingTag?.let { tagViewModel.renameTag(it.id, newName) }
                    editingTag = null
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun TagItem(
    tag: Tag,
    tagViewModel: TagViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu: Boolean by remember { mutableStateOf(false) }
    val entryCount: Long by tagViewModel.getEntryCountByTagId(tag.id)
        .collectAsState(initial = 0L)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GrayMatterTheme.colors.surface)
            .border(1.dp, GrayMatterTheme.colors.neutral800, RoundedCornerShape(16.dp))
            .clickable(onClick = onEdit)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Style,
                    contentDescription = null,
                    tint = GrayMatterTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = GrayMatterTheme.colors.textPrimary
                    )
                    if (entryCount > 0L) {
                        Text(
                            text = "$entryCount ${if (entryCount == 1L) "entry" else "entries"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GrayMatterTheme.colors.textSecondary
                        )
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = GrayMatterTheme.colors.neutral500
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(GrayMatterTheme.colors.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename", color = GrayMatterTheme.colors.textPrimary) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, null, tint = GrayMatterTheme.colors.textPrimary)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = GrayMatterTheme.colors.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, null, tint = GrayMatterTheme.colors.error)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateTagDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name: String by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create Tag", color = GrayMatterTheme.colors.textPrimary)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tag name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = GrayMatterTheme.colors.neutral800,
        confirmButton = {
            TextButton(
                onClick = { onCreate(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Create", color = GrayMatterTheme.colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = GrayMatterTheme.colors.neutral500)
            }
        }
    )
}

@Composable
private fun EditTagDialog(
    tag: Tag,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name: String by remember { mutableStateOf(tag.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Rename Tag", color = GrayMatterTheme.colors.textPrimary)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tag name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = GrayMatterTheme.colors.neutral800,
        confirmButton = {
            TextButton(
                onClick = { onSave(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Save", color = GrayMatterTheme.colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = GrayMatterTheme.colors.neutral500)
            }
        }
    )
}

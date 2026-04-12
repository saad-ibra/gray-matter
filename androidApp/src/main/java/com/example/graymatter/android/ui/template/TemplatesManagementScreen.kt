package com.example.graymatter.android.ui.template

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.domain.CustomTemplate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TemplatesManagementScreen(
    templateViewModel: com.example.graymatter.android.ui.viewmodel.TemplateViewModel,
    onBackClick: () -> Unit
) {
    val templates by templateViewModel.templates.collectAsState()
    var editingTemplate by remember { mutableStateOf<CustomTemplate?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var templateToDeleteId by remember { mutableStateOf<String?>(null) }
    var lastDeletedTemplate by remember { mutableStateOf<CustomTemplate?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayMatterColors.BackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GrayMatterColors.TextPrimary)
                }
                Text(
                    text = "Template Management",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = GrayMatterColors.TextPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    SectionHeader(title = "YOUR TEMPLATES") {
                        IconButton(onClick = {
                            editingTemplate = CustomTemplate(java.util.UUID.randomUUID().toString(), "", listOf(""))
                            showEditor = true
                        }) {
                            Icon(Icons.Default.Add, null, tint = GrayMatterColors.TextPrimary)
                        }
                    }
                }

                items(templates) { template ->
                    TemplateItem(
                        template = template,
                        onClick = {
                            editingTemplate = template
                            showEditor = true
                        }
                    )
                }
            }
        }

        if (showEditor && editingTemplate != null) {
            TemplateEditorDialog(
                template = editingTemplate!!,
                onDismiss = { showEditor = false },
                onSave = { updated ->
                    templateViewModel.saveTemplate(updated)
                    showEditor = false
                },
                onDelete = { id ->
                    templateToDeleteId = id
                    showEditor = false
                }
            )
        }

        if (templateToDeleteId != null) {
            val template = templates.find { it.id == templateToDeleteId }
            AlertDialog(
                onDismissRequest = { templateToDeleteId = null },
                title = { Text("Delete Template", color = Color.White) },
                text = { Text("Are you sure you want to delete '${template?.name ?: "this template"}'? This cannot be undone once the undo window closes.", color = GrayMatterColors.TextSecondary) },
                containerColor = Color(0xFF1A1A1E),
                confirmButton = {
                    TextButton(onClick = {
                        val toDelete = templates.find { it.id == templateToDeleteId }
                        if (toDelete != null) {
                            lastDeletedTemplate = toDelete
                            templateViewModel.deleteTemplate(toDelete.id)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Template '${toDelete.name}' deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Long
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    lastDeletedTemplate?.let { templateViewModel.saveTemplate(it) }
                                }
                            }
                        }
                        templateToDeleteId = null
                    }) {
                        Text("Delete", color = GrayMatterColors.Error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { templateToDeleteId = null }) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String, action: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = GrayMatterColors.Neutral500
        )
        action()
    }
}

@Composable
private fun TemplateItem(template: CustomTemplate, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GrayMatterColors.SurfaceDark)
            .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = GrayMatterColors.TextPrimary
                )
                Text(
                    text = "${template.headings.size} headings",
                    style = MaterialTheme.typography.labelSmall,
                    color = GrayMatterColors.TextSecondary
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = GrayMatterColors.Neutral700)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateEditorDialog(
    template: CustomTemplate,
    onDismiss: () -> Unit,
    onSave: (CustomTemplate) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember { mutableStateOf(template.name) }
    var headings by remember { mutableStateOf(template.headings) }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
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
                .verticalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Template",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = GrayMatterColors.TextPrimary
                )
                IconButton(onClick = { onDelete(template.id) }) {
                    Icon(Icons.Default.Delete, null, tint = GrayMatterColors.Error)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Template Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = GrayMatterColors.SurfaceInput,
                    focusedContainerColor = GrayMatterColors.SurfaceInput,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "HEADINGS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = GrayMatterColors.Neutral500
            )

            Spacer(modifier = Modifier.height(8.dp))

            headings.forEachIndexed { index, heading ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = heading,
                        onValueChange = { newVal ->
                            val newList = headings.toMutableList()
                            newList[index] = newVal
                            headings = newList
                        },
                        placeholder = { Text("Heading ${index + 1}") },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = GrayMatterColors.SurfaceInput,
                            focusedContainerColor = GrayMatterColors.SurfaceInput,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    IconButton(onClick = {
                        headings = headings.toMutableList().apply { removeAt(index) }
                    }) {
                        Icon(Icons.Default.RemoveCircleOutline, null, tint = GrayMatterColors.Error)
                    }
                }
            }

            TextButton(
                onClick = {
                    headings = headings + ""
                    scope.launch {
                        delay(100)
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Heading")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onSave(template.copy(name = name, headings = headings.filter { it.isNotBlank() })) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A3E6A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Template", fontWeight = FontWeight.Bold)
            }
        }
    }
}

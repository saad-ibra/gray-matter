package com.example.graymatter.android.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.domain.CustomTemplate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorDialog(
    template: CustomTemplate,
    onDismiss: () -> Unit,
    onSave: (CustomTemplate) -> Unit,
    onDelete: ((String) -> Unit)? = null
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
                    text = if (template.name.isEmpty()) "Create Template" else "Edit Template",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = GrayMatterColors.TextPrimary
                )
                if (onDelete != null && template.name.isNotEmpty()) {
                    IconButton(onClick = { onDelete(template.id) }) {
                        Icon(Icons.Default.Delete, "Delete Template", tint = GrayMatterColors.Error)
                    }
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
                        Icon(Icons.Default.RemoveCircleOutline, "Remove Heading", tint = GrayMatterColors.Error)
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
                enabled = name.isNotBlank() && headings.any { it.isNotBlank() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GrayMatterColors.CustomizedAccent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Template", fontWeight = FontWeight.Bold)
            }
        }
    }
}

package com.example.graymatter.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.domain.CustomTemplate
import com.example.graymatter.android.ui.theme.GrayMatterTheme

@Composable
fun TemplateSelector(
    templates: List<CustomTemplate>,
    selectedTemplate: CustomTemplate?,
    onTemplateSelect: (CustomTemplate?) -> Unit,
    onCreateTemplate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { showMenu = true }
        ) {
            Icon(
                Icons.Default.DashboardCustomize,
                contentDescription = "Template",
                tint = GrayMatterTheme.colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(GrayMatterTheme.colors.surface)
        ) {
            DropdownMenuItem(
                text = { Text("None (Plain)", color = GrayMatterColors.TypeOpinion) },
                onClick = {
                    onTemplateSelect(null)
                    showMenu = false
                }
            )
            
            templates.forEach { template ->
                DropdownMenuItem(
                    text = { Text(template.name, color = GrayMatterColors.TypeTemplate) },
                    onClick = {
                        onTemplateSelect(template)
                        showMenu = false
                    }
                )
            }
            
            Divider(color = GrayMatterTheme.colors.neutral800, modifier = Modifier.padding(vertical = 4.dp))
            
            DropdownMenuItem(
                text = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, null, tint = GrayMatterColors.TypeTemplate, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Template", color = GrayMatterColors.TypeTemplate)
                    }
                },
                onClick = {
                    onCreateTemplate()
                    showMenu = false
                }
            )
        }
    }
}

@Composable
fun DynamicEntryForm(
    template: CustomTemplate,
    fieldValues: Map<String, String>,
    onFieldValueChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        template.headings.forEach { heading ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = heading.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = GrayMatterColors.TypeTemplate
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GrayMatterTheme.colors.surfaceInput)
                        .border(
                            width = 1.dp,
                            color = GrayMatterTheme.colors.surfaceBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    BasicTextField(
                        value = fieldValues[heading] ?: "",
                        onValueChange = { onFieldValueChange(heading, it) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterTheme.colors.textPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        cursorBrush = SolidColor(GrayMatterColors.TypeTemplate),
                        decorationBox = { inner ->
                            if ((fieldValues[heading] ?: "").isEmpty()) {
                                Text("Enter $heading...", color = GrayMatterTheme.colors.neutral600)
                            }
                            inner()
                        }
                    )
                }
            }
        }
    }
}

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
        TextButton(
            onClick = { showMenu = true },
            colors = ButtonDefaults.textButtonColors(contentColor = GrayMatterColors.Neutral500)
        ) {
            Icon(Icons.Default.DashboardCustomize, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = selectedTemplate?.name ?: "Use Template",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Icon(Icons.Default.ArrowDropDown, null)
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(GrayMatterColors.SurfaceDark)
        ) {
            DropdownMenuItem(
                text = { Text("None (Plain)", color = Color.White) },
                onClick = {
                    onTemplateSelect(null)
                    showMenu = false
                }
            )
            
            templates.forEach { template ->
                DropdownMenuItem(
                    text = { Text(template.name, color = Color.White) },
                    onClick = {
                        onTemplateSelect(template)
                        showMenu = false
                    }
                )
            }
            
            Divider(color = GrayMatterColors.Neutral800, modifier = Modifier.padding(vertical = 4.dp))
            
            DropdownMenuItem(
                text = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, null, tint = GrayMatterColors.CustomizedAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Template", color = GrayMatterColors.CustomizedAccent)
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
                    color = GrayMatterColors.Neutral500
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GrayMatterColors.Neutral950)
                        .border(
                            width = 1.dp,
                            color = GrayMatterColors.Neutral800,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    BasicTextField(
                        value = fieldValues[heading] ?: "",
                        onValueChange = { onFieldValueChange(heading, it) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        cursorBrush = SolidColor(GrayMatterColors.CustomizedAccent),
                        decorationBox = { inner ->
                            if ((fieldValues[heading] ?: "").isEmpty()) {
                                Text("Enter $heading...", color = GrayMatterColors.Neutral700)
                            }
                            inner()
                        }
                    )
                }
            }
        }
    }
}
